#!/bin/bash

# Navigate to project root (assuming script is in scripts/ folder)
# This ensures we can run it from root as ./scripts/start.sh or from scripts/ as ./start.sh
cd "$(dirname "$0")/.." || exit

echo "Running in: $(pwd)"

# Build Backend if source is available
if [ -f "pom.xml" ]; then
    echo "Building Backend..."
    mvn clean package -DskipTests
    if [ $? -ne 0 ]; then
        echo "Backend build failed! Exiting..."
        exit 1
    fi
else
    echo "No pom.xml found. Ensuring JAR exists..."
fi

# Define JAR path
JAR_PATH="target/TradeOption-0.0.1-SNAPSHOT.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "Error: $JAR_PATH not found in $(pwd)!"
    exit 1
fi

echo "Starting TradeOption Backend..."

# Open Browser after a slight delay
# Check OS for open command
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    (sleep 5 && open "http://localhost:8082") &
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Docker/Linux
    if command -v xdg-open > /dev/null; then
        (sleep 5 && xdg-open "http://localhost:8082") &
    fi
elif [[ "$OSTYPE" == "msys" ]]; then
    # Windows Git Bash
    (sleep 5 && start "http://localhost:8082") &
fi

# Run JAR
java -jar "$JAR_PATH"
