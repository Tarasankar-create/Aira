# Aira – Offline Android Voice Assistant

Aira is a fully offline, private personal voice assistant for Android built with Kotlin, Jetpack Compose Material 3, Vosk speech recognition, custom offline wake-word detection, and an extensible deterministic command-processing engine.

---

## 🌟 Key Features

1. **100% On-Device & Private**:
   - Zero telemetry, zero cloud dependencies, zero audio recording storage.
   - All acoustic keyword spotting and continuous speech-to-text processing execute entirely on your device.

2. **Custom Offline Wake-Word Detection ("Hey Assistant")**:
   - Runs locally without requiring Picovoice accounts, paid tokens, or proprietary services.
   - Uses a dedicated, separate acoustic decoding engine constrained strictly to the keyword grammar (`["hey assistant", "[unk]"]`), minimizing CPU/RAM usage and avoiding false positives.

3. **Vosk Offline Speech-to-Text**:
   - Out-of-the-box support for `vosk-model-small-en-us-0.15` (~40MB), specifically tuned for mobile efficiency and 4GB RAM devices.
   - Extensible architecture supporting larger graph models (`vosk-model-en-us-0.22-lgraph`) with in-app download and manual placement options.

4. **Deterministic Command Engine**:
   - Fast, reliable intent matching using regex patterns, synonym dictionaries, and Android system APIs.
   - Dynamic package launcher with launcher activity discovery and `<queries>` declarations.

5. **Android 14+ Compliant Foreground Microphone Service (`AssistantService`)**:
   - Continues listening seamlessly when you switch applications, return to the home screen, or close the main activity.
   - Features a persistent notification with a direct "Stop Listening" quick action.
   - Proper task-removal behavior (`onTaskRemoved`) ensuring foreground listening persists across recent app swipes.

6. **10-Minute Inactivity Timeout**:
   - Service-managed coroutine countdown that resets on every wake-word detection or executed command.
   - Automatically stops the service and releases microphone hardware after 10 minutes (configurable in Settings) of inactivity to protect battery life.

7. **Audio Session & Self-Trigger Prevention**:
   - Microphone input is automatically suppressed while Android TextToSpeech is speaking responses ("Yes, I'm listening.", "Opening YouTube."), preventing the assistant from triggering itself.

8. **Modern Material 3 Interface**:
   - Futuristic dark-themed UI with an animated, amplitude-reactive Assistant Orb.
   - Full conversation history with local persistence and clear-history controls.
   - Comprehensive Settings screen for model management, speech speed, timeouts, and permissions.

---

## 🎙️ Supported Voice Commands

### Application Launching
- *"Open WhatsApp"*
- *"Open YouTube"*
- *"Open Chrome"*
- *"Open Settings"*
- *"Open Camera"*
- *"Open Calculator"*
- *"Open Phone"* / *"Open Dialer"*
- *"Open Messages"*
- *"Open Contacts"*
- *"Open Gmail"*
- *"Open Gallery"*
- *"Launch [Any Installed App Name]"*

### Phone Hardware & Settings Actions
- **Flashlight**: *"Turn on flashlight"*, *"Torch on"*, *"Turn off flashlight"*, *"Torch off"*
- **Battery**: *"What is the battery level?"*, *"Check battery"*, *"Battery percentage"*
- **Settings Shortcuts**:
  - *"Open Wi-Fi settings"*
  - *"Open Bluetooth settings"*
  - *"Open mobile network settings"*
  - *"Open location settings"*
- **Volume**:
  - *"Volume up"*, *"Increase volume"*, *"Louder"*
  - *"Volume down"*, *"Decrease volume"*, *"Quieter"*
  - *"Mute"*, *"Mute media"*, *"Silence"*
- **Date & Time**:
  - *"What time is it?"*, *"Tell me the time"*
  - *"What date is it?"*, *"Today's date"*
- **Web Navigation**:
  - *"Open website google.com"*, *"Go to wikipedia.org"*
- **Assistant Control**:
  - *"Stop listening"*, *"Turn off listening"*, *"Go to sleep"*

---

## 🏗️ Project Architecture

