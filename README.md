# EvilGPS

EvilGPS is an Android app for mock location testing on standard Android devices.

## Features

- Static mock location selection from search or map
- Travel mode with routed movement simulation
- Foreground service that restores active sessions after task removal
- OpenStreetMap search and map-based location picking

## Requirements

- Android Developer Options enabled
- Mock location app set to EvilGPS
- Internet connection for place search and route lookup

## Build

```bash
./gradlew :app:assembleDebug
```

On Windows:

```bat
gradlew.bat :app:assembleDebug
```

## Install

```bat
gradlew.bat installDebug
```

## Notes

- This project is intended for testing, prototyping, and development workflows.
- Search uses Nominatim and route lookup uses OSRM public endpoints.

## Author

Developed by Charan.

## License

MIT. See [LICENSE](LICENSE).