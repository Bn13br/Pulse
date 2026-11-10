<div align="center">

# Pulse

**An audio visualizer and bass vibration for Android, without root**

[English](README.md) · [Português](README.pt.md)

</div>

---

### Introduction

Pulse brings the *Pulse* music visualizer found in EvolutionX ROMs to any Android device as a standalone app. It draws animated bars over your apps while music plays, it can color them with the **album art of the song being played**, and it can turn the bass of the music into haptic feedback, like a tiny bass shaker.

It listens to the **global audio output** through Android's `Visualizer` API, so it works with any music or video app. It runs as an Accessibility service: no persistent notification, and almost no cost while nothing is playing.

> [!NOTE]
> Pulse is an independent project inspired by the Pulse feature of EvolutionX. It is not affiliated with EvolutionX.

---

### Features

**Visualizer**

*   Four styles: bars, bars with gradient, fading blocks and wave.
*   Colors: system theme (Monet), custom hex color, rainbow, an animated lava lamp with adjustable speed, and **album art colors**.
*   Adjustable number of bars, height, gap, corner roundness, sensitivity and opacity.
*   Mirrored mode (bass in the center), slow-falling peaks, and bars at the top or bottom of the screen.
*   Ready-made presets: Classic, Neon, Minimal, Retro blocks, Wave and Album art.
*   Optional display on the lock screen, and an option to hide the bars in landscape (useful for videos).
*   Capture rate limit to save battery.

**Album art colors**

*   The bars take the colors of the cover of the media that is playing, in any music app that shares its cover with Android.
*   Choose between the dominant color only, or a blend of up to three main colors of the cover across the bars.
*   Colors change smoothly when the song changes, instead of jumping.
*   Works with bars, gradient, blocks and wave, including the mirrored mode.
*   Black and white covers give light gray bars, and dark covers get a brighter shade, so the bars stay visible.
*   Falls back to the system theme color while there is no cover to read.
*   Nothing runs for this feature unless the mode is selected and Pulse is on.

**Bass vibration**

*   Turns bass energy (not overall volume) into haptic intensity, using spectral analysis.
*   Modes: Sub-bass, Bass, Beat and Custom (your own frequency range).
*   Controls: sensitivity, max intensity, threshold, bass boost (analysis only), attack, release, constancy, auto normalization and limiter.
*   Presets: Gentle, Punchy beat, Bass shaker and Deep sub-bass.
*   Optional vibration with the screen off or idle.
*   Live bass meter and a test button, to check that it works and how your DSP affects it.
*   Works on devices without amplitude control (uses pulses of different lengths).

**Control**

*   Quick Settings tile to turn Pulse on or off.
*   App filter: hide in, or show only in, the apps you choose.
*   Status detection: Pulse tells you whether it is active, paused, missing a permission or unable to read the audio.
*   Export and import your settings as text, and reset appearance or everything.
*   Copyable diagnostics for bug reports, including the state of the album art colors.
*   Follows the system language: Português and English.

---

### Compatibility

Developed and tested on **Android 13**. The code uses the Android framework only (no external libraries), with checks for newer versions. Android 8.0 (API 26) or higher is recommended.

It builds with **Gradle 7.4.2** and **Android Gradle Plugin 7.3.1** (the combination used by AndroidIDE), with `compileSdk` 33, `minSdk` 26 and Java 8.

> [!TIP]
> Root is **not required**. It is only used by optional shortcuts that grant permissions and enable the service for you.

---

### Installation

1. Build the project (for example with AndroidIDE or Gradle) and install the APK.
2. Open Pulse and allow the audio permission.
3. Turn on the Pulse service in the Android Accessibility settings.
4. Play some music.

If Android blocks the service as a "restricted setting", open the app info, use the menu (⋮) and choose **Allow restricted settings**.

**Album art colors (optional):** choose *Album art colors* in the color options and tap **Grant access**. Android opens its notification access list, where you turn Pulse on. This permission is what lets Android share the current media with the app. The same "restricted setting" rule can apply here.

**Optional (root):** the root shortcut grants the audio permission, allows restricted settings, exempts the app from battery restrictions and enables the service in one step. Album art colors have their own **Grant with root** button, which also allows the notification access.

---

### Building

*   Java project with the package `com.pulse.pulse`. The sources are in `app/src/main`.
*   Tested with Gradle 7.4.2 and Android Gradle Plugin 7.3.1. A newer plugin (8.x) needs Gradle 8.0 or higher.
*   No dependencies: it does not use AppCompat, Material, Kotlin or the Palette library, so the `dependencies` block of the project template can be removed. The cover color extraction is built in.
*   The app uses its own `PulseTheme` (`res/values/pulse_theme.xml`). If the project template added `themes.xml` files that point to Material (`res/values` and `res/values-night`), delete them or leave them empty.
*   Strings are in `res/values` (English) and `res/values-pt` (Portuguese). The app language follows the system language, and English is used for any other language.
*   If you are updating from 2.0, delete the old `Lang.java`, which is no longer used.

Minimal `app/build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application")
}

android {
    namespace = "com.pulse.pulse"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.pulse.pulse"
        minSdk = 26
        targetSdk = 33
        versionCode = 21
        versionName = "2.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
```

---

### How It Works

