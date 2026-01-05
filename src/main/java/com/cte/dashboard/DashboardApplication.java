package com.cte.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DashboardApplication {
/*
    | URL                                    | Beschreibung         |
    |----------------------------------------|----------------------|
    | http://localhost:8081                  | Startseite           |
    | http://localhost:8081/actuator/health  | Health Check         |
    | http://localhost:8081/api/batch/health | Batch Service Status |
    | http://localhost:8081/h2-console       | Datenbank-Console    |
*/
    public static void main(String[] args) {
        SpringApplication.run(DashboardApplication.class, args);
    }
}
