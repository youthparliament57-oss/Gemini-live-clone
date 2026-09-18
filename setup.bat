@echo off
REM Gemini Live Android - Quick Setup Script for Windows
REM Restores required local files (debug.keystore and .env) before building.

echo ==========================================
echo    Gemini Live - Project Setup Script    
echo ==========================================

REM 1. Restore debug.keystore from debug.keystore.base64
if exist debug.keystore.base64 (
  if not exist debug.keystore (
    echo [1/2] Restoring debug.keystore from debug.keystore.base64...
    certutil -decode debug.keystore.base64 debug.keystore >nul 2>&1
    echo       debug.keystore restored.
  ) else (
    echo [1/2] debug.keystore already exists.
  )
)

REM 2. Setup .env file
if not exist .env (
  if exist .env.example (
    echo [2/2] Copying .env.example to .env...
    copy .env.example .env >nul
    echo       .env initialized.
  )
) else (
  echo [2/2] .env already exists.
)

echo.
echo Setup completed successfully!
echo You can now build the project:
echo   gradlew.bat :app:testDebugUnitTest
echo   gradlew.bat :app:assembleDebug
echo ==========================================
pause