1. A global `Visualizer` captures the audio output and returns an FFT.
2. The bars map FFT bands (logarithmic scale) to bar heights, with attack/decay smoothing.
3. For vibration, the energy inside the chosen frequency band is normalized, compared with a slow baseline to separate beats from sustained bass, passed through threshold and curve, smoothed by an attack/release envelope, limited, and sent to the vibration motor.
4. For album art colors, a notification listener reads the cover of the active media session: first the images in the media metadata, then local image links, then the large icon of the media notification. The cover is shrunk, its pixels are grouped by hue, and the most vivid groups become the palette. The bars then fade to the new palette.

Details of the vibration algorithm and integration notes are in [`HAPTICS.md`](HAPTICS.md).

---

### Limitations

*   Each FFT bin covers about 47 Hz (1024 samples at 48 kHz), so sub-bass and bass are separated by weighting, not with fine precision.
*   The capture rate is usually around 20 Hz.
*   Bars cannot be drawn on the always-on / ambient display, because apps cannot draw there. Vibration can work there.
*   Background vibration depends on the Android version and ROM. If the system's haptic feedback is off, vibration may be ignored.
*   Audio processed by DSPs (such as ViPER4Android or JamesDSP) is usually captured after processing, but this depends on the device. Use the live meter to check.
*   Album art colors need the music app to share its cover with Android. Apps that do not share it are not supported, and Pulse keeps the theme color for them.
*   Covers that only exist as an internet link (http/https) cannot be read, because Pulse has no internet permission.
*   Android requires notification access for this mode. It cannot be replaced by another permission.

---

### Privacy

*   No internet permission.
*   Audio is analyzed on the device and is never stored or sent.
*   The service only reads the **package name of the app on screen**, to apply the app filter. It does not read screen content.
*   Album art colors are optional. They use Android's notification access only to read the **cover of the media being played**. Pulse only looks for the media notification, never reads the title or text of any notification, and nothing is stored or sent. The listener stays idle unless the mode is selected and Pulse is on.

---

### Troubleshooting

*   Bars do not move: make sure the audio permission is granted and the service is on. The status shows the error, if any.
*   No vibration: use **Test vibration**, and check the system's vibration and haptic feedback settings.
*   Stops with the screen off: turn off battery optimization for Pulse.
*   Album art colors stay on the theme color: check that notification access is on for Pulse, that the song is actually playing, and that the music app shows its cover in the player. Pause and play again, or change the song, to refresh.
*   Notification access cannot be turned on: it may be blocked as a restricted setting. Open the app info, use the menu (⋮) and choose **Allow restricted settings**, or use the **Grant with root** button.
*   Colors look washed out or gray: the cover itself is probably black and white or very pale. Turn off *Use several cover colors* to use only the dominant color.
*   Build error "Minimum supported Gradle version is 8.0": the project uses a plugin newer than your Gradle. Use plugin version 7.3.1 with Gradle 7.4.2, or move the wrapper to Gradle 8.0 or higher.
*   Build error "resource style/Theme.Material3.DayNight not found": delete the `themes.xml` files left by the project template, or leave them empty.
*   Build error "cannot find symbol ... Prefs.LANG": an old `Lang.java` from 2.0 is still in the project. Delete it.
*   Asking for help: copy the diagnostics from the About screen and send it.
*   Logs: `su -c "logcat -d | grep -i pulse"`

---

### Changelog

**2.1**

*   **Album art colors:** new appearance mode where the bars take the colors of the cover of the playing media, with an option to use one or several colors and a smooth transition between songs.
*   **Album art preset:** ready-made visualizer setting for the new mode.
*   **Optional access:** the mode uses Android's notification access, with a shortcut to the right settings screen and an optional root button.
*   **Language:** the app now follows the system language. The in-app language selector was removed.
*   **Diagnostics:** now include the system language and the state of the album art colors.
*   **Build:** no dependencies, tested with Gradle 7.4.2 and Android Gradle Plugin 7.3.1.

**2.0**

*   **New styles:** fading blocks and wave, plus corner roundness.
*   **Lava lamp:** animated color with adjustable speed.
*   **Presets:** ready-made settings for the visualizer and for the bass vibration.
*   **Hide in landscape:** option to hide the bars when the screen is rotated.
*   **Backup:** export and import your settings as text, and reset everything.
*   **Status detection:** the app now reports when the service is off, a permission is missing or the audio cannot be read.
*   **Diagnostics:** device and settings summary you can copy for bug reports.
*   **Battery:** shortcut to the battery optimization settings.
*   Reorganized interface, with light and dark themes.

**1.x**

*   Visualizer with bars, gradient, mirrored mode, peaks and Monet/custom/rainbow colors.
*   Bass vibration with Sub-bass, Bass, Beat and Custom modes, attack/release, limiter, constancy and test button.
*   Vibration with the screen off or idle.
*   Quick Settings tile, app filter, lock screen option and capture rate limit.
*   Optional root shortcut.
*   Português and English.

---

### Credits

*   [EvolutionX](https://evolution-x.org/): inspiration for the Pulse feature.
*   Android `Visualizer` API: audio capture and FFT.
*   Android `MediaSession` API: access to the cover of the playing media.

---

### License

Add your license here.
