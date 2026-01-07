# Profile-Locking System

## Uebersicht

Das Profile-Locking-System verhindert, dass mehrere Instanzen der CTE-Dashboard-Spring Anwendung mit demselben Spring-Profil gleichzeitig laufen. Dies schuetzt vor Konflikten bei gemeinsam genutzten Ressourcen wie Datenbanken, Ports und Dateien.

## Technischer Ansatz

Das System verwendet **ServerSocket-basiertes Locking**. Jedes Profil reserviert einen eindeutigen TCP-Port auf localhost. Wenn eine Instanz startet, versucht sie einen ServerSocket auf dem zugewiesenen Port zu oeffnen. Ist der Port bereits belegt, bedeutet dies, dass eine andere Instanz mit diesem Profil laeuft.

### Vorteile gegenueber FileLock

| Aspekt | FileLock | ServerSocket |
|--------|----------|--------------|
| Prozessuebergreifend | Unzuverlaessig auf Windows | Zuverlaessig |
| Automatische Freigabe | Manchmal problematisch | OS gibt Port bei Prozessende frei |
| Implementierung | Komplexer (Dateipfade) | Einfacher (nur Port) |

## Port-Zuweisung

| Profil | Lock-Port | Web-Port |
|--------|-----------|----------|
| default | 47200 | 8081 |
| dev | 47201 | 8082 |
| prod | 47202 | 8083 |
| test | 47203 | 8084 |

Unbekannte Profile erhalten einen Port basierend auf dem Hash des Profilnamens:
```
port = 47200 + (profile.hashCode() % 100)
```

## Architektur

```
┌─────────────────────────────────────────────────────────────┐
│                    DashboardApplication.java                 │
│  main() {                                                    │
│    1. registerShutdownHook()                                │
│    2. getActiveProfileFromEnvironment()                     │
│    3. acquireLock() -> bei Fehler: System.exit(1)          │
│    4. SpringApplication.run()                               │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   ProfileLockManager.java                    │
│  - Statische Methoden (kein Spring Bean)                    │
│  - acquireLock(profile) -> ServerSocket oeffnen             │
│  - releaseLock() -> ServerSocket schliessen                 │
│  - isLocked(profile) -> Port-Verfuegbarkeit pruefen         │
│  - getActiveProfileFromEnvironment() -> Profil ermitteln    │
│  - registerShutdownHook() -> Cleanup bei Beendigung         │
└─────────────────────────────────────────────────────────────┘
```

## Implementierung

### ProfileLockManager.java

```java
public class ProfileLockManager {
    private static final int BASE_PORT = 47200;
    private static final Map<String, Integer> PROFILE_PORTS = Map.of(
        "default", BASE_PORT,
        "dev", BASE_PORT + 1,
        "prod", BASE_PORT + 2,
        "test", BASE_PORT + 3
    );

    private static ServerSocket lockSocket;
    private static String currentProfile;

    public static synchronized boolean acquireLock(String profile) {
        int port = getPortForProfile(profile);
        try {
            lockSocket = new ServerSocket(port, 1, InetAddress.getLoopbackAddress());
            currentProfile = profile;
            return true;
        } catch (IOException e) {
            return false; // Port belegt = Profil gesperrt
        }
    }

    public static synchronized void releaseLock() {
        if (lockSocket != null) {
            try { lockSocket.close(); } catch (IOException e) { }
            lockSocket = null;
        }
    }
}
```

### DashboardApplication.java

```java
public static void main(String[] args) {
    // 1. Shutdown Hook registrieren
    ProfileLockManager.registerShutdownHook();

    // 2. Profil ermitteln BEVOR Spring startet
    String profile = ProfileLockManager.getActiveProfileFromEnvironment(args);

    // 3. Lock erwerben
    if (!ProfileLockManager.acquireLock(profile)) {
        System.err.println("FEHLER: Profil '" + profile + "' ist bereits gesperrt!");
        System.exit(1);
    }

    // 4. Spring starten
    SpringApplication.run(DashboardApplication.class, args);
}
```

## Profil-Erkennung

Das aktive Profil wird in folgender Reihenfolge ermittelt:

