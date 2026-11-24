@echo off
if not defined DB_PASSWORD (
    echo Error: Set DB_PASSWORD before running run.bat.
    exit /b 1
)

java -jar target/screenerAPI-0.0.1-SNAPSHOT.jar --scanx.auth=TEMP "--spring.datasource.password=%DB_PASSWORD%"
