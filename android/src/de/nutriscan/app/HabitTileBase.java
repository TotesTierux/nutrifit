package de.nutriscan.app;

import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Kachel in den Schnelleinstellungen: Gewohnheit Nr. slot() starten/stoppen. */
public abstract class HabitTileBase extends TileService {
    abstract int slot();

    @Override
    public void onStartListening() { draw(); }

    @Override
    public void onClick() {
        JSONObject h = QuickActions.habitAt(this, slot());
        if (h == null) {
            QuickActions.openApp(this, "today");
            return;
        }
        QuickActions.toggle(this, h.optString("id"));
        draw();
    }

    private void draw() {
        Tile t = getQsTile();
        if (t == null) return;
        JSONObject h = QuickActions.habitAt(this, slot());
        t.setIcon(Icon.createWithResource(this, R.drawable.ic_stat));
        if (h == null) {
            t.setLabel("Gewohnheit " + (slot() + 1));
            t.setState(Tile.STATE_UNAVAILABLE);
            if (Build.VERSION.SDK_INT >= 29) t.setSubtitle("In NutriFit anlegen");
        } else {
            long active = h.optLong("active", 0);
            t.setLabel(h.optString("ic") + " " + h.optString("name"));
            t.setState(active > 0 ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            if (Build.VERSION.SDK_INT >= 29) {
                t.setSubtitle(active > 0
                        ? "seit " + new SimpleDateFormat("HH:mm", Locale.GERMANY).format(new Date(active))
                        : "Tippen zum Starten");
            }
        }
        t.updateTile();
    }
}
