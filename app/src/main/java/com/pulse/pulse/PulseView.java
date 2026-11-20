package com.pulse.pulse;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;

/** Desenha o espectro: barras, barras em degradê, blocos ou onda. */
public class PulseView extends View {

    private static final int BLOCKS = 12;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();
    private final float[] hsv = new float[3];
    private final float density;

    private float[] cur = new float[0];
    private float[] target = new float[0];
    private float[] peak = new float[0];
    private Shader[] shaders = null;

    private int bars = 0;
    private int style = 0;
    private int colorMode = 0;
    private int customColor = Color.WHITE;
    private int accent = 0xFF80CBC4;
    private float gap = 0.35f;
    private float sens = 1f;
    private float radiusF = 1f;
    private float lavaSpeed = 0.3f;
    private float hueOffset = 0f;

    // cores da capa: palSrc chega de outra thread; palCur é o que está na tela e anima até palTgt
    private boolean coverMulti = true;
    private volatile int[] palSrc = null;
    private volatile int[] palTgt = new int[]{0xFF80CBC4, 0xFF80CBC4, 0xFF80CBC4};
    private final int[] palCur = new int[3];
    private boolean palInit = false;
    private boolean palChanged = false;
    private boolean palMoving = false;
    private boolean top = false;
    private boolean mirror = false;
    private boolean peaks = false;
    private long lastDraw = 0;

    public PulseView(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        paint.setStyle(Paint.Style.FILL);
        if (Build.VERSION.SDK_INT >= 31) {
            accent = context.getColor(android.R.color.system_accent1_300);
        }
    }

    void apply(SharedPreferences p) {
        int n = p.getInt(Prefs.BARS, Prefs.DEF_BARS);
        if (n != bars) {
            // o callback do Visualizer roda em outra thread: troca os arrays antes do contador
            cur = new float[n];
            target = new float[n];
            peak = new float[n];
            bars = n;
        }
        gap = p.getInt(Prefs.GAP, Prefs.DEF_GAP) / 100f;
        sens = p.getInt(Prefs.SENS, Prefs.DEF_SENS) / 100f;
        colorMode = p.getInt(Prefs.CMODE, Prefs.DEF_CMODE);
        customColor = p.getInt(Prefs.COLOR, Prefs.DEF_COLOR);
        top = p.getBoolean(Prefs.TOP, false);
        style = p.getInt(Prefs.STYLE, Prefs.DEF_STYLE);
        mirror = p.getBoolean(Prefs.MIRROR, false);
        peaks = p.getBoolean(Prefs.PEAKS, false);
        radiusF = p.getInt(Prefs.RADIUS, Prefs.DEF_RADIUS) / 100f;
        lavaSpeed = p.getInt(Prefs.LAVA, Prefs.DEF_LAVA) / 100f;
        coverMulti = p.getBoolean(Prefs.COVER_MULTI, true);
        computeTarget();
        if (!palInit) {
            System.arraycopy(palTgt, 0, palCur, 0, 3);
            palInit = true;
        }
        setAlpha(p.getInt(Prefs.ALPHA, Prefs.DEF_ALPHA) / 100f);
        rebuildShaders();
        invalidate();
    }

    private int bands() {
        return mirror ? (bars + 1) / 2 : bars;
    }

    private int indexFor(int s) {
        if (!mirror) return s;
        int half = (bars + 1) / 2;
        return s < half ? half - 1 - s : s - half;
    }

    private int hueColor(float hue) {
        hsv[0] = ((hue % 360f) + 360f) % 360f;
        hsv[1] = 0.65f;
        hsv[2] = 1f;
        return Color.HSVToColor(hsv);
    }

    private int colorFor(int i, int count) {
        switch (colorMode) {
            case 1:
                return customColor;
            case 2:
                return hueColor(360f * i / Math.max(1, count));
            case 3:
                return hueColor(hueOffset + 140f * i / Math.max(1, count));
            case 4:
                return paletteAt(count <= 1 ? 0f : (float) i / (count - 1));
            default:
                return accent;
        }
    }

