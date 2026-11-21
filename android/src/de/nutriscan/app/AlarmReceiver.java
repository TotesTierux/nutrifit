package de.nutriscan.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        Notifs.show(c, i.getIntExtra("id", 1), i.getStringExtra("title"), i.getStringExtra("text"));
        // Ab dem Ziel zählt der laufende Timer hoch statt runter
        if (i.getIntExtra("id", 0) == 101) Notifs.restore(c);
    }
}
