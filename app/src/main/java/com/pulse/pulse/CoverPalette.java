package com.pulse.pulse;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;

import java.util.Arrays;

/**
 * Extrai de uma capa até 3 cores marcantes, sem bibliotecas externas.
 *
 * Reduz a imagem a ~48 px, agrupa os pixels coloridos em 12 faixas de matiz (30° cada) e escolhe
 * as faixas com mais "cor" (saturação x brilho), com matizes bem diferentes entre si. Cada cor sai
 * com saturação e brilho mínimos, para continuar visível sobre qualquer app. Capas em preto e
 * branco viram tons de cinza claros.
 */
final class CoverPalette {
    private static final int SIZE = 48;
    private static final int BINS = 12;

    private CoverPalette() {}

    static int[] fromBitmap(Bitmap src) {
        if (src == null || src.isRecycled() || src.getWidth() <= 0 || src.getHeight() <= 0) {
            return null;
        }
        Bitmap soft = src;
        boolean copied = false;
        if (Build.VERSION.SDK_INT >= 26 && src.getConfig() == Bitmap.Config.HARDWARE) {
            soft = src.copy(Bitmap.Config.ARGB_8888, false);
            if (soft == null) return null;
            copied = true;
        }
        float scale = (float) SIZE / Math.max(soft.getWidth(), soft.getHeight());
        int tw = Math.max(1, Math.round(soft.getWidth() * scale));
        int th = Math.max(1, Math.round(soft.getHeight() * scale));
        Bitmap small = Bitmap.createScaledBitmap(soft, tw, th, true);
        int[] px = new int[tw * th];
        small.getPixels(px, 0, tw, 0, 0, tw, th);
        if (small != soft) small.recycle();
        if (copied) soft.recycle();
        return fromPixels(px);
    }

    static int[] fromPixels(int[] px) {
        float[] wSum = new float[BINS];
        float[] rSum = new float[BINS];
        float[] gSum = new float[BINS];
        float[] bSum = new float[BINS];
        float[] hsv = new float[3];
        float neutralV = 0f;
        int neutralN = 0;
        int opaque = 0;

        for (int i = 0; i < px.length; i++) {
            int p = px[i];
            if (Color.alpha(p) < 128) continue;
            opaque++;
            Color.colorToHSV(p, hsv);
            float s = hsv[1];
            float v = hsv[2];
            if (v < 0.15f) continue;            // quase preto: não conta como cor
            if (s < 0.10f) {                    // cinza ou branco (pastel ainda conta como cor)
                neutralV += v;
                neutralN++;
                continue;
            }
            float w = s * (0.35f + v);
            int bin = Math.min(BINS - 1, (int) (hsv[0] / (360f / BINS)));
            wSum[bin] += w;
            rSum[bin] += w * Color.red(p);
            gSum[bin] += w * Color.green(p);
            bSum[bin] += w * Color.blue(p);
        }
        if (opaque == 0) return null;

        float total = 0f;
        for (int b = 0; b < BINS; b++) total += wSum[b];
        if (total < 0.03f * opaque) {
            // quase sem cor: capa em preto e branco
            float v = neutralN > 0 ? neutralV / neutralN : 0.8f;
            v = Math.max(0.6f, Math.min(0.95f, v));
            return new int[]{gray(v), gray(v - 0.14f), gray(v - 0.28f)};
        }

        int[] chosen = new int[3];
        int[] chosenBin = new int[3];
        int count = 0;
        float top = 0f;
        for (int round = 0; round < 3; round++) {
            int best = -1;
            float bw = 0f;
            for (int b = 0; b < BINS; b++) {
                if (wSum[b] <= 0f) continue;
                boolean ok = true;
                for (int k = 0; k < count; k++) {
                    int d = Math.abs(b - chosenBin[k]);
                    d = Math.min(d, BINS - d);
                    if (d < 2) {            // matiz parecido demais com um já escolhido
                        ok = false;
                        break;
                    }
                }
                if (ok && wSum[b] > bw) {
                    bw = wSum[b];
                    best = b;
                }
            }
            if (best < 0) break;
            if (count > 0 && bw < 0.18f * top) break;   // faixa pequena demais
            if (count == 0) top = bw;
            chosenBin[count] = best;
            chosen[count] = vivid(rSum[best] / wSum[best], gSum[best] / wSum[best],
                    bSum[best] / wSum[best]);
            count++;
        }
        return Arrays.copyOf(chosen, count);
    }

    /** Garante saturação e brilho mínimos para a cor aparecer sobre qualquer fundo. */
    private static int vivid(float r, float g, float b) {
        float[] hsv = new float[3];
        Color.RGBToHSV(Math.round(r), Math.round(g), Math.round(b), hsv);
        hsv[1] = Math.min(1f, Math.max(hsv[1], 0.45f));
        hsv[2] = Math.min(1f, Math.max(hsv[2], 0.72f));
        return Color.HSVToColor(hsv);
    }

    private static int gray(float v) {
        int c = Math.round(255f * Math.max(0.3f, Math.min(1f, v)));
        return Color.rgb(c, c, c);
    }
}
