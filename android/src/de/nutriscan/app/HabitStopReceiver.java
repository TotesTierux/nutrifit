package de.nutriscan.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** „Stopp“ in der Benachrichtigung. */
public class HabitStopReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        String hid = i.getStringExtra("hid");
        if (hid != null) QuickActions.stop(c, hid);
    }
}
