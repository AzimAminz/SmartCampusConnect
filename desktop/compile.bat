@echo off
echo Compiling SmartCampusConnect Desktop Client...
javac -encoding UTF-8 -cp "lib/flatlaf-3.4.1.jar;." Main.java view/*.java model/*.java service/*.java util/*.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b %errorlevel%
)
echo Compilation successful!
pause
