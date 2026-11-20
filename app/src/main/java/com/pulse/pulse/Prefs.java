package com.pulse.pulse;

final class Prefs {
    static final String NAME = "pulse";

    // visual
    static final String BARS = "bars";
    static final String HEIGHT = "height";
    static final String SENS = "sens";
    static final String ALPHA = "alpha";
    static final String GAP = "gap";
    static final String CMODE = "cmode";   // 0 tema, 1 personalizada, 2 arco-íris, 3 lavalamp, 4 capa
    static final String COLOR = "color";
    static final String STYLE = "style";   // 0 barras, 1 degradê, 2 blocos, 3 onda
    static final String MIRROR = "mirror";
    static final String PEAKS = "peaks";
    static final String TOP = "top";
    static final String RADIUS = "radius";
    static final String LAVA = "lava";
    static final String COVER_MULTI = "cover_multi"; // várias cores da capa (padrão: ligado)

    // comportamento
    static final String ENABLED = "enabled";
    static final String SHOW_BARS = "show_bars";
    static final String LOCK = "lock";
    static final String FPS = "fps";
    static final String APPMODE = "appmode"; // 0 todos, 1 esconder nos escolhidos, 2 só nos escolhidos
    static final String APPS = "apps";
    static final String HIDE_LAND = "hide_land";
    static final String HAPTIC_OFF = "haptic_off";

    // vibração nos graves
    static final String HAPTIC = "haptic";
    static final String H_MODE = "h_mode";   // 0 subgrave, 1 grave, 2 batida, 3 personalizado
    static final String H_SENS = "h_sens";
    static final String H_MAX = "h_max";
    static final String H_THR = "h_thr";
    static final String H_BOOST = "h_boost";
    static final String H_ATK = "h_atk";
    static final String H_REL = "h_rel";
    static final String H_FMIN = "h_fmin";
    static final String H_FMAX = "h_fmax";
    static final String H_AGC = "h_agc";
    static final String H_LIM = "h_lim";
    static final String H_CONST = "h_const";

    static final int DEF_BARS = 32;
    static final int DEF_HEIGHT = 140;
    static final int DEF_SENS = 100;
    static final int DEF_ALPHA = 85;
    static final int DEF_GAP = 35;
    static final int DEF_CMODE = 0;
    static final int CMODE_COVER = 4;   // cores da capa da mídia
    static final int DEF_COLOR = 0xFFFFFFFF;
    static final int DEF_STYLE = 0;
    static final int DEF_RADIUS = 100;
    static final int DEF_LAVA = 30;
    static final int DEF_FPS = 20;

    static final int DEF_H_MODE = 1;
    static final int DEF_H_SENS = 100;
    static final int DEF_H_MAX = 80;
    static final int DEF_H_THR = 10;
    static final int DEF_H_BOOST = 0;
    static final int DEF_H_ATK = 15;
    static final int DEF_H_REL = 140;
    static final int DEF_H_FMIN = 20;
    static final int DEF_H_FMAX = 100;
    static final int DEF_H_CONST = 0;

    /** Chaves do visual (sem a posição), usadas pelos presets e pelo botão de restaurar. */
    static final String[] VISUAL_KEYS = {BARS, HEIGHT, SENS, ALPHA, GAP, CMODE, COLOR, STYLE,
            MIRROR, PEAKS, RADIUS, LAVA, COVER_MULTI};

    /** Todas as chaves que entram no backup, por tipo (o import só aceita estas). */
    static final String[] INT_KEYS = {BARS, HEIGHT, SENS, ALPHA, GAP, CMODE, COLOR, STYLE, RADIUS,
            LAVA, FPS, APPMODE, H_MODE, H_SENS, H_MAX, H_THR, H_BOOST, H_ATK, H_REL, H_FMIN,
            H_FMAX, H_CONST};
    static final String[] BOOL_KEYS = {MIRROR, PEAKS, TOP, COVER_MULTI, ENABLED, SHOW_BARS, LOCK, HIDE_LAND,
            HAPTIC_OFF, HAPTIC, H_AGC, H_LIM};
    static final String[] SET_KEYS = {APPS};

    private Prefs() {}
}
