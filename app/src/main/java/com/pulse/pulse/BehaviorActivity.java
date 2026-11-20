package com.pulse.pulse;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Idioma, filtro de apps, desempenho e backup dos ajustes. */
public class BehaviorActivity extends BaseActivity {

    private static final class AppItem {
        final String label;
        final String pkg;

        AppItem(String label, String pkg) {
            this.label = label;
            this.pkg = pkg;
        }
    }

    private Button appsBtn;

    @Override
    int titleRes() {
        return R.string.nav_behavior;
    }

    @Override
    void build(LinearLayout c) {
        // ---- geral ----
        LinearLayout general = card(c, R.string.sec_general);
        switchRow(general, R.string.sw_lock, R.string.sw_lock_sum, Prefs.LOCK, true);
        switchRow(general, R.string.sw_hide_land, R.string.sw_hide_land_sum, Prefs.HIDE_LAND, false);

        // ---- filtro de apps ----
        LinearLayout apps = card(c, R.string.sec_apps);
        spinnerRow(apps, R.string.spin_where, Prefs.APPMODE, 0, new String[]{
                getString(R.string.apps_all), getString(R.string.apps_hide),
                getString(R.string.apps_only)});
        appsBtn = button(apps, R.string.btn_pick_apps, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickApps();
            }
        }, false);
        updateAppsButton();
        note(apps, R.string.apps_note);

        // ---- desempenho ----
        LinearLayout perf = card(c, R.string.sec_perf);
        seekRow(perf, R.string.seek_fps, Prefs.FPS, 5, 30, Prefs.DEF_FPS, " fps");
        note(perf, R.string.fps_note);
        button(perf, R.string.btn_battery, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                } catch (Exception e) {
                    toast(R.string.toast_cant_open);
                }
            }
        }, false);
        note(perf, R.string.battery_note);

        // ---- backup ----
        LinearLayout backup = card(c, R.string.sec_backup);
        note(backup, R.string.backup_note);
        button(backup, R.string.btn_export, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doExport();
            }
        }, false);
        button(backup, R.string.btn_import, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doImport();
            }
        }, false);
        button(backup, R.string.btn_reset_all, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmResetAll();
            }
        }, false);
    }

    // ---------- backup ----------

    private void doExport() {
        try {
            String json = Backup.export(sp);
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Pulse", json));
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_TEXT, json);
            startActivity(Intent.createChooser(send, getString(R.string.btn_export)));
            toast(R.string.toast_copied);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.toast_export_fail, e.getMessage()),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void doImport() {
        final EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        et.setMinLines(5);
        et.setHint(R.string.import_hint);
        et.setTextSize(12);
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                CharSequence t = cm.getPrimaryClip().getItemAt(0).coerceToText(this);
                if (t != null && t.toString().contains("\"app\"")) et.setText(t);
            }
        } catch (Exception ignored) {
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.btn_import)
                .setView(et)
                .setPositiveButton(R.string.btn_import, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        try {
                            int n = Backup.importJson(sp, et.getText().toString());
                            Toast.makeText(BehaviorActivity.this,
                                    getString(R.string.toast_import_ok, n), Toast.LENGTH_SHORT).show();
                            recreate();
                        } catch (Exception e) {
                            toast(R.string.toast_import_bad);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmResetAll() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_all_title)
                .setMessage(R.string.reset_all_msg)
                .setPositiveButton(R.string.reset_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        sp.edit().clear().apply();
                        recreate();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ---------- filtro de apps ----------

    private void updateAppsButton() {
        Set<String> s = sp.getStringSet(Prefs.APPS, null);
        int n = s == null ? 0 : s.size();
        appsBtn.setText(getString(R.string.btn_pick_apps_n, n));
    }

    private void pickApps() {
        PackageManager pm = getPackageManager();
        Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> ris = pm.queryIntentActivities(q, 0);
        final List<AppItem> items = new ArrayList<AppItem>();
        Set<String> seen = new HashSet<String>();
        for (ResolveInfo ri : ris) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || !seen.add(pkg)) continue;
            items.add(new AppItem(ri.loadLabel(pm).toString(), pkg));
        }
        Collections.sort(items, new Comparator<AppItem>() {
            @Override
            public int compare(AppItem a, AppItem b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });

        Set<String> cur = sp.getStringSet(Prefs.APPS, null);
        final String[] labels = new String[items.size()];
        final boolean[] checked = new boolean[items.size()];
        for (int i = 0; i < items.size(); i++) {
            labels[i] = items.get(i).label;
            checked[i] = cur != null && cur.contains(items.get(i).pkg);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_pick_title)
                .setMultiChoiceItems(labels, checked,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which, boolean isChecked) {
                                checked[which] = isChecked;
                            }
                        })
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        Set<String> out = new HashSet<String>();
                        for (int i = 0; i < items.size(); i++) {
                            if (checked[i]) out.add(items.get(i).pkg);
                        }
                        sp.edit().putStringSet(Prefs.APPS, out).apply();
                        updateAppsButton();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
