# Retroid Pocket Duo debugging handover

This document hands over on-device debugging of Duo Volume Link to an agent with adb access to a real Retroid Pocket Duo.

## Goal

Make the hardware volume keys control the lower-screen volume on the Retroid Pocket Duo (Android 16) and Duo Lite (Android 15), the same way upstream Thor Volume Link does on the AYN Thor. Lite is the priority edition; Standard should work too.

Done means: on the connected Duo, the app's main screen shows a live secondary volume, and Secondary and Linked modes audibly change the lower-screen volume from the hardware keys.

## Current state

- Repository: `ctyldsley/DuoVolumeLink`, a fork of `pth2000/ThorVolumeLink`.
- Work branch: `ccr-029e93b1-c1jp9w` (default branch is `master`). Commit there unless the owner says otherwise. Do not open a pull request unless asked.
- CI (`.github/workflows/android.yml`) builds both debug APKs on every branch push and uploads them as the `debug-apks` artifact.
- Both editions compile. **Nothing has been run on a Duo yet.**

Already changed from upstream:

- Renamed to Duo Volume Link; application IDs are `io.github.duovolume.control` (Standard) and `io.github.duovolume.control.lite` (Lite). The Java package is still `io.github.thorvolume.control` to keep upstream merges easy; leave it.
- Project, release and update-check links point at the fork.
- Standard opts out of Android 15+ forced edge-to-edge (`values-v35`, `values-night-v35`).
- `DeviceProbe.java` adds device info and related `Settings.System` keys to the Developer tools report (About, tap version seven times, then Settings, Developer tools, Copy report).

## What is known and what is assumed

Known:

- Lite targets SDK 22 deliberately. AOSP SettingsProvider only lets non-privileged apps write vendor-defined `Settings.System` keys when they target SDK 22 or lower. Raising the target breaks Lite.
- Android 14+ blocks installing apps that target SDK 22 by tapping the APK. Lite must be installed with `adb install --bypass-low-target-sdk-block`.
- On the AYN Thor the lower-screen volume is `Settings.System: secondary_screen_volume_level`, range 0 to 15, and focus tracking uses `Settings.System: focus_change` (a counter whose parity maps to the focused display).

Assumed, not verified:

- The Duo uses the same `secondary_screen_volume_level` key and 0 to 15 range. The only evidence is that the Duo ROM reportedly uses the same `action.tcc.button.key.event` broadcast and a "DualScreenAssistant" component, like the Thor.
- The Duo has a `focus_change` key that behaves the same way.
- The upper screen is `Display.DEFAULT_DISPLAY` and the lower screen is the secondary one.
- The low-target-SDK compatibility write path still works on the Duo's Android 15/16 firmware.

## Where things live

| Concern | File |
| --- | --- |
| Secondary key name, range, clamp | `app/src/main/java/io/github/thorvolume/control/VolumeControl.java` |
| Lite backend (direct `Settings.System` writes) | `app/src/lite/java/io/github/thorvolume/control/EditionBackend.java` |
| Standard backends (Shizuku, root; shell `settings put`) | `app/src/standard/java/io/github/thorvolume/control/` (`EditionBackend`, `SecondaryVolumeUserService`, `RootBackend`) |
| Focus tracking | `FocusChangeSetting.java`, `ThorKeyService.java` |
| Diagnostics | `DeviceProbe.java`, `DeveloperToolsActivity.java` |
| Target SDK and IDs | `app/build.gradle.kts` |

Note that `clamp` to 0 to 15 is duplicated in `VolumeControl`, the Lite `EditionBackend`, `SecondaryVolumeUserService` and possibly `RootBackend`. If the range differs on the Duo, change all of them (ideally by reusing the `VolumeControl` constants).

## Procedure

### 1. Confirm the device

