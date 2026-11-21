package de.nutriscan.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

/** Plant und zeigt die Fasten-Benachrichtigungen. Der Zeitplan kommt aus der Web-App. */
public final class Notifs {
    static final String CH_ALERT = "fast_alerts";
    static final String CH_STATUS = "fast_status";
    static final int ONGOING_ID = 9000;
    private static final String PREFS = "nutriscan_notifs";

    private Notifs() { }

    static void ensureChannels(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CH_ALERT) == null) {
            NotificationChannel a = new NotificationChannel(CH_ALERT, "Fasten-Erinnerungen", NotificationManager.IMPORTANCE_DEFAULT);
            a.setDescription("Meilensteine, Ziel erreicht, Essfenster");
            a.enableLights(true);
            a.setLightColor(Color.parseColor("#14B8A6"));
            nm.createNotificationChannel(a);
        }
        if (nm.getNotificationChannel(CH_STATUS) == null) {
            NotificationChannel s = new NotificationChannel(CH_STATUS, "Fasten-Timer", NotificationManager.IMPORTANCE_LOW);
            s.setDescription("Laufender Timer während des Fastens");
            s.setShowBadge(false);
            nm.createNotificationChannel(s);
        }
    }

    private static PendingIntent openApp(Context c, int req) {
        Intent i = new Intent(c, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra("openTab", req >= 7000 && req < 9000 ? "today" : "fast");
        return PendingIntent.getActivity(c, req, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static Notification.Builder builder(Context c, String channel) {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, channel) : new Notification.Builder(c);
        b.setSmallIcon(R.drawable.ic_stat).setColor(Color.parseColor("#0F766E"));
        return b;
    }

    static void show(Context c, int id, String title, String text) {
        ensureChannels(c);
        Notification n = builder(c, CH_ALERT)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(openApp(c, id))
                .build();
        c.getSystemService(NotificationManager.class).notify(id, n);
    }

    /** Laufende Benachrichtigung mit Countdown bis zum Fastenziel (zählt danach hoch). */
    static void showOngoing(Context c, long startMs, long goalMs, String title, String text) {
        ensureChannels(c);
        boolean reached = System.currentTimeMillis() >= goalMs;
        Notification.Builder b = builder(c, CH_STATUS)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setContentIntent(openApp(c, ONGOING_ID));
        if (!reached && Build.VERSION.SDK_INT >= 24) {
            b.setWhen(goalMs).setChronometerCountDown(true);
        } else {
            b.setWhen(startMs);
        }
        c.getSystemService(NotificationManager.class).notify(ONGOING_ID, b.build());
        prefs(c).edit().putString("ongoing", startMs + "|" + goalMs + "|" + title + "|" + text).apply();
    }

    static void clearOngoing(Context c) {
        c.getSystemService(NotificationManager.class).cancel(ONGOING_ID);
        prefs(c).edit().remove("ongoing").apply();
    }

    static int habitSlot(String hid) { return Math.abs(hid.hashCode() % 500); }

    /** Laufende Gewohnheits-Session mit hochzählendem Timer und Stopp-Knopf. */
    static void showHabit(Context c, String hid, String title, long startMs) {
        ensureChannels(c);
        Intent stop = new Intent(c, HabitStopReceiver.class);
        stop.setAction("de.nutriscan.app.HABIT_STOP." + hid);
        stop.putExtra("hid", hid);
        stop.putExtra("name", title.replace(" läuft", ""));
        PendingIntent pStop = PendingIntent.getBroadcast(c, 7000 + habitSlot(hid), stop, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = builder(c, CH_STATUS)
                .setContentTitle(title)
                .setContentText("Tippe auf Stopp, wenn du fertig bist")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setWhen(startMs)
                .setContentIntent(openApp(c, 7600 + habitSlot(hid)))
                .addAction(new Notification.Action.Builder(null, "■ Stopp", pStop).build());
        c.getSystemService(NotificationManager.class).notify(7000 + habitSlot(hid), b.build());
    }

    static void clearHabit(Context c, String hid) {
        c.getSystemService(NotificationManager.class).cancel(7000 + habitSlot(hid));
    }

    static synchronized void addHabitStop(Context c, String hid, long at) {
        try {
            JSONArray a = new JSONArray(prefs(c).getString("habitStops", "[]"));
            JSONObject o = new JSONObject();
            o.put("h", hid);
            o.put("at", at);
            a.put(o);
            prefs(c).edit().putString("habitStops", a.toString()).commit();
        } catch (Exception ignored) { }
    }

    static synchronized String takeHabitStops(Context c) {
        String s = prefs(c).getString("habitStops", "[]");
        prefs(c).edit().remove("habitStops").commit();
        return s;
    }

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Ersetzt alle geplanten Benachrichtigungen durch die neue Liste [{id,at,title,text}]. */
    static void schedule(Context c, String json) {
        SharedPreferences p = prefs(c);
        AlarmManager am = c.getSystemService(AlarmManager.class);
        try {
            JSONArray old = new JSONArray(p.getString("plan", "[]"));
            for (int i = 0; i < old.length(); i++) {
                am.cancel(alarmIntent(c, old.getJSONObject(i)));
            }
        } catch (Exception ignored) { }
        JSONArray kept = new JSONArray();
        try {
            JSONArray arr = new JSONArray(json);
            long now = System.currentTimeMillis();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                long at = o.getLong("at");
                if (at <= now) continue;
                setAlarm(am, at, alarmIntent(c, o));
                kept.put(o);
            }
        } catch (Exception ignored) { }
        p.edit().putString("plan", kept.toString()).apply();
    }

    /** Nach Neustart: geplante Benachrichtigungen und den laufenden Timer wiederherstellen. */
    static void restore(Context c) {
        SharedPreferences p = prefs(c);
        schedule(c, p.getString("plan", "[]"));
        String og = p.getString("ongoing", null);
        if (og != null) {
            String[] s = og.split("\\|", 4);
            if (s.length == 4) {
                try { showOngoing(c, Long.parseLong(s[0]), Long.parseLong(s[1]), s[2], s[3]); } catch (Exception ignored) { }
            }
        }
    }

    private static PendingIntent alarmIntent(Context c, JSONObject o) {
        Intent i = new Intent(c, AlarmReceiver.class);
        i.setAction("de.nutriscan.app.NOTIFY." + o.optInt("id"));
        i.putExtra("id", o.optInt("id"));
        i.putExtra("title", o.optString("title"));
        i.putExtra("text", o.optString("text"));
        return PendingIntent.getBroadcast(c, o.optInt("id"), i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void setAlarm(AlarmManager am, long at, PendingIntent pi) {
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }
}
