@echo off
REM FaceID Campus - Complete Integration Test Script
REM Tests Python microservice and Java backend

echo.
echo ====================================
echo FaceID Campus - Integration Test
echo ====================================
echo.

REM Colors for output (Windows doesn't support directly, so we use text labels)
setlocal enabledelayedexpansion

REM Configuration
set PYTHON_SERVICE_URL=http://localhost:8001
set JAVA_BACKEND_URL=http://localhost:8080
set API_KEY=SecretCampusKey2026

echo Step 1: Checking if services are running...
echo.

REM Test Python service health
echo Testing Python AI Service (%PYTHON_SERVICE_URL%) ...
curl -s %PYTHON_SERVICE_URL%/health >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] Python service is healthy
) else (
    echo [ERROR] Python service not responding
    echo Make sure to run: docker-compose up -d
    pause
    exit /b 1
)

REM Test Java backend (with API key)
echo Testing Java Backend (%JAVA_BACKEND_URL%) ...
curl -s -H "X-API-KEY: %API_KEY%" %JAVA_BACKEND_URL%/health >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] Java backend is healthy
) else (
    echo [WARNING] Java backend not responding - services may still be starting
)

echo.
echo ====================================
echo Step 2: Test Python Service Only
echo ====================================
echo.

REM Test 1: Python service direct test (health check)
echo Test 2.1: Python Health Check
curl -s %PYTHON_SERVICE_URL%/health | python -m json.tool
echo.

REM Test 2: Create a simple test image and generate embedding
REM (Note: requires actual image file)
echo Test 2.2: Generate Embedding (requires test_image.jpg)
echo.
echo Usage:
echo   curl -X POST http://localhost:8001/generate-embedding ^
echo     -F "file=@path/to/test_image.jpg" ^
echo     -H "Content-Type: multipart/form-data"
echo.

echo.
echo ====================================
echo Step 3: Test Java Backend
echo ====================================
echo.

echo Test 3.1: Java Backend Health Check (requires API key)
curl -s -H "X-API-KEY: %API_KEY%" %JAVA_BACKEND_URL%/health | python -m json.tool 2>nul || echo (May not have /health endpoint)
echo.

echo Test 3.2: Upload Incident Image (requires test_incident.jpg)
echo.
echo Usage:
echo   curl -X POST http://localhost:8080/api/v1/incidents/upload ^
echo     -H "X-API-KEY: SecretCampusKey2026" ^
echo     -F "file=@path/to/incident.jpg"
echo.

echo.
echo ====================================
echo Step 4: Database Status
echo ====================================
echo.

REM Check if PostgreSQL is running via Docker
docker ps | find "campus-postgres" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] PostgreSQL container is running
    echo.
    echo To connect to database:
    echo   docker exec -it campus-postgres psql -U postgres -d faceid_db
) else (
    echo [WARNING] PostgreSQL container not found
)

echo.
echo ====================================
echo Testing Complete
echo ====================================
echo.

echo Summary:
echo - Python AI Service: %PYTHON_SERVICE_URL%
echo - Java Backend: %JAVA_BACKEND_URL%
echo - API Key: %API_KEY%
echo - Database: faceid_db on postgres:5432
echo.

echo Next steps:
echo 1. Prepare test images:
echo    - test_image.jpg (for embedding generation)
echo    - incident_image.jpg (for incident upload)
echo.
echo 2. Run manual tests using curl commands above
echo.
echo 3. View logs:
echo    docker-compose logs -f
echo.

pause
