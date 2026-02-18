# Android AI MCP

Android AI MCP is a personal Android app that uses Accessibility APIs and a cloud LLM planner to execute user-confirmed UI action plans.

## Core flow
1. Add provider API keys in Setup.
2. Enable Accessibility service.
3. Enter a natural-language command.
4. Generate an AI action plan.
5. Review preview and confirm execution.
6. Track results in Logs.

## Providers
- OpenRouter: `moonshotai/kimi-k2.5`
- NVIDIA NIM: `moonshotai/kimi-k2.5`

Provider selection is manual in-app, and the selected provider key is required.

## Safety controls
- Mandatory plan preview before execution
- Stop execution button
- Configurable max step limit in settings (`1-50`, default `20`)
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
- `KEYSTORE_BASE64`
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
