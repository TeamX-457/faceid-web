"""
Face Matching Service - matches detected faces against gallery embeddings
"""

import numpy as np
import json
import logging
from typing import List, Dict, Any

logger = logging.getLogger(__name__)


class FaceMatchingService:
    """Matches faces using cosine similarity"""
    
    @staticmethod
    def find_best_match(
        target_embedding: np.ndarray,
        gallery_embeddings_json: str,
        similarity_threshold: float = 0.40
    ) -> Dict[str, Any]:
        """
        Find the best matching student from gallery embeddings.
        
        Args:
            target_embedding: Embedding vector from detected face
            gallery_embeddings_json: JSON string with gallery data
            similarity_threshold: Minimum similarity score to consider a match
            
        Returns:
            Dict with matched student info or "Unknown" if no good match
        """
        
        # Parse gallery embeddings
        try:
            gallery = json.loads(gallery_embeddings_json) if gallery_embeddings_json else []
        except json.JSONDecodeError:
            logger.warning("Failed to parse gallery embeddings JSON")
            gallery = []
        
        if not gallery:
            return {
                "name": "Unknown / Unregistered",
                "score": 0.0
            }
        
        best_match = None
        best_score = -1.0
        
        for student_data in gallery:
            try:
                student_id = student_data.get("student_id")
                student_name = student_data.get("name", "Unknown")
                student_class = student_data.get("student_class")
                embedding_list = student_data.get("embedding", [])
                
                if not embedding_list:
                    continue
                
                # Convert to numpy array
                gallery_embedding = np.array(embedding_list, dtype=np.float32)
                
                # Calculate cosine similarity
                similarity = FaceMatchingService._cosine_similarity(
                    target_embedding,
                    gallery_embedding
                )
                
                if similarity > best_score:
                    best_score = similarity
                    best_match = {
                        "student_id": student_id,
                        "name": student_name,
                        "student_class": student_class,
                        "score": float(similarity)
                    }
                    
            except Exception as e:
                logger.warning(f"Error processing gallery entry: {e}")
                continue
        
        # Check if best match meets threshold
        if best_match and best_score >= similarity_threshold:
            return best_match
        else:
            return {
                "name": "Unknown / Unregistered",
                "score": float(best_score) if best_score > 0 else 0.0
            }
    
    @staticmethod
    def _cosine_similarity(vector_a: np.ndarray, vector_b: np.ndarray) -> float:
        """
        Calculate cosine similarity between two vectors.
        
        Args:
            vector_a: First embedding vector
            vector_b: Second embedding vector
            
        Returns:
            Similarity score between -1 and 1 (typically 0 to 1 for normalized embeddings)
        """
        if len(vector_a) != len(vector_b):
            return 0.0
        
        dot_product = np.dot(vector_a, vector_b)
        norm_a = np.linalg.norm(vector_a)
        norm_b = np.linalg.norm(vector_b)
        
        if norm_a == 0.0 or norm_b == 0.0:
            return 0.0
        
        similarity = dot_product / (norm_a * norm_b)
        return float(np.clip(similarity, 0.0, 1.0))
