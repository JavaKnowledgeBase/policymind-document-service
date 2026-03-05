# ==============================
# PolicyMind AI - Dockerfile
# ==============================

FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build

COPY pom.xml ./
COPY src ./src

RUN mvn -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /build/target/policymind-document-service-0.0.1-SNAPSHOT.jar app.jar
COPY resource_docs ./resource_docs

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
