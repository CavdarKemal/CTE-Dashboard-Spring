# Deployment-Optionen für CTE-Dashboard

Dieses Dokument beschreibt drei Möglichkeiten, die CTE-Dashboard-Anwendung auszuliefern und zu deployen.

---

## Option 1: Docker Image exportieren (Datei-basiert)

### Beschreibung
Das Docker-Image wird als `.tar`-Datei exportiert und kann auf einem USB-Stick, Netzlaufwerk oder per Dateitransfer weitergegeben werden. Der Empfänger lädt das Image in seine lokale Docker-Installation.

### Vorteile
- Kein Internet beim Empfänger erforderlich
- Keine Registry-Infrastruktur nötig
- Einfach für einmalige Weitergabe
- Volle Kontrolle über die Datei

### Nachteile
- Große Dateien (100-500 MB typisch)
- Keine automatischen Updates
- Versionsverwaltung manuell
- Nicht skalierbar für viele Empfänger

### Voraussetzungen
**Beim Ersteller:**
- Docker Desktop installiert
- Genügend Speicherplatz für Image

**Beim Empfänger:**
- Docker Desktop installiert
- PostgreSQL (via Docker oder lokal)

### Schritt-für-Schritt Anleitung

#### A) Image erstellen und exportieren (Ersteller)

```bash
# 1. In das Projektverzeichnis wechseln
cd CTE-Dashboard-Spring

# 2. Docker-Image bauen
docker-compose build

# 3. Image als tar-Datei exportieren
docker save cte-dashboard-spring-app:latest -o cte-dashboard-app.tar

# 4. Optional: Komprimieren für kleinere Dateigröße
gzip cte-dashboard-app.tar
# Ergebnis: cte-dashboard-app.tar.gz (ca. 50-60% kleiner)
```

#### B) Auslieferungspaket zusammenstellen

Folgende Dateien an den Empfänger weitergeben:

```
Auslieferung/
├── cte-dashboard-app.tar      # Docker-Image
├── docker-compose.yml         # Container-Orchestrierung
└── INSTALL.txt                # Installationsanleitung
```

#### C) Installation beim Empfänger

```bash
# 1. Image in Docker laden
docker load -i cte-dashboard-app.tar

# Falls komprimiert:
gunzip -c cte-dashboard-app.tar.gz | docker load

# 2. Prüfen ob Image geladen wurde
docker images | grep cte-dashboard

# 3. Mit docker-compose starten
docker-compose up -d

# 4. Status prüfen
docker ps
curl http://localhost:8081/actuator/health
```

#### D) Alternative: Ohne docker-compose

```bash
# PostgreSQL-Container starten
docker run -d \
  --name cte-dashboard-db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=cte_dashboard \
  -p 5432:5432 \
  postgres:15-alpine

# Warten bis PostgreSQL bereit ist
sleep 10

# App-Container starten
docker run -d \
  --name cte-dashboard-app \
  --link cte-dashboard-db:postgres \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/cte_dashboard \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  -p 8081:8081 \
  cte-dashboard-spring-app:latest
```

### Nützliche Befehle

```bash
# Image-Größe anzeigen
docker images cte-dashboard-spring-app

# Image entfernen
docker rmi cte-dashboard-spring-app:latest

# Alle Container stoppen und entfernen
docker-compose down

# Container-Logs anzeigen
docker logs cte-dashboard-app
```

---

## Option 2: Container Registry (Cloud-basiert)

### Beschreibung
Das Docker-Image wird in eine zentrale Registry hochgeladen. Empfänger ziehen das Image direkt aus der Registry. Dies ist der Standard für professionelle Deployments.

### Vorteile
- Automatische Versionierung mit Tags
- Zentrale Verwaltung aller Images
- Einfache Updates (neues Image pushen)
- Skalierbar für beliebig viele Empfänger
- Integration mit CI/CD-Pipelines
- Automatische Sicherheits-Scans (je nach Registry)

### Nachteile
- Registry-Infrastruktur erforderlich
- Internet-Zugang beim Empfänger nötig
- Eventuell Kosten (je nach Anbieter)
- Authentifizierung einrichten

### Bekannte Registry-Anbieter

