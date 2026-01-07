package com.cte.dashboard;

import com.cte.dashboard.util.ProfileLockManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DashboardApplication {
/*
    Ports pro Profil:
    - default: 8081
    - dev:     8082
    - prod:    8083
    - test:    8084

    | URL                                    | Beschreibung         |
    |----------------------------------------|----------------------|
    | http://localhost:{port}                | Startseite           |
    | http://localhost:{port}/actuator/health| Health Check         |
    | http://localhost:{port}/api/batch/health| Batch Service Status|
    | http://localhost:{port}/h2-console     | Datenbank-Console    |
*/
    public static void main(String[] args) {
        // 1. Shutdown Hook registrieren
        ProfileLockManager.registerShutdownHook();

        // 2. Profil ermitteln und Lock erwerben BEVOR Spring startet
        String profile = ProfileLockManager.getActiveProfileFromEnvironment(args);
        System.out.println("[ProfileLock] Aktives Profil: " + profile);

        if (!ProfileLockManager.acquireLock(profile)) {
            System.err.println("=====================================================");
            System.err.println("FEHLER: Profil '" + profile + "' ist bereits von einer");
            System.err.println("        anderen Instanz gesperrt!");
            System.err.println("        Bitte beenden Sie die andere Instanz zuerst.");
            System.err.println("=====================================================");
            System.exit(1);
        }

        // 3. Spring starten
        SpringApplication.run(DashboardApplication.class, args);
    }
}
