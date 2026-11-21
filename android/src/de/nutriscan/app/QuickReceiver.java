package de.nutriscan.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Tipp auf eine Gewohnheit im Widget. */
public class QuickReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        String hid = i.getStringExtra("hid");
        if (hid != null) QuickActions.toggle(c, hid);
    }
}
