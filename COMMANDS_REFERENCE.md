# Quick Commands Reference

## Essential Commands

### 1. Setup & Initialization

```powershell
# Navigate to project root
cd c:\Users\Owner\Documents\centralhold

# Download ONNX models
cd faceid-python-service
.\download_models.bat
cd ..

# Build all Docker images
docker-compose build

# Start all services
docker-compose up -d --build
```

### 2. Status & Logs

```powershell
# Check all services status
docker-compose ps

# View logs from all services
docker-compose logs -f

# View specific service logs
docker-compose logs -f python-ai-service
docker-compose logs -f java-backend
docker-compose logs -f postgres

# Follow last 50 lines
docker-compose logs --tail=50 -f
```

### 3. Service Health Checks

```powershell
# Python AI Service (no auth needed)
curl http://localhost:8001/health

# Java Backend (requires API key)
$headers = @{"X-API-KEY" = "SecretCampusKey2026"}
Invoke-WebRequest -Uri "http://localhost:8080/health" -Headers $headers

# Database connectivity
docker exec campus-postgres psql -U postgres -d faceid_db -c "SELECT 1"
```

### 4. Testing APIs

```powershell
# Generate face embedding (student enrollment)
$form = @{file = Get-Item "path/to/student_photo.jpg"}
Invoke-WebRequest -Uri "http://localhost:8001/generate-embedding" `
  -Method POST -Form $form

# Detect and match faces
$form = @{file = Get-Item "path/to/incident.jpg"}
$galleryJson = '[{"student_id": 1, "name": "John", "student_class": "Grade 11", "embedding": [...]}]'
$form.Add("gallery_embeddings", $galleryJson)

Invoke-WebRequest -Uri "http://localhost:8001/detect-and-match" `
  -Method POST -Form $form

# Upload incident to Java backend
$headers = @{"X-API-KEY" = "SecretCampusKey2026"}
$form = @{file = Get-Item "path/to/incident.jpg"}

Invoke-WebRequest -Uri "http://localhost:8080/api/v1/incidents/upload" `
  -Method POST -Headers $headers -Form $form | ConvertFrom-Json | ConvertTo-Json
```

### 5. Database Management

```powershell
# Connect to PostgreSQL
docker exec -it campus-postgres psql -U postgres -d faceid_db

# Common queries (run inside psql)
# List students:
SELECT id, full_name, student_class FROM students;

# List incidents:
SELECT id, timestamp, media_path FROM incidents ORDER BY timestamp DESC LIMIT 10;

# View incident details:
SELECT * FROM incidents WHERE id = 1 \gx

# Count total records:
SELECT COUNT(*) as total_students FROM students;
SELECT COUNT(*) as total_incidents FROM incidents;

# Delete all test data:
DELETE FROM incidents;
DELETE FROM students;
```

### 6. Container Management

```powershell
# Stop all services (keeps data)
docker-compose stop

# Stop and remove containers (keeps volumes)
docker-compose down

# Stop and remove everything including data
docker-compose down -v

# Restart a specific service
docker-compose restart java-backend

# Rebuild and restart Python service only
docker-compose up -d --build python-ai-service

# Shell access to containers
docker exec -it faceid-python-service bash
docker exec -it faceid-java-backend bash
docker exec -it campus-postgres bash
```

### 7. Development - Local Testing

```powershell
# Start PostgreSQL only
docker run --name campus-postgres `
  -e POSTGRES_DB=faceid_db -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres `
  -p 5432:5432 -d postgres:15-alpine

# Start Python service locally (no Docker)
cd faceid-python-service
pip install -r requirements.txt
python -m uvicorn app.main:app --reload --port 8001

# In another terminal, start Java backend locally
cd project
$env:PYTHON_SERVICE_URL = "http://localhost:8001"
.\mvnw.cmd spring-boot:run

# Test from third terminal
curl http://localhost:8001/health
$headers = @{"X-API-KEY" = "SecretCampusKey2026"}
Invoke-WebRequest -Uri "http://localhost:8080/health" -Headers $headers
```

### 8. Troubleshooting

```powershell
# Check Docker network
docker network ls
docker network inspect centralhold_faceid-network

# Check if ports are in use
netstat -ano | findstr :8080
netstat -ano | findstr :8001
netstat -ano | findstr :5432

# Force remove container
docker rm -f faceid-python-service

# Rebuild specific image
docker build -t faceid-python-service ./faceid-python-service

# Check container resource usage
docker stats

# View container details
docker inspect faceid-java-backend

# Test service connectivity from Java container
docker exec faceid-java-backend curl http://python-ai-service:8001/health

# Check environment variables in container
docker exec faceid-java-backend env | sort

