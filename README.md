# HR Zones for Karoo

A Karoo 3 data field that shows your heart rate zone, styled after the Apple Watch workout view.

<p align="center"><img src="docs/karoo-hr-zones.jpg" alt="The HR Zone Bar field on a Karoo 3, showing ZONE 1 at 111 bpm" width="360"></p>

Zones come from the HR zones in your Karoo rider profile.

## Install

Download the APK from the [latest release](https://github.com/dmccullum/karoo-hr-zones/releases/latest), then sideload it onto your Karoo 3. You need `adb` from the [Android platform-tools](https://developer.android.com/tools/releases/platform-tools).

1. On the Karoo: Settings → About → tap the build number 7 times, then enable USB debugging in Developer options.
2. Connect it over USB and accept the debugging prompt on the Karoo.
3. Run `adb install -r karoo-hr-zones-<version>.apk` (use the file name you downloaded).
4. Edit a ride profile page, tap a field, and pick **HR Zones → HR Zone Bar**.

Upgrading from 1.0: release 1.1 is signed with a new key, so Android won't install it over 1.0. Run `adb uninstall com.danielmccullum.hrzones` first, then install 1.1 and add the field to your pages again. Later updates install over 1.1 with `adb install -r`.

## Build from source

Requires JDK 17 and the Android SDK (platform 35). Android Studio is the easiest way to get both.

1. Create a GitHub token with `read:packages` scope. karoo-ext is published on GitHub Packages, which requires a token even for public packages.
2. Add to `~/.gradle/gradle.properties`:
   ```
   gpr.user=<your GitHub username>
   gpr.key=<token>
   ```
3. Run `./gradlew assembleDebug`. The APK is written to `app/build/outputs/apk/debug/app-debug.apk`; install it as above.

`./gradlew assembleRelease` signs with your own keystore if you set `hrzones.storeFile`, `hrzones.storePassword`, `hrzones.keyAlias` and `hrzones.keyPassword` in `~/.gradle/gradle.properties`; otherwise it falls back to the debug key. An app signed with a different key has to be uninstalled before it can be replaced.

## Files

- `HrZoneBarDataType.kt`: streams heart rate and renders the field (including the editor preview sweep)
- `ZoneBarRenderer.kt`: draws the header, zone segments, pill and pointer; zone colors are set here
- `KarooLayout.kt`: Karoo's own header text and icon sizes for each field size
- `ZonePosition.kt`: maps a BPM value to a zone index and position within that zone

## License

Copyright (C) 2026 Daniel McCullum. Released under the [GNU General Public License v3.0](LICENSE).