    /** Degradê: um shader por banda, só recriado quando a config/tamanho muda (ou a cor anima). */
    private void rebuildShaders() {
        int h = getHeight();
        int n = bands();
        if (style != 1 || h == 0 || n == 0) {
            shaders = null;
            return;
        }
        shaders = new Shader[n];
        float y0 = top ? 0f : h;
        float y1 = top ? h : 0f;
        for (int i = 0; i < n; i++) {
            int c = colorFor(i, n);
            int tip = (c & 0x00FFFFFF) | 0x26000000;
            shaders[i] = new LinearGradient(0f, y0, 0f, y1, c, tip, Shader.TileMode.CLAMP);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        rebuildShaders();
    }

    /** Define as cores da capa (1 a 3, a dominante primeiro); null volta à cor do tema. Pode vir de qualquer thread. */
    void setPalette(int[] colors) {
        palSrc = colors;
        computeTarget();
        postInvalidateOnAnimation();
    }

    private void computeTarget() {
        int[] s = palSrc;
        int[] t = new int[3];
        if (s == null || s.length == 0) {
            t[0] = accent;
            t[1] = accent;
            t[2] = accent;
        } else if (!coverMulti || s.length == 1) {
            t[0] = s[0];
            t[1] = s[0];
            t[2] = s[0];
        } else if (s.length == 2) {
            t[0] = s[0];
            t[1] = mix(s[0], s[1], 0.5f);
            t[2] = s[1];
        } else {
            t[0] = s[0];
            t[1] = s[1];
            t[2] = s[2];
        }
        palTgt = t;
    }

    /** Aproxima as cores exibidas das cores-alvo (transição suave ao trocar de faixa). */
    private void stepPalette(float dt) {
        int[] tgt = palTgt;
        if (!palInit) {
            System.arraycopy(tgt, 0, palCur, 0, 3);
            palInit = true;
        }
        float k = 1f - (float) Math.exp(-dt / 0.28f);
        boolean changed = false;
        boolean moving = false;
        for (int i = 0; i < 3; i++) {
            int c = palCur[i];
            int t = tgt[i];
            if (c == t) continue;
            int n = Color.rgb(stepCh(Color.red(c), Color.red(t), k),
                    stepCh(Color.green(c), Color.green(t), k),
                    stepCh(Color.blue(c), Color.blue(t), k));
            palCur[i] = n;
            changed = true;
            if (n != t) moving = true;
        }
        palChanged = changed;
        palMoving = moving;
    }

    private static int stepCh(int a, int b, float k) {
        int d = b - a;
        if (Math.abs(d) <= 2) return b;
        int v = a + Math.round(d * k);
        if (v == a) v = a + (d > 0 ? 1 : -1);
        return v;
    }

    private static int mix(int a, int b, float t) {
        return Color.rgb(
                Math.round(Color.red(a) + (Color.red(b) - Color.red(a)) * t),
                Math.round(Color.green(a) + (Color.green(b) - Color.green(a)) * t),
                Math.round(Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t));
    }

    /** Cor da paleta na posição 0..1 (0 = primeira cor, 1 = última). */
    private int paletteAt(float pos) {
        float x = Math.max(0f, Math.min(1f, pos)) * 2f;
        if (x <= 1f) return mix(palCur[0], palCur[1], x);
        return mix(palCur[1], palCur[2], x - 1f);
    }

    /** Recebe o FFT do Visualizer e converte em níveis 0..1 por banda (escala logarítmica). */
    void setFft(byte[] fft) {
        float[] tg = target;
        int n = Math.min(bands(), tg.length);
        int total = fft.length / 2;
        int limit = Math.min(total - 1, 160);
        if (n == 0 || limit < 8) return;
        for (int i = 0; i < n; i++) {
            int lo = (int) Math.pow(limit, (double) i / n);
            int hi = (int) Math.pow(limit, (double) (i + 1) / n);
            if (lo < 1) lo = 1;
            if (hi <= lo) hi = lo + 1;
            float pk = 0f;
            for (int k = lo; k < hi && k <= limit; k++) {
                float re = fft[2 * k];
                float im = fft[2 * k + 1];
                float m = (float) Math.sqrt(re * re + im * im);
                if (m > pk) pk = m;
            }
            float v = (float) Math.sqrt(pk / 128f) * (1f + 0.8f * i / n) * sens;
            tg[i] = v > 1f ? 1f : v;
        }
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0 || bars == 0 || cur.length < bars) return;

        long now = SystemClock.uptimeMillis();
        float dt = lastDraw == 0 ? 0.016f : Math.min(0.1f, (now - lastDraw) / 1000f);
        lastDraw = now;
        if (colorMode == 3) hueOffset = (hueOffset + (10f + 170f * lavaSpeed) * dt) % 360f;
        stepPalette(dt);

        int n = bands();
        boolean busy = false;
        for (int i = 0; i < n; i++) {
            float d = target[i] - cur[i];
            cur[i] += d * (d > 0 ? 0.6f : 0.15f);
            if (Math.abs(d) > 0.004f || cur[i] > 0.004f) busy = true;
            if (cur[i] >= peak[i]) {
                peak[i] = cur[i];
            } else {
                peak[i] -= 0.008f;
                if (peak[i] < cur[i]) peak[i] = cur[i];
            }
            if (peaks && peak[i] > 0.004f) busy = true;
        }

        if (style == 3) drawWave(c, w, h, n);
        else if (style == 2) drawBlocks(c, w, h, n);
        else drawBars(c, w, h, n);

        if (busy || (colorMode == 4 && palMoving)) postInvalidateOnAnimation();
    }

