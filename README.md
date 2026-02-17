# Android MCP Agent

An Android application that acts as a Model Context Protocol (MCP) server, allowing AI agents to control the device via WebSocket.

## Features
- **WebSocket Server**: Runs on port 8765
- **Accessibility Service**: Performs clicks, scrolls, and text input
- **Secure Pairing**: Time-limited 6-digit codes and auth tokens
- **Action Logging**: Audit trail of all executed commands

## Build Instructions

### GitHub Actions (Recommended)
This project is configured with GitHub Actions to automatically build the APK on push.
1. Push the code to your GitHub repository.
2. Go to the "Actions" tab in your repository.
3. Select the "Android CI" workflow.
4. Download the `app-debug` artifact from the latest run.

### Local Build
Prerequisites:
- JDK 17
- Android SDK
- Gradle 8.11.1 (or use the included wrapper)

```bash
# Using Gradle Wrapper (recommended)
./gradlew assembleDebug

# Or with your local Gradle installation
gradle assembleDebug
```

## Usage
1. **Install & Open**: Install the APK and open the app.
2. **Enable Service**: Go to `Dashboard` -> `Open Accessibility Settings` -> Enable `Android MCP Agent`.
3. **Start Server**: Tap `Start Server` on the Dashboard.
4. **Pair Client**:
   - Go to `Pairing` tab.
   - Tap `Generate Pairing Code`.
   - Send `pair` command from your AI client with the 6-digit code.

## Protocol
See `MCPCommand.kt` for the full list of supported actions and JSON format.
