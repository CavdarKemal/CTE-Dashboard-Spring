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

## Erweiterungspunkte

1. **Neue Batch Jobs:** In `BatchConfig.java` registrieren
2. **Neue Entities:** Package `entity/` + Repository in `repository/`
3. **Neue REST Endpoints:** Package `controller/`
4. **Neue Services:** Package `service/`
