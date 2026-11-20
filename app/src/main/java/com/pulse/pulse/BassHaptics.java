package com.pulse.pulse;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

/**
 * Transforma a energia dos graves (FFT do Visualizer) em vibração do motor háptico.
 *
 * Módulo independente: só precisa de Context, das prefs (chaves em Prefs) e de alguém que
 * chame process(fft, samplingRateMilliHz, uptimeMillis) a cada frame de FFT.
 *
 * Pipeline: FFT -> energia na banda [fmin,fmax] -> boost (só análise) -> normalização
 * automática -> sensibilidade -> (nível + punch sobre a linha de base) -> threshold + curva
 * -> envelope attack/release -> limiter -> intensidade máxima -> amplitude do motor.
 */
final class BassHaptics {

    /** Medidores para a tela de diagnóstico (0..1). */
    static volatile float meterRaw = 0f;
    static volatile float meterOut = 0f;

    private static final float FLOOR = 12f;      // abaixo disso é ruído: não amplifica
    private static final float FIXED_REF = 110f; // referência fixa quando a normalização está off
    private static final float MIN_AMP = 24f;    // motores não giram bem abaixo disso

    private final Vibrator vib;
    private final boolean hasAmp;
    private final AudioAttributes attrs = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
            .build();

    // config
    private boolean enabled;
    private boolean active = true; // o serviço desliga quando só as barras são desejadas
    private int mode;
    private float sens, maxI, thr, boost, atkMs, relMs, lo, hi;
    private boolean agc, limiter;
    private float constancy; // 0..1

    // estado
    private float ref = FLOOR, base = 0f, env = 0f, hot = 0f;
    private long lastTs = 0, lastVib = 0;
    private long lastPresent = Long.MIN_VALUE / 4; // último instante com grave "presente"
    private boolean vibrating = false;

    // pesos por bin da FFT para a banda atual
    private float[] w = new float[0];
    private float wSr = -1f, wLo = -1f, wHi = -1f;
    private int wN = -1;

    BassHaptics(Context c) {
        vib = findVibrator(c);
        hasAmp = vib != null && vib.hasAmplitudeControl();
    }

