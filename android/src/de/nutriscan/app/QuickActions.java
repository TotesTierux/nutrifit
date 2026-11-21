package de.nutriscan.app;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.service.quicksettings.TileService;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Schnellstart ohne geöffnete App (Kacheln, Widget, Benachrichtigung).
 * Die App spiegelt ihren Zustand hierher ("quick"), Aktionen von außen landen
 * als Ereignisse in einer Warteschlange ("events") und werden beim Öffnen übernommen.
 */
public final class QuickActions {
    private QuickActions() { }

    static synchronized JSONObject state(Context c) {
        try { return new JSONObject(Notifs.prefs(c).getString("quick", "{}")); }
        catch (Exception e) { return new JSONObject(); }
    }

    static synchronized void saveState(Context c, JSONObject s) {
        Notifs.prefs(c).edit().putString("quick", s.toString()).commit();
    }

    /** Von der App: aktueller Zustand (Gewohnheiten, Training, Kalorien). */
    static void setFromApp(Context c, String json) {
        try { saveState(c, new JSONObject(json)); } catch (Exception ignored) { }
        refresh(c);
    }

    static JSONObject habit(JSONObject s, String hid) {
        JSONArray hs = s.optJSONArray("habits");
        if (hs == null) return null;
        for (int i = 0; i < hs.length(); i++) {
            JSONObject h = hs.optJSONObject(i);
            if (h != null && hid.equals(h.optString("id"))) return h;
        }
        return null;
    }

    static JSONObject habitAt(Context c, int slot) {
        JSONArray hs = state(c).optJSONArray("habits");
        return hs != null && slot < hs.length() ? hs.optJSONObject(slot) : null;
    }

    /** Start/Stopp einer Gewohnheit von außerhalb der App. */
    static synchronized void toggle(Context c, String hid) {
        JSONObject s = state(c);
        JSONObject h = habit(s, hid);
        if (h == null) return;
        long now = System.currentTimeMillis();
        try {
            if (h.optLong("active", 0) > 0) {
                h.put("active", 0);
                event(c, "stop", hid, now);
                Notifs.clearHabit(c, hid);
            } else {
                h.put("active", now);
                event(c, "start", hid, now);
                Notifs.showHabit(c, hid, h.optString("ic") + " " + h.optString("name") + " läuft", now);
            }
        } catch (Exception ignored) { }
        saveState(c, s);
        refresh(c);
    }

    /** Stopp aus der Benachrichtigung. */
    static synchronized void stop(Context c, String hid) {
        JSONObject s = state(c);
        JSONObject h = habit(s, hid);
        try { if (h != null) h.put("active", 0); } catch (Exception ignored) { }
        saveState(c, s);
        event(c, "stop", hid, System.currentTimeMillis());
        Notifs.clearHabit(c, hid);
        refresh(c);
    }

    static synchronized void event(Context c, String type, String hid, long at) {
        try {
            JSONArray a = new JSONArray(Notifs.prefs(c).getString("habitStops", "[]"));
            JSONObject o = new JSONObject();
            o.put("t", type);
            o.put("h", hid);
            o.put("at", at);
            a.put(o);
            Notifs.prefs(c).edit().putString("habitStops", a.toString()).commit();
        } catch (Exception ignored) { }
    }

    /** Widget und Kacheln neu zeichnen. */
    static void refresh(Context c) {
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            int[] ids = m.getAppWidgetIds(new ComponentName(c, QuickWidget.class));
            if (ids.length > 0) QuickWidget.updateAll(c, m, ids);
        } catch (Exception ignored) { }
        try {
            TileService.requestListeningState(c, new ComponentName(c, HabitTile1.class));
            TileService.requestListeningState(c, new ComponentName(c, HabitTile2.class));
            TileService.requestListeningState(c, new ComponentName(c, TrainTile.class));
        } catch (Exception ignored) { }
    }

    static Intent openApp(Context c, String tab) {
        Intent i = new Intent(c, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra("openTab", tab);
        return i;
    }
}
