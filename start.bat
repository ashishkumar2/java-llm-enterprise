@echo off
REM One-command local dev startup for Windows.
REM Usage: start.bat [infra|build|all|stop]

SET ROOT=%~dp0
SET JAVA_HOME=%ROOT%.tools\jdk-25.0.2
SET PATH=%JAVA_HOME%\bin;%ROOT%.tools\apache-maven-3.9.16\bin;%PATH%

SET CMD=%1
IF "%CMD%"=="" SET CMD=help

IF "%CMD%"=="infra" GOTO infra
IF "%CMD%"=="build" GOTO build
IF "%CMD%"=="all"   GOTO all
IF "%CMD%"=="stop"  GOTO stop
GOTO help

:infra
echo Starting infrastructure...
docker-compose up -d postgres redis zookeeper kafka
echo Waiting 15s for Kafka...
timeout /t 15 /nobreak >nul
docker-compose ps
GOTO end

:build
echo Building all services...
call mvn clean package -DskipTests --no-transfer-progress
echo Build complete.
GOTO end

:all
echo Starting infrastructure...
docker-compose up -d postgres redis zookeeper kafka
echo Waiting 15s for Kafka...
timeout /t 15 /nobreak >nul

echo Building all services...
call mvn clean package -DskipTests --no-transfer-progress

echo.
echo Starting services in separate windows...
start "AI Orchestrator :8081" cmd /k "SET JAVA_HOME=%JAVA_HOME% && SET PATH=%PATH% && SET SPRING_PROFILES_ACTIVE=local && mvn -pl ai-orchestrator-service spring-boot:run"
start "Ingestion :8082"       cmd /k "SET JAVA_HOME=%JAVA_HOME% && SET PATH=%PATH% && SET SPRING_PROFILES_ACTIVE=local && mvn -pl ingestion-service spring-boot:run"

echo Waiting 20s for services to start before gateway...
timeout /t 20 /nobreak >nul

start "Gateway :8080" cmd /k "SET JAVA_HOME=%JAVA_HOME% && SET PATH=%PATH% && SET SPRING_PROFILES_ACTIVE=local && mvn -pl gateway-service spring-boot:run"

echo.
echo Services starting in separate windows:
echo   Gateway         ^> http://localhost:8080/actuator/health
echo   AI Orchestrator ^> http://localhost:8081/actuator/health
echo   Ingestion       ^> http://localhost:8082/actuator/health
GOTO end

:stop
echo Stopping infrastructure...
docker-compose down
echo Done. Close the service terminal windows manually.
GOTO end

:help
echo Usage: start.bat [infra^|build^|all^|stop]
echo   infra  -- start Docker infrastructure only
echo   build  -- compile all Maven modules
echo   all    -- start infra + build + run all three services
echo   stop   -- stop Docker infrastructure

:end
