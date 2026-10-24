# Luma Music Universal — cross-platform starter

This is a **starter source project**, not prebuilt installers. It uses Flutter/Dart to share the main UI across Android, Windows and Linux.

## Current starter features
- Minimalist Luma Music UI with light/dark mode and accent-color selection.
- Add local audio files through the file picker.
- Play local audio and direct HTTPS audio URLs.
- Search the in-session library.
- Basic accumulated playback-time statistic saved locally.
- Import screen placeholders for Spotify, SoundCloud and Yandex Music.

## Not implemented yet
- Real OAuth/API integrations for Spotify, SoundCloud or Yandex Music.
- Downloading protected streaming tracks or extracting audio from service URLs.
- Native Android home-screen widget and full background media controls.
- A true Windows installer or Linux AppImage/DEB package.
- Automatic syncing of libraries across devices.

A service's playlist URL is not the same as a direct audio stream. Provider integration requires supported APIs, developer credentials and permission checks. Availability without a VPN depends on the provider and region; this app does not bypass geo-restrictions.

## Build prerequisites
Install Flutter SDK and a platform toolchain. Official setup:
https://docs.flutter.dev/get-started/install
Platform support reference:
https://docs.flutter.dev/reference/supported-platforms

### Get dependencies
From the project root:
```bash
flutter pub get
```

### Android APK (ARMv7 + ARM64)
```bash
flutter build apk --release --target-platform android-arm,android-arm64
```
Output: `build/app/outputs/flutter-apk/app-release.apk`

For separate smaller APKs:
```bash
flutter build apk --release --split-per-abi
```
Outputs for `armeabi-v7a` and `arm64-v8a` are created separately. Android 7.0+ can run this project with compatible Flutter SDK/tooling and plugin versions.

### Windows
Build on a Windows machine:
```powershell
flutter config --enable-windows-desktop
flutter create --platforms=windows .
flutter pub get
flutter build windows --release
```
Output is in `build/windows/x64/runner/Release/`. Package the output folder with an installer tool separately.

### Linux 64-bit
Build on Linux:
```bash
flutter config --enable-linux-desktop
flutter create --platforms=linux .
flutter pub get
flutter build linux --release
```
Output is in `build/linux/x64/release/bundle/`. Package as AppImage/DEB separately.

## Compatibility caveats
Current Flutter officially supports Windows 10/11, not Windows 7/8, and its current Linux desktop support is for 64-bit targets. Official platform matrix:
https://docs.flutter.dev/reference/supported-platforms
For Windows 7/8 or 32-bit Linux, a separate legacy build based on an older toolchain or a different framework may be necessary; this project does not claim those targets are tested or supported.

## Recommended next work
1. Generate native platform folders with `flutter create --platforms=android,windows,linux .`
2. Build and smoke-test each target.
3. Implement provider API/OAuth integrations one at a time.
4. Add native Android widget/background media session.
5. Add permitted downloads, persistence, artwork and robust playback-state tracking.
6. Package desktop builds into platform-specific installers.
