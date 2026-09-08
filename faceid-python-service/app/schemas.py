"""
Pydantic schemas for request/response validation
"""


from pydantic import BaseModel


class FaceBox(BaseModel):
    """Bounding box coordinates of a detected face"""
    x: int
    y: int
    width: int
    height: int
    confidence: float


class MatchResult(BaseModel):
    """Result of matching a detected face against gallery embeddings"""
    face_box: FaceBox
    matched_student_id: int | None = None
    matched_student_name: str
    matched_student_class: str | None = None
    confidence_score: float


class DetectAndMatchRequest(BaseModel):
    """Request to detect and match faces"""
    # Note: Image file is passed as multipart, not in JSON
    gallery_embeddings: list[dict]  # List of {student_id, embedding, name, student_class}


class DetectAndMatchResponse(BaseModel):
    """Response from detect and match endpoint"""
    success: bool
    detected_faces_count: int
    matches: list[MatchResult]
    message: str


class GenerateEmbeddingResponse(BaseModel):
    """Response from generate embedding endpoint"""
    success: bool
    embedding: list[float]
    message: str
