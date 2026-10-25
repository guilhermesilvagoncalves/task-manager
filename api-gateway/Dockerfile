FROM maven:3.10.0-eclipse-temurin-25-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
COPY task-service/pom.xml task-service/
RUN mvn -B -q -pl api-gateway dependency:go-offline
COPY api-gateway/src api-gateway/src
RUN mvn -B -q -pl api-gateway package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /build/api-gateway/target/api-gateway-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
