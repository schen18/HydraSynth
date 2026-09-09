# Hydra Synth Android

An interactive live visual synth editor and runner for Android, powered by [Hydra Visual Synth](https://hydra.ojack.xyz/) (WebGL / WebAudio) in a native Jetpack Compose application.

## Features

- **Live Visual Coding:** Code reactive, analog-style video feedback loops and generative visuals in real-time.
- **Embedded WebGL Engine:** Powered by the official Hydra synth library running via WebGL canvas acceleration.
- **Interactive Camera & Audio Input:** Feed live device camera frames or ambient audio signals (`s0.initCam()`, audio modulation) directly into visual patches.
- **Custom Syntax Highlighting:** Monospace visual code editor with real-time colorization for Hydra sources, outputs, math functions, transforms, and JS keywords.
- **Preset & Custom Script Persistence:** Built-in preset patches and full Room DB support to save, load, and manage custom visual patches locally.
- **Modern Jetpack Compose UI:** Material 3 dark theme, edge-to-edge layout support, smooth IME handling, and reactive architecture.

## Getting Started

### Prerequisites

- [Android Studio Ladybug or newer](https://developer.android.com/studio)
- Android SDK 26+ (Android 8.0 Oreo or higher)

### Build & Run

1. Open Android Studio.
2. Select **Open** and choose this project directory.
3. Allow Gradle to sync dependencies.
4. Run the `:app` configuration on an Android emulator or physical device.

## Project Structure

```
hydrasynth/
├── app/
│   ├── src/main/
│   │   ├── assets/
│   │   │   ├── hydra-synth.js    # Core Hydra Synth JS library
│   │   │   ├── index.html        # WebGL canvas container
│   │   │   └── runner.js         # JS bridge interface & script executor
│   │   ├── java/com/dissonance/wfarer/hydra/
│   │   │   ├── db/               # Room Database (Entity, DAO, Repository)
│   │   │   ├── ui/               # Jetpack Compose UI & ViewModel
│   │   │   └── MainActivity.kt   # App Entrypoint & WebView Bridge
│   │   └── res/                  # Android Resources
```

## Architecture & Tech Stack

- **UI Framework:** Jetpack Compose (Material 3)
- **Database:** Room (KSP / Kotlin Coroutines Flow)
- **Engine:** Android WebView + WebGL Web Assembly/JS (Hydra Engine)
- **Language:** Kotlin 2.2+
