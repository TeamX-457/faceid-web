# FaceID Campus - Complete Testing Guide

## Prerequisites for Testing

1. All services running via Docker Compose:
   ```powershell
   docker-compose up -d
   docker-compose ps
   ```

2. Test images available (for upload testing)

## Service URLs

- **Python AI Service**: http://localhost:8001
- **Java Backend**: http://localhost:8080
- **PostgreSQL**: localhost:5432
- **API Key**: `SecretCampusKey2026`

---

## Test 1: Python Service - Health Check

### Using PowerShell

```powershell
$response = Invoke-WebRequest -Uri "http://localhost:8001/health" -UseBasicParsing
$response.Content | ConvertFrom-Json | ConvertTo-Json
```

**Expected Response:**
```json
{
  "status": "healthy",
  "service": "FaceID AI Microservice",
  "version": "1.0.0"
}
```

### Using curl

```bash
curl http://localhost:8001/health
```

---

## Test 2: Python Service - Generate Embedding

Used for student enrollment. Returns face embedding from a clear photo.

### Using PowerShell

```powershell
# Replace path/to/student_photo.jpg with actual image
$form = @{
    file = Get-Item -Path "C:\path\to\student_photo.jpg"
}

$response = Invoke-WebRequest `
    -Uri "http://localhost:8001/generate-embedding" `
    -Method POST `
    -Form $form `
    -UseBasicParsing

$response.Content | ConvertFrom-Json | ConvertTo-Json
```

**Expected Response:**
```json
{
  "success": true,
  "embedding": [
    0.123, 0.456, 0.789, ...  // 512-dimensional vector
  ],
  "message": "Embedding generated successfully"
}
```

### Using curl

```bash
curl -X POST http://localhost:8001/generate-embedding \
  -F "file=@path/to/student_photo.jpg"
```

---

## Test 3: Python Service - Detect and Match (With Gallery)

Detects faces and matches against gallery embeddings.

### Using PowerShell

```powershell
# First, prepare gallery data (embeddings from enrolled students)
$galleryData = @(
    @{
        student_id = 1
        name = "John Doe"
        student_class = "Grade 11-B"
        embedding = @(0.123, 0.456, 0.789, ...)  # 512 values
    },
    @{
        student_id = 2
        name = "Jane Smith"
        student_class = "Grade 10-A"
        embedding = @(0.234, 0.567, 0.890, ...)
    }
) | ConvertTo-Json

$form = @{
    file = Get-Item -Path "C:\path\to\incident_image.jpg"
    gallery_embeddings = $galleryData
}

$response = Invoke-WebRequest `
    -Uri "http://localhost:8001/detect-and-match" `
    -Method POST `
    -Form $form `
    -UseBasicParsing

$response.Content | ConvertFrom-Json | ConvertTo-Json
```

**Expected Response:**
```json
{
  "success": true,
  "detected_faces_count": 2,
  "matches": [
    {
      "face_box": {
        "x": 120,
        "y": 80,
        "width": 100,
        "height": 100,
        "confidence": 0.94
      },
      "matched_student_id": 1,
      "matched_student_name": "John Doe",
      "matched_student_class": "Grade 11-B",
      "confidence_score": 0.8241
    },
    {
      "face_box": {
        "x": 350,
        "y": 90,
        "width": 95,
        "height": 95,
        "confidence": 0.92
      },
      "matched_student_id": null,
      "matched_student_name": "Unknown / Unregistered",
      "matched_student_class": null,
      "confidence_score": 0.35
    }
  ],
  "message": "Detection and matching completed successfully"
}
```

### Using curl

```bash
# Prepare gallery embeddings JSON file (gallery.json)
curl -X POST http://localhost:8001/detect-and-match \
  -F "file=@path/to/incident_image.jpg" \
  -F "gallery_embeddings=@gallery.json"
```

---

## Test 4: Java Backend - Upload Incident (Full Integration Test)

### Using PowerShell

```powershell
# This tests the complete flow:
# Java Backend → Python AI Service → Face Detection & Matching

$headers = @{"X-API-KEY" = "SecretCampusKey2026"}
$form = @{
    file = Get-Item -Path "C:\path\to\incident_image.jpg"
}

$response = Invoke-WebRequest `
    -Uri "http://localhost:8080/api/v1/incidents/upload" `
    -Method POST `
    -Headers $headers `
    -Form $form `
    -UseBasicParsing

$response.Content | ConvertFrom-Json | ConvertTo-Json
```

**Expected Response:**
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
      "matchedStudentId": 1,
      "matchedStudentName": "John Doe",
      "matchedStudentClass": "Grade 11-B",
      "confidenceScore": 0.8241
    }
  ]
}
```

### Using curl

```bash
curl -X POST http://localhost:8080/api/v1/incidents/upload \
  -H "X-API-KEY: SecretCampusKey2026" \
  -F "file=@path/to/incident_image.jpg"
```

---

## Test 5: Database Verification

### Connect to PostgreSQL

```powershell
docker exec -it campus-postgres psql -U postgres -d faceid_db
```

