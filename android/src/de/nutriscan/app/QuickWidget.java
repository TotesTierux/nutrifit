package de.nutriscan.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

/** Homescreen-Widget: Gewohnheiten starten/stoppen, Training öffnen, Kalorien des Tages. */
public class QuickWidget extends AppWidgetProvider {
    private static final int[] ROW = {R.id.row0, R.id.row1, R.id.row2};
    private static final int[] NAME = {R.id.name0, R.id.name1, R.id.name2};
    private static final int[] CHRONO = {R.id.chrono0, R.id.chrono1, R.id.chrono2};
    private static final int[] ACT = {R.id.act0, R.id.act1, R.id.act2};

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        updateAll(c, m, ids);
    }

    static void updateAll(Context c, AppWidgetManager m, int[] ids) {
        JSONObject s = QuickActions.state(c);
        JSONArray hs = s.optJSONArray("habits");
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget);
        long nowWall = System.currentTimeMillis(), nowEl = SystemClock.elapsedRealtime();
        for (int i = 0; i < ROW.length; i++) {
            JSONObject h = hs != null && i < hs.length() ? hs.optJSONObject(i) : null;
            if (h == null) { v.setViewVisibility(ROW[i], View.GONE); continue; }
            v.setViewVisibility(ROW[i], View.VISIBLE);
            String hid = h.optString("id");
            long active = h.optLong("active", 0);
            v.setTextViewText(NAME[i], h.optString("ic") + "  " + h.optString("name"));
            if (active > 0) {
                v.setViewVisibility(CHRONO[i], View.VISIBLE);
                v.setChronometer(CHRONO[i], nowEl - (nowWall - active), null, true);
                v.setTextViewText(ACT[i], "■ Stopp");
                v.setInt(ROW[i], "setBackgroundResource", R.drawable.row_active);
            } else {
                v.setViewVisibility(CHRONO[i], View.GONE);
                v.setChronometer(CHRONO[i], nowEl, null, false);
                v.setTextViewText(ACT[i], "▶ Start");
                v.setInt(ROW[i], "setBackgroundResource", R.drawable.row_bg);
            }
            Intent t = new Intent(c, QuickReceiver.class);
            t.setAction("de.nutriscan.app.TOGGLE." + hid);
            t.putExtra("hid", hid);
            v.setOnClickPendingIntent(ROW[i], PendingIntent.getBroadcast(c, 300 + i, t, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }
        boolean training = s.optBoolean("training", false);
        v.setTextViewText(R.id.train, training ? "🏋️  Training läuft – fortsetzen" : "🏋️  Training starten");
        v.setOnClickPendingIntent(R.id.train, PendingIntent.getActivity(c, 310, QuickActions.openApp(c, "train"), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        v.setTextViewText(R.id.kcal, s.optString("kcal", "NutriFit"));
        v.setOnClickPendingIntent(R.id.kcal, PendingIntent.getActivity(c, 311, QuickActions.openApp(c, "today"), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        m.updateAppWidget(ids, v);
    }
}
