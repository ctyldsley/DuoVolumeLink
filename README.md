# Duo Volume Link

[简体中文](README.zh-CN.md) · English

[![Android CI](https://github.com/ctyldsley/DuoVolumeLink/actions/workflows/android.yml/badge.svg)](https://github.com/ctyldsley/DuoVolumeLink/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-5.0%2B-3DDC84?logo=android&logoColor=white)](#editions)

Duo Volume Link is an open-source volume controller for dual-screen handhelds: the Retroid Pocket Duo / Duo Lite and the AYN Thor. It routes the hardware volume keys to the primary screen, the secondary screen, or both screens at a linked relative level.

It is a fork of [Thor Volume Link](https://github.com/pth2000/ThorVolumeLink) by pth2000, adapted so it installs and runs on the Android 15/16 firmware of the Retroid Pocket Duo family.

## Retroid Pocket Duo status

The Lite edition is **confirmed working on a Retroid Pocket Duo** running Android 16 (firmware `RPDuo_V1.0.0.108_20261018`), installed through adb as described below. On that device:

- The lower-screen volume is stored in `Settings.System: secondary_screen_volume_level`, the same key and 0-15 range as on the AYN Thor, and writing it changes the volume audibly.
- Secondary and Linked modes control the lower-screen volume from the hardware volume keys.
- `Settings.System: focus_change` exists and its parity tracks the focused screen, as on the Thor.

Not yet confirmed: the Standard edition (Shizuku or Root) on the Duo, Follow focus mode in everyday use, and the Retroid Pocket Duo Lite (Android 15). Standard is expected to work with Shizuku, because the same `settings put` shell write it relies on was tested over adb on the device above.

If the secondary volume shows as unavailable on your device, open **About**, tap the version seven times, then open **Settings → Developer tools** and use **Copy report**. The report lists the device model, Android version and every volume/screen-related `Settings.System` key the firmware exposes; attach it to an issue so the right key can be added.

Changes from upstream:

- Separate package names, so it installs alongside Thor Volume Link
- Standard edition opts out of Android 15+ forced edge-to-edge so the UI is not drawn under the system bars
- Developer tools report includes a device and settings probe
- Install instructions for Lite on Android 14 and newer (see below)

## Features

- Primary, secondary, linked, and follow-focus volume-key modes
- Direct control and proportional synchronization of the secondary-screen volume, or linked adjustment that keeps a manually set volume balance
- Optional automatic following for primary-volume changes made through Android or other apps
- Optional hardware-key mode switching with configurable key behavior and a selectable rotation of modes
- Live primary and secondary volume status

## Volume modes

| Mode | Volume-key behavior |
| --- | --- |
| Primary | Keeps Android's native volume-key behavior, adjusts only the primary media volume, and shows the system volume panel. |
| Secondary | Lets the app handle the volume keys and adjust only the secondary-screen volume without changing the primary volume. |
| Linked | Adjusts the primary media volume and maps the secondary screen to the same relative percentage. Optional automatic following also synchronizes changes made through Android or other apps. Choosing **Keep volume balance** as the linked adjustment in Settings makes both screens move together while preserving a difference set by hand instead of overwriting it. |
| Follow focus | Controls the most recently interacted-with display: the upper display uses primary volume and the lower display uses secondary volume. The target remains fixed while a volume key is held. |

All four modes support short presses and native-style press-and-hold adjustment. **Adjustment step** under **Settings → Key & controls** applies when the app handles the hardware volume keys: in Secondary and Linked modes, and when Follow focus targets the lower display. Primary mode and Follow focus on the upper display retain Android's native step size. When the hardware mode key is enabled, holding it cycles through the modes selected under **Settings → Key & controls → Modes in rotation** (at least two); otherwise, modes remain selectable from the app's main screen.

**Using the app while locked:** All modes work normally while the device is locked as long as the display remains on. After the display turns off, the volume keys can directly adjust only the primary volume; in Linked mode, the secondary volume follows those changes. Due to system limitations, holding a volume key may not adjust continuously, and Secondary or Follow focus cannot control the lower display in this state.

## Editions

| Edition | Package | Minimum Android | Secondary-volume backend | Recommended for |
| --- | --- | ---: | --- | --- |
| Lite | `io.github.duovolume.control.lite` | Android 5.0 / API 21 | `Settings.System` compatibility path | AYN Thor (Android 13); Retroid Pocket Duo only via an adb install |
| Standard | `io.github.duovolume.control` | Android 6.0 / API 23 | Shizuku or Root | Retroid Pocket Duo / Duo Lite (Android 15/16), installs normally |

Lite is the recommended edition for the stock AYN Thor firmware (Android 13). It only needs Android's modify-system-settings approval and does not depend on a companion app or service. Lite intentionally targets SDK 22: AOSP keeps a compatibility path that lets apps targeting Android 5.1 or lower modify custom `Settings.System` entries, while apps targeting newer SDK levels are rejected. AYN's `secondary_screen_volume_level` is such a vendor-defined entry.

See the relevant [AOSP SettingsProvider implementation](https://android.googlesource.com/platform/frameworks/base/+/master/packages/SettingsProvider/src/com/android/providers/settings/SettingsProvider.java) and Android's [`WRITE_SETTINGS` documentation](https://developer.android.com/reference/android/provider/Settings.System#canWrite(android.content.Context)).

Standard targets a modern Android SDK and uses additional authorization to provide the same secondary-volume controls. It is intended for users who value current Android security and compatibility behavior; it is not a more feature-complete edition. The editions can be installed together, but do not enable both accessibility key services at the same time.

### Installing Lite on Android 14 and newer (Retroid Pocket Duo)

Android 14 refuses to install apps that target SDK 22 or lower, and Android 15/16 raise that floor to SDK 24. Tapping the Lite APK on a Retroid Pocket Duo therefore fails with "App not installed as app isn't compatible". Lite cannot simply raise its target SDK: the `Settings.System` compatibility path it relies on only exists for apps targeting SDK 22 or lower.

The block is an installer policy, not a runtime one, and adb can bypass it:

```bash
adb install --bypass-low-target-sdk-block DuoVolumeLink-lite.apk
```

With no computer to hand, the same works on the device from any shell with adb privileges (for example a Shizuku or wireless-debugging terminal app):

```bash
pm install --bypass-low-target-sdk-block /data/local/tmp/DuoVolumeLink-lite.apk
```

Once installed this way Lite needs nothing else running, and keeps working across reboots. Android shows a one-off "built for an older version of Android" notice on first launch. If you would rather not use adb at all, install Standard instead.

## Getting started

1. Install an edition: Lite on the AYN Thor; on the Retroid Pocket Duo either Standard, or Lite through adb as described above.
2. Open Duo Volume Link and allow modify-system-settings access.
3. Enable the app's key service in Android Accessibility settings.
4. Open **Settings → Key & controls** and decide whether a hardware key should switch modes.
5. Select Primary, Secondary, Linked, or Follow focus on the main screen.

If you choose Standard, follow the authorization guidance on the main screen. Only the method selected by the user is active.

When hardware-key mode switching is disabled, the configured mode key keeps its original system behavior. Volume Up and Volume Down continue to follow the mode selected in the app.

## Privacy and permissions

The accessibility service filters hardware keys and uses only the display ID attached to UI events to calibrate Follow Focus. It does not retrieve window content or read screen text, touch input, or data from other apps.

- `MODIFY_AUDIO_SETTINGS` adjusts the primary media volume.
- `VIBRATE` provides mode-change feedback.
- `INTERNET` is used only when the user manually checks the latest GitHub Release.
- `WAKE_LOCK` briefly keeps the CPU active only while completing linked-volume synchronization with the screen off.
- Lite requests modify-system-settings access for its compatibility backend.
- Standard requests permission from the single backend selected by the user: either Shizuku for its UserService or `su` for Root access.

## How it works

AYN Thor firmware and Retroid Pocket Duo firmware both expose the secondary-screen volume through:

```text
Settings.System: secondary_screen_volume_level
Range: 0–15
```

Follow Focus observes the firmware’s `Settings.System: focus_change` value, which increments as focus changes between displays. After a reboot, the app invalidates the old odd/even mapping and calibrates it once from a reliable display source. Standard can use the current system focus state; the app window and accessibility display events provide fallback anchors. Once calibrated, only `focus_change` is used until the next reboot. Until an anchor is available, the app safely falls back to the primary display. The target is locked for the duration of each volume-key press.

Lite accesses this setting through Android's target-SDK compatibility behavior. Standard reads and writes it through either an official Shizuku UserService or a Root shell powered by [libsu](https://github.com/topjohnwu/libsu). It never uses both backends at once. Shared mode, preference, accessibility-service, and UI code lives in `app/src/main`; backend-specific code lives in `app/src/lite` and `app/src/standard`.

## Building

Requirements:

- Android Studio or Android SDK command-line tools
- Android SDK 35
- JDK 17 for Gradle

Clone the repository and build both editions:

```bash
git clone https://github.com/ctyldsley/DuoVolumeLink.git
cd DuoVolumeLink
./gradlew assembleLiteDebug assembleStandardDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat assembleLiteDebug assembleStandardDebug
```

Debug APKs are written to:

```text
app/build/outputs/apk/lite/debug/
app/build/outputs/apk/standard/debug/
```

Run static checks with:

```bash
./gradlew lintLiteDebug lintStandardDebug
```

## License

Duo Volume Link is available under the [MIT License](LICENSE).

The Standard edition uses Shizuku API under the MIT License and libsu under the Apache License 2.0. See [third-party notices](docs/THIRD_PARTY_NOTICES.md).
