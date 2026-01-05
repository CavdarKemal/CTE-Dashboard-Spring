# Build Stage
FROM ubuntu:24.04 AS builder

# JDK 25 und Maven installieren
RUN apt-get update && apt-get install -y \
    wget \
    maven \
    && rm -rf /var/lib/apt/lists/*

# JDK 25 EA von Adoptium herunterladen
RUN wget -q https://download.java.net/java/early_access/jdk25/24/GPL/openjdk-25-ea+24_linux-x64_bin.tar.gz \
    && tar -xzf openjdk-25-ea+24_linux-x64_bin.tar.gz \
    && mv jdk-25 /opt/java \
    && rm openjdk-25-ea+24_linux-x64_bin.tar.gz

ENV JAVA_HOME=/opt/java
ENV PATH="${JAVA_HOME}/bin:${PATH}"

WORKDIR /app

# Dependencies cachen
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime Stage
FROM ubuntu:24.04 AS runtime

# JDK 25 EA herunterladen (nur JRE-Module)
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

RUN wget -q https://download.java.net/java/early_access/jdk25/24/GPL/openjdk-25-ea+24_linux-x64_bin.tar.gz \
    && tar -xzf openjdk-25-ea+24_linux-x64_bin.tar.gz \
    && mv jdk-25 /opt/java \
    && rm openjdk-25-ea+24_linux-x64_bin.tar.gz

ENV JAVA_HOME=/opt/java
ENV PATH="${JAVA_HOME}/bin:${PATH}"

WORKDIR /app

# JAR aus Build-Stage kopieren
COPY --from=builder /app/target/*.jar app.jar

# Nicht als Root ausführen
RUN useradd -r -s /bin/false appuser
USER appuser

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