    private static Vibrator findVibrator(Context c) {
        if (Build.VERSION.SDK_INT >= 31) {
            VibratorManager vm = (VibratorManager) c.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return vm != null ? vm.getDefaultVibrator() : null;
        }
        return (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
    }

    static String hardwareInfo(Context c) {
        Vibrator v = findVibrator(c);
        if (v == null) return "Vibrator: none";
        return "Vibrator: " + v.hasVibrator() + ", amplitude control: " + v.hasAmplitudeControl();
    }

    void configure(SharedPreferences p) {
        enabled = p.getBoolean(Prefs.HAPTIC, false);
        mode = p.getInt(Prefs.H_MODE, Prefs.DEF_H_MODE);
        sens = p.getInt(Prefs.H_SENS, Prefs.DEF_H_SENS) / 100f;
        maxI = p.getInt(Prefs.H_MAX, Prefs.DEF_H_MAX) / 100f;
        thr = p.getInt(Prefs.H_THR, Prefs.DEF_H_THR) / 100f;
        boost = (float) Math.pow(10.0, p.getInt(Prefs.H_BOOST, Prefs.DEF_H_BOOST) / 20.0);
        atkMs = p.getInt(Prefs.H_ATK, Prefs.DEF_H_ATK);
        relMs = p.getInt(Prefs.H_REL, Prefs.DEF_H_REL);
        agc = p.getBoolean(Prefs.H_AGC, true);
        limiter = p.getBoolean(Prefs.H_LIM, true);
        constancy = p.getInt(Prefs.H_CONST, Prefs.DEF_H_CONST) / 100f;
        switch (mode) {
            case 0: lo = 20f; hi = 60f; break;
            case 1: lo = 60f; hi = 120f; break;
            case 2: lo = 40f; hi = 140f; break;
            default:
                lo = p.getInt(Prefs.H_FMIN, Prefs.DEF_H_FMIN);
                hi = p.getInt(Prefs.H_FMAX, Prefs.DEF_H_FMAX);
                if (hi < lo + 10f) hi = lo + 10f;
        }
        if (!enabled) reset();
    }

    void setActive(boolean a) {
        if (active == a) return;
        active = a;
        if (!a) reset();
    }

    void reset() {
        stopMotor();
        ref = FLOOR;
        base = 0f;
        env = 0f;
        hot = 0f;
        lastTs = 0;
        lastPresent = Long.MIN_VALUE / 4;
        meterRaw = 0f;
        meterOut = 0f;
    }

    /** Peso de cada bin k = fração do intervalo [(k-0.5)d,(k+0.5)d] que cai dentro de [lo,hi]. */
    private void ensureWeights(float sr, int n) {
        if (sr == wSr && n == wN && lo == wLo && hi == wHi) return;
        wSr = sr;
        wN = n;
        wLo = lo;
        wHi = hi;
        float d = sr / n; // largura de cada bin em Hz (~47 Hz com 1024 amostras a 48 kHz)
        int kmax = Math.min(n / 2 - 1, (int) Math.ceil(hi / d) + 1);
        if (kmax < 1) kmax = 1;
        w = new float[kmax + 1];
        for (int k = 1; k <= kmax; k++) {
            float a = Math.max(lo, (k - 0.5f) * d);
            float b = Math.min(hi, (k + 0.5f) * d);
            w[k] = b > a ? (b - a) / d : 0f;
        }
    }

    void process(byte[] fft, int srMilliHz, long now) {
        int n = fft.length;
        if (n < 64) return;
        float sr = srMilliHz > 0 ? srMilliHz / 1000f : 44100f;
        ensureWeights(sr, n);

        // 1) energia na banda (bin 0 = DC é ignorado; bin k: re=fft[2k], im=fft[2k+1])
        float e = 0f;
        for (int k = 1; k < w.length; k++) {
            float wk = w[k];
            if (wk == 0f) continue;
            int i = 2 * k;
            if (i + 1 >= n) break;
            float re = fft[i];
            float im = fft[i + 1];
            e += wk * (re * re + im * im);
        }
        float raw = (float) Math.sqrt(e);
        meterRaw = Math.min(1f, raw / FIXED_REF);
        if (!enabled || !active || vib == null) {
            meterOut = 0f;
            return;
        }

        float dt = lastTs == 0 ? 50f : Math.max(10f, Math.min(200f, now - lastTs));
        lastTs = now;

        // 2) boost só na análise + normalização automática (referência de pico que decai devagar)
        float a = raw * boost;
        float level;
        if (agc) {
            if (a > ref) ref += (a - ref) * 0.6f;
            else ref += (Math.max(a, FLOOR) - ref) * (1f - (float) Math.exp(-dt / 4000f));
            if (ref < FLOOR) ref = FLOOR;
            level = a / ref;
        } else {
            level = a / FIXED_REF;
        }
        float x = Math.min(1f, level * sens);

        // 3) linha de base lenta: separa grave sustentado de batida (punch)
        base += (x - base) * (1f - (float) Math.exp(-dt / 900f));
        float punch = Math.max(0f, x - base);
        // constância reduz o corte do grave sustentado (0 = corta 40%, 1 = não corta)
        float lev = Math.max(0f, x - 0.4f * (1f - constancy) * base);
        float d;
        if (mode == 2) d = Math.min(1f, 2.5f * punch + 0.2f * lev);
        else d = Math.min(1f, 0.9f * lev + 0.6f * punch);

        // 4) threshold + curva (pequenas variações dão quase nada)
        if (d > Math.max(thr, 0.05f)) lastPresent = now;
        float s = d <= thr ? 0f : (d - thr) / (1f - thr);
        s = (float) Math.pow(s, 1.5);
        // constância: enquanto o grave esteve presente há pouco (100..500 ms), mantém
        // uma vibração mínima, tapando os buracos entre as batidas
        if (constancy > 0f && now - lastPresent <= 100f + 400f * constancy) {
            s = Math.max(s, 0.30f * constancy);
        }

        // 5) envelope follower com attack/release
        float tau = s > env ? atkMs : relMs;
        env += (s - env) * (1f - (float) Math.exp(-dt / Math.max(1f, tau)));

        // 6) limiter: se ficar perto do máximo por muito tempo, reduz até -50%
        float out = env;
        if (limiter) {
            if (env > 0.85f) hot = Math.min(2000f, hot + dt);
            else hot = Math.max(0f, hot - 2f * dt);
            out *= 1f - 0.5f * (hot / 2000f);
        }
        float intensity = Math.max(0f, Math.min(1f, out)) * maxI;
        meterOut = intensity;

        // 7) motor
        if (intensity < 0.03f) {
            stopMotor();
            return;
        }
        long dur = (long) Math.max(40f, Math.min(120f, dt * 1.4f));
        if (hasAmp) {
            int amp = (int) (MIN_AMP + (255f - MIN_AMP) * intensity);
            vibrate(VibrationEffect.createOneShot(dur, Math.max(1, Math.min(255, amp))));
        } else if (now - lastVib >= 55) {
            // sem controle de amplitude: a "força" vira duração do pulso
            long pulse = intensity < 0.25f ? 12 : intensity < 0.5f ? 25 : intensity < 0.75f ? 45 : dur;
            vibrate(VibrationEffect.createOneShot(pulse, VibrationEffect.DEFAULT_AMPLITUDE));
            lastVib = now;
        }
        vibrating = true;
    }

    private void vibrate(VibrationEffect fx) {
        try {
            vib.vibrate(fx, attrs);
        } catch (Throwable ignored) {
        }
    }

    private void stopMotor() {
        if (vibrating && vib != null) {
            try {
                vib.cancel();
            } catch (Throwable ignored) {
            }
        }
        vibrating = false;
    }

    /** Vibração manual de teste. Retorna -1 sem motor, 0 sem controle de amplitude, 1 com. */
    static int test(Context c) {
        BassHaptics h = new BassHaptics(c);
        if (h.vib == null || !h.vib.hasVibrator()) return -1;
        try {
            if (h.hasAmp) {
                long[] t = {0, 150, 100, 150, 100, 300};
                int[] a = {0, 60, 0, 150, 0, 255};
                h.vib.vibrate(VibrationEffect.createWaveform(t, a, -1), h.attrs);
            } else {
                long[] t = {0, 40, 120, 90, 120, 300};
                h.vib.vibrate(VibrationEffect.createWaveform(t, -1), h.attrs);
            }
        } catch (Throwable ignored) {
        }
        return h.hasAmp ? 1 : 0;
    }
}