| Anbieter | URL | Kostenlos | Privat |
|----------|-----|-----------|--------|
| Docker Hub | hub.docker.com | Ja (1 privates Repo) | Bezahlt |
| GitHub Container Registry | ghcr.io | Ja | Ja |
| GitLab Container Registry | registry.gitlab.com | Ja | Ja |
| AWS ECR | aws.amazon.com/ecr | Nein | Ja |
| Azure ACR | azure.microsoft.com/acr | Nein | Ja |
| Google GCR | gcr.io | Nein | Ja |

### Schritt-für-Schritt Anleitung

#### A) Docker Hub (Beispiel)

**Vorbereitung:**
```bash
# 1. Docker Hub Account erstellen auf https://hub.docker.com

# 2. Anmelden
docker login
# Benutzername und Passwort eingeben
```

**Image taggen und pushen:**
```bash
# 3. Image für Registry taggen
# Format: <username>/<repository>:<tag>
docker tag cte-dashboard-spring-app:latest meinuser/cte-dashboard:1.0.0
docker tag cte-dashboard-spring-app:latest meinuser/cte-dashboard:latest

# 4. Image hochladen
docker push meinuser/cte-dashboard:1.0.0
docker push meinuser/cte-dashboard:latest
```

**Beim Empfänger:**
```bash
# 5. Image herunterladen
docker pull meinuser/cte-dashboard:1.0.0

# 6. Container starten
docker run -d -p 8081:8081 meinuser/cte-dashboard:1.0.0
```

#### B) GitHub Container Registry (ghcr.io)

**Vorbereitung:**
```bash
# 1. Personal Access Token (PAT) auf GitHub erstellen
#    Settings → Developer settings → Personal access tokens
#    Berechtigung: write:packages

# 2. Mit Token anmelden
echo $GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin
```

**Image taggen und pushen:**
```bash
# 3. Image taggen
docker tag cte-dashboard-spring-app:latest ghcr.io/meinuser/cte-dashboard:1.0.0

# 4. Image hochladen
docker push ghcr.io/meinuser/cte-dashboard:1.0.0
```

#### C) Private/Self-Hosted Registry

```bash
# Eigene Registry starten (für Unternehmen)
docker run -d -p 5000:5000 --name registry registry:2

# Image taggen für lokale Registry
docker tag cte-dashboard-spring-app:latest localhost:5000/cte-dashboard:1.0.0

# Pushen
docker push localhost:5000/cte-dashboard:1.0.0
```

### Versionierung mit Tags

```bash
# Semantic Versioning empfohlen
docker tag cte-dashboard-spring-app:latest meinuser/cte-dashboard:1.0.0
docker tag cte-dashboard-spring-app:latest meinuser/cte-dashboard:1.0
docker tag cte-dashboard-spring-app:latest meinuser/cte-dashboard:1
docker tag cte-dashboard-spring-app:latest meinuser/cte-dashboard:latest

# Alle Tags pushen
docker push meinuser/cte-dashboard --all-tags
```

### docker-compose.yml für Registry-Image

```yaml
services:
  postgres:
    image: postgres:15-alpine
    container_name: cte-dashboard-db
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: cte_dashboard
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 5s
      retries: 5

  app:
    image: meinuser/cte-dashboard:1.0.0  # <-- Image aus Registry
    container_name: cte-dashboard-app
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/cte_dashboard
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
    ports:
      - "8081:8081"
    depends_on:
      postgres:
        condition: service_healthy

volumes:
  postgres_data:
```

---

## Option 3: Source Code ausliefern

### Beschreibung
Der Empfänger erhält den Quellcode und baut das Docker-Image selbst. Dies ist ideal für Open-Source-Projekte oder wenn der Empfänger Anpassungen vornehmen möchte.

### Vorteile
- Volle Transparenz (Quellcode einsehbar)
- Empfänger kann anpassen
- Kleinste Übertragungsgröße
- Keine Registry nötig
- Ideal für Open Source

### Nachteile
- Build-Zeit beim Empfänger (2-5 Minuten)
- Abhängigkeit von Maven Central (Internet)
- Empfänger braucht technisches Verständnis
- Build kann fehlschlagen (Dependency-Probleme)

### Voraussetzungen beim Empfänger
- Docker Desktop installiert
- Internet-Zugang (für Maven Dependencies)
- Ca. 2 GB freier Speicherplatz