    private void drawBars(Canvas c, int w, int h, int n) {
        if (style == 1 && (colorMode == 3 || (colorMode == 4 && palChanged))) rebuildShaders();
        float slot = (float) w / bars;
        float bw = slot * (1f - gap);
        float pad = (slot - bw) / 2f;
        float r = bw / 2f * radiusF;
        float capH = 3f * density;
        boolean grad = style == 1 && shaders != null;

        for (int s = 0; s < bars; s++) {
            int idx = indexFor(s);
            float l = s * slot + pad;
            float bh = cur[idx] * h;
            if (bh >= 1f) {
                if (grad && idx < shaders.length) {
                    paint.setShader(shaders[idx]);
                } else {
                    paint.setShader(null);
                    paint.setColor(colorFor(idx, n));
                }
                if (top) {
                    rect.set(l, -r, l + bw, bh);
                } else {
                    rect.set(l, h - bh, l + bw, h + r);
                }
                c.drawRoundRect(rect, r, r, paint);
            }
            if (peaks) {
                float ph = peak[idx] * h;
                if (ph > bh + 1f) {
                    paint.setShader(null);
                    paint.setColor(colorFor(idx, n));
                    if (top) {
                        rect.set(l, ph, l + bw, ph + capH);
                    } else {
                        rect.set(l, h - ph - capH, l + bw, h - ph);
                    }
                    c.drawRoundRect(rect, capH / 2f * radiusF, capH / 2f * radiusF, paint);
                }
            }
        }
        paint.setShader(null);
    }

