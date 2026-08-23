package com.helloapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

public class AlarmScheduler {

    public static void scheduleNext(Context context, AlarmEntity alarm) {
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = pendingIntent(context, alarm);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, alarm.hour);
        cal.set(Calendar.MINUTE, alarm.minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // If the time already passed today, schedule for tomorrow.
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        mgr.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                cal.getTimeInMillis(),
                pi
        );
    }

    public static void cancel(Context context, AlarmEntity alarm) {
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = pendingIntent(context, alarm);
        mgr.cancel(pi);
    }

    private static PendingIntent pendingIntent(Context context, AlarmEntity alarm) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("alarm_id", alarm.id);
        intent.putExtra("alarm_label", alarm.label);
        // Use alarm.id as the unique request code so each alarm gets its own PendingIntent.
        return PendingIntent.getBroadcast(context, (int) alarm.id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
