package com.pulse.pulse;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.provider.Settings;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/** Atalhos opcionais (precisam de root): concedem permissões e ativam serviços do Pulse. */
final class RootSetup {
    private RootSetup() {}

    /** Áudio, configurações restritas, isenção de bateria e ativação do serviço de Acessibilidade. */
    static void run(final BaseActivity a) {
        final String pkg = a.getPackageName();
        String comp = new ComponentName(a, PulseService.class).flattenToString();
        String cur = Settings.Secure.getString(a.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        String val;
        if (cur == null || cur.isEmpty() || cur.equals("null")) val = comp;
        else if (cur.contains(pkg + "/")) val = cur;
        else val = cur + ":" + comp;

        exec(a,
                "pm grant " + pkg + " android.permission.RECORD_AUDIO; "
                + "appops set " + pkg + " ACCESS_RESTRICTED_SETTINGS allow; "
                + "appops set " + pkg + " RECORD_AUDIO allow; "
                + "appops set " + pkg + " RUN_IN_BACKGROUND allow; "
                + "appops set " + pkg + " RUN_ANY_IN_BACKGROUND allow; "
                + "cmd deviceidle whitelist +" + pkg + "; "
                + "am set-standby-bucket " + pkg + " active; "
                + "settings put secure enabled_accessibility_services '" + val + "'; "
                + "settings put secure accessibility_enabled 1; "
                + "echo FIM");
    }

    /** Só o acesso às notificações, usado pelo modo "cores da capa". */
    static void grantCoverAccess(final BaseActivity a) {
        String pkg = a.getPackageName();
        String comp = new ComponentName(a, PulseMediaListener.class).flattenToString();
        exec(a,
                "appops set " + pkg + " ACCESS_RESTRICTED_SETTINGS allow; "
                + "cmd notification allow_listener " + comp + "; "
                + "echo FIM");
    }

    private static void exec(final BaseActivity a, final String script) {
        Toast.makeText(a, R.string.toast_root, Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String result;
                try {
                    Process p = new ProcessBuilder("su", "-c", script)
                            .redirectErrorStream(true).start();
                    StringBuilder sb = new StringBuilder();
                    BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line).append('\n');
                    p.waitFor();
                    result = sb.toString().trim();
                } catch (Exception e) {
                    result = a.getString(R.string.root_unavailable, e.getMessage());
                }
                final String msg = result;
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(a)
                                .setTitle(R.string.root_result_title)
                                .setMessage(msg)
                                .setPositiveButton(R.string.ok, null)
                                .show();
                        a.onRootDone();
                    }
                });
            }
        }).start();
    }
}
