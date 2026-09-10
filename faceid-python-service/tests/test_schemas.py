"""Tests for Pydantic request/response schemas."""

import pytest
from pydantic import ValidationError

from app.schemas import (
    DetectAndMatchResponse,
    FaceBox,
    GenerateEmbeddingResponse,
    MatchResult,
)


def test_face_box_requires_all_fields():
    box = FaceBox(x=10, y=20, width=100, height=120, confidence=0.95)
    assert box.x == 10
    assert box.confidence == pytest.approx(0.95)

    with pytest.raises(ValidationError):
        FaceBox(x=10, y=20, width=100, height=120)  # missing confidence


def test_match_result_allows_unmatched_student():
    result = MatchResult(
        face_box=FaceBox(x=0, y=0, width=10, height=10, confidence=0.9),
        matched_student_name="Unknown / Unregistered",
        confidence_score=0.12,
    )
    assert result.matched_student_id is None
    assert result.matched_student_class is None


def test_detect_and_match_response_serializes_nested_matches():
    response = DetectAndMatchResponse(
        success=True,
        detected_faces_count=1,
        matches=[
            MatchResult(
                face_box=FaceBox(x=1, y=2, width=3, height=4, confidence=0.5),
                matched_student_id=7,
                matched_student_name="Jordan Lee",
                matched_student_class="Grade 11",
                confidence_score=0.81,
            )
        ],
        message="ok",
    )
    payload = response.model_dump()
    assert payload["matches"][0]["matched_student_id"] == 7
    assert payload["matches"][0]["face_box"]["confidence"] == pytest.approx(0.5)


def test_generate_embedding_response_requires_embedding_list():
    response = GenerateEmbeddingResponse(success=True, embedding=[0.1, 0.2, 0.3], message="ok")
    assert len(response.embedding) == 3
