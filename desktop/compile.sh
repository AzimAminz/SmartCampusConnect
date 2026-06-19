#!/bin/bash
echo "Compiling SmartCampusConnect Desktop Client on macOS..."
javac -encoding UTF-8 -cp "lib/flatlaf-3.4.1.jar:." Main.java view/*.java model/*.java service/*.java util/*.java
if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi
echo "Compilation successful!"
