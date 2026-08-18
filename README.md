# Android AI MCP

Android AI MCP is a personal Android automation app that uses Accessibility APIs and cloud LLM planning, with mandatory preview/confirmation before execution.

## Core flow
1. Configure provider keys and model IDs in Setup.
2. Enable Accessibility service.
3. Enter command manually, by voice wake, or from a saved template.
4. Generate plan (validated against configured max steps).
5. Review preview and confirm execution.
6. Track runs in Logs.

## Providers and models
- OpenRouter (strict free-model policy)
- NVIDIA NIM

Model ID selection is runtime-configurable:
- OpenRouter model ID: stored in app settings (`openRouterModelId`)
- NVIDIA model ID: stored in app settings (`nvidiaModelId`)

OpenRouter free enforcement:
1. App fetches OpenRouter model catalog and filters models with zero prompt + completion price.
2. If catalog is unavailable, only `:free` model IDs are allowed.
3. If catalog is available and selected model is not free, plan generation is blocked.

## Voice wake
- On-device Android SpeechRecognizer
- Wake word configurable (default `AI`)
- Voice service runs as a foreground microphone service when enabled
- Voice commands still require in-app preview confirmation before execution

## Templates
- Save command + provider + model + limits as reusable templates
- Run templates repeatedly from Templates tab

## Credential vault
- Encrypted password storage with Android Keystore AES-GCM
- Match by `appPackage + fieldHint + accountHint`
- Biometric session unlock required before fills
- `fill_saved_password` steps require explicit runtime approval
- No plaintext passwords are written to logs

## Safety controls
- Mandatory plan preview before execution
- Stop execution button
- Manual handoff mode when secure/blocked screens are detected
- Configurable max step limit (`1-50`, default `20`)
- Sequential execution with delay (default `700ms`)

## Build requirements
- JDK 17
- Android SDK (compile/target SDK 36)
- Gradle wrapper in repo

## Local build
```bash
./gradlew :app:assembleDebug
```

## Build inputs (CI contract)
- `-PciVersionCode=<int>`: overrides `defaultConfig.versionCode` for CI builds.
- `-PciSignDebug=true|false`: signs debug with stable keystore when `true` and signing config exists.

## Release-signed build setup (local)
Copy `keystore.properties.example` to `keystore.properties` in project root, or set equivalent environment variables.

Expected values:
- `storeFile`
- `storePassword`
- `keyAlias`
- `keyPassword`

Then run:
```bash
./gradlew :app:assembleRelease
```

## GitHub Actions signing secrets
The workflow requires these repository secrets:
- `KEYSTORE_BASE64` (raw base64 string only, not `KEYSTORE_BASE64=...`)
- `KEY_ALIAS`
- `KEY_PASSWORD`
- `STORE_PASSWORD`

## CI artifact behavior
- Release APK package: `com.android.ai.mcp`
- Debug APK package: `com.android.ai.mcp.debug`
- CI sets `versionCode` from GitHub run number to keep updates monotonic.
- CI builds and publishes both signed debug and signed release APKs.

## Install guidance (Samsung S25 / Android 16)
- Use the signed release APK for normal installs and updates.
- Use the signed debug APK for side-by-side testing only.
- Enable install from unknown apps for the installer app you use.

One-time migration if older conflicting installs exist:
1. Uninstall `com.android.ai.mcp`
2. Uninstall `com.android.ai.mcp.debug`
3. Install the latest signed release APK

## Android install triage script (Windows)
Use this script to capture exact `INSTALL_FAILED_*` output and apply deterministic fixes:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\triage-android-install.ps1 -ApkPath "C:\path\to\app-release.apk"
```

If you hit `INSTALL_FAILED_UPDATE_INCOMPATIBLE` and want automatic cleanup across users:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\triage-android-install.ps1 -ApkPath "C:\path\to\app-release.apk" -ApplyFix
```

Notes:
- Script checks `com.android.ai.mcp` and `com.android.ai.mcp.debug`.
- Pass `-AdbPath "C:\path\to\adb.exe"` if `adb` is not on PATH.