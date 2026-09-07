@echo off
REM Download YuNet face detection model
echo Downloading YuNet face detection model...
mkdir models 2>nul
cd models

REM YuNet model
powershell -Command "(New-Object Net.WebClient).DownloadFile('https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx', 'face_detection_yunet_2023mar.onnx')"

echo YuNet model downloaded.

REM ArcFace/MobileFaceNet embedding model
REM Note: Using InsightFace model which is compatible with ONNX Runtime
echo Downloading ArcFace embedding model...
powershell -Command "(New-Object Net.WebClient).DownloadFile('https://github.com/deepinsight/insightface/releases/download/v0.7/arcface_w600k_r50.onnx', 'arcface.onnx')" 2>nul

if not exist arcface.onnx (
    echo Failed to download from primary source. Trying alternative...
    powershell -Command "(New-Object Net.WebClient).DownloadFile('https://media.githubusercontent.com/media/onnx/models/master/validated/vision/body_analysis/arcface/model/arcface-resnet100-msfdrop75.onnx', 'arcface.onnx')" 2>nul
)

if exist arcface.onnx (
    echo ArcFace model downloaded successfully.
) else (
    echo WARNING: ArcFace model download failed. Please download manually or use a pre-existing model.
)

cd ..
echo.
echo Models downloaded to faceid-python-service/models/
echo.
