# Build Stage
FROM eclipse-temurin:23-jdk AS builder

WORKDIR /app

# Maven installieren
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Dependencies cachen
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Build (mit Java 23 für Docker)
COPY src ./src
RUN mvn clean package -DskipTests -Dmaven.compiler.release=23

# Runtime Stage
FROM eclipse-temurin:23-jre

WORKDIR /app

# JAR aus Build-Stage kopieren
COPY --from=builder /app/target/*.jar app.jar

# Nicht als Root ausführen
RUN useradd -r -s /bin/false appuser
USER appuser

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