1. **Kommandozeilen-Argument:** `--spring.profiles.active=dev`
2. **System Property:** `-Dspring.profiles.active=dev`
3. **Umgebungsvariable:** `SPRING_PROFILES_ACTIVE=dev`
4. **Default:** `default`

### Batch-Scripts (empfohlen)

```cmd
:: run-dev.cmd
@echo off
set SPRING_PROFILES_ACTIVE=dev
set JAVA_HOME=C:\Program Files\Java\jdk-25
echo Starting with profile: dev
mvn spring-boot:run
```

## Verhalten

### Szenario 1: Erste Instanz startet

```
[ProfileLock] Aktives Profil: dev
[ProfileLock] Lock fuer Profil 'dev' erworben (Port 47201)

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
...
Tomcat started on port 8082 (http)
```

### Szenario 2: Zweite Instanz mit gleichem Profil

```
[ProfileLock] Aktives Profil: dev
[ProfileLock] FEHLER: Profil 'dev' ist bereits von einer anderen Instanz gesperrt (Port 47201)
=====================================================
FEHLER: Profil 'dev' ist bereits von einer
        anderen Instanz gesperrt!
        Bitte beenden Sie die andere Instanz zuerst.
=====================================================
```

### Szenario 3: Mehrere Profile gleichzeitig

Verschiedene Profile koennen parallel laufen:

```bash
# Terminal 1
run-default.cmd    # -> Port 8081, Lock 47200

# Terminal 2
run-dev.cmd        # -> Port 8082, Lock 47201

# Terminal 3
run-prod.cmd       # -> Port 8083, Lock 47202
```

### Szenario 4: Anwendung beenden

```
[ProfileLock] Shutdown Hook: Gebe Lock frei...
```

Der Lock wird automatisch freigegeben durch:
- Normales Beenden (Ctrl+C, SIGTERM)
- Shutdown Hook
- OS bei Prozess-Crash

## Warum statische Methoden?

Der Lock muss **VOR** dem Spring-Context erworben werden:

| Zeitpunkt | Problem |
|-----------|---------|
| @PostConstruct | Web-Server bereits gestartet |
| ApplicationReadyEvent | Zu spaet, Port-Konflikt bereits aufgetreten |
| **main() vor SpringApplication.run()** | Richtig! Kein Spring-Context noetig |

Daher verwendet ProfileLockManager statische Methoden statt @Component.

## Dateien

| Datei | Beschreibung |
|-------|--------------|
| `src/main/java/com/cte/dashboard/util/ProfileLockManager.java` | Lock-Logik |
| `src/main/java/com/cte/dashboard/DashboardApplication.java` | Integration |
| `run-default.cmd` | Startet mit default-Profil |
| `run-dev.cmd` | Startet mit dev-Profil |
| `run-prod.cmd` | Startet mit prod-Profil |

## Integration in andere Projekte

1. `ProfileLockManager.java` kopieren
2. In `main()` vor `SpringApplication.run()` einfuegen:
   ```java
   ProfileLockManager.registerShutdownHook();
   String profile = ProfileLockManager.getActiveProfileFromEnvironment(args);
   if (!ProfileLockManager.acquireLock(profile)) {
       System.exit(1);
   }
   ```
3. Ports in `application-{profile}.yml` anpassen

## Troubleshooting

### Lock wird nicht freigegeben

Falls ein Lock nach einem Crash haengen bleibt (sollte nicht passieren):

```cmd
:: Pruefen welcher Prozess den Port haelt
netstat -ano | findstr 47201

:: Prozess beenden
taskkill /PID <pid> /F
```

### Profil wird nicht erkannt

Debugging-Ausgabe:
```
[ProfileLock] Aktives Profil: default
```

Falls "default" angezeigt wird obwohl ein anderes Profil erwartet:
1. Umgebungsvariable pruefen: `echo %SPRING_PROFILES_ACTIVE%`
2. Batch-Script verwenden statt manuellem Aufruf

## Verwandte Dokumentation

- [TemplateGUI ENVIRONMENT_LOCKING.md](../../TemplateGUI/docs/ENVIRONMENT_LOCKING.md) - Aehnliches System fuer Swing-Anwendung
