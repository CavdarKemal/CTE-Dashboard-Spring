# CTE-Dashboard

Ein Spring Boot 4.0 Template-Projekt mit Spring Batch Integration.

## Technologie-Stack

| Komponente | Version | Beschreibung |
|------------|---------|--------------|
| Java | 25 | JDK 25 (Early Access) |
| Spring Boot | 4.0.0 | Aktuellste Major-Version |
| Spring Batch | 6.0 | Batch-Verarbeitung |
| Spring Data JPA | 4.0 | Datenbankzugriff |
| H2 | 2.x | Entwicklungs-Datenbank |
| PostgreSQL | 15+ | Produktions-Datenbank |
| Maven | 3.9+ | Build-Tool |

## Projektstruktur

```
CTE-Dashboard-Spring/
├── src/main/java/com/cte/dashboard/
│   ├── DashboardApplication.java      # Hauptklasse
│   ├── config/
│   │   └── BatchConfig.java           # Spring Batch Konfiguration
│   ├── batch/
│   │   ├── SampleItemReader.java      # Batch Reader
│   │   ├── SampleItemProcessor.java   # Batch Processor
│   │   └── SampleItemWriter.java      # Batch Writer
│   ├── controller/
│   │   ├── HomeController.java        # Root-Endpoint
│   │   └── BatchJobController.java    # Batch REST API
│   ├── entity/
│   │   └── BatchJobInfo.java          # JPA Entity
│   ├── repository/
│   │   └── BatchJobInfoRepository.java
│   └── service/
│       └── BatchJobService.java       # Batch Job Service
├── src/main/resources/
│   ├── application.yml                # Hauptkonfiguration (H2)
│   ├── application-dev.yml            # Entwicklungs-Profil
│   ├── application-prod.yml           # Produktions-Profil (PostgreSQL)
│   └── application-test.yml           # Test-Profil
├── src/test/java/
│   └── com/cte/dashboard/
│       └── DashboardApplicationTests.java
├── data/                              # H2 Datenbank-Dateien (gitignored)
├── pom.xml
├── Dockerfile
├── docker-compose.yml
└── README.md
```

## Schnellstart

### Voraussetzungen

- JDK 25 installiert
- Maven 3.9+
- (Optional) Docker für PostgreSQL

### Entwicklung starten

```bash
# Projekt kompilieren
mvn clean compile

# Tests ausführen
mvn test

# Anwendung starten (mit H2)
mvn spring-boot:run
```

Die Anwendung startet auf **http://localhost:8081**

### Mit IntelliJ IDEA

1. Projekt öffnen: `File → Open → CTE-Dashboard-Spring`
2. Maven-Import bestätigen
3. JDK 25 als Project SDK einstellen
4. `DashboardApplication.java` ausführen

## REST API Endpoints

| Method | URL | Beschreibung |
|--------|-----|--------------|
| GET | `/` | Anwendungsstatus |
| GET | `/actuator/health` | Health Check |
| GET | `/api/batch/health` | Batch Service Status |
| POST | `/api/batch/run/sample` | Sample Batch Job starten |
| GET | `/api/batch/jobs` | Letzte 10 Jobs anzeigen |
| GET | `/api/batch/jobs/status/{status}` | Jobs nach Status filtern |

## Datenbank-Konfiguration

### Entwicklung (Standard): H2 File-basiert

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/cte_dashboard;AUTO_SERVER=TRUE
    username: sa
    password:
```

**IntelliJ Database-Verbindung:**
- URL: `jdbc:h2:file:<projektpfad>/data/cte_dashboard`
- User: `sa`
- Password: (leer)

### Produktion: PostgreSQL

```bash
# PostgreSQL mit Docker starten
docker-compose up postgres

# Anwendung mit Prod-Profil starten
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

Oder als Environment-Variable:
```bash
export SPRING_PROFILES_ACTIVE=prod
mvn spring-boot:run
```

## Spring Batch Architektur

### Job-Struktur

```
sampleJob
└── sampleStep
    ├── SampleItemReader   (liest Daten)
    ├── SampleItemProcessor (verarbeitet Daten)
    └── SampleItemWriter   (schreibt Ergebnisse)
```

### Eigene Jobs erstellen

1. **Reader** implementieren:
```java
@Component
public class MyItemReader implements ItemReader<MyInput> {
    @Override
    public MyInput read() {
        // Daten lesen
    }
}
```

2. **Processor** implementieren:
```java
@Component
public class MyItemProcessor implements ItemProcessor<MyInput, MyOutput> {
    @Override
    public MyOutput process(MyInput item) {
        // Daten verarbeiten
    }
}
```

3. **Writer** implementieren:
```java
@Component
public class MyItemWriter implements ItemWriter<MyOutput> {
    @Override
    public void write(Chunk<? extends MyOutput> items) {
        // Daten schreiben
    }
}
```

4. **Job in BatchConfig registrieren**

## Docker Deployment

### Nur PostgreSQL

```bash
docker-compose up postgres
```

### Komplette Anwendung

```bash
docker-compose up --build
```

**Services:**
- App: http://localhost:8080
- PostgreSQL: localhost:5432

## Konfigurationsprofile

| Profil | Datenbank | Logging | Verwendung |
|--------|-----------|---------|------------|
| (default) | H2 File | DEBUG | Lokale Entwicklung |
| dev | H2 File | DEBUG | Entwicklung |
| test | H2 Memory | INFO | Unit Tests |
| prod | PostgreSQL | WARN | Produktion |

## Bekannte Hinweise

### Spring Boot 4.0 / Spring Batch 6.0

- `JobLauncher` ist deprecated (funktioniert aber noch)
- `chunk(int, TransactionManager)` ist deprecated
- Package-Struktur für Spring Batch hat sich geändert:
  - `org.springframework.batch.core.job.Job`
  - `org.springframework.batch.core.step.Step`
  - `org.springframework.batch.infrastructure.item.*`

### Java 25

- Early Access Version
- Lombok nicht kompatibel (wurde entfernt)
- Manuelle Getter/Setter erforderlich

## Weiterentwicklung

### Geplante Features

- [ ] Authentifizierung (Spring Security)
- [ ] Frontend (Thymeleaf oder React)
- [ ] Scheduling für Batch Jobs
- [ ] Monitoring Dashboard
- [ ] OpenAPI/Swagger Dokumentation

## Lizenz

Internes Projekt - CTE Dashboard