    /** Estilo "blocos que esmaecem": cada barra vira uma pilha de blocos, mais transparentes na ponta. */
    private void drawBlocks(Canvas c, int w, int h, int n) {
        float slot = (float) w / bars;
        float bw = slot * (1f - gap);
        float pad = (slot - bw) / 2f;
        float blockH = (float) h / BLOCKS;
        float bgap = Math.max(1f, blockH * 0.18f);
        float rr = Math.min(bw / 2f, blockH / 2f) * radiusF;
        paint.setShader(null);

        for (int s = 0; s < bars; s++) {
            int idx = indexFor(s);
            float l = s * slot + pad;
            int col = colorFor(idx, n);
            int lit = (int) Math.ceil(cur[idx] * BLOCKS - 0.001f);
            if (lit > BLOCKS) lit = BLOCKS;
            for (int j = 0; j < lit; j++) {
                float a = 1f - 0.7f * j / (float) BLOCKS;
                paint.setColor(col);
                paint.setAlpha((int) (255f * a));
                blockRect(l, bw, j, blockH, bgap, h);
                c.drawRoundRect(rect, rr, rr, paint);
            }
            if (peaks) {
                int pj = (int) (peak[idx] * BLOCKS);
                if (pj > lit && pj <= BLOCKS) {
                    paint.setColor(col);
                    paint.setAlpha(255);
                    blockRect(l, bw, pj - 1, blockH, bgap, h);
                    c.drawRoundRect(rect, rr, rr, paint);
                }
            }
        }
    }

    private void blockRect(float l, float bw, int j, float blockH, float bgap, int h) {
        float y1;
        float y2;
        if (top) {
            y1 = j * blockH;
            y2 = y1 + blockH - bgap;
        } else {
            y2 = h - j * blockH;
            y1 = y2 - blockH + bgap;
        }
        rect.set(l, y1, l + bw, y2);
    }

    /** Estilo "onda": curva suave preenchida passando pelo topo das barras. */
    private void drawWave(Canvas c, int w, int h, int n) {
        float slot = (float) w / bars;
        float baseY = top ? 0f : h;
        path.rewind();
        path.moveTo(0f, baseY);
        path.lineTo(0f, yFor(indexFor(0), h));
        for (int s = 0; s < bars - 1; s++) {
            float xs = slot * (s + 0.5f);
            float ys = yFor(indexFor(s), h);
            float xn = slot * (s + 1.5f);
            float yn = yFor(indexFor(s + 1), h);
            path.quadTo(xs, ys, (xs + xn) / 2f, (ys + yn) / 2f);
        }
        path.lineTo(w, yFor(indexFor(bars - 1), h));
        path.lineTo(w, baseY);
        path.close();

        if (colorMode == 2 || colorMode == 3) {
            int stops = 7;
            int[] cs = new int[stops];
            float[] ps = new float[stops];
            for (int k = 0; k < stops; k++) {
                float pos = (float) k / (stops - 1);
                ps[k] = pos;
                cs[k] = colorMode == 2 ? hueColor(360f * pos) : hueColor(hueOffset + 140f * pos);
            }
            paint.setShader(new LinearGradient(0f, 0f, w, 0f, cs, ps, Shader.TileMode.CLAMP));
        } else if (colorMode == 4) {
            int[] cs;
            float[] ps;
            if (mirror) {
                cs = new int[]{palCur[2], palCur[1], palCur[0], palCur[1], palCur[2]};
                ps = new float[]{0f, 0.25f, 0.5f, 0.75f, 1f};
            } else {
                cs = new int[]{palCur[0], palCur[1], palCur[2]};
                ps = new float[]{0f, 0.5f, 1f};
            }
            paint.setShader(new LinearGradient(0f, 0f, w, 0f, cs, ps, Shader.TileMode.CLAMP));
        } else {
            int col = colorFor(0, n);
            int tip = (col & 0x00FFFFFF) | 0x59000000;
            paint.setShader(new LinearGradient(0f, baseY, 0f, top ? h : 0f, col, tip,
                    Shader.TileMode.CLAMP));
        }
        c.drawPath(path, paint);
        paint.setShader(null);
    }

    private float yFor(int idx, int h) {
        float v = cur[idx] * h;
        return top ? v : h - v;
    }
}
