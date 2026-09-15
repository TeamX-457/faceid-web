"""
Face Enhancement Service - lightweight deblur/cleanup for blurry or low-light face crops.

Deliberately pure-OpenCV (no PyTorch/GFPGAN-class model): those need hundreds of MB of
weights plus a much heavier runtime, which doesn't fit a free-tier, CPU-only Render instance
well. This pipeline (denoise -> unsharp mask sharpen -> CLAHE contrast + optional upscale)
is fast (well under a second for a typical face crop) and meaningfully improves legibility for
a human reviewer without the deploy risk of a large model.
"""

import logging

import cv2
import numpy as np

logger = logging.getLogger(__name__)

# Crops smaller than this (on their shorter side) get upscaled before enhancing - a small,
# heavily-compressed face crop benefits more from resolution than from sharpening alone.
_UPSCALE_THRESHOLD_PX = 220
_UPSCALE_FACTOR = 2.0


class FaceEnhancementService:
    """Cleans up a blurry/dark face crop so a reviewer can see it more clearly."""

    def enhance(self, image_data: bytes) -> bytes:
        nparr = np.frombuffer(image_data, np.uint8)
        image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        if image is None:
            raise ValueError("Failed to decode image. Ensure it's a valid image file.")

        height, width = image.shape[:2]
        if min(height, width) < _UPSCALE_THRESHOLD_PX:
            image = cv2.resize(
                image,
                (int(width * _UPSCALE_FACTOR), int(height * _UPSCALE_FACTOR)),
                interpolation=cv2.INTER_LANCZOS4,
            )

        denoised = cv2.fastNlMeansDenoisingColored(image, None, h=7, hColor=7, templateWindowSize=7, searchWindowSize=21)

        # Unsharp mask: subtracting a blurred copy from the original boosts edge contrast,
        # which reads as "less blurry" without amplifying noise the way a naive sharpen kernel would.
        gaussian = cv2.GaussianBlur(denoised, (0, 0), sigmaX=3)
        sharpened = cv2.addWeighted(denoised, 1.5, gaussian, -0.5, 0)

        # CLAHE on the lightness channel only, so poor lighting (common in CCTV-style footage)
        # gets corrected without shifting color balance.
        lab = cv2.cvtColor(sharpened, cv2.COLOR_BGR2LAB)
        l_channel, a_channel, b_channel = cv2.split(lab)
        clahe = cv2.createCLAHE(clipLimit=2.5, tileGridSize=(8, 8))
        l_channel = clahe.apply(l_channel)
        enhanced = cv2.cvtColor(cv2.merge((l_channel, a_channel, b_channel)), cv2.COLOR_LAB2BGR)

        success, encoded = cv2.imencode(".jpg", enhanced, [cv2.IMWRITE_JPEG_QUALITY, 92])
        if not success:
            raise RuntimeError("Failed to encode enhanced image")
        return encoded.tobytes()
