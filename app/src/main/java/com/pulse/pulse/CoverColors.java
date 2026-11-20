package com.pulse.pulse;

import android.content.Context;
import android.provider.Settings;

/** Cores da capa atual: ponte entre o leitor de mídia (PulseMediaListener) e as barras. */
final class CoverColors {

    interface Listener {
        void onCoverColors(int[] colors);
    }

    /** Paleta atual (1 a 3 cores, a dominante primeiro); null = nenhuma capa disponível. */
    static volatile int[] colors = null;

    private static volatile Listener listener;

    private CoverColors() {}

    static void setListener(Listener l) {
        listener = l;
    }

    static void publish(int[] c) {
        colors = c;
        Listener l = listener;
        if (l != null) l.onCoverColors(c);
    }

    /** O acesso às notificações é o que o Android exige para ler a mídia que está tocando. */
    static boolean accessGranted(Context c) {
        String flat = Settings.Secure.getString(c.getContentResolver(),
                "enabled_notification_listeners");
        return flat != null && flat.contains(c.getPackageName() + "/");
    }
}
