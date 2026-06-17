#!/usr/bin/env bash
# One-command local dev startup. Usage: ./start.sh [infra|all|stop]
set -e

ROOT="$(cd "$(dirname "$0")" && pwd)"
export JAVA_HOME="$ROOT/.tools/jdk-25.0.2"
export PATH="$JAVA_HOME/bin:$ROOT/.tools/apache-maven-3.9.16/bin:$PATH"

cmd="${1:-all}"

case "$cmd" in
  infra)
    echo "Starting infrastructure..."
    docker-compose up -d postgres redis zookeeper kafka
    echo "Waiting for Kafka to be ready..."
    sleep 15
    docker-compose ps
    ;;
  build)
    echo "Building all services..."
    mvn clean package -DskipTests --no-transfer-progress
    echo "Build complete."
    ;;
  all)
    echo "Starting infrastructure..."
    docker-compose up -d postgres redis zookeeper kafka
    echo "Waiting for Kafka to be ready (15s)..."
    sleep 15

    echo "Building all services..."
    mvn clean package -DskipTests --no-transfer-progress

    echo "Starting AI Orchestrator on :8081..."
    SPRING_PROFILES_ACTIVE=local mvn -pl ai-orchestrator-service spring-boot:run --no-transfer-progress &
    ORCH_PID=$!

    echo "Starting Ingestion Service on :8082..."
    SPRING_PROFILES_ACTIVE=local mvn -pl ingestion-service spring-boot:run --no-transfer-progress &
    ING_PID=$!

    echo "Waiting for services to start (20s)..."
    sleep 20

    echo "Starting Gateway on :8080..."
    SPRING_PROFILES_ACTIVE=local mvn -pl gateway-service spring-boot:run --no-transfer-progress &
    GW_PID=$!

    echo ""
    echo "All services started:"
    echo "  Gateway       → http://localhost:8080/actuator/health"
    echo "  AI Orchestrator → http://localhost:8081/actuator/health"
    echo "  Ingestion     → http://localhost:8082/actuator/health"
    echo ""
    echo "Press Ctrl+C to stop all services."
    wait $ORCH_PID $ING_PID $GW_PID
    ;;
  stop)
    echo "Stopping infrastructure..."
    docker-compose down
    echo "Killing any running service processes..."
    pkill -f "spring-boot:run" 2>/dev/null || true
    echo "Done."
    ;;
  *)
    echo "Usage: ./start.sh [infra|build|all|stop]"
    echo "  infra  — start Docker infrastructure only"
    echo "  build  — compile all Maven modules"
    echo "  all    — start infra + build + run all three services"
    echo "  stop   — stop infra and kill service processes"
    ;;
esac