### Common Queries

```sql
-- List all enrolled students
SELECT id, full_name, student_class, enrollment_date FROM students;

-- List all incidents
SELECT id, timestamp, media_path FROM incidents ORDER BY timestamp DESC;

-- View incident details with match results
SELECT id, timestamp, match_results FROM incidents WHERE id = 1 \gx
```

---

## Test 6: Error Cases

### Test Missing API Key

```powershell
$form = @{
    file = Get-Item -Path "C:\path\to\incident_image.jpg"
}

$response = Invoke-WebRequest `
    -Uri "http://localhost:8080/api/v1/incidents/upload" `
    -Method POST `
    -Form $form `
    -UseBasicParsing `
    -ErrorAction SilentlyContinue

# Expected: 401 Unauthorized
```

### Test Invalid Image

```powershell
$headers = @{"X-API-KEY" = "SecretCampusKey2026"}
$form = @{
    file = Get-Item -Path "C:\path\to\text_file.txt"  # Not an image
}

$response = Invoke-WebRequest `
    -Uri "http://localhost:8080/api/v1/incidents/upload" `
    -Method POST `
    -Headers $headers `
    -Form $form `
    -UseBasicParsing `
    -ErrorAction SilentlyContinue

# Expected: 500 or 400 with error message
```

### Test Python Service Unreachable

```powershell
# Stop Python service first
docker-compose stop python-ai-service

# Try incident upload
$headers = @{"X-API-KEY" = "SecretCampusKey2026"}
$form = @{
    file = Get-Item -Path "C:\path\to\incident_image.jpg"
}

$response = Invoke-WebRequest `
    -Uri "http://localhost:8080/api/v1/incidents/upload" `
    -Method POST `
    -Headers $headers `
    -Form $form `
    -UseBasicParsing `
    -ErrorAction SilentlyContinue

# Expected: 500 with message "AI service is currently unavailable"

# Restart service
docker-compose start python-ai-service
```

---

## Test 7: Performance Testing

### Generate Sample Embeddings

```powershell
# This creates test embeddings for database seeding

function New-RandomEmbedding {
    $embedding = @()
    for ($i = 0; $i -lt 512; $i++) {
        $embedding += [System.Math]::Round([System.Random]::new().NextDouble(), 4)
    }
    return $embedding -join ","
}

# Insert test students
$embedding = New-RandomEmbedding
docker exec campus-postgres psql -U postgres -d faceid_db -c `
    "INSERT INTO students (full_name, student_class, embedding_vector, enrollment_date) VALUES ('Test Student', 'Grade 11', '$embedding', now());"
```

### Load Test - Multiple Incident Uploads

```powershell
$headers = @{"X-API-KEY" = "SecretCampusKey2026"}

for ($i = 1; $i -le 5; $i++) {
    Write-Host "Upload $i of 5..."
    
    $form = @{
        file = Get-Item -Path "C:\path\to\incident_image.jpg"
    }
    
    $response = Invoke-WebRequest `
        -Uri "http://localhost:8080/api/v1/incidents/upload" `
        -Method POST `
        -Headers $headers `
        -Form $form `
        -UseBasicParsing
    
    $incidentId = ($response.Content | ConvertFrom-Json).incidentId
    Write-Host "  -> Incident ID: $incidentId"
    
    Start-Sleep -Seconds 1
}
```

---

## Troubleshooting During Testing

### Python Service Not Responding

```powershell
# Check logs
docker-compose logs python-ai-service

# Verify models exist
docker exec faceid-python-service ls -la /app/models/

# Check if dependencies installed
docker exec faceid-python-service pip list
```

### Java Backend Connection Issues

```powershell
# Check Java logs
docker-compose logs java-backend

# Test connectivity from Java container to Python
docker exec faceid-java-backend curl http://python-ai-service:8001/health

# Verify environment variables
docker exec faceid-java-backend env | grep PYTHON
```

### Database Connection Issues

```powershell
# Check if PostgreSQL is running
docker-compose ps postgres

# View PostgreSQL logs
docker-compose logs postgres

# Test database connection
docker exec campus-postgres psql -U postgres -c "SELECT 1"
```

---

## Success Criteria

✅ All tests passed when:

1. Python service returns healthy status
2. Embedding generation works with clear face photos
3. Detect-and-match returns correct face locations and student matches
4. Java backend successfully calls Python service
5. Incidents are stored in database with correct match results
6. Error cases handled gracefully (no crashes, clear error messages)
7. Performance acceptable (< 2 seconds per incident with < 5 faces)

---

## Test Data

### Sample Student Gallery JSON

```json
[
  {
    "student_id": 1,
    "name": "John Doe",
    "student_class": "Grade 11-B",
    "embedding": [0.123, 0.456, 0.789, ...]
  },
  {
    "student_id": 2,
    "name": "Jane Smith",
    "student_class": "Grade 10-A",
    "embedding": [0.234, 0.567, 0.890, ...]
  }
]
```

### Sample Incident Upload Response

See Test 4 section above for complete response format.
