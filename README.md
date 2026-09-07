# FaceID Campus Security System - Microservices Architecture

Complete face recognition system for campus security with Java Spring Boot backend and Python AI microservice.

## Architecture Overview

```
Mobile App
    ↓
Java Spring Boot Backend (port 8080)
    ↓
Python FastAPI Microservice (port 8001)
    ↓
OpenCV (YuNet) + ONNX Runtime (ArcFace)
```

## Prerequisites

- Docker & Docker Compose
- Java 17+
- Python 3.11+
- Maven 3.9+
- At least 4GB RAM available

## Quick Start (Docker Compose)

### 1. Download ONNX Models

Navigate to Python service and download models:

```powershell
cd faceid-python-service
.\download_models.bat
```

Or manually with PowerShell:

```powershell
mkdir -p models

# Download YuNet (face detection)
Invoke-WebRequest -Uri "https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx" -OutFile "models/face_detection_yunet_2023mar.onnx"

# Download ArcFace (face embedding) - Try InsightFace first
Invoke-WebRequest -Uri "https://github.com/deepinsight/insightface/releases/download/v0.7/arcface_w600k_r50.onnx" -OutFile "models/arcface.onnx"
```

### 2. Build and Start All Services

From the root directory (c:\Users\Owner\Documents\centralhold):

```powershell
# Build and start all services
docker-compose up -d --build

# View logs
docker-compose logs -f

# Check service status
docker-compose ps
```

### 3. Verify Services are Running

```powershell
# Python AI service health
Invoke-WebRequest -Uri "http://localhost:8001/health"

# Java backend health (requires API key)
$headers = @{"X-API-KEY" = "SecretCampusKey2026"}
Invoke-WebRequest -Uri "http://localhost:8080/health" -Headers $headers
```

## Local Development (Without Docker)

### 1. Start PostgreSQL

```powershell
docker run --name campus-postgres `
  -e POSTGRES_DB=faceid_db `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=postgres `
  -p 5432:5432 `
  -d postgres:15-alpine
```

### 2. Start Python Microservice

```powershell
cd faceid-python-service

# Install dependencies
pip install -r requirements.txt

# Download models (if not already done)
.\download_models.bat

# Run the service
python -m uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
```

The service will be available at `http://localhost:8001`

### 3. Start Java Backend

In a separate terminal:

```powershell
cd project

# Set environment variable for Python service
$env:PYTHON_SERVICE_URL = "http://localhost:8001"

# Run the application
.\mvnw.cmd spring-boot:run
```

The backend will be available at `http://localhost:8080`

## API Endpoints

### Python AI Microservice (port 8001)

#### 1. Detect and Match Faces

```powershell
$headers = @{"Content-Type" = "application/json"}
$form = @{
    file = Get-Item -Path "path/to/image.jpg"
    gallery_embeddings = '[{"student_id": 1, "name": "John Doe", "student_class": "Grade 11", "embedding": [...]}]'
}

$response = Invoke-WebRequest `
    -Uri "http://localhost:8001/detect-and-match" `
    -Method POST `
    -Form $form

$response.Content | ConvertFrom-Json | ConvertTo-Json
```

#### 2. Generate Embedding (Student Enrollment)

```powershell
$form = @{
    file = Get-Item -Path "path/to/student_photo.jpg"
}

$response = Invoke-WebRequest `
    -Uri "http://localhost:8001/generate-embedding" `
    -Method POST `
    -Form $form

$response.Content | ConvertFrom-Json
```

#### 3. Health Check

```powershell
Invoke-WebRequest -Uri "http://localhost:8001/health"
```

### Java Spring Boot Backend (port 8080)

All endpoints require API key header: `X-API-KEY: SecretCampusKey2026`

#### 1. Upload Incident Image

```powershell
$headers = @{"X-API-KEY" = "SecretCampusKey2026"}
$form = @{
    file = Get-Item -Path "path/to/incident.jpg"
}

$response = Invoke-WebRequest `
    -Uri "http://localhost:8080/api/v1/incidents/upload" `
    -Method POST `
    -Headers $headers `
    -Form $form

$response.Content | ConvertFrom-Json | ConvertTo-Json
```

**Response Example:**
```json
{
  "incidentId": 1,
  "timestamp": "2026-09-01T13:15:30.123456",
  "mediaPath": "/app/uploads/abc-def-incident.jpg",
  "matches": [
    {
      "faceBox": {
        "x": 120,
        "y": 80,
        "width": 100,
        "height": 100,
        "confidence": 0.94
      },
      "matchedStudentId": 42,
      "matchedStudentName": "John Doe",
      "matchedStudentClass": "Grade 11-B",
      "confidenceScore": 0.8241
    }
  ]
}
```

## Project Structure

