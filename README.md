# HR Zones for Karoo

A Karoo 3 data field that shows heart rate in the style of the Apple Watch workout view. A row of zone segments fills the field; the current zone expands into a pill labelled "ZONE n" (or "Zn" in small fields) with a pointer showing your position inside it, and the other zones are dimmed. A heart icon and the BPM number sit in the header. The field draws everything itself, using the system IBM Plex Sans Condensed font and the zone colors from Karoo's own HR zone gauge. In the profile page editor the preview sweeps between 85 and 190 bpm.

Zones come from the HR zones set in your Karoo rider profile. If no HR sensor is connected, the bar shows with no zone highlighted.

## Build

Requires JDK 17 and the Android SDK (install Android Studio).

1. Create a GitHub token with `read:packages` scope. karoo-ext is published on GitHub Packages, which requires a token even for public packages.
2. Add to `~/.gradle/gradle.properties`:
   ```
   gpr.user=<your GitHub username>
   gpr.key=<token>
   ```
3. Run `./gradlew assembleDebug`. The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Install on Karoo 3

1. On the Karoo: Settings → About → tap the build number 7 times, then enable USB debugging in Developer options.
2. Connect USB and run `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
3. Edit a ride profile page, tap a field, and pick **HR Zones → HR Zone Bar**.

## Files

- `HrZoneBarDataType.kt`: streams heart rate and renders the field (including the editor preview sweep)
- `ZoneBarRenderer.kt`: draws the header, zone segments, pill and pointer; zone colors are set here
- `ZonePosition.kt`: maps a BPM value to a zone index and position within that zone

## License

Copyright (C) 2026 Daniel McCullum. Released under the [GNU General Public License v3.0](LICENSE).
