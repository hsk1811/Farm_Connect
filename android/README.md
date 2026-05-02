# FarmConnect Android App

Native Android application for the Contract Farming System.

## Technology Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Hilt
- **Networking**: Retrofit + OkHttp
- **Local Storage**: DataStore Preferences + Room (for offline caching)
- **Navigation**: Jetpack Navigation Compose

## Project Structure

```
app/src/main/java/com/farmconnect/app/
├── FarmConnectApp.kt         # Hilt Application
├── MainActivity.kt           # Main entry point
├── data/
│   ├── api/
│   │   └── ApiService.kt     # Retrofit API interface
│   ├── local/
│   │   └── AuthPreferences.kt # DataStore preferences
│   ├── models/
│   │   └── Models.kt         # Data classes
│   └── repository/
│       └── Repositories.kt   # Repository implementations
├── di/
│   └── NetworkModule.kt      # Hilt DI module
├── ui/
│   ├── components/
│   │   └── Components.kt     # Reusable UI components
│   ├── navigation/
│   │   └── Navigation.kt     # Navigation setup
│   ├── screens/
│   │   ├── AuthScreens.kt    # Login, Register
│   │   ├── DashboardScreens.kt # Farmer/Buyer dashboards
│   │   ├── ListingScreens.kt # Listing management
│   │   └── CommonScreens.kt  # Negotiations, Contracts
│   └── theme/
│       ├── Color.kt          # Color palette
│       ├── Shape.kt          # Shape definitions
│       ├── Theme.kt          # Material 3 theme
│       └── Type.kt           # Typography
└── viewmodel/
    ├── AuthViewModel.kt      # Authentication state
    └── ViewModels.kt         # Feature ViewModels
```

## Building the APK

### Prerequisites

1. Android Studio Hedgehog or later
2. JDK 17 or later
3. Android SDK 34

### Build Steps

```bash
# Navigate to android directory
cd android

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing)
./gradlew assembleRelease
```

The APK will be generated at:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## Configuration

### API Base URL

The API base URL is configured in `app/build.gradle.kts`:

```kotlin
// Debug (uses Android emulator localhost)
buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:3000/api/\"")

// Release (update with your production URL)
buildConfigField("String", "API_BASE_URL", "\"https://your-api.com/api/\"")
```

## Features

### Farmer Flow
- ✅ Dashboard with stats
- ✅ Create/Edit/Manage listings
- ✅ View negotiations
- ✅ Manage contracts
- ✅ Track fulfillment
- ✅ Record payments

### Buyer Flow
- ✅ Browse listings with search
- ✅ Save favorites
- ✅ Initiate negotiations
- ✅ Confirm contracts
- ✅ Track deliveries
- ✅ Record payments

### Common Features
- ✅ JWT-based authentication
- ✅ Role-based navigation
- ✅ Status indicators
- ✅ Error handling
- ✅ Loading states
- ✅ Empty states
