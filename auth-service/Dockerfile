FROM maven:3.10.0-eclipse-temurin-25-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
COPY task-service/pom.xml task-service/
COPY auth-service/pom.xml auth-service/
RUN mvn -B -q -pl auth-service dependency:go-offline
COPY auth-service/src auth-service/src
RUN mvn -B -q -pl auth-service package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /build/auth-service/target/auth-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
