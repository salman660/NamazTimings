package com.apptic.namaztimings.Reciever;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.apptic.namaztimings.R;

public class PrayerNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("PrayerNotification", "PrayerNotificationReceiver triggered");
        // Extract prayer name from the intent
        String prayerName = intent.getStringExtra("prayer_name");

        // Create a notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "prayer_channel")
                .setSmallIcon(R.drawable.namaz_logo) // Replace with your notification icon
                .setContentTitle("Prayer Time")
                .setContentText(prayerName + " prayer time has started.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Create a notification channel (required for Android Oreo and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "prayer_channel",
                    "Prayer Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(69696, builder.build()); // Use a unique ID for each notification
    }
}
