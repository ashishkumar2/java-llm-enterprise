# Skill: run-services

Start one or all services for local development.

## Usage
- `/run-services` — print instructions for running all three services
- `/run-services gateway` — start only the gateway
- `/run-services orchestrator` — start only the AI orchestrator
- `/run-services ingestion` — start only the ingestion service

## Steps

1. Verify infrastructure is up:
   ```bash
   docker-compose ps
   ```
   Ensure `postgres`, `redis`, `zookeeper`, and `kafka` show as healthy/running. If not:
   ```bash
   docker-compose up -d postgres redis zookeeper kafka
   ```

2. Set build environment:
   ```bash
   export JAVA_HOME="$(pwd)/.tools/jdk-25.0.2"
   export PATH="$JAVA_HOME/bin:$(pwd)/.tools/apache-maven-3.9.16/bin:$PATH"
   ```

3. Start the requested service(s). Always start gateway LAST.

   **AI Orchestrator** (port 8081):
   ```bash
   SPRING_PROFILES_ACTIVE=local mvn -pl ai-orchestrator-service spring-boot:run
   ```

   **Ingestion Service** (port 8082):
   ```bash
   SPRING_PROFILES_ACTIVE=local mvn -pl ingestion-service spring-boot:run
   ```

   **Gateway** (port 8080 — start after the other two):
   ```bash
   SPRING_PROFILES_ACTIVE=local mvn -pl gateway-service spring-boot:run
   ```

4. Verify all services healthy:
   ```bash
   curl -s http://localhost:8080/actuator/health | jq .status
   curl -s http://localhost:8081/actuator/health | jq .status
   curl -s http://localhost:8082/actuator/health | jq .status
   ```
