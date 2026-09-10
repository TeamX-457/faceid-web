# syntax=docker/dockerfile:1

# Use official Python runtime as base image
FROM python:3.11-slim

# Set working directory in container
WORKDIR /app

# Install system dependencies for OpenCV, plus curl to fetch the ONNX models below
RUN apt-get update && apt-get install -y \
    libsm6 \
    libxext6 \
    libxrender-dev \
    libgl1 \
    libglib2.0-0 \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first (for better layer caching)
COPY requirements.txt .

# Install Python dependencies
RUN --mount=type=cache,target=/root/.cache/pip pip install -r requirements.txt

# Copy application code
COPY . .

# ONNX model files are gitignored (100MB+, not something to keep in version control), so they
# don't exist in the build context here - fetch the same known-good sources used locally.
RUN mkdir -p models \
    && curl -fL -o models/face_detection_yunet_2023mar.onnx \
       https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx \
    && curl -fL -o models/arcface.onnx \
       https://huggingface.co/public-data/insightface/resolve/main/models/buffalo_l/w600k_r50.onnx

# Expose port (informational - actual bind port comes from $PORT at runtime, e.g. on Render)
EXPOSE 8001

# Health check (shell form so $PORT expands; defaults to 8001 for local/Docker runs)
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD python -c "import os, urllib.request; urllib.request.urlopen('http://localhost:' + os.environ.get('PORT', '8001') + '/health')"

# Run the application (shell form so $PORT expands; defaults to 8001 for local/Docker runs)
CMD python -m uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8001}
