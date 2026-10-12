<div align="center">

# Pulse

**An audio visualizer and bass vibration for Android, without root**

[English](README.md) · [Português](README.pt.md)

</div>

---

### Introduction

Pulse brings the *Pulse* music visualizer found in EvolutionX ROMs to any Android device as a standalone app. It draws animated bars over your apps while music plays, and it can turn the bass of the music into haptic feedback, like a tiny bass shaker.

It listens to the **global audio output** through Android's `Visualizer` API, so it works with any music or video app. It runs as an Accessibility service: no persistent notification, and almost no cost while nothing is playing.

> [!NOTE]
> Pulse is an independent project inspired by the Pulse feature of EvolutionX. It is not affiliated with EvolutionX.

---

### Features

**Visualizer**

*   Four styles: bars, bars with gradient, fading blocks and wave.
*   Colors: system theme (Monet), custom hex color, rainbow, and an animated lava lamp with adjustable speed.
*   Adjustable number of bars, height, gap, corner roundness, sensitivity and opacity.
*   Mirrored mode (bass in the center), slow-falling peaks, and bars at the top or bottom of the screen.
*   Ready-made presets: Classic, Neon, Minimal, Retro blocks and Wave.
*   Optional display on the lock screen, and an option to hide the bars in landscape (useful for videos).
*   Capture rate limit to save battery.

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
*   Copyable diagnostics for bug reports.
*   Languages: Português and English, selectable inside the app.

---

### Compatibility

Developed and tested on **Android 16**. The code uses the Android framework only (no external libraries), with checks for newer versions. Android 8.0 (API 26) or higher is recommended.

> [!TIP]
> Root is **not required**. It is only used by an optional shortcut that grants permissions and enables the service for you.

---

### Installation

1. Build the project (for example with AndroidIDE or Gradle) and install the APK.
2. Open Pulse and allow the audio permission.
3. Turn on the Pulse service in the Android Accessibility settings.
4. Play some music.

If Android blocks the service as a "restricted setting", open the app info, use the menu (⋮) and choose **Allow restricted settings**.

**Optional (root):** the root shortcut grants the audio permission, allows restricted settings, exempts the app from battery restrictions and enables the service in one step.

---

### Building

*   Java project with the package `com.pulse.pulse`. The sources are in `app/src/main`.
*   No dependencies: it does not use AppCompat or Material, so no theme from the project template is needed.
*   Strings are in `res/values` (English) and `res/values-pt` (Portuguese).

---

### How It Works

1. A global `Visualizer` captures the audio output and returns an FFT.
2. The bars map FFT bands (logarithmic scale) to bar heights, with attack/decay smoothing.
3. For vibration, the energy inside the chosen frequency band is normalized, compared with a slow baseline to separate beats from sustained bass, passed through threshold and curve, smoothed by an attack/release envelope, limited, and sent to the vibration motor.

Details of the algorithm and integration notes are in [`HAPTICS.md`](HAPTICS.md).

---

### Limitations

*   Each FFT bin covers about 47 Hz (1024 samples at 48 kHz), so sub-bass and bass are separated by weighting, not with fine precision.
*   The capture rate is usually around 20 Hz.
*   Bars cannot be drawn on the always-on / ambient display, because apps cannot draw there. Vibration can work there.
*   Background vibration depends on the Android version and ROM. If the system's haptic feedback is off, vibration may be ignored.
*   Audio processed by DSPs (such as ViPER4Android or JamesDSP) is usually captured after processing, but this depends on the device. Use the live meter to check.

---

### Privacy

*   No internet permission.
*   Audio is analyzed on the device and is never stored or sent.
*   The service only reads the **package name of the app on screen**, to apply the app filter. It does not read screen content.

---

### Troubleshooting

*   Bars do not move: make sure the audio permission is granted and the service is on. The status shows the error, if any.
*   No vibration: use **Test vibration**, and check the system's vibration and haptic feedback settings.
*   Stops with the screen off: turn off battery optimization for Pulse.
*   Asking for help: copy the diagnostics from the About screen and send it.
*   Logs: `su -c "logcat -d | grep -i pulse"`

---

### Changelog

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
