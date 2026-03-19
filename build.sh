#!/bin/bash
echo "============================================="
echo "   MovieCode TV - Build Script"
echo "============================================="
echo ""

# Check for Java
if ! command -v java &> /dev/null; then
    echo "[ERROR] Java not found. Please install JDK 17+."
    echo "Download: https://adoptium.net/"
    exit 1
fi

# Check for Android SDK
if [ -z "$ANDROID_HOME" ] && [ ! -d "$ANDROID_HOME/platforms" ]; then
    echo "[ERROR] Android SDK not found."
    echo "Please set ANDROID_HOME environment variable."
    echo "Download: https://developer.android.com/studio"
    exit 1
fi

# Check for Gradle
if ! command -v gradle &> /dev/null; then
    echo "[INFO] Gradle not found in PATH, using wrapper..."
    chmod +x ./gradlew
    GRADLE_CMD="./gradlew"
else
    GRADLE_CMD="gradle"
fi

# Build debug APK
echo "[STEP 1/3] Cleaning project..."
$GRADLE_CMD clean

echo ""
echo "[STEP 2/3] Building debug APK..."
$GRADLE_CMD assembleDebug

if [ $? -ne 0 ]; then
    echo ""
    echo "[ERROR] Build failed!"
    exit 1
fi

echo ""
echo "[STEP 3/3] Verifying APK..."
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo ""
    echo "============================================="
    echo "   BUILD SUCCESSFUL!"
    echo "============================================="
    echo ""
    echo "APK Location:"
    echo "   $(pwd)/app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "File size:"
    ls -lh app/build/outputs/apk/debug/app-debug.apk | awk '{print $5}'
    echo ""
else
    echo "[ERROR] APK not found!"
    exit 1
fi
