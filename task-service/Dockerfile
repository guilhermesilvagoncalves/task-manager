FROM maven:3.10.0-eclipse-temurin-25-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
COPY task-service/pom.xml task-service/
COPY auth-service/pom.xml auth-service/
COPY e2e-tests/pom.xml e2e-tests/
RUN mvn -B -q -pl task-service dependency:go-offline
COPY task-service/src task-service/src
RUN mvn -B -q -pl task-service package -DskipTests

FROM eclipse-temurin:25-jre-alpine
# Base image OS packages can lag behind Alpine security fixes (e.g. brotli); apply them at build time.
RUN apk upgrade --no-cache
WORKDIR /app
COPY --from=build /build/task-service/target/task-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
