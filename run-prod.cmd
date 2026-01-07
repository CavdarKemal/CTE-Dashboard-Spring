@echo off
set SPRING_PROFILES_ACTIVE=prod
set JAVA_HOME=C:\Program Files\Java\jdk-25
echo Starting with profile: prod
mvn spring-boot:run
