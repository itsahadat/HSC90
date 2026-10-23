package com.banglabs.hsc90;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent in) {
        String a = in.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(a) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)) {
            Reminders.scheduleAll(c);
        } else if (Reminders.ACTION_TIMER.equals(a)) {
            Reminders.notify(c, 9000, in.getStringExtra("title"), in.getStringExtra("text"));
        } else if (Reminders.ACTION_ROUTINE.equals(a)) {
            int idx = in.getIntExtra("idx", 0);
            String time = in.getStringExtra("time");
            String text = in.getStringExtra("text");
            Reminders.notify(c, 100 + idx, time + " · HSC রুটিন", text);
            Reminders.scheduleSlot(c, idx, time, text);   // same time tomorrow
        }
    }
}
