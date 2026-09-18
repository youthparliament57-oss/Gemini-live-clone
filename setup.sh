#!/usr/bin/env bash
# Gemini Live Android - Quick Setup Script
# Restores required local files (debug.keystore and .env) before building.

set -e

echo "=========================================="
echo "   Gemini Live - Project Setup Script    "
echo "=========================================="

# 1. Restore debug.keystore from debug.keystore.base64
if [ -f "debug.keystore.base64" ]; then
  if [ ! -f "debug.keystore" ]; then
    echo "[1/3] Restoring debug.keystore from debug.keystore.base64..."
    base64 -d debug.keystore.base64 > debug.keystore
    echo "      -> debug.keystore created."
  else
    echo "[1/3] debug.keystore already present."
  fi
else
  if [ ! -f "debug.keystore" ]; then
    echo "[1/3] Generating default Android debug.keystore..."
    keytool -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
    echo "      -> Generated debug.keystore."
  fi
fi

# 2. Setup .env file
if [ ! -f ".env" ]; then
  echo "[2/3] Initializing .env from .env.example..."
  if [ -f ".env.example" ]; then
    cp .env.example .env
  else
    echo "GEMINI_API_KEY=MY_GEMINI_API_KEY" > .env
  fi
  echo "      -> .env initialized."
else
  echo "[2/3] .env file already present."
fi

# 3. Grant execute permissions to gradlew
if [ -f "gradlew" ]; then
  echo "[3/3] Granting execute permissions to gradlew..."
  chmod +x gradlew
fi

echo ""
echo "Setup completed successfully!"
echo "You can now run:"
echo "  ./gradlew :app:testDebugUnitTest   # Run unit tests"
echo "  ./gradlew :app:assembleDebug       # Build debug APK"
echo "=========================================="
