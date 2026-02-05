#!/bin/bash
echo "Packaging TradeOption Application..."

# Define paths
DIST_DIR="dist"
TARGET_JAR="target/TradeOption-0.0.1-SNAPSHOT.jar"
ELECTRON_DIR="electron"

# Clean dist folder
if [ -d "$DIST_DIR" ]; then
    rm -rf "$DIST_DIR"
fi
mkdir -p "$DIST_DIR"

# Check if JAR exists
if [ ! -f "$TARGET_JAR" ]; then
    echo "Error: $TARGET_JAR not found. Please run 'mvn clean package -DskipTests' first."
    exit 1
fi

# Create directory structure
mkdir -p "$DIST_DIR/target"
mkdir -p "$DIST_DIR/electron"

# Copy JAR
echo "Copying JAR..."
cp "$TARGET_JAR" "$DIST_DIR/target/"

# Copy Config
echo "Copying config.json..."
cp "config.json" "$DIST_DIR/"

# Copy Electron App (source only for now, assume npm install happens on start or pre-bundled)
echo "Copying Electron Source..."
# Copy everything from electron/ except node_modules
rsync -av --exclude='node_modules' "$ELECTRON_DIR/" "$DIST_DIR/electron/"

# Copy Start Scripts
echo "Copying Start Scripts..."
cp scripts/start.sh "$DIST_DIR/"
cp scripts/start.bat "$DIST_DIR/"

# Make start script executable
chmod +x "$DIST_DIR/start.sh"

echo "Packaging Complete. Application is available in '$DIST_DIR/'."
