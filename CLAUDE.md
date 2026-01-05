# CLAUDE.md - Projektkontext für Claude Code

## Projektübersicht

**CTE-Dashboard-Spring** ist ein Spring Boot 4.0 Template-Projekt mit Spring Batch Integration. Es dient als Ausgangspunkt für Batch-Verarbeitungs-Anwendungen.

## Technische Eckdaten

- **Framework:** Spring Boot 4.0.0
- **Java Version:** 25 (JDK 25)
- **Build Tool:** Maven
- **Batch Framework:** Spring Batch 6.0
- **Datenbank:** H2 (Dev), PostgreSQL (Prod)
- **IDE:** IntelliJ IDEA

## Wichtige Dateien

| Datei | Beschreibung |
|-------|--------------|
| `pom.xml` | Maven-Konfiguration mit allen Dependencies |
| `src/main/resources/application.yml` | Hauptkonfiguration (H2, Port 8081) |
| `src/main/resources/application-prod.yml` | PostgreSQL-Konfiguration |
| `DashboardApplication.java` | Spring Boot Hauptklasse |
| `BatchConfig.java` | Spring Batch Job/Step Definition |
| `docker-compose.yml` | Docker Setup für PostgreSQL |

## Architektur-Entscheidungen

### Kein Lombok
Lombok wurde entfernt wegen Inkompatibilität mit Java 25. Alle Entities verwenden manuelle Getter/Setter.

### Spring Batch 6.0 Package-Struktur
Die Imports für Spring Batch 6.0 unterscheiden sich von früheren Versionen:
```java
// Jobs und Steps
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.parameters.JobParameters;

// Item Reader/Processor/Writer
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.Chunk;
```

### H2 File-basiert
H2 ist als File-basierte Datenbank konfiguriert (`jdbc:h2:file:./data/cte_dashboard`) statt In-Memory, um:
- Daten zwischen Neustarts zu behalten
- Zugriff von IntelliJ Database Tool zu ermöglichen
- `AUTO_SERVER=TRUE` erlaubt mehrere gleichzeitige Verbindungen

## Befehle

```bash
# Kompilieren
mvn clean compile

# Tests
mvn test

# Starten (Dev mit H2)
mvn spring-boot:run

# Starten (Prod mit PostgreSQL)
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# Docker PostgreSQL
docker-compose up postgres
```

## Endpoints

- **App:** http://localhost:8081
- **Health:** http://localhost:8081/actuator/health
- **Batch API:** http://localhost:8081/api/batch/*

## Bekannte Deprecation Warnings

Diese Warnings sind erwartet und können ignoriert werden:
- `JobLauncher` ist deprecated
- `chunk(int, TransactionManager)` ist deprecated

## Coding Standards

- Java 25 Features können verwendet werden
- Keine Lombok-Annotationen
- Spring Constructor Injection (nicht @Autowired)
- YAML für Konfiguration (nicht .properties)
- Deutsche Commit-Messages erlaubt

## Docker

### Lokale Entwicklung
```bash
# Nur PostgreSQL starten
docker-compose up postgres -d

# App mit Prod-Profil starten
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Komplettes Docker-Deployment
```bash
# Beide Container bauen und starten
docker-compose up --build -d

# Status prüfen
docker ps

# Logs anzeigen
docker logs cte-dashboard-app

# Stoppen
docker-compose down
```

### Docker-Image exportieren (für Auslieferung)
```bash
docker-compose build
docker save cte-dashboard-spring-app:latest -o cte-dashboard-app.tar
```

**Hinweis:** Dockerfile verwendet Java 23 (eclipse-temurin:23), da JDK 25 EA nicht als Docker-Image verfügbar ist. Lokal wird JDK 25 verwendet.

## Deployment

Siehe `DEPLOYMENT.md` für detaillierte Auslieferungsoptionen:
- **Option 1:** Docker Image Export (tar-Datei)
- **Option 2:** Container Registry (Docker Hub, ghcr.io)
- **Option 3:** Source Code Auslieferung

## Erweiterungspunkte

1. **Neue Batch Jobs:** In `BatchConfig.java` registrieren
2. **Neue Entities:** Package `entity/` + Repository in `repository/`
3. **Neue REST Endpoints:** Package `controller/`
4. **Neue Services:** Package `service/`

## Aktueller Stand (05.01.2026)

- Projekt vollständig lauffähig (lokal + Docker)
- 4 Git-Commits
- Docker-Image exportiert: `cte-dashboard-app.tar` (145 MB)
- Dokumentation: README.md, CLAUDE.md, DEPLOYMENT.md
