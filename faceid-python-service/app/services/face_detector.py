"""
Face Detection Service using YuNet model via OpenCV DNN
"""

import cv2
import numpy as np
from typing import List, Tuple
import logging
import os

logger = logging.getLogger(__name__)


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
    
    def detect_faces(self, image_data: bytes) -> Tuple[List[Tuple], np.ndarray]:
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
        
        # Set input size dynamically based on image
        self.detector.setInputSize(image.shape[:2][::-1])
        
        # Detect faces
        _, detections = self.detector.detect(image)
        
        faces = []
        if detections is not None:
            for detection in detections:
                x, y, w, h, conf = detection[:5]
                faces.append((int(x), int(y), int(w), int(h), float(conf)))
        
        logger.info(f"Detected {len(faces)} faces")
        return faces, image
