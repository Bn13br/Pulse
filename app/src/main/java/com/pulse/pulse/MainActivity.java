package com.pulse.pulse;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Tela inicial: estado do Pulse, configuração guiada e atalhos para as outras telas. */
public class MainActivity extends BaseActivity {

    private static final String ASKED_AUDIO = "asked_audio";

    private View dot;
    private TextView stTitle;
    private TextView stSub;
    private LinearLayout setupWrap;
    private TextView icAudio;
    private Button btAudio;
    private TextView icSvc;
    private Button btSvc;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateStatus();
            ui.postDelayed(this, 1000);
        }
    };

    @Override
    int titleRes() {
        return 0;
    }

    @Override
    boolean showBack() {
        return false;
    }

    @Override
    void build(LinearLayout c) {
        // ---- estado + liga/desliga ----
        LinearLayout hero = card(c, 0);
        LinearLayout st = new LinearLayout(this);
        st.setOrientation(LinearLayout.HORIZONTAL);
        st.setGravity(Gravity.CENTER_VERTICAL);
        st.setPadding(0, dp(12), 0, 0);
        dot = new View(this);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        dot.setBackground(g);
        st.addView(dot, new LinearLayout.LayoutParams(dp(12), dp(12)));
        stTitle = text("", 20, cText, true);
        stTitle.setPadding(dp(10), 0, 0, 0);
        st.addView(stTitle);
        hero.addView(st);
        stSub = text("", 14, cText2, false);
        stSub.setPadding(0, dp(4), 0, dp(4));
        hero.addView(stSub);
        divider(hero);
        switchRow(hero, R.string.sw_enable, R.string.sw_enable_sum, Prefs.ENABLED, true);

        // ---- configuração guiada (some quando está tudo certo) ----
        setupWrap = new LinearLayout(this);
        setupWrap.setOrientation(LinearLayout.VERTICAL);
        c.addView(setupWrap);
        LinearLayout setup = card(setupWrap, R.string.sec_setup);
        View.OnClickListener askAudio = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestAudio();
            }
        };
        View.OnClickListener openSvc = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                toast(R.string.toast_find_pulse);
            }
        };
        LinearLayout r1 = checkRow(setup, R.string.step_audio, R.string.step_audio_sum,
                R.string.btn_allow, askAudio, true);
        icAudio = (TextView) r1.getChildAt(0);
        btAudio = (Button) r1.getChildAt(2);
        divider(setup);
        LinearLayout r2 = checkRow(setup, R.string.step_service, R.string.step_service_sum,
                R.string.btn_open, openSvc, false);
        icSvc = (TextView) r2.getChildAt(0);
        btSvc = (Button) r2.getChildAt(2);
        note(setup, R.string.restricted_note);
        button(setup, R.string.btn_root, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RootSetup.run(MainActivity.this);
            }
        }, false);

        // ---- atalhos rápidos ----
        LinearLayout quick = card(c, R.string.sec_quick);
        switchRow(quick, R.string.sw_show_bars, 0, Prefs.SHOW_BARS, true);
        divider(quick);
        switchRow(quick, R.string.sw_haptic, R.string.sw_haptic_sum, Prefs.HAPTIC, false);

        // ---- navegação ----
        LinearLayout nav = card(c, R.string.sec_customize);
        navRow(nav, R.string.nav_visual, R.string.nav_visual_sum, VisualActivity.class);
        divider(nav);
        navRow(nav, R.string.nav_haptics, R.string.nav_haptics_sum, HapticsActivity.class);
        divider(nav);
        navRow(nav, R.string.nav_behavior, R.string.nav_behavior_sum, BehaviorActivity.class);
        divider(nav);
        navRow(nav, R.string.nav_about, R.string.nav_about_sum, AboutActivity.class);
    }

    /** Linha com ícone de status, título/resumo e botão. Filhos: 0=ícone, 1=texto, 2=botão. */
    private LinearLayout checkRow(LinearLayout parent, int titleRes, int sumRes, int btnRes,
                                  View.OnClickListener l, boolean first) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        TextView ic = text("○", 20, cText2, true);
        ic.setPadding(0, 0, dp(12), 0);
        row.addView(ic);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(text(getString(titleRes), 16, cText, false));
        TextView s = text(getString(sumRes), 13, cText2, false);
        s.setPadding(0, dp(2), 0, 0);
        col.addView(s);
        row.addView(col, new LinearLayout.LayoutParams(0, WRAP, 1f));
        Button b = smallButton(btnRes, l);
        row.addView(b);
        parent.addView(row);
        return row;
    }

    private void requestAudio() {
        boolean askedBefore = sp.getBoolean(ASKED_AUDIO, false);
        if (askedBefore && !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
            // o Android não mostra mais o pedido: abre as configurações do app
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", getPackageName(), null));
            startActivity(i);
            toast(R.string.toast_allow_mic);
        } else {
            sp.edit().putBoolean(ASKED_AUDIO, true).apply();
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        updateStatus();
    }

    @Override
    void onRootDone() {
        updateStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ui.post(tick);
    }

    @Override
    protected void onPause() {
        ui.removeCallbacks(tick);
        super.onPause();
    }

    private boolean serviceEnabled() {
        String en = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return en != null && en.contains(getPackageName() + "/");
    }

    private void setStatus(int color, int titleRes, String sub) {
        ((GradientDrawable) dot.getBackground()).setColor(color);
        stTitle.setText(titleRes);
        stSub.setText(sub);
    }

    private void updateStatus() {
        boolean audio = checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        boolean svc = serviceEnabled();
        boolean done = audio && svc;

        setupWrap.setVisibility(done ? View.GONE : View.VISIBLE);
        icAudio.setText(audio ? "✔" : "○");
        icAudio.setTextColor(audio ? cOk : cText2);
        btAudio.setVisibility(audio ? View.GONE : View.VISIBLE);
        icSvc.setText(svc ? "✔" : "○");
        icSvc.setTextColor(svc ? cOk : cText2);
        btSvc.setVisibility(svc ? View.GONE : View.VISIBLE);

        if (!done) {
            setStatus(cWarn, R.string.st_setup, getString(R.string.st_setup_sub));
        } else if (!sp.getBoolean(Prefs.ENABLED, true)) {
            setStatus(cText2, R.string.st_paused, getString(R.string.st_paused_sub));
        } else if (!PulseService.connected) {
            setStatus(cWarn, R.string.st_not_running, getString(R.string.st_not_running_sub));
        } else if (PulseService.state == PulseService.STATE_ERROR) {
            setStatus(cWarn, R.string.st_error,
                    getString(R.string.st_error_sub, String.valueOf(PulseService.error)));
        } else if (PulseService.state == PulseService.STATE_ACTIVE) {
            setStatus(cOk, R.string.st_active, getString(R.string.st_active_sub));
        } else {
            setStatus(cOk, R.string.st_ready, getString(R.string.st_ready_sub));
        }
    }
}
