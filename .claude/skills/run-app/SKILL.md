---
name: run-app
description: Build, install, and launch the Android prototype using only the command-line toolchain (no Android Studio). Use when asked to run, debug, or screenshot the app.
---

# Run the Android prototype (CLI only)

This app is native Android (Kotlin + Gradle). It cannot run under Expo, Node, or a browser — it needs an emulator or a USB-connected phone. Android Studio is **not** required; the command-line toolchain below is the supported path.

## Prerequisites check

Verify before building; if missing, follow `docs/DEV_SETUP.md`:

```bash
java -version          # need JDK 17
echo $ANDROID_HOME     # SDK root (macOS Homebrew: /opt/homebrew/share/android-commandlinetools)
adb devices            # platform-tools on PATH
```

## Build

```bash
./gradlew :app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`. Gradle auto-downloads missing SDK platforms (compileSdk 36) once licenses are accepted.

## Run target

Prefer a device already listed by `adb devices`. Otherwise boot the shared AVD (create it per `docs/DEV_SETUP.md` if absent):

```bash
emulator -avd senior -no-snapshot -no-audio &
adb wait-for-device
adb shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done'
```

## Install and launch

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n org.seniorconnect.app/.MainActivity
```

## Verify / debug

```bash
adb exec-out screencap -p > /tmp/app.png        # screenshot; expect 4 buttons: CALL, YOUTUBE, SPEAK, CAMERA
adb logcat -d | grep -i anagentforelders        # app logs
```

The buttons intentionally have no behavior yet (see AGENTS.md scope). A tap that does nothing is not a bug.
