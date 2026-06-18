FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY common-library/ common-library/
COPY ingestion-service/ ingestion-service/
COPY ai-orchestrator-service/pom.xml ai-orchestrator-service/pom.xml
COPY gateway-service/pom.xml gateway-service/pom.xml
RUN mvn clean package -pl common-library,ingestion-service -am -DskipTests -B --no-transfer-progress

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/ingestion-service/target/*.jar app.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
