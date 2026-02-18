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

## Release-signed build setup
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