### Minimales Auslieferungspaket

```
CTE-Dashboard-Spring/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/cte/dashboard/
│       │       ├── DashboardApplication.java
│       │       ├── config/
│       │       ├── batch/
│       │       ├── controller/
│       │       ├── entity/
│       │       ├── repository/
│       │       └── service/
│       └── resources/
│           ├── application.yml
│           ├── application-prod.yml
│           └── ...
├── pom.xml
├── Dockerfile
├── docker-compose.yml
└── README.md
```

### Schritt-für-Schritt Anleitung

#### A) Auslieferungspaket erstellen

**Option A1: Git Repository (empfohlen)**
```bash
# Repository auf GitHub/GitLab erstellen und pushen
git remote add origin https://github.com/meinuser/cte-dashboard.git
git push -u origin master

# Empfänger klont das Repository
git clone https://github.com/meinuser/cte-dashboard.git
```

**Option A2: ZIP-Archiv**
```bash
# Projekt als ZIP exportieren (ohne .git, target, etc.)
cd ..
zip -r CTE-Dashboard-Spring.zip CTE-Dashboard-Spring \
  -x "CTE-Dashboard-Spring/.git/*" \
  -x "CTE-Dashboard-Spring/target/*" \
  -x "CTE-Dashboard-Spring/data/*" \
  -x "CTE-Dashboard-Spring/*.tar"
```

**Windows PowerShell:**
```powershell
Compress-Archive -Path CTE-Dashboard-Spring -DestinationPath CTE-Dashboard-Spring.zip
```

#### B) Installation beim Empfänger

```bash
# 1. Projekt entpacken/klonen
unzip CTE-Dashboard-Spring.zip
cd CTE-Dashboard-Spring

# 2. Docker-Image bauen und Container starten
docker-compose up --build -d

# 3. Build-Fortschritt beobachten
docker-compose logs -f

# 4. Nach erfolgreichem Start testen
curl http://localhost:8081/actuator/health
```

### .gitignore für saubere Auslieferung

```gitignore
# Build
target/
*.jar
*.war

# IDE
.idea/
*.iml
.vscode/

# Datenbank
data/
*.db

# Docker Export
*.tar
*.tar.gz

# Logs
*.log

# OS
.DS_Store
Thumbs.db
```

### Offline-Build ermöglichen

Falls der Empfänger keinen Internet-Zugang hat:

```bash
# Dependencies vorab herunterladen
mvn dependency:go-offline

# Repository-Ordner mitsenden
# ~/.m2/repository (ca. 500 MB für dieses Projekt)
```

---

## Vergleichstabelle

| Kriterium | Option 1: Image Export | Option 2: Registry | Option 3: Source |
|-----------|------------------------|-------------------|------------------|
| **Dateigröße** | 150-200 MB | Upload einmalig | 50-100 KB |
| **Internet beim Empfänger** | Nein | Ja | Ja (Maven) |
| **Build-Zeit** | Keine | Keine | 2-5 Min |
| **Technisches Wissen** | Gering | Mittel | Mittel |
| **Updates** | Neue Datei senden | `docker pull` | `git pull` |
| **Versionierung** | Manuell | Tags | Git Tags |
| **Skalierbarkeit** | Schlecht | Sehr gut | Gut |
| **Anpassungen möglich** | Nein | Nein | Ja |
| **CI/CD Integration** | Schlecht | Sehr gut | Gut |
| **Kosten** | Keine | Registry-Kosten | Keine |

---

## Empfehlung nach Anwendungsfall

| Anwendungsfall | Empfohlene Option |
|----------------|-------------------|
| Einmalige Weitergabe an Kollegen | Option 1 |
| Demo für Kunden ohne Internet | Option 1 |
| Team-Entwicklung | Option 2 oder 3 |
| Produktion/Cloud | Option 2 |
| Open-Source-Projekt | Option 3 |
| CI/CD Pipeline | Option 2 |
| Air-Gapped Environment | Option 1 |

---

## Zusätzliche Ressourcen

- [Docker Documentation](https://docs.docker.com/)
- [Docker Hub](https://hub.docker.com/)
- [GitHub Container Registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)
- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)
