# CTE-Dashboard-Spring – Tutorial

> **Zielgruppe:** Erfahrene Java-Entwickler ohne Vorkenntnisse in Spring Batch  
> **Projekt:** [CTE-Dashboard-Spring auf GitHub](https://github.com/CavdarKemal/CTE-Dashboard-Spring)

---

## Inhaltsverzeichnis

1. [Was ist dieses Projekt?](#1-was-ist-dieses-projekt)
2. [Technologie-Stack](#2-technologie-stack)
3. [Grundkonzepte: Spring Batch](#3-grundkonzepte-spring-batch)
4. [Projektstruktur](#4-projektstruktur)
5. [Voraussetzungen und Setup](#5-voraussetzungen-und-setup)
6. [Anwendung starten](#6-anwendung-starten)
7. [Die REST API](#7-die-rest-api)
8. [Spring Batch: Aufbau und Konfiguration](#8-spring-batch-aufbau-und-konfiguration)
9. [Datenbankschicht: Entity und Repository](#9-datenbankschicht-entity-und-repository)
10. [Multi-Profil-Konfiguration](#10-multi-profil-konfiguration)
11. [Profile Lock: Doppelstart verhindern](#11-profile-lock-doppelstart-verhindern)
12. [Docker-Deployment](#12-docker-deployment)
13. [Tests](#13-tests)
14. [Eigene Batch-Jobs hinzufügen](#14-eigene-batch-jobs-hinzufügen)
15. [Nächste Schritte](#15-nächste-schritte)

---

## 1. Was ist dieses Projekt?

**CTE-Dashboard-Spring** ist ein **Spring Boot 4.0 Template** für Batch-Verarbeitungs-Anwendungen. Es kombiniert Spring Batch 6.0 mit einer REST API zur Job-Steuerung und zeigt, wie man modulare, profilbasierte Enterprise-Anwendungen baut.

### Charakteristik: Template, kein fertiges Produkt

Der mitgelieferte Batch-Job verarbeitet fünf hartcodierte Zeichenketten. Das ist **Absicht** — das Projekt ist ein Gerüst, das man für eigene ETL-Prozesse anpassen kann.

### Was das Projekt bietet

| Funktion | Beschreibung |
|----------|-------------|
| **Batch-Ausführung** | Spring Batch 6.0 Job via REST-Endpunkt starten |
| **Job-Tracking** | Alle Ausführungen mit Status und Zeitstempeln in der DB speichern |
| **Multi-Profil** | `default`, `dev`, `prod`, `test` — je eigene Datenbank und Ports |
| **Profile Lock** | Verhindert versehentliche Doppelstarts |
| **Docker-Ready** | PostgreSQL + App über Docker Compose betreibbar |
| **Actuator** | Health, Metrics und Info-Endpunkte out-of-the-box |

---

## 2. Technologie-Stack

| Komponente | Version | Rolle |
|------------|---------|-------|
| **Java** | 25 (EA) | Laufzeitumgebung |
| **Spring Boot** | 4.0.0 | Anwendungsrahmen |
| **Spring Batch** | 6.0 | Batch-Verarbeitung |
| **Spring Data JPA** | 4.0 | ORM und Datenbankzugriff |
| **PostgreSQL** | 15+ | Produktionsdatenbank |
| **H2** | 2.x | Entwicklungsdatenbank (dateibasiert) |
| **Maven** | 3.9+ | Build-Tool |
| **Docker** | aktuell | Containerisierung |

> **Hinweis zu Lombok:** Lombok ist wegen Java 25 EA **nicht verfügbar**. Alle Getter/Setter sind manuell geschrieben.

---

## 3. Grundkonzepte: Spring Batch

### Job, Step, Chunk

Spring Batch organisiert Arbeit in drei Ebenen:

```
Job
└── Step(s)
    └── Chunk-Verarbeitung
        ├── ItemReader   — liest Daten
        ├── ItemProcessor — transformiert Daten
        └── ItemWriter   — schreibt Ergebnisse
```

**Job:** Eine vollständige Batch-Aufgabe (z.B. „Monatliche Abrechnung"). Wird per `JobLauncher` gestartet.

**Step:** Ein Verarbeitungsschritt innerhalb des Jobs. Ein Job kann mehrere Steps haben.

**Chunk:** Spring Batch liest und verarbeitet Daten in Blöcken (Chunks). Erst wenn ein Block vollständig verarbeitet ist, werden die Ergebnisse geschrieben und ein Commit ausgeführt. Das erlaubt Fehlertoleranz und Wiederholbarkeit.

### JobParameters

Jede Job-Ausführung erhält eindeutige **JobParameters** — sonst verweigert Spring Batch den Start (gleiche Parameter = bereits gelaufener Job). Das Projekt verwendet den aktuellen Zeitstempel als Parameter.

### JobExecution vs. BatchJobInfo

Spring Batch speichert interne Metadaten in eigenen Tabellen (`BATCH_JOB_*`). Das Projekt legt zusätzlich eine eigene `batch_job_info`-Tabelle an, um eine saubere REST-API für die Job-Historie anzubieten.

---

## 4. Projektstruktur

```
CTE-Dashboard-Spring/
├── src/main/java/com/cte/dashboard/
│   ├── DashboardApplication.java        # Einstiegspunkt mit Profile-Lock
│   ├── config/
│   │   └── BatchConfig.java             # Job- und Step-Definitionen
│   ├── batch/
│   │   ├── SampleItemReader.java        # Liest Beispieldaten
│   │   ├── SampleItemProcessor.java     # Wandelt in Großbuchstaben um
│   │   └── SampleItemWriter.java        # Protokolliert Ergebnisse
│   ├── controller/
│   │   ├── HomeController.java          # GET / (Health-Check)
│   │   └── BatchJobController.java      # REST-API für Batch-Operationen
│   ├── entity/
│   │   └── BatchJobInfo.java            # JPA-Entity (Job-Ausführungsprotokoll)
│   ├── repository/
│   │   └── BatchJobInfoRepository.java  # Spring Data JPA Repository
│   ├── service/
│   │   └── BatchJobService.java         # Orchestriert Job-Start und -Status
│   └── util/
│       └── ProfileLockManager.java      # Verhindert Mehrfachstart
├── src/main/resources/
│   ├── application.yml                  # Standard/Entwicklungsprofil (H2, Port 8081)
│   ├── application-dev.yml              # Dev-Profil (Port 8082, SQL-Logging)
│   ├── application-prod.yml             # Prod-Profil (PostgreSQL, Port 8083)
│   └── application-test.yml             # Test-Profil (H2 In-Memory, Port 8084)
├── Dockerfile                           # Multi-Stage Build (Java 23)
├── docker-compose.yml                   # PostgreSQL + App
├── run-default.cmd                      # Starter: Standard-Profil
├── run-dev.cmd                          # Starter: Dev-Profil
└── run-prod.cmd                         # Starter: Prod-Profil
```

---

## 5. Voraussetzungen und Setup

### Systemvoraussetzungen

| Software | Zweck |
|----------|-------|
| JDK 25 (oder 23) | Java-Laufzeitumgebung |
| Maven 3.9+ | Build und Abhängigkeitsverwaltung |
| Docker Desktop | PostgreSQL-Container (für Prod-Profil) |

### Repository klonen und bauen

```cmd
cd E:\Projekte\ClaudeCode
git clone https://github.com/CavdarKemal/CTE-Dashboard-Spring.git
cd CTE-Dashboard-Spring

ci.cmd 25
```

---

## 6. Anwendung starten

### Profil-Übersicht

| Profil | Port | Datenbank | Besonderheiten |
|--------|------|-----------|----------------|
| `default` | 8081 | H2 Datei (`./data/`) | H2-Konsole unter `/h2-console` |
| `dev` | 8082 | H2 Datei | SQL-Logging, DEBUG |
| `prod` | 8083 | PostgreSQL | Kein H2, reduziertes Logging |
| `test` | 8084 | H2 In-Memory | Schema wird neu erstellt |

### Standard-Profil starten (H2, kein Docker nötig)

```cmd
run-default.cmd
```

Oder direkt:

```cmd
mvn spring-boot:run
```

### Dev-Profil starten

```cmd
run-dev.cmd
```

### Prod-Profil starten (PostgreSQL via Docker)

```cmd
REM Zuerst Datenbank starten:
docker-compose up postgres -d

REM Dann Anwendung:
run-prod.cmd
```

### Anwendung ist bereit, wenn …

```
Started DashboardApplication in 3.2 seconds
```

… in der Konsole erscheint.

### Health-Check

```bash
curl http://localhost:8081/actuator/health
# {"status":"UP","components":{"db":{"status":"UP"},...}}
```

---

## 7. Die REST API

Alle Endpunkte liefern JSON. Port je nach aktivem Profil.

### Übersicht

| Methode | Pfad | Beschreibung |
|---------|------|-------------|
| `GET` | `/` | Status und Zeitstempel |
| `GET` | `/actuator/health` | Spring Boot Health-Details |
| `GET` | `/api/batch/health` | Batch-Service-Status |
| `POST` | `/api/batch/run/sample` | Sample-Job starten |
| `GET` | `/api/batch/jobs` | Letzte 10 Job-Ausführungen |
| `GET` | `/api/batch/jobs/status/{status}` | Jobs nach Status filtern |

### Job starten

```bash
curl -X POST http://localhost:8081/api/batch/run/sample
```

Antwort:

```json
{
  "id": 1,
  "jobName": "sampleJob",
  "status": "COMPLETED",
  "startTime": "2026-04-03T10:30:45.123456",
  "endTime": "2026-04-03T10:30:46.456789",
  "itemsProcessed": 5,
  "errorMessage": null,
  "createdAt": "2026-04-03T10:30:45.123456",
  "updatedAt": "2026-04-03T10:30:46.456789"
}
```

### Job-Historie abfragen

```bash
# Letzte 10 Jobs
curl http://localhost:8081/api/batch/jobs

# Nur abgeschlossene Jobs
curl http://localhost:8081/api/batch/jobs/status/COMPLETED

# Nur fehlgeschlagene Jobs
curl http://localhost:8081/api/batch/jobs/status/FAILED
```

### Mögliche Status-Werte

| Status | Bedeutung |
|--------|-----------|
| `STARTED` | Job läuft gerade |
| `COMPLETED` | Job erfolgreich abgeschlossen |
| `FAILED` | Job mit Fehler beendet |

---

## 8. Spring Batch: Aufbau und Konfiguration

### BatchConfig.java

Die zentrale Konfigurationsdatei definiert Job und Step:

```java
@Configuration
public class BatchConfig {

    @Bean
    public Job sampleJob(JobRepository jobRepository,
                         Step sampleStep) {
        return new JobBuilder("sampleJob", jobRepository)
                .start(sampleStep)
                .build();
    }

    @Bean
    public Step sampleStep(JobRepository jobRepository,
                           PlatformTransactionManager transactionManager,
                           SampleItemReader reader,
                           SampleItemProcessor processor,
                           SampleItemWriter writer) {
        return new StepBuilder("sampleStep", jobRepository)
                .<String, String>chunk(10, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
}
```

> **Spring Batch 6.0:** Die API nutzt `JobBuilder` und `StepBuilder` mit explizitem `JobRepository`. Das ist die neue API — ältere Tutorials mit `@EnableBatchProcessing` und Injection von `JobBuilderFactory` sind veraltet.

### SampleItemReader

```java
@Component
public class SampleItemReader implements ItemReader<String> {
    private final List<String> items = List.of(
        "Item 1", "Item 2", "Item 3", "Item 4", "Item 5"
    );
    private int index = 0;

    @Override
    public String read() {
        if (index < items.size()) {
            return items.get(index++);
        }
        return null; // null signalisiert: keine Daten mehr
    }
}
```

> `null` als Rückgabe beendet die Lese-Phase. Das ist der Standard-Vertrag in Spring Batch.

### SampleItemProcessor

```java
@Component
public class SampleItemProcessor implements ItemProcessor<String, String> {

    @Override
    public String process(String item) {
        return item.toUpperCase();
    }
}
```

### SampleItemWriter

```java
@Component
public class SampleItemWriter implements ItemWriter<String> {

    @Override
    public void write(Chunk<? extends String> chunk) {
        chunk.getItems().forEach(item ->
            log.info("Verarbeitet: {}", item)
        );
    }
}
```

> **Spring Batch 6.0:** Der Writer erhält ein `Chunk<T>`-Objekt statt `List<T>`. `chunk.getItems()` liefert die verarbeiteten Elemente.

### BatchJobService

Der Service orchestriert den Job-Start und schreibt das Ergebnis in die eigene Tabelle:

```java
@Service
public class BatchJobService {

    public BatchJobInfo runSampleJob() {
        BatchJobInfo jobInfo = new BatchJobInfo("sampleJob");
        jobInfo.setStatus("STARTED");
        repository.save(jobInfo);

        try {
            JobParameters params = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

            JobExecution execution = jobLauncher.run(sampleJob, params);
            jobInfo.setStatus(execution.getStatus().toString());
            jobInfo.setItemsProcessed(5);
        } catch (Exception e) {
            jobInfo.setStatus("FAILED");
            jobInfo.setErrorMessage(e.getMessage());
        } finally {
            jobInfo.setEndTime(LocalDateTime.now());
            repository.save(jobInfo);
        }

        return jobInfo;
    }
}
```

---

## 9. Datenbankschicht: Entity und Repository

### BatchJobInfo Entity

```java
@Entity
@Table(name = "batch_job_info")
public class BatchJobInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String jobName;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer itemsProcessed;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        startTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

> `@PrePersist` und `@PreUpdate` sind JPA-Lifecycle-Callbacks — sie setzen Zeitstempel automatisch, ohne dass der Service-Code sich darum kümmern muss.

### BatchJobInfoRepository

```java
public interface BatchJobInfoRepository extends JpaRepository<BatchJobInfo, Long> {

    // Letzte 10 Jobs (für GET /api/batch/jobs)
    List<BatchJobInfo> findTop10ByOrderByCreatedAtDesc();

    // Nach Status filtern (für GET /api/batch/jobs/status/{status})
    List<BatchJobInfo> findByStatusOrderByCreatedAtDesc(String status);

    // Nach Job-Name filtern
    List<BatchJobInfo> findByJobNameOrderByCreatedAtDesc(String jobName);
}
```

Spring Data JPA leitet aus den Methodennamen automatisch SQL-Queries ab — kein `@Query` nötig.

---

## 10. Multi-Profil-Konfiguration

### application.yml (Standard)

```yaml
spring:
  application:
    name: cte-dashboard
  datasource:
    url: jdbc:h2:file:./data/cte_dashboard;AUTO_SERVER=TRUE
    username: sa
    password:
    driver-class-name: org.h2.Driver
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: update
  h2:
    console:
      enabled: true     # erreichbar unter /h2-console
      path: /h2-console
  batch:
    jdbc:
      initialize-schema: always

server:
  port: 8081
```

### application-prod.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/cte_dashboard
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: update
  h2:
    console:
      enabled: false

server:
  port: 8083

logging:
  level:
    com.cte: INFO
    org.springframework.batch: WARN
```

### Profil aktivieren

```cmd
REM Umgebungsvariable (dauerhaft für Session)
set SPRING_PROFILES_ACTIVE=prod

REM Oder als Maven-Parameter
mvn spring-boot:run -Dspring-boot.run.profiles=dev

REM Oder als JVM-Parameter beim direkten JAR-Start
java -Dspring.profiles.active=prod -jar cte-dashboard.jar
```

---

## 11. Profile Lock: Doppelstart verhindern

### Das Problem

Wer aus Versehen dieselbe Anwendung zweimal mit demselben Profil startet (z.B. zwei Terminal-Fenster), bekommt Portkonflikte und widersprüchliche Datenbankzugriffe.

### Die Lösung: ServerSocket als OS-Lock

`ProfileLockManager` reserviert beim Start einen TCP-Port als exklusiven Lock:

| Profil | Lock-Port |
|--------|-----------|
| `default` | 47200 |
| `dev` | 47201 |
| `prod` | 47202 |
| `test` | 47203 |

```java
public class ProfileLockManager {

    public static void acquireLock(String profile) {
        int port = getLockPort(profile);
        try {
            ServerSocket socket = new ServerSocket(port);
            // Lock gehalten solange die Anwendung läuft
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try { socket.close(); } catch (IOException ignored) {}
            }));
        } catch (IOException e) {
            System.err.println("Profil '" + profile + "' ist bereits gestartet (Port " + port + " belegt).");
            System.exit(1);
        }
    }
}
```

**Was passiert beim Doppelstart:**
1. Erste Instanz: reserviert Port 47202 → startet erfolgreich
2. Zweite Instanz: Port 47202 bereits belegt → Fehlermeldung + sofortiger Exit

Beim Herunterfahren gibt der JVM-Shutdown-Hook den Socket (und damit den Port) automatisch frei.

---

## 12. Docker-Deployment

### docker-compose.yml

```yaml
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: cte_dashboard
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    build: .
    ports:
      - "8081:8081"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/cte_dashboard
    depends_on:
      postgres:
        condition: service_healthy
```

### Gesamter Stack starten

```cmd
docker-compose up --build
```

### Nur Datenbank starten (App lokal im Profil `prod`)

```cmd
docker-compose up postgres -d
run-prod.cmd
```

### Dockerfile (Multi-Stage)

```dockerfile
# Build-Stage
FROM eclipse-temurin:23-jdk AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime-Stage
FROM eclipse-temurin:23-jre
WORKDIR /app
RUN adduser --system appuser
USER appuser
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

> **Hinweis:** Das Dockerfile nutzt Java 23 (kein Java 25 EA als Docker-Image verfügbar). Das kompilierte JAR läuft trotzdem korrekt.

---

## 13. Tests

### Standardtest: Context Loading

```java
@SpringBootTest
class DashboardApplicationTests {

    @Test
    void contextLoads() {
        // Spring-Kontext wird geladen — schlägt fehl, wenn Konfiguration fehlerhaft
    }
}
```

### Tests ausführen

```cmd
cit.cmd 25
```

Das Test-Profil (`application-test.yml`) nutzt H2 In-Memory — kein Docker nötig.

### Actuator-Endpunkte für Monitoring

```bash
# Health-Details
curl http://localhost:8081/actuator/health

# Metriken
curl http://localhost:8081/actuator/metrics

# Umgebungsdetails
curl http://localhost:8081/actuator/env
```

---

## 14. Eigene Batch-Jobs hinzufügen

So wird aus dem Template eine produktive Anwendung:

### Schritt 1: Reader implementieren

```java
@Component
public class MeinItemReader implements ItemReader<MeinDatentyp> {

    private final MeinRepository repository;
    private Iterator<MeinDatentyp> iterator;

    public MeinItemReader(MeinRepository repository) {
        this.repository = repository;
    }

    @Override
    public MeinDatentyp read() {
        if (iterator == null) {
            iterator = repository.findAll().iterator();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }
}
```

### Schritt 2: Processor implementieren

```java
@Component
public class MeinItemProcessor implements ItemProcessor<MeinDatentyp, MeinErgebnis> {

    @Override
    public MeinErgebnis process(MeinDatentyp item) {
        // Transformation, Validierung, Anreicherung
        return new MeinErgebnis(item);
    }
}
```

### Schritt 3: Writer implementieren

```java
@Component
public class MeinItemWriter implements ItemWriter<MeinErgebnis> {

    private final MeinZielRepository zielRepository;

    @Override
    public void write(Chunk<? extends MeinErgebnis> chunk) {
        zielRepository.saveAll(chunk.getItems());
    }
}
```

### Schritt 4: Job in BatchConfig registrieren

```java
@Bean
public Job meinJob(JobRepository jobRepository, Step meinStep) {
    return new JobBuilder("meinJob", jobRepository)
            .start(meinStep)
            .build();
}

@Bean
public Step meinStep(JobRepository jobRepository,
                     PlatformTransactionManager txManager,
                     MeinItemReader reader,
                     MeinItemProcessor processor,
                     MeinItemWriter writer) {
    return new StepBuilder("meinStep", jobRepository)
            .<MeinDatentyp, MeinErgebnis>chunk(100, txManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
}
```

### Schritt 5: REST-Endpunkt hinzufügen

```java
// In BatchJobController:
@PostMapping("/run/mein-job")
public ResponseEntity<BatchJobInfo> runMeinJob() {
    return ResponseEntity.ok(batchJobService.runMeinJob());
}
```

---

## 15. Nächste Schritte

### Spring Batch Monitoring: Spring Batch Admin / Actuator

Die internen Batch-Tabellen (`BATCH_JOB_*`) können über die H2-Konsole (Entwicklung) oder direkt via PostgreSQL eingesehen werden:

```sql
SELECT * FROM BATCH_JOB_EXECUTION ORDER BY CREATE_TIME DESC LIMIT 10;
SELECT * FROM BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID = 1;
```

### Job-Scheduling

Für automatische Ausführung nach Zeitplan:

```java
@Scheduled(cron = "0 0 2 * * ?")  // Täglich um 02:00 Uhr
public void scheduledJob() {
    batchJobService.runSampleJob();
}
```

Erfordert `@EnableScheduling` in der Hauptklasse.

### Weiterführende Dokumentation im Projekt

| Dokument | Inhalt |
|----------|--------|
| [`README.md`](README.md) | Kurzübersicht und Schnellstart |
| [`DEPLOYMENT.md`](DEPLOYMENT.md) | Deployment-Optionen (Docker, Registry, Quellcode) |

---

*Erstellt: April 2026*
