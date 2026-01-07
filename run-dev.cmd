@echo off
set SPRING_PROFILES_ACTIVE=dev
set JAVA_HOME=C:\Program Files\Java\jdk-25
echo Starting with profile: dev
mvn spring-boot:run