```bash
adb devices
adb shell getprop ro.product.manufacturer
adb shell getprop ro.product.model
adb shell getprop ro.product.device
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell dumpsys display | grep -E "mDisplayId|DisplayDeviceInfo" | head -20
```

### 2. Check the assumed keys

```bash
adb shell settings get system secondary_screen_volume_level
adb shell settings get system focus_change
```

### 3. Find the real key if the assumed one is missing or does not move

Snapshot, ask the owner to change **only the lower-screen volume** with the Duo's own quick menu (double-tap Home), snapshot again, diff:

```bash
for ns in system global secure; do adb shell settings list $ns > ${ns}_before.txt; done
# owner changes lower-screen volume
for ns in system global secure; do adb shell settings list $ns > ${ns}_after.txt; diff ${ns}_before.txt ${ns}_after.txt; done
```

Repeat at minimum and maximum to learn the range. If no setting changes, compare `adb shell dumpsys audio` and `adb shell dumpsys media.audio_policy` before and after, and inspect the vendor assistant (`adb shell pm list packages | grep -i -E "dual|odin|retroid|tcc"`, then `adb shell dumpsys package <pkg>`) to find how it applies the volume. Stop and report to the owner before designing a non-settings approach.

### 4. Prove writing works from the shell

```bash
adb shell settings put system <key> <value>
```

Confirm the lower-screen volume audibly changes. If the shell write is ignored, the key is only a mirror and the app approach needs rethinking; report this.

### 5. Do the same for focus

Tap the upper and lower screens alternately and watch `adb shell settings get system focus_change` (or diff snapshots as above). Follow focus mode is secondary; if there is no equivalent key, leave the mode falling back to primary and note it in the README.

### 6. Build and install

Get APKs either from CI (push the branch, download `debug-apks`) or locally with JDK 17 and Android SDK 35:

```bash
./gradlew assembleLiteDebug assembleStandardDebug
adb install --bypass-low-target-sdk-block -r app/build/outputs/apk/lite/debug/app-lite-debug.apk
adb install -r app/build/outputs/apk/standard/debug/app-standard-debug.apk
```

Debug builds from different machines or CI runs have different signatures; uninstall first if install fails with a signature mismatch (`adb uninstall io.github.duovolume.control.lite`).

### 7. Test Lite on the device

The owner must grant modify-system-settings access and enable the accessibility key service in the app. Do not enable the Lite and Standard key services at the same time. Then check:

- Main screen shows a secondary volume value, not "unavailable".
- Secondary mode: volume keys change the lower-screen volume only.
- Linked mode: both move together.
- Watch for failures: `adb logcat -s SettingsProvider AndroidRuntime` plus the app's "recent error" line. A `SecurityException` or `IllegalArgumentException` from SettingsProvider on write means the compatibility path is closed on this firmware; report that, since Lite then cannot work without privilege.

### 8. Test Standard

Same checks with Shizuku (or root) authorised. Also confirm the UI is not drawn under the status or navigation bars.

## Likely fixes by outcome

- **Same key, works**: no code change. Update the "Retroid Pocket Duo status" section of `README.md` to say it is confirmed, with model and firmware version.
- **Different key or range**: make the key and maximum device-dependent in `VolumeControl` (select by which key exists at runtime, so the Thor keeps working), and remove the hard-coded 0 to 15 clamps in the backends. Update `DeviceProbe`, the README "How it works" section and the `root_mode_message` string, which names the key.
- **Displays reversed** (lower screen is the default display): review `FocusChangeSetting.calibrateFromDisplay` and the "upper/lower display" strings.

## Constraints

- Keep AYN Thor behaviour working; this fork supports both devices.
- Keep Lite at `targetSdk = 22`.
- Keep the upstream MIT copyright line and `LICENSE` intact.
- UK English and plain hyphens only in docs, strings and commit messages (no em dashes).
- Ask the owner before anything destructive on the device (factory resets, uninstalling other apps, changing unrelated settings). Restore any setting you change while probing.
