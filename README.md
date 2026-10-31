# HR Zones for Karoo

A Karoo 3 data field that shows heart rate with one colored bar segment per HR zone. The current zone is drawn at full color with a marker at your position inside it; the other zones are dimmed. Karoo draws the field header and the BPM number itself, so the text matches built-in fields.

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

- `HrZoneBarDataType.kt`: streams heart rate and renders the field
- `ZoneBarRenderer.kt`: draws the zone segments and marker; zone colors are set here
- `ZonePosition.kt`: maps a BPM value to a zone index and position within that zone
