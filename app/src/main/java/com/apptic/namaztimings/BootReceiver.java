package com.apptic.namaztimings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Re-schedule the prayer time alarms when the device is rebooted
           // schedulePrayerTimeAlarms(context);
        }
    }
}