# View Python service environment
docker exec faceid-python-service env | sort
```

### 9. Build Commands

```powershell
# Build Java backend JAR
cd project
.\mvnw.cmd clean package -DskipTests

# Build Docker images only (no start)
docker-compose build

# Build with no cache
docker-compose build --no-cache

# Build specific service
docker-compose build java-backend
```

### 10. Performance & Monitoring

```powershell
# View real-time container stats
docker stats --no-stream

# View memory usage
docker ps --size

# Check disk usage
docker system df

# Prune unused resources
docker system prune -a

# View image layers
docker history faceid-java-backend
```

## Common Scenarios

### Scenario 1: Fresh Start

```powershell
cd c:\Users\Owner\Documents\centralhold

# Clean everything
docker-compose down -v
docker system prune -a -f

# Download models
cd faceid-python-service
.\download_models.bat
cd ..

# Start fresh
docker-compose up -d --build
docker-compose ps
```

### Scenario 2: Update Python Service Code

```powershell
# Make changes to Python code
# Then rebuild and restart

docker-compose up -d --build python-ai-service
docker-compose logs -f python-ai-service
```

### Scenario 3: Update Java Backend Code

```powershell
# Update Java code
# Rebuild and restart

docker-compose up -d --build java-backend
docker-compose logs -f java-backend
```

### Scenario 4: Reset Database

```powershell
# Remove database volume
docker-compose down -v

# Start services (fresh database)
docker-compose up -d
```

### Scenario 5: Database Backup

```powershell
# Backup database to SQL file
docker exec campus-postgres pg_dump -U postgres faceid_db > backup.sql

# Restore from backup
docker exec -i campus-postgres psql -U postgres faceid_db < backup.sql
```

## Configuration Quick Reference

### Python Service (.env or docker-compose)
```
LOG_LEVEL=INFO
PYTHONUNBUFFERED=1
```

### Java Backend (application.properties)
```
app.api.key=SecretCampusKey2026
app.ai.python-service-url=http://localhost:8001
app.upload.dir=uploads/
```

### Database Connection
```
DB_HOST=localhost
DB_PORT=5432
DB_NAME=faceid_db
DB_USER=postgres
DB_PASS=postgres
```

## Useful PowerShell Functions

```powershell
# Add to your PowerShell profile for quick access

function faceid-start { 
    cd c:\Users\Owner\Documents\centralhold; 
    docker-compose up -d --build;
    docker-compose ps
}

function faceid-stop { 
    cd c:\Users\Owner\Documents\centralhold;
    docker-compose down
}

function faceid-logs { 
    cd c:\Users\Owner\Documents\centralhold;
    docker-compose logs -f
}

function faceid-test { 
    curl http://localhost:8001/health;
    Write-Host "---";
    curl -H "X-API-KEY: SecretCampusKey2026" http://localhost:8080/health
}

function faceid-db { 
    docker exec -it campus-postgres psql -U postgres -d faceid_db
}
```

Add to PowerShell profile:
```powershell
# $PROFILE location
notepad $PROFILE

# Add the functions above to enable quick commands like:
# faceid-start, faceid-stop, faceid-logs, faceid-test, faceid-db
```

## File Size Reference

Typical file sizes after setup:

```
Models: ~250MB
  - face_detection_yunet_2023mar.onnx: ~228 KB
  - arcface.onnx: ~84-100 MB
  
Docker Images: ~2.5GB total
  - postgres:15-alpine: ~125 MB
  - python:3.11-slim: ~120 MB
  - java-backend (built): ~400 MB
  
Database (initial): ~10 MB
  - Empty schema: ~1-2 MB
  - Per 1000 incidents: ~50-100 MB

Uploaded Images: Variable
  - Typical incident image: 2-5 MB
```

## API Response Examples

### Success Response (Incident Upload)
```json
Status: 200 OK

{
  "incidentId": 1,
  "timestamp": "2026-09-01T13:15:30.123456",
  "mediaPath": "/app/uploads/abc123-def456-ghi789.jpg",
  "matches": [
    {
      "faceBox": {"x": 100, "y": 50, "width": 150, "height": 150, "confidence": 0.95},
      "matchedStudentId": 5,
      "matchedStudentName": "John Doe",
      "matchedStudentClass": "Grade 11-A",
      "confidenceScore": 0.82
    }
  ]
}
```

### Error Response (Missing API Key)
```json
Status: 401 Unauthorized

"Unauthorized: Invalid API Key"
```

### Error Response (Service Unavailable)
```json
Status: 500 Internal Server Error

{
  "detail": "AI service is currently unavailable. Please try again later."
}
```

---

**Last Updated**: September 2026
**Version**: 1.0.0
