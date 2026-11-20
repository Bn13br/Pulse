package com.pulse.pulse;

import android.app.Notification;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Lê a capa da mídia que está tocando para o modo "cores da capa".
 *
 * O Android só entrega as sessões de mídia a apps com acesso às notificações. Este serviço só
 * trabalha quando o modo de cor "capa" está escolhido e o Pulse está ligado; fora disso fica
 * parado, sem nenhum callback registrado. Das notificações, olha apenas as de mídia (para usar o
 * ícone grande como reserva) e nunca lê o texto delas. Nada é guardado nem enviado.
 */
public class PulseMediaListener extends NotificationListenerService
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "PulseCover";

    static volatile boolean connected = false;
    /** De onde veio a última capa (para o diagnóstico): metadata, uri, notification ou none. */
    static volatile String lastSource = "none";

    private SharedPreferences sp;
    private ComponentName me;
    private MediaSessionManager msm;
    private HandlerThread thread;
    private Handler bg;

    // tudo abaixo só é mexido na thread "bg"
    private boolean started = false;
    private String lastTrack = null;
    private final List<MediaController> controllers = new ArrayList<MediaController>();

    private final Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {
            sync();
        }
    };
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            update();
        }
    };
    private final Runnable endRunnable = new Runnable() {
        @Override
        public void run() {
            end();
        }
    };

    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsListener =
            new MediaSessionManager.OnActiveSessionsChangedListener() {
                @Override
                public void onActiveSessionsChanged(List<MediaController> list) {
                    setControllers(list);
                }
            };

    private final MediaController.Callback callback = new MediaController.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            scheduleUpdate();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            scheduleUpdate();
        }

        @Override
        public void onSessionDestroyed() {
            scheduleUpdate();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        sp = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);
        me = new ComponentName(this, PulseMediaListener.class);
        thread = new HandlerThread("pulse-cover", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        bg = new Handler(thread.getLooper());
        sp.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onListenerConnected() {
        connected = true;
        bg.post(syncRunnable);
    }

    @Override
    public void onListenerDisconnected() {
        connected = false;
        bg.post(syncRunnable);
    }

    @Override
    public void onDestroy() {
        connected = false;
        sp.unregisterOnSharedPreferenceChangeListener(this);
        bg.removeCallbacks(updateRunnable);
        bg.post(endRunnable);
        thread.quitSafely();
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String key) {
        if (Prefs.CMODE.equals(key) || Prefs.ENABLED.equals(key)) bg.post(syncRunnable);
    }

    private boolean wanted() {
        return sp.getBoolean(Prefs.ENABLED, true)
                && sp.getInt(Prefs.CMODE, Prefs.DEF_CMODE) == Prefs.CMODE_COVER;
    }

    private void sync() {
        boolean want = connected && wanted();
        if (want && !started) begin();
        else if (!want && started) end();
        else if (want) scheduleUpdate();
    }

    private void begin() {
        try {
            msm = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
            msm.addOnActiveSessionsChangedListener(sessionsListener, me, bg);
            started = true;
            setControllers(msm.getActiveSessions(me));
        } catch (Throwable t) {
            Log.w(TAG, "Sem acesso às sessões de mídia", t);
            started = false;
        }
    }

    private void end() {
        try {
            if (msm != null) msm.removeOnActiveSessionsChangedListener(sessionsListener);
        } catch (Throwable ignored) {
        }
        for (MediaController c : controllers) {
            try {
                c.unregisterCallback(callback);
            } catch (Throwable ignored) {
            }
        }
        controllers.clear();
        started = false;
        lastTrack = null;
        lastSource = "none";
        if (CoverColors.colors != null) CoverColors.publish(null);
    }

    private void setControllers(List<MediaController> list) {
        for (MediaController c : controllers) {
            try {
                c.unregisterCallback(callback);
            } catch (Throwable ignored) {
            }
        }
        controllers.clear();
        if (list != null) {
            for (MediaController c : list) {
                try {
                    c.registerCallback(callback, bg);
                    controllers.add(c);
                } catch (Throwable ignored) {
                }
            }
        }
        scheduleUpdate();
    }

    /** As atualizações de metadados chegam em rajada (a capa costuma vir depois): espera um pouco. */
    private void scheduleUpdate() {
        bg.removeCallbacks(updateRunnable);
        bg.postDelayed(updateRunnable, 250);
    }

    private void update() {
        if (!started) return;
        MediaController pick = null;
        for (MediaController c : controllers) {
            PlaybackState st = c.getPlaybackState();
            if (st != null && st.getState() == PlaybackState.STATE_PLAYING) {
                pick = c;
                break;
            }
        }
        if (pick == null && !controllers.isEmpty()) pick = controllers.get(0);
        if (pick == null) {
            lastTrack = null;
            lastSource = "none";
            publishIfChanged(null);
            return;
        }

        MediaMetadata md = pick.getMetadata();
        String track = pick.getPackageName() + "|"
                + (md == null ? "" : md.getString(MediaMetadata.METADATA_KEY_TITLE) + "|"
                + md.getString(MediaMetadata.METADATA_KEY_ARTIST) + "|"
                + md.getString(MediaMetadata.METADATA_KEY_ALBUM));

        Bitmap bmp = null;
        String source = "none";
        if (md != null) {
            bmp = bitmapFromMetadata(md);
            if (bmp != null) source = "metadata";
            else {
                bmp = bitmapFromUri(md);
                if (bmp != null) source = "uri";
            }
        }
        if (bmp == null) {
            bmp = largeIconFromNotification(pick.getPackageName());
            if (bmp != null) source = "notification";
        }

        if (bmp == null) {
            // a capa pode chegar logo depois: na mesma faixa mantém as cores; em outra, volta ao padrão
            if (!track.equals(lastTrack)) {
                lastTrack = track;
                lastSource = "none";
                publishIfChanged(null);
            }
            return;
        }
        lastTrack = track;
        lastSource = source;
        publishIfChanged(CoverPalette.fromBitmap(bmp));
    }

    private void publishIfChanged(int[] c) {
        if (!Arrays.equals(c, CoverColors.colors)) CoverColors.publish(c);
    }

    // ---------- de onde vem a capa ----------

    private Bitmap bitmapFromMetadata(MediaMetadata md) {
        String[] keys = {MediaMetadata.METADATA_KEY_ALBUM_ART, MediaMetadata.METADATA_KEY_ART,
                MediaMetadata.METADATA_KEY_DISPLAY_ICON};
        for (String k : keys) {
            Bitmap b = md.getBitmap(k);
            if (b != null) return b;
        }
        return null;
    }

    /** Só content:, file: e android.resource: (o Pulse não tem permissão de internet). */
    private Bitmap bitmapFromUri(MediaMetadata md) {
        String[] keys = {MediaMetadata.METADATA_KEY_ALBUM_ART_URI, MediaMetadata.METADATA_KEY_ART_URI,
                MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI};
        for (String k : keys) {
            String u = md.getString(k);
            if (u == null || u.length() == 0) continue;
            Bitmap b = decodeUri(Uri.parse(u));
            if (b != null) return b;
        }
        return null;
    }

    private Bitmap decodeUri(Uri uri) {
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equals("content") || scheme.equals("file")
                || scheme.equals("android.resource"))) {
            return null;
        }
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) return null;
            try {
                BitmapFactory.decodeStream(in, null, o);
            } finally {
                in.close();
            }
            int max = Math.max(o.outWidth, o.outHeight);
            int sample = 1;
            while (max / sample > 256) sample *= 2;
            o.inJustDecodeBounds = false;
            o.inSampleSize = sample;
            in = getContentResolver().openInputStream(uri);
            if (in == null) return null;
            try {
                return BitmapFactory.decodeStream(in, null, o);
            } finally {
                in.close();
            }
        } catch (Throwable t) {
            return null;
        }
    }

    /** Reserva: o ícone grande da notificação de mídia do mesmo app (só as notificações de mídia). */
    private Bitmap largeIconFromNotification(String pkg) {
        try {
            StatusBarNotification[] list = getActiveNotifications();
            if (list == null) return null;
            for (StatusBarNotification sbn : list) {
                if (!pkg.equals(sbn.getPackageName())) continue;
                Notification n = sbn.getNotification();
                if (n == null || n.extras == null
                        || !n.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) {
                    continue;
                }
                Icon icon = n.getLargeIcon();
                if (icon == null) continue;
                Bitmap b = toBitmap(icon.loadDrawable(this));
                if (b != null) return b;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private Bitmap toBitmap(Drawable d) {
        if (d == null) return null;
        if (d instanceof BitmapDrawable) return ((BitmapDrawable) d).getBitmap();
        int w = d.getIntrinsicWidth() > 0 ? Math.min(256, d.getIntrinsicWidth()) : 64;
        int h = d.getIntrinsicHeight() > 0 ? Math.min(256, d.getIntrinsicHeight()) : 64;
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, w, h);
        d.draw(c);
        return b;
    }
}
