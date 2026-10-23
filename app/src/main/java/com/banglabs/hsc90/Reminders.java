package com.banglabs.hsc90;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

/** Daily routine reminders and the study-timer alarm. */
public final class Reminders {
    static final String ACTION_ROUTINE = "com.banglabs.hsc90.ROUTINE";
    static final String ACTION_TIMER = "com.banglabs.hsc90.TIMER";
    private static final String PREFS = "reminders";
    private static final String CHANNEL = "routine";
    private static final int TIMER_ID = 9000;

    private Reminders() {}

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Called from the web app whenever the routine changes. */
    static void saveAndSchedule(Context c, String json) {
        int old = prefs(c).getInt("count", 0);
        for (int i = 0; i < old; i++) cancel(c, i);
        prefs(c).edit().putString("json", json).apply();
        scheduleAll(c);
    }

    /** Schedules every saved routine slot (also used after a reboot). */
    static void scheduleAll(Context c) {
        try {
            JSONArray arr = new JSONArray(prefs(c).getString("json", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                scheduleSlot(c, i, o.optString("time", "08:00"), o.optString("text", ""));
            }
            prefs(c).edit().putInt("count", arr.length()).apply();
        } catch (Exception ignored) { }
    }

    static void scheduleSlot(Context c, int idx, String hhmm, String text) {
        String[] p = hhmm.split(":");
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(p[0]));
        cal.set(Calendar.MINUTE, Integer.parseInt(p[1]));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis() + 1000) cal.add(Calendar.DAY_OF_YEAR, 1);

        Intent i = new Intent(c, AlarmReceiver.class).setAction(ACTION_ROUTINE)
                .putExtra("idx", idx).putExtra("time", hhmm).putExtra("text", text);
        set(c, cal.getTimeInMillis(), PendingIntent.getBroadcast(c, idx, i, flags()));
    }

    static void cancel(Context c, int idx) {
        Intent i = new Intent(c, AlarmReceiver.class).setAction(ACTION_ROUTINE);
        PendingIntent pi = PendingIntent.getBroadcast(c, idx, i, flags());
        ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).cancel(pi);
    }

    static void scheduleTimer(Context c, long at, String title, String text) {
        Intent i = new Intent(c, AlarmReceiver.class).setAction(ACTION_TIMER)
                .putExtra("title", title).putExtra("text", text);
        set(c, at, PendingIntent.getBroadcast(c, TIMER_ID, i, flags()));
    }

    static void cancelTimer(Context c) {
        Intent i = new Intent(c, AlarmReceiver.class).setAction(ACTION_TIMER);
        ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE))
                .cancel(PendingIntent.getBroadcast(c, TIMER_ID, i, flags()));
    }

    private static int flags() {
        return PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
    }

    private static void set(Context c, long at, PendingIntent pi) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
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

    static void notify(Context c, int id, String title, String text) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL) == null) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "রুটিন রিমাইন্ডার", NotificationManager.IMPORTANCE_HIGH);
            ch.enableVibration(true);
            nm.createNotificationChannel(ch);
        }
        PendingIntent open = PendingIntent.getActivity(c, 0,
                new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(c, CHANNEL) : new Notification.Builder(c);
        b.setSmallIcon(R.drawable.ic_notify)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(open)
                .setAutoCancel(true);
        if (Build.VERSION.SDK_INT < 26) b.setPriority(Notification.PRIORITY_HIGH).setDefaults(Notification.DEFAULT_ALL);
        try { nm.notify(id, b.build()); } catch (SecurityException ignored) { }
    }
}
