package com.pulse.pulse;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.audiofx.Visualizer;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Exportar/importar ajustes (JSON) e texto de diagnóstico para relatar problemas. */
final class Backup {
    private Backup() {}

    static String export(SharedPreferences sp) throws JSONException {
        Map<String, ?> all = sp.getAll();
        JSONObject v = new JSONObject();
        for (String k : Prefs.INT_KEYS) {
            Object o = all.get(k);
            if (o instanceof Integer) v.put(k, ((Integer) o).intValue());
        }
        for (String k : Prefs.BOOL_KEYS) {
            Object o = all.get(k);
            if (o instanceof Boolean) v.put(k, ((Boolean) o).booleanValue());
        }
        for (String k : Prefs.SET_KEYS) {
            Object o = all.get(k);
            if (o instanceof Set) {
                JSONArray a = new JSONArray();
                for (Object s : (Set<?>) o) a.put(String.valueOf(s));
                v.put(k, a);
            }
        }
        JSONObject root = new JSONObject();
        root.put("app", "pulse");
        root.put("version", 1);
        root.put("prefs", v);
        return root.toString(2);
    }

    /** Retorna quantos ajustes foram importados. Só chaves conhecidas e do tipo certo entram. */
    static int importJson(SharedPreferences sp, String text) throws JSONException {
        JSONObject root = new JSONObject(text.trim());
        if (!"pulse".equals(root.optString("app"))) throw new JSONException("not a Pulse backup");
        JSONObject v = root.getJSONObject("prefs");
        SharedPreferences.Editor e = sp.edit();
        int n = 0;
        for (String k : Prefs.INT_KEYS) {
            Object o = v.opt(k);
            if (o instanceof Number) {
                e.putInt(k, ((Number) o).intValue());
                n++;
            }
        }
        for (String k : Prefs.BOOL_KEYS) {
            Object o = v.opt(k);
            if (o instanceof Boolean) {
                e.putBoolean(k, ((Boolean) o).booleanValue());
                n++;
            }
        }
        for (String k : Prefs.SET_KEYS) {
            JSONArray a = v.optJSONArray(k);
            if (a != null) {
                Set<String> s = new HashSet<String>();
                for (int i = 0; i < a.length(); i++) s.add(a.optString(i));
                e.putStringSet(k, s);
                n++;
            }
        }
        e.apply();
        return n;
    }

    static String versionName(Context c) {
        try {
            String v = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
            return v == null ? "?" : v;
        } catch (Exception e) {
            return "?";
        }
    }

    static String diagnostics(Context c) {
        StringBuilder sb = new StringBuilder();
        sb.append("Pulse ").append(versionName(c)).append('\n');
        sb.append("Android ").append(Build.VERSION.RELEASE).append(" (API ")
                .append(Build.VERSION.SDK_INT).append(")\n");
        sb.append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append(" / ")
                .append(Build.DISPLAY).append('\n');
        sb.append("Language: ").append(java.util.Locale.getDefault().toLanguageTag()).append('\n');
        sb.append("Audio permission: ").append(c.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED).append('\n');
        sb.append("Service connected: ").append(PulseService.connected).append('\n');
        sb.append("Cover: notification access ").append(CoverColors.accessGranted(c))
                .append(", listener ").append(PulseMediaListener.connected)
                .append(", source ").append(PulseMediaListener.lastSource)
                .append(", colors ").append(CoverColors.colors == null ? 0 : CoverColors.colors.length)
                .append('\n');
        sb.append("State: ").append(PulseService.state).append('\n');
        if (PulseService.error != null) sb.append("Last error: ").append(PulseService.error).append('\n');
        sb.append(BassHaptics.hardwareInfo(c)).append('\n');
        try {
            int[] r = Visualizer.getCaptureSizeRange();
            sb.append("Visualizer: capture size ").append(r[0]).append('-').append(r[1])
                    .append(", max rate ").append(Visualizer.getMaxCaptureRate()).append(" mHz\n");
        } catch (Throwable t) {
            sb.append("Visualizer: unavailable (").append(t.getClass().getSimpleName()).append(")\n");
        }
        try {
            sb.append("Settings: ").append(export(c.getSharedPreferences(Prefs.NAME,
                    Context.MODE_PRIVATE)).replace("\n", "")).append('\n');
        } catch (Throwable ignored) {
        }
        return sb.toString();
    }
}
