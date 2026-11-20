package com.pulse.pulse;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Estilo, cores, tamanho e forma das barras, com pré-visualização ao vivo. */
public class VisualActivity extends BaseActivity {

    private static final int[] DEMO_PALETTE = {0xFFFF7043, 0xFFFFCA28, 0xFF26C6DA};

    private PulseView preview;
    private LinearLayout coverBox;
    private LinearLayout coverGrant;
    private LinearLayout swatchRow;
    private TextView coverAccess;
    private TextView coverStatus;
    private int[] lastCover = null;
    private final byte[] fake = new byte[1024];
    private float t = 0f;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            synth();
            if (CoverColors.colors != lastCover) updateCoverCard();
            ui.postDelayed(this, 50);
        }
    };
    private final SharedPreferences.OnSharedPreferenceChangeListener prefListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences p, String key) {
                    if (preview != null) preview.apply(p);
                    if (Prefs.CMODE.equals(key)) updateCoverCard();
                }
            };

    @Override
    int titleRes() {
        return R.string.nav_visual;
    }

    @Override
    void build(LinearLayout c) {
        // ---- pré-visualização ----
        FrameLayout box = new FrameLayout(this);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xFF0E0E12);
        g.setCornerRadius(dp(20));
        box.setBackground(g);
        box.setClipToOutline(true);
        preview = new PulseView(this);
        preview.apply(sp);
        box.addView(preview, new FrameLayout.LayoutParams(MATCH, MATCH));
        TextView cap = text(getString(R.string.preview), 12, 0x99FFFFFF, false);
        cap.setPadding(dp(14), dp(10), 0, 0);
        box.addView(cap, new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP | Gravity.START));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, dp(150));
        lp.topMargin = dp(8);
        c.addView(box, lp);

        button(c, R.string.btn_presets, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickPreset();
            }
        }, true);

        // ---- estilo e cor ----
        LinearLayout style = card(c, R.string.sec_style);
        spinnerRow(style, R.string.spin_style, Prefs.STYLE, Prefs.DEF_STYLE, new String[]{
                getString(R.string.style_bars), getString(R.string.style_gradient),
                getString(R.string.style_blocks), getString(R.string.style_wave)});
        final android.widget.Spinner mode = spinnerRow(style, R.string.spin_color, Prefs.CMODE,
                Prefs.DEF_CMODE, new String[]{
                        getString(R.string.color_monet), getString(R.string.color_custom),
                        getString(R.string.color_rainbow), getString(R.string.color_lava),
                        getString(R.string.color_cover)});
        buildCoverBox(style);
        seekRow(style, R.string.seek_lava, Prefs.LAVA, 1, 100, Prefs.DEF_LAVA, "%");

        LinearLayout hexRow = new LinearLayout(this);
        hexRow.setOrientation(LinearLayout.HORIZONTAL);
        hexRow.setGravity(Gravity.CENTER_VERTICAL);
        hexRow.setPadding(0, dp(10), 0, 0);
        final EditText hex = new EditText(this);
        hex.setHint(R.string.hex_hint);
        hex.setSingleLine(true);
        hex.setTextColor(cText);
        hex.setHintTextColor(cText2);
        hexRow.addView(hex, new LinearLayout.LayoutParams(0, WRAP, 1f));
        hexRow.addView(smallButton(R.string.btn_apply, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String s = hex.getText().toString().trim().replace("#", "");
                    int col = Color.parseColor("#" + s) | 0xFF000000;
                    sp.edit().putInt(Prefs.COLOR, col).putInt(Prefs.CMODE, 1).apply();
                    mode.setSelection(1);
                    toast(R.string.toast_color_ok);
                } catch (IllegalArgumentException e) {
                    toast(R.string.toast_hex_bad);
                }
            }
        }));
        style.addView(hexRow);
        divider(style);
        switchRow(style, R.string.sw_mirror, R.string.sw_mirror_sum, Prefs.MIRROR, false);
        switchRow(style, R.string.sw_peaks, R.string.sw_peaks_sum, Prefs.PEAKS, false);
        switchRow(style, R.string.sw_top, 0, Prefs.TOP, false);

        // ---- tamanho e forma ----
        LinearLayout size = card(c, R.string.sec_size);
        seekRow(size, R.string.seek_bars, Prefs.BARS, 8, 64, Prefs.DEF_BARS, "");
        seekRow(size, R.string.seek_height, Prefs.HEIGHT, 40, 400, Prefs.DEF_HEIGHT, " dp");
        seekRow(size, R.string.seek_gap, Prefs.GAP, 0, 80, Prefs.DEF_GAP, "%");
        seekRow(size, R.string.seek_radius, Prefs.RADIUS, 0, 100, Prefs.DEF_RADIUS, "%");
        seekRow(size, R.string.seek_sens, Prefs.SENS, 40, 300, Prefs.DEF_SENS, "%");
        seekRow(size, R.string.seek_alpha, Prefs.ALPHA, 10, 100, Prefs.DEF_ALPHA, "%");

        button(c, R.string.btn_reset, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmReset();
            }
        }, false);
    }

    // ---------- cores da capa ----------

    private void buildCoverBox(LinearLayout style) {
        coverBox = new LinearLayout(this);
        coverBox.setOrientation(LinearLayout.VERTICAL);

        coverAccess = text("", 14, cText, true);
        coverAccess.setPadding(0, dp(12), 0, 0);
        coverBox.addView(coverAccess);

        coverStatus = text("", 13, cText2, false);
        coverStatus.setPadding(0, dp(4), 0, dp(4));
        coverBox.addView(coverStatus);

        swatchRow = new LinearLayout(this);
        swatchRow.setOrientation(LinearLayout.HORIZONTAL);
        swatchRow.setPadding(0, dp(4), 0, dp(4));
        for (int i = 0; i < 3; i++) {
            View sw = new View(this);
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(0xFF808080);
            sw.setBackground(d);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(24), dp(24));
            lp.rightMargin = dp(8);
            swatchRow.addView(sw, lp);
        }
        coverBox.addView(swatchRow);

        switchRow(coverBox, R.string.cover_multi, R.string.cover_multi_sum, Prefs.COVER_MULTI, true);

        coverGrant = new LinearLayout(this);
        coverGrant.setOrientation(LinearLayout.VERTICAL);
        button(coverGrant, R.string.btn_cover_grant, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openListenerSettings();
            }
        }, true);
        button(coverGrant, R.string.btn_cover_root, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RootSetup.grantCoverAccess(VisualActivity.this);
            }
        }, false);
        note(coverGrant, R.string.restricted_note);
        coverBox.addView(coverGrant);

        style.addView(coverBox);
    }

    private void openListenerSettings() {
        try {
            Intent i;
            if (Build.VERSION.SDK_INT >= 30) {
                i = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS);
                i.putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                        new ComponentName(this, PulseMediaListener.class).flattenToString());
            } else {
                i = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            }
            startActivity(i);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            } catch (Exception e2) {
                toast(R.string.toast_cant_open);
            }
        }
    }

    /** Mostra/esconde o cartão da capa e atualiza o estado, as bolinhas de cor e a pré-visualização. */
    private void updateCoverCard() {
        if (coverBox == null) return;
        int[] c = CoverColors.colors;
        lastCover = c;
        if (preview != null) preview.setPalette(c != null ? c : DEMO_PALETTE);

        boolean cover = sp.getInt(Prefs.CMODE, Prefs.DEF_CMODE) == Prefs.CMODE_COVER;
        coverBox.setVisibility(cover ? View.VISIBLE : View.GONE);
        if (!cover) return;

        boolean ok = CoverColors.accessGranted(this);
        coverAccess.setText(ok ? R.string.cover_access_ok : R.string.cover_access_no);
        coverAccess.setTextColor(ok ? cOk : cWarn);
        coverGrant.setVisibility(ok ? View.GONE : View.VISIBLE);
        if (!ok) {
            coverStatus.setText(R.string.cover_hint_access);
        } else if (c == null) {
            coverStatus.setText(R.string.cover_none);
        } else {
            coverStatus.setText(getString(R.string.cover_using, c.length));
        }
        for (int i = 0; i < 3; i++) {
            View sw = swatchRow.getChildAt(i);
            if (c == null || i >= c.length) {
                sw.setVisibility(View.GONE);
            } else {
                sw.setVisibility(View.VISIBLE);
                ((GradientDrawable) sw.getBackground()).setColor(c[i]);
            }
        }
    }

    @Override
    void onRootDone() {
        updateCoverCard();
    }

    private void pickPreset() {
        String[] names = new String[Presets.VISUAL_NAMES.length];
        for (int i = 0; i < names.length; i++) names[i] = getString(Presets.VISUAL_NAMES[i]);
        new AlertDialog.Builder(this)
                .setTitle(R.string.btn_presets)
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        Presets.applyVisual(sp, which);
                        recreate();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_title)
                .setMessage(R.string.reset_msg)
                .setPositiveButton(R.string.reset_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        SharedPreferences.Editor e = sp.edit();
                        for (String k : Prefs.VISUAL_KEYS) e.remove(k);
                        e.remove(Prefs.TOP);
                        e.apply();
                        recreate();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** Espectro de mentira (graves fortes + pulsos) só para a pré-visualização. */
    private void synth() {
        t += 0.05f;
        float beat = (float) Math.pow(Math.max(0.0, Math.sin(t * 5.5)), 6.0);
        for (int k = 1; k <= 160; k++) {
            float f = k / 160f;
            float env = (float) (Math.exp(-3.0 * f) * 0.9 + 0.12);
            float wob = 0.55f + 0.45f * (float) Math.sin(t * 3f + k * 0.45f);
            float m = 120f * env * wob * (0.45f + 0.7f * beat);
            fake[2 * k] = (byte) Math.max(-127, Math.min(127, (int) m));
            fake[2 * k + 1] = 0;
        }
        preview.setFft(fake);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sp.registerOnSharedPreferenceChangeListener(prefListener);
        updateCoverCard();
        ui.post(tick);
    }

    @Override
    protected void onPause() {
        ui.removeCallbacks(tick);
        sp.unregisterOnSharedPreferenceChangeListener(prefListener);
        super.onPause();
    }
}
