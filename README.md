# OpenYi

### A companion app for Yi Action Camera.

OpenYi is a cross-platform alternative application for the Yi Action Camera. It is designed to be a modern replacement for the official Yi Action app on modern operating systems, with extended functionality for deep control of the camera.

> [!WARNING]
>
> **OpenYi** has features that allow direct interfacing with Ambarella SoC's internal partition. While most settings should be safe, direct FTP access or saving untested configurations to internal memory may cause your camera to irreversibly bootloop.
>
> Appropriate warnings are displayed when accessing potentially unsafe features. **Safety of your camera lies in your hands to use those features carefully**.
>
> **The developer assumes no liability for any damage caused by the use of this application.**

## Features

### Basic

- Live preview
- Status monitoring (battery, SD card)
- Remote control
- Self-timer
- Media management
- SD card formatting
- Camera lights on/off

### Extended

- Direct filesystem access
- Advanced logging
- Persistent configurations
- Loadable camera presets (profiles)
- Button remapping and keybinding
- ISO and shutter speed control
- Image processing and manipulation
- Resolution and bitrate adjustment
- RAW file format
- STA network mode

...and much more.

> [!IMPORTANT]
>
> Extended features are accessible after enabling telnet on your camera. This is done by placing an empty file named `enable_info_display.script` (Yi Action 1080p) or `console_enable.script` (Yi Action 4K/4K+) in the root directory of the SD card.
>
> The app will guide you through this process during onboarding and in settings.

## Compatibility

### Full

- Yi Action Camera 1080p (firmware 1.2.13)

### Partial

- Yi Action Camera 1080p (other firmware versions)
- Yi Action Camera 4K
- Yi Action Camera 4K+

> [!NOTE]
> Partial support excludes support for some advanced features that rely on memory injection, such as bitrate and resolution control. Basic features and most of the extended features should work fine.

**Full support for all Yi Action Cameras across all firmware versions is a goal and will be extended in future versions of the software. Contributions are very much welcome.**

While not a primary goal, other cameras based on the Ambarella SoCs (like older GoPro Hero models) are very much welcome to be supported via community contributions.

## Building

> [!TIP]
>
> Use `./gradlew` in bash/zsh (Linux, Mac, BSD etc.)
>
> Use `gradlew.bat` in Windows.

### Android

#### Prerequisites

- **JDK 17** or higher (JDK 21 recommended)
- **Android SDK** (API `37` platforms and build-tools)
- `ANDROID_HOME` environment variable set to your Android SDK location (or `sdk.dir` defined in `local.properties`)

#### Commands

- **Build Debug APK:**
  ```bash
  ./gradlew :androidApp:assembleDebug
  ```
  Output: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`

- **Build Release APK:**
  ```bash
  ./gradlew :androidApp:assembleRelease
  ```
  Output: `androidApp/build/outputs/apk/release/androidApp-release-unsigned.apk`

- **Install & run on a device/emulator:**
  ```bash
  ./gradlew :androidApp:installDebug
  ```

### iOS

#### Prerequisites

- **macOS** with **Xcode** (and Xcode Command Line Tools)
- **JDK 17** or higher (JDK 21 recommended)
- (Required for physical device) Apple Developer Team ID set in `iosApp/Configuration/Config.xcconfig` (`TEAM_ID`)

#### Commands

- **Build for iOS Simulator:**
  ```bash
  xcodebuild -project iosApp/iosApp.xcodeproj \
    -scheme iosApp \
    -configuration Debug \
    -destination 'generic/platform=iOS Simulator' \
    build
  ```

- **Build Release for iOS Device:**
  ```bash
  xcodebuild -project iosApp/iosApp.xcodeproj \
    -scheme iosApp \
    -configuration Release \
    -destination 'generic/platform=iOS' \
    build
  ```

- **Build shared Kotlin framework directly:**
  ```bash
  # Simulator (ARM64)
  ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

  # Device (ARM64)
  ./gradlew :shared:linkDebugFrameworkIosArm64
  ```

## Contributing

**Thank you for contributing to OpenYi!**

Contribution guidelines are minimal and mostly handled by Git hooks.

### Requirements:

- **Keep changes minimal**: clear, simple code with one feature or fix per pull request.
- **Maintain UI integrity**: frontend changes must conform to the existing visual language and layout standards.
- **Ensure stability**: code must build cleanly and pass all automated tests and validation checks.
- **Commit messages**: use imperative mood (`Add feature`, not `Added feature`), 80 characters or fewer per line; Conventional Commits preferred.
- **Document intent**: clearly state what the change does in the title, and describe the motivation in detail in the PR description.

## DISCLAIMER

**OpenYi** is an independent, open-source project and is not affiliated, associated, authorized, endorsed by, or in any way officially connected with YI Technology Inc., Xiaomi Group, or any of their subsidiaries or affiliates. 

All product names, trademarks, and registered trademarks mentioned are property of their respective owners and are used solely for identification and interoperability purposes.
