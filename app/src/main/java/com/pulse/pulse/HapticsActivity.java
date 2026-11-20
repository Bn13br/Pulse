package com.pulse.pulse;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/** Vibração nos graves: modos, ajustes finos, presets e medidor ao vivo. */
public class HapticsActivity extends BaseActivity {

    private ProgressBar meterRawBar;
    private ProgressBar meterOutBar;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            meterRawBar.setProgress((int) (BassHaptics.meterRaw * 100));
            meterOutBar.setProgress((int) (BassHaptics.meterOut * 100));
            ui.postDelayed(this, 100);
        }
    };

    @Override
    int titleRes() {
        return R.string.nav_haptics;
    }

    @Override
    void build(LinearLayout c) {
        LinearLayout main = card(c, 0);
        switchRow(main, R.string.sw_haptic, R.string.sw_haptic_sum, Prefs.HAPTIC, false);
        divider(main);
        switchRow(main, R.string.sw_haptic_off, R.string.sw_haptic_off_sum, Prefs.HAPTIC_OFF, false);
        button(main, R.string.btn_presets, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickPreset();
            }
        }, true);
        button(main, R.string.btn_test, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int r = BassHaptics.test(HapticsActivity.this);
                int msg = r < 0 ? R.string.toast_test_none
                        : r == 0 ? R.string.toast_test_noamp
                        : R.string.toast_test_amp;
                Toast.makeText(HapticsActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        }, false);

        LinearLayout mode = card(c, R.string.sec_hmode);
        spinnerRow(mode, R.string.spin_mode, Prefs.H_MODE, Prefs.DEF_H_MODE, new String[]{
                getString(R.string.mode_sub), getString(R.string.mode_bass),
                getString(R.string.mode_beat), getString(R.string.mode_custom)});
        seekRow(mode, R.string.seek_h_fmin, Prefs.H_FMIN, 20, 150, Prefs.DEF_H_FMIN, " Hz");
        seekRow(mode, R.string.seek_h_fmax, Prefs.H_FMAX, 40, 300, Prefs.DEF_H_FMAX, " Hz");
        note(mode, R.string.custom_range_note);

        LinearLayout resp = card(c, R.string.sec_hresponse);
        seekRow(resp, R.string.seek_sens, Prefs.H_SENS, 50, 300, Prefs.DEF_H_SENS, "%");
        seekRow(resp, R.string.seek_h_max, Prefs.H_MAX, 10, 100, Prefs.DEF_H_MAX, "%");
        seekRow(resp, R.string.seek_h_thr, Prefs.H_THR, 0, 50, Prefs.DEF_H_THR, "%");
        seekRow(resp, R.string.seek_h_boost, Prefs.H_BOOST, 0, 18, Prefs.DEF_H_BOOST, " dB");
        seekRow(resp, R.string.seek_h_atk, Prefs.H_ATK, 5, 200, Prefs.DEF_H_ATK, " ms");
        seekRow(resp, R.string.seek_h_rel, Prefs.H_REL, 20, 600, Prefs.DEF_H_REL, " ms");
        seekRow(resp, R.string.seek_h_const, Prefs.H_CONST, 0, 100, Prefs.DEF_H_CONST, "%");
        divider(resp);
        switchRow(resp, R.string.sw_agc, R.string.sw_agc_sum, Prefs.H_AGC, true);
        switchRow(resp, R.string.sw_limiter, R.string.sw_limiter_sum, Prefs.H_LIM, true);

        LinearLayout meter = card(c, R.string.sec_meter);
        note(meter, R.string.meter_hint);
        meter.addView(text(getString(R.string.meter_raw), 14, cText, false));
        meterRawBar = bar();
        meter.addView(meterRawBar);
        TextView l2 = text(getString(R.string.meter_out), 14, cText, false);
        l2.setPadding(0, dp(10), 0, 0);
        meter.addView(l2);
        meterOutBar = bar();
        meter.addView(meterOutBar);
    }

    private ProgressBar bar() {
        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(100);
        pb.setProgressTintList(ColorStateList.valueOf(cAccent));
        return pb;
    }

    private void pickPreset() {
        String[] names = new String[Presets.HAPTIC_NAMES.length];
        for (int i = 0; i < names.length; i++) names[i] = getString(Presets.HAPTIC_NAMES[i]);
        new AlertDialog.Builder(this)
                .setTitle(R.string.btn_presets)
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        Presets.applyHaptic(sp, which);
                        recreate();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
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
}
