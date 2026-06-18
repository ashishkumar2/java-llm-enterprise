FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY common-library/ common-library/
COPY gateway-service/ gateway-service/
COPY ai-orchestrator-service/pom.xml ai-orchestrator-service/pom.xml
COPY ingestion-service/pom.xml ingestion-service/pom.xml
RUN mvn clean package -pl common-library,gateway-service -am -DskipTests -B --no-transfer-progress

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/gateway-service/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
