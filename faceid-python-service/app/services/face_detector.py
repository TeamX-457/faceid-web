"""
Face Detection Service using YuNet model via OpenCV DNN
"""

import logging
import os

import cv2
import numpy as np

logger = logging.getLogger(__name__)

# YuNet was being fed full camera-resolution photos (often 3000-4000px on the long side)
# uncapped - on a free-tier, memory-constrained Render instance that's enough to crash the
# whole process (seen as a bare 502 from Render's proxy, not even a normal error response).
# Capping the long side before detection keeps memory/CPU bounded; detected boxes are scaled
# back up to the original image's coordinate space before being returned, so nothing downstream
# (embedding cropping, face_box sent to clients) needs to know detection ran on a smaller copy.
_MAX_DETECTION_DIMENSION = 1280


class FaceDetectionService:
    """Detects faces in images using OpenCV's YuNet model"""

    def __init__(self, model_path: str = "models/face_detection_yunet_2023mar.onnx"):
        """
        Initialize the face detector.
        
        Args:
            model_path: Path to the YuNet ONNX model file
        """
        self.model_path = model_path
        self.detector = None
        
        if os.path.exists(model_path):
            try:
                self.detector = cv2.FaceDetectorYN.create(
                    model_path,
                    "",
                    (320, 320),
                    score_threshold=0.6,
                    nms_threshold=0.3,
                    top_k=5000,
                    backend_id=cv2.dnn.DNN_BACKEND_DEFAULT,
                    target_id=cv2.dnn.DNN_TARGET_CPU
                )
                logger.info(f"YuNet face detector loaded successfully from {model_path}")
            except Exception as e:
                logger.error(f"Failed to load YuNet model: {e}")
                self.detector = None
        else:
            logger.warning(f"YuNet model not found at {model_path}. Download it first.")
    
    def detect_faces(self, image_data: bytes) -> tuple[list[tuple], np.ndarray]:
        """
        Detect faces in an image.
        
        Args:
            image_data: Raw image bytes
            
        Returns:
            Tuple of:
            - List of face detections: [(x, y, width, height, confidence), ...]
            - Image array (numpy array in BGR format)
        """
        if self.detector is None:
            raise RuntimeError("Face detector not initialized. YuNet model is required.")
        
        # Decode image from bytes
        nparr = np.frombuffer(image_data, np.uint8)
        image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        
        if image is None:
            raise ValueError("Failed to decode image. Ensure it's a valid image file.")

        # Run detection on a capped-size copy to keep memory/CPU bounded on a real
        # camera-resolution photo; scale factor maps detected boxes back to the original.
        height, width = image.shape[:2]
        longest_side = max(height, width)
        scale = _MAX_DETECTION_DIMENSION / longest_side if longest_side > _MAX_DETECTION_DIMENSION else 1.0
        detection_image = (
            cv2.resize(image, (int(width * scale), int(height * scale)), interpolation=cv2.INTER_AREA)
            if scale != 1.0
            else image
        )

        self.detector.setInputSize(detection_image.shape[:2][::-1])

        # Detect faces
        _, detections = self.detector.detect(detection_image)

        faces = []
        if detections is not None:
            for detection in detections:
                x, y, w, h, conf = detection[:5]
                faces.append((
                    int(x / scale), int(y / scale), int(w / scale), int(h / scale), float(conf),
                ))

        logger.info(f"Detected {len(faces)} faces (detection ran at scale={scale:.3f})")
        return faces, image
