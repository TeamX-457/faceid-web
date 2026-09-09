"""
API-level tests for the FastAPI app. Run without ONNX model files present (as in CI) -
FaceDetectionService/FaceEmbeddingService degrade to a disabled state rather than crashing
on import, so these endpoints should return a clean error instead of raising.
"""

import io

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_check():
    response = client.get("/health")
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "healthy"
    assert body["service"] == "FaceID AI Microservice"


def _fake_image_bytes():
    # Not a real image - just needs to reach the detector, which fails fast when the
    # YuNet model isn't loaded (as in this test environment), before decoding matters.
    return io.BytesIO(b"not-a-real-image")


def test_generate_embedding_without_model_returns_clean_error():
    files = {"file": ("test.jpg", _fake_image_bytes(), "image/jpeg")}
    response = client.post("/generate-embedding", files=files)
    assert response.status_code == 500
    assert "detail" in response.json()


def test_detect_and_match_without_model_returns_clean_error():
    files = {"file": ("test.jpg", _fake_image_bytes(), "image/jpeg")}
    response = client.post("/detect-and-match", files=files, data={"gallery_embeddings": "[]"})
    assert response.status_code == 500
    assert "detail" in response.json()
