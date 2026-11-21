package de.nutriscan.app;

import android.app.PendingIntent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** Kachel „Training“: öffnet den Trainingsbereich (oder das laufende Training). */
public class TrainTile extends TileService {
    @Override
    public void onStartListening() {
        Tile t = getQsTile();
        if (t == null) return;
        boolean running = QuickActions.state(this).optBoolean("training", false);
        t.setIcon(Icon.createWithResource(this, R.drawable.ic_stat));
        t.setLabel("🏋️ Training");
        t.setState(running ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        if (Build.VERSION.SDK_INT >= 29) t.setSubtitle(running ? "läuft – fortsetzen" : "starten");
        t.updateTile();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onClick() {
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 320, QuickActions.openApp(this, "train"),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        } else {
            startActivityAndCollapse(QuickActions.openApp(this, "train"));
        }
    }
}
