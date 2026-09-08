"""
Face Embedding Service using MobileFaceNet/ArcFace model via ONNX Runtime
"""

import logging
import os

import cv2
import numpy as np
import onnxruntime as ort

logger = logging.getLogger(__name__)


class FaceEmbeddingService:
    """Generates face embeddings using ONNX-based MobileFaceNet/ArcFace model"""
    
    def __init__(self, model_path: str = "models/arcface.onnx"):
        """
        Initialize the embedding generator.
        
        Args:
            model_path: Path to the ArcFace/MobileFaceNet ONNX model file
        """
        self.model_path = model_path
        self.session = None
        self.embedding_size = 512
        
        if os.path.exists(model_path):
            try:
                # Create ONNX Runtime session
                self.session = ort.InferenceSession(
                    model_path,
                    providers=['CPUExecutionProvider']
                )
                logger.info(f"ArcFace embedding model loaded successfully from {model_path}")
                
                # Get model input/output info
                self.input_name = self.session.get_inputs()[0].name
                self.output_name = self.session.get_outputs()[0].name
                
            except Exception as e:
                logger.error(f"Failed to load ArcFace model: {e}")
                self.session = None
        else:
            logger.warning(f"ArcFace model not found at {model_path}. Download it first.")
    
    def generate_embedding(self, image: np.ndarray, x: int, y: int, w: int, h: int) -> np.ndarray:
        """
        Generate a face embedding from a detected face region.
        
        Args:
            image: Image array in BGR format (from OpenCV)
            x, y, w, h: Bounding box coordinates and size
            
        Returns:
            Normalized embedding vector (numpy array of floats)
        """
        if self.session is None:
            raise RuntimeError("Embedding model not initialized. ArcFace model is required.")
        
        # Crop the face region with safety bounds
        x1 = max(0, x)
        y1 = max(0, y)
        x2 = min(image.shape[1], x + w)
        y2 = min(image.shape[0], y + h)
        
        face_region = image[y1:y2, x1:x2]
        
        # Resize to model input size (typically 112x112 for MobileFaceNet)
        face_resized = cv2.resize(face_region, (112, 112))
        
        # Convert BGR to RGB
        face_rgb = cv2.cvtColor(face_resized, cv2.COLOR_BGR2RGB)
        
        # Normalize pixel values to [-1, 1]
        face_normalized = (face_rgb.astype(np.float32) - 127.5) / 128.0
        
        # Prepare input tensor (NCHW format: 1, 3, 112, 112)
        input_tensor = np.expand_dims(np.transpose(face_normalized, (2, 0, 1)), 0).astype(np.float32)
        
        # Run inference
        try:
            output = self.session.run(
                [self.output_name],
                {self.input_name: input_tensor}
            )
            
            # Extract embedding and normalize
            embedding = output[0].flatten().astype(np.float32)
            embedding = self._l2_normalize(embedding)
            
            return embedding
            
        except Exception as e:
            logger.error(f"Error generating embedding: {e}")
            raise
    
    @staticmethod
    def _l2_normalize(vector: np.ndarray) -> np.ndarray:
        """L2 normalize a vector"""
        norm = np.linalg.norm(vector)
        if norm == 0:
            return vector
        return vector / norm
