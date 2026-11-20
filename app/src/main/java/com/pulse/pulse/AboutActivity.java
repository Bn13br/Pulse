package com.pulse.pulse;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Sobre: versão, como funciona, privacidade, créditos e diagnóstico para relatar problemas. */
public class AboutActivity extends BaseActivity {

    @Override
    int titleRes() {
        return R.string.nav_about;
    }

    @Override
    void build(LinearLayout c) {
        LinearLayout top = card(c, 0);
        TextView name = text(getString(R.string.app_name), 26, cText, true);
        name.setPadding(0, dp(8), 0, 0);
        top.addView(name);
        top.addView(text(getString(R.string.about_version, Backup.versionName(this)), 13, cText2,
                false));
        TextView d = text(getString(R.string.about_desc), 15, cText, false);
        d.setPadding(0, dp(10), 0, dp(4));
        top.addView(d);

        LinearLayout how = card(c, R.string.sec_how);
        bullet(how, R.string.how_1);
        bullet(how, R.string.how_2);
        bullet(how, R.string.how_3);
        bullet(how, R.string.how_4);

        LinearLayout priv = card(c, R.string.sec_privacy);
        bullet(priv, R.string.priv_1);
        bullet(priv, R.string.priv_2);
        bullet(priv, R.string.priv_3);
        bullet(priv, R.string.priv_4);

        LinearLayout lim = card(c, R.string.sec_limits);
        bullet(lim, R.string.lim_1);
        bullet(lim, R.string.lim_2);
        bullet(lim, R.string.lim_3);
        bullet(lim, R.string.lim_4);

        LinearLayout help = card(c, R.string.sec_help);
        note(help, R.string.diag_note);
        button(help, R.string.btn_diag_copy, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String t = Backup.diagnostics(AboutActivity.this);
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Pulse", t));
                toast(R.string.toast_copied);
            }
        }, true);
        button(help, R.string.btn_diag_share, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("text/plain");
                send.putExtra(Intent.EXTRA_TEXT, Backup.diagnostics(AboutActivity.this));
                startActivity(Intent.createChooser(send, getString(R.string.btn_diag_share)));
            }
        }, false);

        LinearLayout cred = card(c, R.string.sec_credits);
        bullet(cred, R.string.credit_evo);
        bullet(cred, R.string.credit_api);
    }

    private void bullet(LinearLayout parent, int res) {
        TextView t = text("•  " + getString(res), 14, cText, false);
        t.setPadding(0, dp(6), 0, dp(2));
        parent.addView(t);
    }
}
