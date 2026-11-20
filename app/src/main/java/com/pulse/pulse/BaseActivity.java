package com.pulse.pulse;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Base das telas: tema claro/escuro, cores do sistema (Monet), cartões e linhas de configuração.
 * Só usa o framework do Android (sem AppCompat/Material), para compilar em qualquer projeto.
 */
public abstract class BaseActivity extends Activity {
    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    SharedPreferences sp;
    boolean night;
    int cBg, cCard, cText, cText2, cAccent, cOnAccent, cOk, cWarn, cLine;
    LinearLayout content;

    /** Título da tela; 0 usa o nome do app. */
    abstract int titleRes();

    boolean showBack() {
        return true;
    }

    abstract void build(LinearLayout content);

    /** Chamado quando o atalho de root termina (as telas podem atualizar o status). */
    void onRootDone() {}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);
        initColors();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(cBg);
        root.addView(header());

        ScrollView sv = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(4), dp(16), dp(32));
        sv.addView(content, new FrameLayout.LayoutParams(MATCH, WRAP));
        root.addView(sv, new LinearLayout.LayoutParams(MATCH, 0, 1f));

        // Android 15+ com targetSdk 35+ desenha sob as barras do sistema: aplica os recuos.
        if (Build.VERSION.SDK_INT >= 35 && getApplicationInfo().targetSdkVersion >= 35) {
            root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                @SuppressWarnings("deprecation")
                public android.view.WindowInsets onApplyWindowInsets(View v,
                                                                     android.view.WindowInsets ins) {
                    v.setPadding(ins.getSystemWindowInsetLeft(), ins.getSystemWindowInsetTop(),
                            ins.getSystemWindowInsetRight(), ins.getSystemWindowInsetBottom());
                    return ins;
                }
            });
        }

        setContentView(root);
        barColors();
        build(content);
    }

    // ---------- cores e tema ----------

    private void initColors() {
        // abaixo do Android 10 o tema do sistema é sempre claro: não usa a paleta escura
        night = Build.VERSION.SDK_INT >= 29
                && (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        cBg = night ? 0xFF101013 : 0xFFF4F4F7;
        cCard = night ? 0xFF1D1D22 : 0xFFFFFFFF;
        cText = night ? 0xFFEDEDF0 : 0xFF1B1B1F;
        cText2 = night ? 0xFFA6A6AE : 0xFF62626A;
        cLine = night ? 0x22FFFFFF : 0x14000000;
        cOk = night ? 0xFF66BB6A : 0xFF2E7D32;
        cWarn = night ? 0xFFFFB74D : 0xFFB26A00;
        cAccent = night ? 0xFF80CBC4 : 0xFF00796B;
        if (Build.VERSION.SDK_INT >= 31) {
            cAccent = getColor(night ? android.R.color.system_accent1_200
                    : android.R.color.system_accent1_600);
        }
        double lum = (0.299 * Color.red(cAccent) + 0.587 * Color.green(cAccent)
                + 0.114 * Color.blue(cAccent)) / 255.0;
        cOnAccent = lum > 0.55 ? 0xFF000000 : 0xFFFFFFFF;
    }

    @SuppressWarnings("deprecation")
    private void barColors() {
        getWindow().setStatusBarColor(cBg);
        getWindow().setNavigationBarColor(cBg);
        if (!night) {
            int f = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) f |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            getWindow().getDecorView().setSystemUiVisibility(f);
        }
    }

    // ---------- helpers de layout ----------

    int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    TextView text(CharSequence s, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    Drawable ripple() {
        return new RippleDrawable(ColorStateList.valueOf(0x33808080), null,
                new ColorDrawable(Color.WHITE));
    }

    void toast(int res) {
        Toast.makeText(this, res, Toast.LENGTH_SHORT).show();
    }

    private View header() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setPadding(dp(showBack() ? 8 : 20), dp(14), dp(16), dp(6));
        if (showBack()) {
            TextView back = text("←", 26, cText, false);
            back.setPadding(dp(12), dp(4), dp(12), dp(4));
            back.setContentDescription(getString(R.string.back));
            back.setBackground(ripple());
            back.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
            h.addView(back);
        }
        int t = titleRes();
        h.addView(text(getString(t == 0 ? R.string.app_name : t), showBack() ? 22 : 30, cText, true));
        return h;
    }

    /** Cartão arredondado, com um título de seção opcional acima. */
    LinearLayout card(LinearLayout parent, int titleRes) {
        if (titleRes != 0) {
            TextView t = text(getString(titleRes), 13, cAccent, true);
            t.setAllCaps(true);
            t.setLetterSpacing(0.06f);
            t.setPadding(dp(8), dp(22), dp(8), dp(8));
            parent.addView(t);
        }
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(8), dp(16), dp(10));
        GradientDrawable g = new GradientDrawable();
        g.setColor(cCard);
        g.setCornerRadius(dp(20));
        c.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.topMargin = dp(titleRes == 0 ? 12 : 0);
        parent.addView(c, lp);
        return c;
    }

    void divider(LinearLayout parent) {
        View v = new View(this);
        v.setBackgroundColor(cLine);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, dp(1));
        lp.topMargin = dp(6);
        lp.bottomMargin = dp(6);
        parent.addView(v, lp);
    }

    TextView note(LinearLayout parent, int res) {
        TextView t = text(getString(res), 13, cText2, false);
        t.setPadding(0, dp(8), 0, dp(4));
        parent.addView(t);
        return t;
    }

    private LinearLayout titleColumn(int titleRes, int summaryRes) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(text(getString(titleRes), 16, cText, false));
        if (summaryRes != 0) {
            TextView s = text(getString(summaryRes), 13, cText2, false);
            s.setPadding(0, dp(2), 0, 0);
            col.addView(s);
        }
        return col;
    }

    // ---------- linhas de configuração ----------

    Switch switchRow(LinearLayout parent, int titleRes, int summaryRes, final String key,
                     boolean def) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));
        row.addView(titleColumn(titleRes, summaryRes), new LinearLayout.LayoutParams(0, WRAP, 1f));
        final Switch sw = new Switch(this);
        sw.setChecked(sp.getBoolean(key, def));
        sw.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton b, boolean on) {
                sp.edit().putBoolean(key, on).apply();
            }
        });
        sw.setClickable(false);
        row.addView(sw);
        row.setBackground(ripple());
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sw.toggle();
            }
        });
        parent.addView(row);
        return sw;
    }

    void seekRow(LinearLayout parent, int labelRes, final String key, final int min, final int max,
                 int def, final String unit) {
        int cur = Math.max(min, Math.min(max, sp.getInt(key, def)));
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setPadding(0, dp(12), 0, 0);
        head.addView(text(getString(labelRes), 15, cText, false),
                new LinearLayout.LayoutParams(0, WRAP, 1f));
        final TextView val = text(cur + unit, 14, cAccent, true);
        head.addView(val);
        SeekBar sb = new SeekBar(this);
        sb.setMax(max - min);
        sb.setProgress(cur - min);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                int v = p + min;
                val.setText(v + unit);
                if (fromUser) sp.edit().putInt(key, v).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {}

            @Override
            public void onStopTrackingTouch(SeekBar s) {}
        });
        parent.addView(head);
        parent.addView(sb);
    }

    Spinner spinnerRow(LinearLayout parent, int labelRes, final String key, int def,
                       String[] items) {
        TextView l = text(getString(labelRes), 15, cText, false);
        l.setPadding(0, dp(12), 0, 0);
        parent.addView(l);
        Spinner s = new Spinner(this);
        s.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, items));
        s.setSelection(sp.getInt(key, def));
        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> a, View v, int pos, long id) {
                if (sp.getInt(key, -1) != pos) sp.edit().putInt(key, pos).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> a) {}
        });
        parent.addView(s);
        return s;
    }

    private void styleButton(Button b, boolean primary) {
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setMinHeight(dp(44));
        b.setStateListAnimator(null);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(14));
        if (primary) {
            g.setColor(cAccent);
            b.setTextColor(cOnAccent);
        } else {
            g.setColor(Color.TRANSPARENT);
            g.setStroke(dp(1), night ? 0x44FFFFFF : 0x33000000);
            b.setTextColor(cText);
        }
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33808080), g, null));
    }

    Button button(LinearLayout parent, int textRes, View.OnClickListener l, boolean primary) {
        Button b = new Button(this);
        b.setText(textRes);
        b.setOnClickListener(l);
        styleButton(b, primary);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.topMargin = dp(10);
        parent.addView(b, lp);
        return b;
    }

    /** Botão compacto para usar dentro de uma linha (não é adicionado a nenhum pai). */
    Button smallButton(int textRes, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(textRes);
        b.setOnClickListener(l);
        styleButton(b, true);
        b.setMinHeight(dp(36));
        b.setPadding(dp(14), 0, dp(14), 0);
        return b;
    }

    /** Linha de navegação: título, resumo e seta; abre outra tela. */
    void navRow(LinearLayout parent, int titleRes, int summaryRes, final Class<?> target) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, dp(12));
        row.addView(titleColumn(titleRes, summaryRes), new LinearLayout.LayoutParams(0, WRAP, 1f));
        row.addView(text("›", 24, cText2, false));
        row.setBackground(ripple());
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new android.content.Intent(BaseActivity.this, target));
            }
        });
        parent.addView(row);
    }
}