```
centralhold/
├── docker-compose.yml                 # Main orchestration
├── project/                           # Java Spring Boot backend
│   ├── Dockerfile
│   ├── pom.xml
│   ├── mvnw & mvnw.cmd
│   ├── src/main/java/com/campus/security/faceid/
│   │   ├── FaceidCampusBackendApplication.java
│   │   ├── config/
│   │   │   ├── ApiKeyAuthFilter.java
│   │   │   ├── SecurityConfig.java
│   │   │   └── HttpClientConfig.java
│   │   ├── controller/
│   │   │   └── IncidentController.java
│   │   ├── dto/
│   │   │   ├── FaceBoxDTO.java
│   │   │   ├── MatchResultDTO.java
│   │   │   ├── IncidentResponseDTO.java
│   │   │   ├── GalleryEmbeddingDTO.java
│   │   │   ├── PythonDetectAndMatchResponse.java
│   │   │   ├── PythonGenerateEmbeddingResponse.java
│   │   │   └── PythonMatchResult.java
│   │   ├── model/
│   │   │   ├── Student.java
│   │   │   └── Incident.java
│   │   ├── repository/
│   │   │   ├── StudentRepository.java
│   │   │   └── IncidentRepository.java
│   │   └── service/
│   │       ├── IncidentService.java
│   │       ├── StudentService.java
│   │       └── ExternalAIService.java
│   ├── src/main/resources/
│   │   └── application.properties
│   ├── models/                        # Optional local models
│   └── uploads/                       # Incident images
│
└── faceid-python-service/             # Python AI microservice
    ├── Dockerfile
    ├── requirements.txt
    ├── download_models.bat
    ├── app/
    │   ├── main.py
    │   ├── schemas.py
    │   ├── services/
    │   │   ├── face_detector.py
    │   │   ├── face_embedder.py
    │   │   └── face_matcher.py
    │   └── __init__.py
    └── models/                        # YuNet and ArcFace models
        ├── face_detection_yunet_2023mar.onnx
        └── arcface.onnx
```

## Configuration

### Java Backend (application.properties)

```properties
# Python Microservice URL
app.ai.python-service-url=http://localhost:8001

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/faceid_db
spring.datasource.username=postgres
spring.datasource.password=postgres

# API Security
app.api.key=SecretCampusKey2026

# Upload Directory
app.upload.dir=uploads/
```

### Environment Variables (Docker)

```bash
DB_HOST=postgres          # Database host
DB_PORT=5432              # Database port
DB_NAME=faceid_db         # Database name
DB_USER=postgres          # Database user
DB_PASS=postgres          # Database password
PYTHON_SERVICE_URL=http://python-ai-service:8001  # Python service URL
```

## Testing Workflow

### 1. Enroll Students (Create Test Data)

```powershell
# Create a test student with embedding
$headers = @{"X-API-KEY" = "SecretCampusKey2026"}

# This would be done through a separate enrollment endpoint
# For now, manually insert via database:
# INSERT INTO students (full_name, student_class, embedding_vector, enrollment_date)
# VALUES ('John Doe', 'Grade 11-B', '0.123,0.456,...', now());
```

### 2. Test Python Service Directly

```powershell
# Generate embedding from a test image
$form = @{
    file = Get-Item -Path "test_image.jpg"
}

$response = Invoke-WebRequest `
    -Uri "http://localhost:8001/generate-embedding" `
    -Method POST `
    -Form $form

$embedding = ($response.Content | ConvertFrom-Json).embedding
```

### 3. Test Full Integration

```powershell
# Upload incident image (will detect and match against enrolled students)
$headers = @{"X-API-KEY" = "SecretCampusKey2026"}
$form = @{
    file = Get-Item -Path "incident_image.jpg"
}

$response = Invoke-WebRequest `
    -Uri "http://localhost:8080/api/v1/incidents/upload" `
    -Method POST `
    -Headers $headers `
    -Form $form

$response.Content | ConvertFrom-Json | ConvertTo-Json
```

## Stopping Services

```powershell
# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v

# Stop PostgreSQL only
docker stop campus-postgres
```

## Troubleshooting

### Python Service Won't Start

```powershell
# Check Python service logs
docker-compose logs python-ai-service

# Verify models are in place
ls faceid-python-service/models/

# Try downloading models manually
cd faceid-python-service
python -c "import cv2; print(cv2.__version__)"
python -c "import onnxruntime; print(onnxruntime.__version__)"
```

### Java Backend Can't Connect to Python Service

```powershell
# From Java container, test connectivity
docker exec faceid-java-backend curl http://python-ai-service:8001/health

# Check Docker network
docker network ls
docker network inspect centralhold_faceid-network
```

### Database Connection Issues

```powershell
# Check if PostgreSQL is running
docker exec campus-postgres pg_isready -U postgres

# View database logs
docker logs campus-postgres

# Connect to database
docker exec -it campus-postgres psql -U postgres -d faceid_db
```

## Performance Tuning

- Increase Docker memory: `docker run -m 4g ...`
- Use GPU for ONNX Runtime (requires CUDA): Set provider to `['CUDAExecutionProvider']`
- Cache gallery embeddings in memory (Java backend)
- Use connection pooling (Spring already does this with HikariCP)

## Security Considerations

- Change default API key in production: `app.api.key=YourSecureKey`
- Use HTTPS/TLS for all endpoints in production
- Implement rate limiting on incident upload endpoint
- Store enrollment photos securely
- Regular database backups

## License

Proprietary - FaceID Campus Security System
