# Lightweight developer setup (no Android Studio)

This project builds and runs with the Android **command-line** toolchain only. Android Studio works too, but is optional and much heavier. Expo does not apply — this is a native Kotlin app, not React Native.

You need three things: **JDK 17**, the **Android command-line tools**, and either a **USB phone** or the **standalone emulator**.

## macOS (Apple Silicon or Intel)

```bash
brew install openjdk@17 android-commandlinetools

# add to ~/.zshrc
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-36" "build-tools;35.0.0"
```

Emulator (skip if you'll use a real phone — a phone is the lightest option):

```bash
sdkmanager "emulator" "system-images;android-35;google_apis;arm64-v8a"   # Intel Mac: x86_64 instead of arm64-v8a
avdmanager create avd -n senior -k "system-images;android-35;google_apis;arm64-v8a" -d pixel_6
```

## Windows

1. Install [Temurin JDK 17](https://adoptium.net).
2. Download **"Command line tools only"** from the bottom of <https://developer.android.com/studio> and unzip so the tools live at `C:\Android\cmdline-tools\latest\`.
3. In PowerShell:

```powershell
setx ANDROID_HOME C:\Android
setx PATH "%PATH%;C:\Android\platform-tools;C:\Android\emulator"
C:\Android\cmdline-tools\latest\bin\sdkmanager --licenses
C:\Android\cmdline-tools\latest\bin\sdkmanager "platform-tools" "platforms;android-36" "build-tools;35.0.0"
```

Emulator (optional; enable the "Windows Hypervisor Platform" Windows feature for speed):

```powershell
C:\Android\cmdline-tools\latest\bin\sdkmanager "emulator" "system-images;android-35;google_apis;x86_64"
C:\Android\cmdline-tools\latest\bin\avdmanager create avd -n senior -k "system-images;android-35;google_apis;x86_64" -d pixel_6
```

## Real phone instead of an emulator

Enable **Developer Options → USB debugging** on any Android phone (Android 6+), plug it in, accept the prompt, and confirm it appears in `adb devices`. All install/launch commands below work identically.

## Build, install, launch

```bash
./gradlew :app:assembleDebug          # Windows: gradlew.bat :app:assembleDebug
emulator -avd senior &                # skip when using a phone
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n org.seniorconnect.app/.MainActivity
```

You should see "What would you like to do?" with four large buttons. They have no behavior yet by design.

## Debugging

```bash
adb logcat | grep -i anagentforelders     # live logs
adb exec-out screencap -p > app.png       # screenshot
```