```
app/
  src/
    main/
      java/com/example/aira/
        AiraApplication.kt                  # Application class & global initialization
        MainActivity.kt                     # Jetpack Compose single-activity entry point

        ui/
          AiraViewModel.kt                  # Unified UI ViewModel
          components/
            AiraOrb.kt                      # Glowing amplitude-reactive visualizer
          home/
            HomeScreen.kt                   # Home dashboard, toggle & status pills
          conversation/
            ConversationScreen.kt           # Interaction history timeline
          settings/
            SettingsScreen.kt               # Model manager, timeout & audio settings
          navigation/
            AiraNavigation.kt               # Material 3 NavigationBar & Screen Scaffold
          theme/
            Color.kt, Theme.kt, Type.kt     # Deep dark futuristic Material 3 theme

        service/
          AssistantService.kt               # Foreground Service with microphone type
          AssistantStateHolder.kt           # Reactive StateFlow communication bridge

        speech/
          AudioRecorder.kt                  # Low-latency 16kHz 16-bit Mono PCM recorder
          WakeWordDetector.kt               # "Hey Assistant" acoustic grammar keyword detector
          SpeechRecognizer.kt               # Full-vocabulary Vosk recognizer with silence detection
          SpeechModelManager.kt             # Model download, extraction & validation

        commands/
          CommandIntent.kt                  # Sealed class of assistant intents
          CommandRegistry.kt                # Deterministic pattern matching & aliases
          CommandProcessor.kt               # Intent dispatcher & response coordinator
          AppLauncher.kt                    # Installed package resolver & launcher
          PhoneActions.kt                   # Flashlight, Battery, Volume, Settings, Clock

        tts/
          TextToSpeechManager.kt            # Android TextToSpeech with utterance lifecycle

        data/
          model/AssistantState.kt           # ListeningState & AssistantStatus data classes
          model/AssistantHistoryItem.kt     # Conversation history item model
          preferences/PreferencesManager.kt # SharedPreferences & configuration storage
          history/HistoryRepository.kt      # History state repository

        utils/
          NotificationHelper.kt             # Foreground service persistent notification
          PermissionManager.kt              # Audio, notification & battery checks

    test/
      java/com/example/aira/
        commands/CommandRegistryTest.kt     # Unit tests for voice command parsing
        speech/SpeechModelValidationTest.kt # Unit tests for model directory validation
        service/AssistantStateTest.kt       # Unit tests for service state transitions
```

---

## 📦 Speech Model Installation

Aira uses offline Vosk models. You can install the model in two ways:

### Option A: Automatic In-App Download (Recommended)
1. Open Aira and navigate to the **Settings** tab.
2. Under **Offline Speech Models**, locate `vosk-model-small-en-us-0.15`.
3. Tap **Get**. Aira will automatically download the ZIP archive from the official Alphacephei repository, extract it into the app's local storage directory, and set it as active.

### Option B: Manual Placement via PC or File Manager
1. Download the model archive:
   - **Small Model (40MB)**: [https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip](https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip)
   - **Large Model (128MB)**: [https://alphacephei.com/vosk/models/vosk-model-en-us-0.22-lgraph.zip](https://alphacephei.com/vosk/models/vosk-model-en-us-0.22-lgraph.zip)
2. Extract the ZIP file. The extracted folder should contain `am/final.mdl`, `conf/mfcc.conf`, `graph/HCLG.fst`, etc.
3. Connect your Android device to your computer via USB (or use an on-device file manager).
4. Copy the folder to:
   ```
   /Android/data/com.example.aira/files/models/vosk-model-small-en-us-0.15/
   ```
5. Open Aira. The Home screen status badge will display **Speech Model: Ready**.

---

## 🔋 Battery Optimization & Background Execution

Android OEM operating systems (such as Samsung OneUI, Xiaomi MIUI/HyperOS, Oppo ColorOS) may terminate background microphone services. To ensure uninterrupted background wake-word detection:

1. In Aira, go to **Settings** → **Permissions & Background**.
2. Tap **Configure** under **Battery Optimization**.
3. Select **Unrestricted** / **Don't Optimize** for Aira.
4. When listening is enabled, Aira maintains an ongoing foreground service with `FOREGROUND_SERVICE_TYPE_MICROPHONE` and an active notification, in accordance with Android 14+ background execution requirements.

---

## 🛠️ Build and Installation Instructions

### Prerequisites
- Android Studio Ladybug / Meerkat (or newer)
- JDK 17 or JDK 21
- Android SDK Platform 34 or 35 (compileSdk 35 / minSdk 26)

### 1. Build Debug APK
Run the following Gradle command from the project root:
```bash
./gradlew assembleDebug
```
The compiled debug APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 2. Install onto Device or Emulator
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

### 4. Build Release APK
To assemble a release build:
```bash
./gradlew assembleRelease
```
*(Note: To sign a release build for production distribution, configure your signing keystore in `app/build.gradle.kts` under `signingConfigs`.)*

---

## 🔒 Privacy & License

- **Privacy**: No audio, transcript, or personal telemetry is transmitted over the network. All processing is 100% local.
- **Model License**: Vosk models are licensed under the Apache License 2.0.
- **Aira Application**: Licensed under the Apache License 2.0.
