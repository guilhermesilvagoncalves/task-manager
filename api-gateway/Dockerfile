FROM maven:3.10.0-eclipse-temurin-25-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
COPY task-service/pom.xml task-service/
COPY auth-service/pom.xml auth-service/
COPY e2e-tests/pom.xml e2e-tests/
RUN mvn -B -q -pl api-gateway dependency:go-offline
COPY api-gateway/src api-gateway/src
RUN mvn -B -q -pl api-gateway package -DskipTests

FROM eclipse-temurin:25-jre-alpine
# Base image OS packages can lag behind Alpine security fixes (e.g. brotli); apply them at build time.
RUN apk upgrade --no-cache
WORKDIR /app
COPY --from=build /build/api-gateway/target/api-gateway-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
