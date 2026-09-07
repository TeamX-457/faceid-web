"""
FaceID Python Microservice
Handles face detection, embedding generation, and matching
for the FaceID Campus security system
"""

from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import JSONResponse
import logging
import os
from app.schemas import (
    DetectAndMatchRequest,
    DetectAndMatchResponse,
    GenerateEmbeddingResponse,
    MatchResult,
    FaceBox
)
from app.services.face_detector import FaceDetectionService
from app.services.face_embedder import FaceEmbeddingService
from app.services.face_matcher import FaceMatchingService

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize FastAPI app
app = FastAPI(
    title="FaceID AI Microservice",
    description="Face detection, embedding, and matching service for FaceID Campus",
    version="1.0.0"
)

# Initialize services
detector = FaceDetectionService()
embedder = FaceEmbeddingService()
matcher = FaceMatchingService()

logger.info("FaceID AI Microservice initialized successfully")


@app.post("/detect-and-match", response_model=DetectAndMatchResponse)
async def detect_and_match(
    file: UploadFile = File(...),
    gallery_embeddings: str = ""  # JSON string of gallery embeddings
):
    """
    Detect faces in uploaded image and match against gallery embeddings.
    
    Args:
        file: Image file (jpg, png, etc.)
        gallery_embeddings: JSON string containing list of {student_id, embedding}
        
    Returns:
        List of match results for each detected face
    """
    try:
        # Read image file
        image_data = await file.read()
        
        # Detect faces
        faces, image_array = detector.detect_faces(image_data)
        logger.info(f"Detected {len(faces)} faces in uploaded image")
        
        # Generate embeddings for each detected face and match
        match_results = []
        
        for face_idx, (x, y, w, h, conf) in enumerate(faces):
            try:
                # Generate embedding for this face
                embedding = embedder.generate_embedding(image_array, x, y, w, h)
                
                # Match against gallery embeddings
                best_match = matcher.find_best_match(
                    embedding,
                    gallery_embeddings,
                    similarity_threshold=0.40
                )
                
                match_result = MatchResult(
                    face_box=FaceBox(x=x, y=y, width=w, height=h, confidence=float(conf)),
                    matched_student_id=best_match.get("student_id"),
                    matched_student_name=best_match.get("name", "Unknown / Unregistered"),
                    matched_student_class=best_match.get("student_class"),
                    confidence_score=float(best_match.get("score", 0.0))
                )
                match_results.append(match_result)
                
            except Exception as e:
                logger.error(f"Error processing face {face_idx}: {str(e)}")
                continue
        
        return DetectAndMatchResponse(
            success=True,
            detected_faces_count=len(faces),
            matches=match_results,
            message="Detection and matching completed successfully"
        )
        
    except Exception as e:
        logger.error(f"Error in detect_and_match: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/generate-embedding", response_model=GenerateEmbeddingResponse)
async def generate_embedding(file: UploadFile = File(...)):
    """
    Generate face embedding from a clear student enrollment photo.
    
    Args:
        file: Clear photo of a single face
        
    Returns:
        Face embedding vector as a list of floats
    """
    try:
        # Read image file
        image_data = await file.read()
        
        # Detect faces
        faces, image_array = detector.detect_faces(image_data)
        
        if len(faces) == 0:
            raise HTTPException(
                status_code=400,
                detail="No face detected in the image. Please provide a clear photo with a single face."
            )
        
        if len(faces) > 1:
            logger.warning(f"Multiple faces detected ({len(faces)}). Using the largest/most confident face.")
        
        # Use the first (most confident) face
        x, y, w, h, conf = faces[0]
        
        # Generate embedding
        embedding = embedder.generate_embedding(image_array, x, y, w, h)
        
        return GenerateEmbeddingResponse(
            success=True,
            embedding=embedding.tolist(),
            message="Embedding generated successfully"
        )
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error in generate_embedding: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/health")
async def health_check():
    """Health check endpoint for orchestration and monitoring."""
    return {
        "status": "healthy",
        "service": "FaceID AI Microservice",
        "version": "1.0.0"
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8001,
        reload=False
    )
