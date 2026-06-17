@echo off
echo Starting SmartCampusConnect Desktop Client...
java -cp "lib/flatlaf-3.4.1.jar;." Main
if %errorlevel% neq 0 (
    echo Application terminated with error code %errorlevel%
    pause
)
