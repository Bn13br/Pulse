<div align="center">

# Pulse

**A lightweight audio visualizer and bass-vibration overlay for Android**

[English](README.md) · [Português](README.pt.md)

</div>

---

### Introduction

Pulse brings the *Pulse* music visualizer found in EvolutionX ROMs to any Android device as a standalone app. It draws animated bars on screen while music is playing and can also turn the bass of the music into haptic feedback, like a tiny bass shaker.

It listens to the **global audio output** through Android's `Visualizer` API, so it works with any music or video app. It runs as an Accessibility Service, which means no persistent notification and almost no cost while nothing is playing.

> [!NOTE]
> Pulse is an independent project inspired by the Pulse feature of EvolutionX. It is not affiliated with EvolutionX.

---

### Features

**Visualizer**

*   Bars drawn over any app, at the bottom or top of the screen.
*   Number of bars, height, sensitivity, opacity and gap are adjustable.
*   Styles: solid or gradient (fade), mirrored from the center, and slow-falling peaks.
*   Colors: system theme (Monet), custom hex color, or rainbow.
*   Optional display on the lock screen.
*   Capture rate limit to save battery.
*   One-tap reset of the visual settings.

**Bass vibration**

*   Converts bass energy (not overall volume) into haptic intensity using spectral analysis.
*   Modes: Sub-bass, Bass, Beat and Custom (your own frequency range).
*   Controls: sensitivity, max intensity, threshold, bass boost (analysis only), attack, release, constancy, auto normalization and limiter.
*   Optional vibration with the screen off or idle.
*   Test button and a live bass meter for debugging.
*   Works with devices without amplitude control (uses pulses of different lengths).

**Convenience**

*   Quick Settings tile to turn Pulse on or off.
*   App filter: hide in, or show only in, the apps you choose.
*   Languages: Português and English, selectable inside the app.

---

### Compatibility

Developed for **Android 13**. Other versions are untested.

> [!TIP]
> Root is **not required**. It is only used by an optional shortcut that grants permissions and enables the service for you.

---

### Installation

1. Build the project (for example with AndroidIDE or Gradle) and install the APK.
2. Open Pulse and tap **Allow audio**.
3. Tap **Enable the Pulse service** and turn it on in the Accessibility settings.
4. Play some music.

If Android blocks the service as a "restricted setting", open the app info, use the menu and choose **Allow restricted settings**.

**Optional (root):** tap **Shortcut: set everything up with root** to grant the audio permission, allow restricted settings, exempt the app from battery restrictions and enable the service in one step.

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
*   Bars cannot be drawn on the always-on / ambient display, because apps cannot draw there.
*   Background vibration depends on the Android version and ROM. If the system's haptic feedback is off, vibration may be ignored.
*   Audio processed by DSPs (such as ViPER4Android or JamesDSP) is usually captured after processing, but this depends on the device. Use the bass meter to check.

---

### Privacy

*   No internet permission.
*   The service only reads the **package name of the app on screen**, to apply the app filter. It does not read screen content.

---

### Troubleshooting

*   Bars do not move: make sure the audio permission is granted and the service is enabled.
*   No vibration: tap **Test vibration**, and check the system's vibration and haptic feedback settings.
*   Need logs: `su -c "logcat -d | grep -i pulse"`

---

### Credits

*   [EvolutionX](https://evolution-x.org/): inspiration for the Pulse feature.
*   Android `Visualizer` API: audio capture and FFT.

*Code written 100% using the artificial intelligence "Claude"*
