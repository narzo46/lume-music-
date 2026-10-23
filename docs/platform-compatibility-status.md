# Platform compatibility status

## Android
- Minimum version: Android 7.0
- Architectures: ARMv7, ARM64, x86/x64 where feasible
- Build format: APK / AAB
- CI status: Android APK build checked in GitHub Actions
- Notes: Compatibility is validated only after successful build and runtime checks.

## Windows
- Minimum version: Windows 7
- Architectures: x86, x64, ARM64 where feasible
- Build format: EXE / MSI
- CI status: Windows release build checked in GitHub Actions
- Notes: Old Windows versions require separate compatibility checks and dependencies.

## Linux
- Supported distros: Ubuntu, Debian, Fedora, Linux Mint, Arch Linux
- Architectures: x86, x64, ARM64
- Build format: AppImage, DEB, RPM, Flatpak
- CI status: Linux build job prepared for validation in GitHub Actions
- Notes: Package-specific compatibility depends on system libraries and runtime environment.

## Policy
- A platform is considered supported only after successful build and test validation.
- Security and stability are prioritized over compatibility with obsolete systems.
- Separate builds for legacy and modern environments should be maintained where required.
