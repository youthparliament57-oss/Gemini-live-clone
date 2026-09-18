# Gemini Live - Real-Time AI Voice & Vision Android App

A cutting-edge, native Android application built with **Jetpack Compose**, **Material 3**, and **Google Gemini 3.5 Flash**. Experience fluid, low-latency conversational AI with live interactive audio visualizers, real-time camera vision analysis, and session transcripts.

---

## Features

- **Live Voice Interaction**: Low-latency speech input and natural audio response playback using Android Text-to-Speech & SpeechRecognizer.
- **Multimodal Camera Vision**: Capture and analyze camera frames in real-time with Gemini 3.5 Flash.
- **Dynamic Organic Audio Orb**: Fluid procedural visualizer that reacts to user speech and AI speaking states.
- **Gemini API Key Manager**: Built-in dialog to input, test, and securely persist your Gemini API key with live connection verification.
- **Session History & Transcripts**: Room database storage for full conversation transcripts with search and export capabilities.
- **Production Android CI/CD**: Ready-to-use GitHub Actions workflow for automated testing and APK building.

---

## Quick Start & Build Instructions

### Prerequisites
- **JDK**: Java 17 or Java 21 (Temurin recommended)
- **Android SDK**: Platform API 36 (`platforms;android-36`) & Build-Tools 36.0.0
- **Gradle**: 9.3.1 (included via Gradle wrapper `./gradlew`)

### 1. Clone the Repository
```bash
git clone <repository-url>
cd gemini-live
```

### 2. Run Setup Script (Recommended)
This restores the debug signing keystore from `debug.keystore.base64` and initializes your `.env` configuration file:

**On Linux / macOS:**
```bash
chmod +x setup.sh gradlew
./setup.sh
```

**On Windows:**
```cmd
setup.bat
```

### 3. Run Unit & Robolectric Tests
```bash
./gradlew :app:testDebugUnitTest
```
*(On Windows: `gradlew.bat :app:testDebugUnitTest`)*

### 4. Build Debug APK
```bash
./gradlew :app:assembleDebug
```
The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## API Key Configuration

You can configure your Gemini API Key in any of the following ways:

1. **In-App (Recommended)**:
   - Tap the **Key icon** in the top bar of the app.
   - Enter your key (obtained from [Google AI Studio](https://aistudio.google.com/app/apikey)).
   - Tap **Test Key** to verify connectivity with Gemini 3.5 Flash, then tap **Save Key**.

2. **Environment File (`.env`)**:
   - Create or edit `.env` in the project root:
     ```properties
     GEMINI_API_KEY=AIzaSy...
     ```
   - The app's `Secrets Gradle Plugin` automatically injects this into `BuildConfig.GEMINI_API_KEY`.

3. **Google AI Studio Secrets Panel**:
   - In Google AI Studio Build, configure `GEMINI_API_KEY` under the Secrets panel.

---

## GitHub Actions CI/CD

This repository includes a production-ready GitHub Actions workflow at [`.github/workflows/android.yml`](.github/workflows/android.yml).

On every push or pull request to `main` or `master`:
- Sets up JDK 17 and Android SDK 36
- Restores the debug keystore to prevent signing errors
- Runs all unit and Robolectric tests
- Assembles the debug APK and publishes artifacts for download

---

## Signing & Keystore Notes

- **Debug Signing**: The project includes `debug.keystore.base64` which encodes the standard Android debug certificate (`storePassword = "android"`, `keyAlias = "androiddebugkey"`). The CI pipeline and `setup.sh` automatically decode this to `debug.keystore` to prevent any signing failures.
- **Release Signing**: Configured via environment variables `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` in `app/build.gradle.kts`.

---

## Tech Stack & Architecture

- **UI**: Jetpack Compose, Material 3, Dynamic Color
- **Architecture**: MVVM with AndroidViewModel & Kotlin Coroutines StateFlow
- **AI API**: Google Gemini 3.5 Flash via Retrofit & OkHttp
- **Local Persistence**: Android Room Database (KSP) & SharedPreferences
- **Camera**: AndroidX CameraX (Camera2, Lifecycle, PreviewView)
- **Testing**: Robolectric, JUnit 4, Roborazzi
