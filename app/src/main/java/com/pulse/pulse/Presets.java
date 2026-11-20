package com.pulse.pulse;

import android.content.SharedPreferences;

/** Combinações prontas de ajustes (visual e vibração). */
final class Presets {
    static final int[] VISUAL_NAMES = {R.string.pv_classic, R.string.pv_neon, R.string.pv_minimal,
            R.string.pv_blocks, R.string.pv_wave, R.string.pv_cover};
    static final int[] HAPTIC_NAMES = {R.string.hp_gentle, R.string.hp_punchy, R.string.hp_shaker,
            R.string.hp_sub};

    private Presets() {}

    static void applyVisual(SharedPreferences sp, int i) {
        SharedPreferences.Editor e = sp.edit();
        for (String k : Prefs.VISUAL_KEYS) e.remove(k); // parte do padrão; a posição não muda
        switch (i) {
            case 1: // Neon
                e.putInt(Prefs.STYLE, 1).putInt(Prefs.BARS, 40).putInt(Prefs.GAP, 30)
                        .putInt(Prefs.CMODE, 3).putBoolean(Prefs.PEAKS, true)
                        .putBoolean(Prefs.MIRROR, true).putInt(Prefs.ALPHA, 95)
                        .putInt(Prefs.HEIGHT, 180);
                break;
            case 2: // Minimalista
                e.putInt(Prefs.BARS, 24).putInt(Prefs.HEIGHT, 80).putInt(Prefs.GAP, 55)
                        .putInt(Prefs.ALPHA, 70);
                break;
            case 3: // Blocos retrô
                e.putInt(Prefs.STYLE, 2).putInt(Prefs.BARS, 28).putInt(Prefs.GAP, 25)
                        .putInt(Prefs.RADIUS, 20).putInt(Prefs.CMODE, 2).putInt(Prefs.HEIGHT, 200)
                        .putInt(Prefs.ALPHA, 90);
                break;
            case 4: // Onda
                e.putInt(Prefs.STYLE, 3).putInt(Prefs.BARS, 48).putInt(Prefs.HEIGHT, 160)
                        .putInt(Prefs.ALPHA, 80);
                break;
            case 5: // Capa do álbum
                e.putInt(Prefs.STYLE, 1).putInt(Prefs.CMODE, Prefs.CMODE_COVER).putInt(Prefs.BARS, 36)
                        .putInt(Prefs.GAP, 30).putInt(Prefs.HEIGHT, 160).putInt(Prefs.ALPHA, 92);
                break;
            default: // Clássico = padrão
                break;
        }
        e.apply();
    }

    static void applyHaptic(SharedPreferences sp, int i) {
        SharedPreferences.Editor e = sp.edit();
        e.putBoolean(Prefs.H_AGC, true).putBoolean(Prefs.H_LIM, true);
        switch (i) {
            case 0: // Suave
                e.putInt(Prefs.H_MODE, 1).putInt(Prefs.H_SENS, 90).putInt(Prefs.H_MAX, 50)
                        .putInt(Prefs.H_THR, 15).putInt(Prefs.H_BOOST, 0).putInt(Prefs.H_ATK, 30)
                        .putInt(Prefs.H_REL, 250).putInt(Prefs.H_CONST, 0);
                break;
            case 1: // Batida marcante
                e.putInt(Prefs.H_MODE, 2).putInt(Prefs.H_SENS, 120).putInt(Prefs.H_MAX, 90)
                        .putInt(Prefs.H_THR, 12).putInt(Prefs.H_BOOST, 3).putInt(Prefs.H_ATK, 8)
                        .putInt(Prefs.H_REL, 80).putInt(Prefs.H_CONST, 0);
                break;
            case 2: // Bass shaker (constante)
                e.putInt(Prefs.H_MODE, 1).putInt(Prefs.H_SENS, 140).putInt(Prefs.H_MAX, 80)
                        .putInt(Prefs.H_THR, 3).putInt(Prefs.H_BOOST, 4).putInt(Prefs.H_ATK, 35)
                        .putInt(Prefs.H_REL, 380).putInt(Prefs.H_CONST, 70);
                break;
            default: // Subgrave profundo
                e.putInt(Prefs.H_MODE, 0).putInt(Prefs.H_SENS, 130).putInt(Prefs.H_MAX, 85)
                        .putInt(Prefs.H_THR, 8).putInt(Prefs.H_BOOST, 6).putInt(Prefs.H_ATK, 25)
                        .putInt(Prefs.H_REL, 300).putInt(Prefs.H_CONST, 40);
                break;
        }
        e.apply();
    }
}
