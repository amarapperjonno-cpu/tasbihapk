# দৈনিক আমল ও মোনাজাত — Android APK Project

Target URL: https://amarapperjonno-cpu.github.io/tasbih/
Package ID: `io.github.amarapperjonno.tasbih`
Version: `1.0.0` (code `1`)
Android SDK: Min API 24 · Target API 35

---

## Fastest Way to Get Your `.apk` File (No PC / No Android Studio Needed)

This project includes a pre-configured GitHub Actions workflow (`.github/workflows/build-apk.yml`) that compiles a ready-to-install `.apk` file automatically in the cloud (~90 seconds).

1. Create a new repository on GitHub (e.g. `tasbih-apk`).
2. Extract this ZIP and upload all files (including the `.github` folder) to the repository.
3. Open the **Actions** tab on GitHub — the **Build Android APK** workflow starts automatically.
4. When it finishes (green checkmark), open **Releases** (or the workflow **Artifacts**) and download `Tasbih-v1.0.0.apk` straight to your Android phone.
5. Tap the downloaded `.apk` file on your phone and allow **Install unknown apps** when prompted.

---

## Build Locally with Android Studio

1. Open this extracted folder in **Android Studio** (Ladybug or newer).
2. Wait for Gradle Sync to finish.
3. Click **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**.
4. Find the compiled APK at `app/build/outputs/apk/debug/app-debug.apk`.

## Build via Command Line (Gradle)

```bash
gradle assembleDebug
```
