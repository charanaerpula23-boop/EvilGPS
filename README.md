# EvilGPS

[**Download APK (v1.0 - ARM64, 9.4 MB)**](https://github.com/charanaerpula23-boop/EvilGPS/releases/download/v1.0/EvilGPS-v1.0-arm64.apk)

EvilGPS is an Android mock location app for testing location-dependent behavior on standard Android phones. It supports both fixed spoofed coordinates and route-based travel simulation, with search, map picking, and a foreground service that keeps sessions alive more reliably.

## What It Does

- Pick a single mock location from the map or search and keep the device pinned there
- Simulate travel between two locations along a road route
- Search places by name with Android Geocoder (Google Play Services)
- Select locations visually on a map picker
- Continue active mock sessions through a foreground service
- Restore active sessions after task removal or process restart

## Feature Summary

- Static mock mode
- Travel mode with route interpolation
- Place search with location suggestions
- Map-first location selection flow
- White / black / pink Material 3 UI
- Custom map selection pin
- Session persistence for active mock runs

## How It Works

EvilGPS uses Android's mock location testing path through `LocationManager` test providers. The app registers itself as a mock location app through Developer Options, then publishes fake GPS/network locations from a foreground service.

Travel mode fetches a road route and emits gradual position updates over time to simulate movement instead of jumping directly from point A to point B.

## Requirements

- Android 10+ (`minSdk 29`)
- Developer Options enabled on the device
- EvilGPS selected in `Developer Options > Select mock location app`
- Internet connection for place search and route lookup

## Setup On Device

1. Install the app.
2. Open Android settings.
3. Enable Developer Options if not already enabled.
4. Go to `Developer Options > Select mock location app`.
5. Choose `EvilGPS`.
6. Open the app and pick a location or route.

## Usage

### Static Mock

1. Search for a location or tap the location card to open the map.
2. Pick the target point.
3. Adjust accuracy if needed.
4. Tap `Start Mocking`.
5. Tap `Stop Mocking` to end the session.

### Travel Mode

1. Choose a start location.
2. Choose an end location.
3. Set the travel speed.
4. Tap `Fetch Route via Road`.
5. Tap `Start Travel`.
6. The app will move the reported device location along the route.

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Android foreground service
- osmdroid for the map view
- Android Geocoder for location search (Nominatim fallback)
- OSRM public API for road routing

## Project Structure

- `app/src/main/java/com/charanhyper/tech/greydailer/`: core app logic, service, view models, and screens
- `app/src/main/java/com/charanhyper/tech/greydailer/ui/`: Compose UI screens and shared UI components
- `app/src/main/java/com/charanhyper/tech/greydailer/ui/theme/`: app theme, colors, and typography
- `app/src/main/res/`: Android resources, strings, icons, and XML config

## Build

### Windows

```bat
gradlew.bat :app:assembleDebug
```

### macOS / Linux

```bash
./gradlew :app:assembleDebug
```

## Install

### Windows

```bat
gradlew.bat installDebug
```

### macOS / Linux

```bash
./gradlew installDebug
```

## Development Notes

- Search results come from Android's built-in Geocoder (Google Play Services) with Nominatim as fallback.
- Route generation depends on the public OSRM endpoint.
- Active sessions are persisted and restored by the foreground service.
- The current package name still uses the original internal namespace: `com.charanhyper.tech.greydailer`.

## Open Source

This repository is open source under the MIT license. Contributions, bug reports, and cleanup PRs are welcome.

## Author

Developed by Charan.

## License

MIT. See [LICENSE](LICENSE).