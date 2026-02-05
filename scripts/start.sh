#!/bin/bash
cd electron

# Build Backend if source is available
cd ..
if [ -f "pom.xml" ]; then
    echo "Building Backend..."
    mvn clean package -DskipTests
    if [ $? -ne 0 ]; then
        echo "Backend build failed! Exiting..."
        exit 1
    fi
else
    echo "No pom.xml found, skipping build (running in distribution mode?)"
fi
cd electron

# Check if node_modules exists, if not install
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install
fi

echo "Starting TradeOption..."
npm start
