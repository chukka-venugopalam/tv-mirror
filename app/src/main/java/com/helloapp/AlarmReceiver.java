package com.helloapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "alarm_channel";
    private static final String CHANNEL_NAME = "Alarms";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        long alarmId = intent.getLongExtra("alarm_id", -1);
        String label = intent.getStringExtra("alarm_label");
        if (label == null || label.isEmpty()) {
            label = "Alarm";
        }

        // Look up the exact time from the database for the notification text.
        executor.execute(() -> {
            AssistantDB db = AssistantDB.getInstance(context);
            AlarmEntity alarm = db.alarmDao().getById(alarmId);
            String timeText;
            if (alarm != null) {
                timeText = String.format("%02d:%02d", alarm.hour, alarm.minute);
            } else {
                timeText = "now";
            }

            // Schedule next alarm if enabled (daily repeat).
            if (alarm != null && alarm.enabled) {
                AlarmScheduler.scheduleNext(context, alarm);
            }

            // Build the notification.
            createChannel(context);
            Intent tapIntent = new Intent(context, AlarmsActivity.class);
            tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pending = PendingIntent.getActivity(context, (int) alarmId,
                    tapIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle(label)
                    .setContentText("Alarm at " + timeText)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pending);

            // Play the default alarm ringtone with the notification.
            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (ringtoneUri == null) {
                ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            builder.setSound(ringtoneUri);

            NotificationManager mgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mgr.notify((int) alarmId, builder.build());
        });
    }

    private void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            NotificationManager mgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mgr.createNotificationChannel(channel);
        }
    }
}
