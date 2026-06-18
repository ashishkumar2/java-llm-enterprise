FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY common-library/ common-library/
COPY ai-orchestrator-service/ ai-orchestrator-service/
COPY ingestion-service/pom.xml ingestion-service/pom.xml
COPY gateway-service/pom.xml gateway-service/pom.xml
RUN mvn clean package -pl common-library,ai-orchestrator-service -am -DskipTests -B --no-transfer-progress

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/ai-orchestrator-service/target/*.jar app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
