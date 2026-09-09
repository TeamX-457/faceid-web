"""Tests for FaceMatchingService - pure logic, no model files required."""

import json

import numpy as np
import pytest

from app.services.face_matcher import FaceMatchingService


def _gallery_json(entries):
    return json.dumps(entries)


def test_cosine_similarity_identical_vectors_is_one():
    vector = np.array([1.0, 2.0, 3.0], dtype=np.float32)
    similarity = FaceMatchingService._cosine_similarity(vector, vector)
    assert similarity == pytest.approx(1.0, abs=1e-5)


def test_cosine_similarity_orthogonal_vectors_is_zero():
    a = np.array([1.0, 0.0], dtype=np.float32)
    b = np.array([0.0, 1.0], dtype=np.float32)
    assert FaceMatchingService._cosine_similarity(a, b) == pytest.approx(0.0, abs=1e-6)


def test_cosine_similarity_mismatched_lengths_returns_zero():
    a = np.array([1.0, 2.0, 3.0], dtype=np.float32)
    b = np.array([1.0, 2.0], dtype=np.float32)
    assert FaceMatchingService._cosine_similarity(a, b) == 0.0


def test_cosine_similarity_zero_vector_returns_zero():
    a = np.zeros(4, dtype=np.float32)
    b = np.array([1.0, 2.0, 3.0, 4.0], dtype=np.float32)
    assert FaceMatchingService._cosine_similarity(a, b) == 0.0


def test_find_best_match_empty_gallery_is_unknown():
    target = np.array([1.0, 0.0, 0.0], dtype=np.float32)
    result = FaceMatchingService.find_best_match(target, _gallery_json([]))
    assert result["name"] == "Unknown / Unregistered"
    assert result["score"] == 0.0


def test_find_best_match_malformed_json_is_unknown():
    target = np.array([1.0, 0.0, 0.0], dtype=np.float32)
    result = FaceMatchingService.find_best_match(target, "not valid json")
    assert result["name"] == "Unknown / Unregistered"


def test_find_best_match_above_threshold_returns_student():
    target = np.array([1.0, 0.0, 0.0], dtype=np.float32)
    gallery = _gallery_json([
        {"student_id": 1, "name": "Jordan Lee", "student_class": "Grade 11", "embedding": [1.0, 0.0, 0.0]},
    ])
    result = FaceMatchingService.find_best_match(target, gallery, similarity_threshold=0.40)
    assert result["name"] == "Jordan Lee"
    assert result["student_id"] == 1
    assert result["score"] == pytest.approx(1.0, abs=1e-5)


def test_find_best_match_below_threshold_is_unknown_but_reports_score():
    target = np.array([1.0, 0.0], dtype=np.float32)
    gallery = _gallery_json([
        {"student_id": 1, "name": "Jordan Lee", "student_class": "Grade 11", "embedding": [0.0, 1.0]},
    ])
    result = FaceMatchingService.find_best_match(target, gallery, similarity_threshold=0.40)
    assert result["name"] == "Unknown / Unregistered"
    assert result["score"] == pytest.approx(0.0, abs=1e-6)


def test_find_best_match_picks_closest_of_several_candidates():
    target = np.array([1.0, 0.0, 0.0], dtype=np.float32)
    gallery = _gallery_json([
        {"student_id": 1, "name": "Far Match", "student_class": "A", "embedding": [0.1, 0.99, 0.0]},
        {"student_id": 2, "name": "Close Match", "student_class": "B", "embedding": [0.98, 0.2, 0.0]},
    ])
    result = FaceMatchingService.find_best_match(target, gallery, similarity_threshold=0.40)
    assert result["name"] == "Close Match"
    assert result["student_id"] == 2


def test_find_best_match_skips_entries_without_embedding():
    target = np.array([1.0, 0.0], dtype=np.float32)
    gallery = _gallery_json([
        {"student_id": 1, "name": "No Embedding", "student_class": "A", "embedding": []},
        {"student_id": 2, "name": "Has Embedding", "student_class": "B", "embedding": [1.0, 0.0]},
    ])
    result = FaceMatchingService.find_best_match(target, gallery, similarity_threshold=0.40)
    assert result["name"] == "Has Embedding"
