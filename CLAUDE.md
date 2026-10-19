# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

OghmAI is an Android vocabulary learning app built with Kotlin and Jetpack Compose. It's a companion to the [OghmAI backend service](https://github.com/gASK13/oghmai-server/) and serves as a proof-of-concept for AI-powered language immersion.

## Development Environment

**This is a Windows development environment.** Standard bash commands may not work. Use Windows-compatible commands or ask before assuming Unix tools are available.

### Build Commands

```bash
# Build the project (Windows)
gradlew.bat build

# Run on connected device/emulator
gradlew.bat installDebug

# Run tests
gradlew.bat test

# Clean build
gradlew.bat clean
```

### Configuration Setup

The app requires `local.properties` file in the project root with the following keys:
```properties
api.endpoint=<backend-api-url>
cognito.client.id=<aws-cognito-client-id>
cognito.pool.id=<aws-cognito-pool-id>
cognito.region=<aws-region>
```

These are loaded in `app/build.gradle.kts` and exposed via `BuildConfig` for use in the app. This file is gitignored to keep secrets out of version control.

## Architecture

### Authentication Flow

The app uses AWS Cognito for authentication, managed by `AuthManager` (singleton):
- `AuthManager.initialize()` must be called on app startup (handled in `MainActivity.onCreate`)
- Session persistence is automatic via AWS Mobile Client
- The `AuthInterceptor` attaches auth tokens to all API requests via Retrofit
- Navigation guards in `MainActivity` redirect unauthenticated users to `LoginScreen`

### API Layer

- **RetrofitInstance**: Singleton that creates the Retrofit client with auth and error handling interceptors
- **ApiService**: Retrofit interface defining all backend endpoints
- All API calls are suspend functions and should be called from coroutines
- The base URL is loaded from `BuildConfig.API_ENDPOINT`

### Data Models

All data models are defined in `model/Model.kt`:
- **WordResult**: Complete word information with meanings, test history, and status
- **WordStatus enum**: UNSAVED, NEW, LEARNED, KNOWN, MASTERED
- **WordTypeEnum**: NOUN, VERB, PRONOUN, ADJECTIVE, OTHER
- **TestChallenge/TestResult**: Used for knowledge testing features
- **MatchChallenge**: Used for matching word-translation pairs

### Navigation Structure

Navigation is implemented in `MainActivity.OghmAINavHost()` using Jetpack Navigation Compose:
- Start destination determined by authentication state
- Main screens: login, main (menu), describeWord, listWords, wordDetail/{word}, settings, testWords, matchChallenge, explainTenses/{word}
- Custom slide animations for screen transitions
- Auth validation on main screen navigation

### Screen Organization

All UI screens are in the `ui` package:
- **WordDiscoveryScreen**: Unified screen for word lookup via description or direct word input
- **WordListingScreen**: Browse saved words with filtering by status and search
- **WordDetailScreen**: View detailed word information with meanings and examples
- **ChallengeScreen**: Test knowledge with AI-generated challenges
- **MatchChallengeScreen**: Match words with translations
- **ExplanationScreen**: Generic screen for explanations (e.g., verb tenses)
- **SettingsScreen**: App settings
- Reusable components in `ui/components/`

### Text-to-Speech Integration

`TextToSpeechWrapper` service handles pronunciation:
- Initialized in `MainActivity` and passed to screens that need it
- Must be shut down in `MainActivity.onDestroy()`
- Used primarily for reading Italian words aloud

### State Management

Compose state is managed locally in screens using:
- `remember` and `mutableStateOf` for UI state
- `rememberSaveable` for state that survives configuration changes
- `LaunchedEffect` for side effects and API calls
- Coroutines launched with `rememberCoroutineScope()`

## Common Patterns

### Making API Calls

```kotlin
val coroutineScope = rememberCoroutineScope()
var result by remember { mutableStateOf<WordResult?>(null) }
var isLoading by remember { mutableStateOf(false) }

coroutineScope.launch {
    isLoading = true
    try {
        val response = RetrofitInstance.apiService.getWord(word)
        result = response
    } catch (e: Exception) {
        Log.e("Tag", "Error", e)
        // Handle error
    } finally {
        isLoading = false
    }
}
```

### Navigation

```kotlin
// Navigate to a new screen
navController.navigate("wordDetail/$word")

// Navigate back
navController.popBackStack()

// Navigate and clear back stack
navController.navigate("main") {
    popUpTo(navController.graph.startDestinationId) { inclusive = true }
}
```

### Using ScaffoldWithTopBar

Most screens use the custom `ScaffoldWithTopBar` component which provides consistent navigation:
```kotlin
ScaffoldWithTopBar(
    title = "Screen Title",
    isMainScreen = false,  // true to show app icon instead of back button
    showOptionsMenu = true,  // optional menu
    optionsMenuItems = listOf(...)
) { padding ->
    // Screen content
}
```

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material3
- **Navigation**: Navigation Compose
- **Networking**: Retrofit + Gson
- **Authentication**: AWS Cognito (AWS Mobile Client SDK)
- **Min SDK**: 24, Target SDK: 34, Compile SDK: 35

## Development Notes

- Most code initially generated via AI ("vibe coding" approach per README)
- Backend uses FastAPI with AWS Lambda, DynamoDB, and Amazon Bedrock (LLMs)
- Target language for learning is Italian (though architecture could support others)
