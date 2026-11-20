package com.pulse.pulse;

import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class PulseTileService extends TileService {

    @Override
    public void onStartListening() {
        update();
    }

    @Override
    public void onClick() {
        SharedPreferences sp = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);
        sp.edit().putBoolean(Prefs.ENABLED, !sp.getBoolean(Prefs.ENABLED, true)).apply();
        update();
    }

    private void update() {
        Tile t = getQsTile();
        if (t == null) return;
        SharedPreferences sp = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);
        boolean on = sp.getBoolean(Prefs.ENABLED, true);
        t.setState(on ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        t.setLabel("Pulse");
        t.setIcon(Icon.createWithResource(this, R.drawable.ic_pulse));
        t.updateTile();
    }
}
