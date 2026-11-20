package com.pulse.pulse;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.audiofx.Visualizer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.List;
import java.util.Set;

/**
 * Serviço de acessibilidade usado como "hospedeiro" leve: o sistema o mantém vivo,
 * não aparece notificação e permite overlay sem a permissão SYSTEM_ALERT_WINDOW.
 * O Visualizer só existe enquanto há música tocando, a tela está ligada e os filtros permitem.
 */
public class PulseService extends AccessibilityService
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    static final int STATE_OFF = 0;     // desligado pelo usuário
    static final int STATE_IDLE = 1;    // ligado, esperando música (ou filtrado)
    static final int STATE_ACTIVE = 2;  // capturando e desenhando/vibrando
    static final int STATE_ERROR = 3;   // Visualizer não abriu

    /** Estado visível para a tela inicial do app (mesmo processo). */
    static volatile boolean connected = false;
    static volatile int state = STATE_OFF;
    static volatile String error = null;

    private static final String TAG = "Pulse";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            refresh();
        }
    };

    private WindowManager wm;
    private AudioManager am;
    private PowerManager pm;
    private KeyguardManager km;
    private SharedPreferences sp;
    private BassHaptics bass;
    private PowerManager.WakeLock wakeLock;

    private PulseView view;
    private Visualizer visualizer;
    private boolean registered = false;
    private String fgPkg = null;

    private final AudioManager.AudioPlaybackCallback playbackCallback =
            new AudioManager.AudioPlaybackCallback() {
                @Override
                public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
                    scheduleRefresh();
                }
            };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            scheduleRefresh();
        }
    };

    private final CoverColors.Listener coverListener = new CoverColors.Listener() {
        @Override
        public void onCoverColors(int[] colors) {
            PulseView pv = view;
            if (pv != null) pv.setPalette(colors);
        }
    };

    private final Visualizer.OnDataCaptureListener listener =
            new Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(Visualizer v, byte[] data, int rate) {}

                @Override
                public void onFftDataCapture(Visualizer v, byte[] fft, int rate) {
                    PulseView pv = view;
                    if (pv != null) pv.setFft(fft);
                    BassHaptics b = bass;
                    if (b != null) b.process(fft, rate, SystemClock.uptimeMillis());
                }
            };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        am = (AudioManager) getSystemService(AUDIO_SERVICE);
        pm = (PowerManager) getSystemService(POWER_SERVICE);
        km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        sp = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);
        bass = new BassHaptics(this);
        bass.configure(sp);

        sp.registerOnSharedPreferenceChangeListener(this);
        am.registerAudioPlaybackCallback(playbackCallback, handler);
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        f.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(receiver, f);
        registered = true;

        CoverColors.setListener(coverListener);
        connected = true;
        state = STATE_IDLE;
        scheduleRefresh();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || sp == null) return;
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence cs = event.getPackageName();
        if (cs == null) return;
        String pkg = cs.toString();
        if (pkg.equals(fgPkg)) return;
        if (pkg.equals(getPackageName()) || pkg.equals("com.android.systemui") || isIme(pkg)) {
            return;
        }
        fgPkg = pkg;
        if (sp.getInt(Prefs.APPMODE, 0) != 0) scheduleRefresh();
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        scheduleRefresh(); // "esconder na horizontal"
    }

    @Override
    public boolean onUnbind(Intent intent) {
        connected = false;
        state = STATE_OFF;
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        CoverColors.setListener(null);
        connected = false;
        state = STATE_OFF;
        handler.removeCallbacks(refreshRunnable);
        stop();
        if (registered) {
            try {
                sp.unregisterOnSharedPreferenceChangeListener(this);
                am.unregisterAudioPlaybackCallback(playbackCallback);
                unregisterReceiver(receiver);
            } catch (Throwable ignored) {
            }
            registered = false;
        }
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String key) {
        // mudanças que alteram a taxa/uso do Visualizer: reinicia
        if (Prefs.FPS.equals(key) || Prefs.SHOW_BARS.equals(key) || Prefs.HAPTIC.equals(key)) {
            stop();
        }
        if (bass != null) bass.configure(p);
        if (view != null) {
            view.apply(p);
            try {
                wm.updateViewLayout(view, layoutParams());
            } catch (Throwable ignored) {
            }
        }
        scheduleRefresh();
    }

    private boolean isIme(String pkg) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm == null) return false;
        List<InputMethodInfo> list = imm.getEnabledInputMethodList();
        for (InputMethodInfo i : list) {
            if (pkg.equals(i.getPackageName())) return true;
        }
        return false;
    }

    private boolean allowedByApp() {
        int mode = sp.getInt(Prefs.APPMODE, 0);
        if (mode == 0) return true;
        Set<String> set = sp.getStringSet(Prefs.APPS, null);
        boolean in = set != null && fgPkg != null && set.contains(fgPkg);
        return mode == 1 ? !in : in;
    }

    private void scheduleRefresh() {
        handler.removeCallbacks(refreshRunnable);
        handler.postDelayed(refreshRunnable, 150);
    }

    private void refresh() {
        if (!sp.getBoolean(Prefs.ENABLED, true)) {
            stop();
            state = STATE_OFF;
            return;
        }
        if (!am.isMusicActive()) {
            stop();
            state = STATE_IDLE;
            return;
        }
        boolean haptic = sp.getBoolean(Prefs.HAPTIC, false);
        boolean screenOn = pm.isInteractive();
        boolean wantBars;
        boolean wantHaptic;
        if (screenOn) {
            boolean landscape = getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_LANDSCAPE;
            boolean ok = allowedByApp()
                    && (sp.getBoolean(Prefs.LOCK, true) || !km.isKeyguardLocked());
            wantBars = ok && sp.getBoolean(Prefs.SHOW_BARS, true)
                    && !(landscape && sp.getBoolean(Prefs.HIDE_LAND, false));
            wantHaptic = ok && haptic;
        } else {
            // tela apagada/ociosa: não há overlay; só vibração, se o usuário permitiu
            wantBars = false;
            wantHaptic = haptic && sp.getBoolean(Prefs.HAPTIC_OFF, false);
        }
        if (!wantBars && !wantHaptic) {
            stop();
            state = STATE_IDLE;
            return;
        }
        if (!startVisualizer()) {
            state = STATE_ERROR;
            return;
        }
        setBars(wantBars);
        if (bass != null) bass.setActive(wantHaptic);
        setWakeLock(!screenOn && wantHaptic);
        error = null;
        state = STATE_ACTIVE;
    }

    private boolean startVisualizer() {
        if (visualizer != null) return true;
        try {
            Visualizer v = new Visualizer(0);
            v.setEnabled(false);
            int[] range = Visualizer.getCaptureSizeRange();
            v.setCaptureSize(Math.min(1024, range[1]));
            int fps = sp.getInt(Prefs.FPS, Prefs.DEF_FPS);
            if (sp.getBoolean(Prefs.HAPTIC, false)) fps = Math.max(fps, 20); // vibração precisa de resposta
            v.setDataCaptureListener(listener,
                    Math.min(Visualizer.getMaxCaptureRate(), fps * 1000), false, true);
            v.setEnabled(true);
            visualizer = v;
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "Visualizer indisponível (permissão de áudio?)", t);
            error = t.getClass().getSimpleName() + ": " + t.getMessage();
            releaseVisualizer();
            return false;
        }
    }

    private void setBars(boolean want) {
        if (want && view == null) {
            PulseView pv = new PulseView(this);
            pv.apply(sp);
            pv.setPalette(CoverColors.colors);
            try {
                wm.addView(pv, layoutParams());
                view = pv;
            } catch (Throwable t) {
                Log.w(TAG, "Falha ao adicionar overlay", t);
            }
        } else if (!want && view != null) {
            try {
                wm.removeView(view);
            } catch (Throwable ignored) {
            }
            view = null;
        }
    }

    /** Mantém a CPU acordada só enquanto vibra com a tela apagada. */
    private void setWakeLock(boolean on) {
        try {
            if (on) {
                if (wakeLock == null) {
                    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Pulse:haptics");
                    wakeLock.setReferenceCounted(false);
                }
                if (!wakeLock.isHeld()) wakeLock.acquire();
            } else if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (Throwable t) {
            Log.w(TAG, "WakeLock", t);
        }
    }

    private void stop() {
        releaseVisualizer();
        setBars(false);
        if (bass != null) bass.reset();
        setWakeLock(false);
    }

    private void releaseVisualizer() {
        if (visualizer != null) {
            try {
                visualizer.setEnabled(false);
                visualizer.release();
            } catch (Throwable ignored) {
            }
            visualizer = null;
        }
    }

    private WindowManager.LayoutParams layoutParams() {
        int h = (int) (sp.getInt(Prefs.HEIGHT, Prefs.DEF_HEIGHT)
                * getResources().getDisplayMetrics().density);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                h,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = sp.getBoolean(Prefs.TOP, false) ? Gravity.TOP : Gravity.BOTTOM;
        if (Build.VERSION.SDK_INT >= 28) {
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        lp.setTitle("Pulse");
        return lp;
    }
}
