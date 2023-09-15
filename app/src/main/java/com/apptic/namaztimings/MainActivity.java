package com.apptic.namaztimings;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.apptic.namaztimings.Adapter.PrayerTimingsAdapter;
import com.apptic.namaztimings.Model.PrayerTiming;
import com.apptic.namaztimings.Reciever.PrayerNotificationReceiver;
import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.HighLatitudeRule;
import com.batoulapps.adhan.PrayerTimes;
import com.batoulapps.adhan.data.DateComponents;
import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    private static final String NOTIFICATION_CHANNEL_ID = "NamazTimingsChannel";
    private static final int DND_REQUEST_CODE = 7333; // Use any unique value
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int PRAYER_NOTIFICATION_ID = 2;

    private boolean isAnimationRunning = true; // Initially set to true to start animation


    private RecyclerView allPrayersRecyclerView;

    private TextView daytoday;
    private PrayerTimingsAdapter prayerTimingsAdapter;
    private List<PrayerTiming> prayerTimingsList;

    private RecyclerView recyclerView;
    private LinearSnapHelper snapHelper;
    private LinearLayoutManager layoutManager;
    private Timer scrollTimer;
    private int scrollPosition = 0;

    private AudioManager audioManager;
    private Handler animationHandler = new Handler();

    private TextView timeTextView, dateTextView, currentPrayerName;

    private TextView daytodayeng, daytodayurdu;

    private ImageView settingsicon;
    private TextView nextPrayerNameTextView, nextPrayerTimeTextView, nextPrayerEndTimeTextView;

    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settingsicon = findViewById(R.id.settingsicon);

        // Add OnClickListener to the settingsicon ImageView
        settingsicon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open TimeZoneSettings activity
                Intent intent = new Intent(MainActivity.this, TimeZoneSettings.class);
                startActivity(intent);
            }
        });

        currentPrayerName = findViewById(R.id.currentPrayerName);
        daytodayeng = findViewById(R.id.daytodayeng);
        daytodayurdu = findViewById(R.id.daytodayurdu);
        // Get the current day in English and Arabic
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.ENGLISH);
        SimpleDateFormat arabicDayFormat = new SimpleDateFormat("EEEE", new Locale("ur"));

        String englishDay = dayFormat.format(calendar.getTime());
        String urduday = arabicDayFormat.format(calendar.getTime());

        daytodayeng.setText(englishDay);
        daytodayurdu.setText(urduday);


        // Initialize the prayerTimingsList
        prayerTimingsList = new ArrayList<>();

        // Call this after loading prayer timings
        schedulePrayerTimeAlarms();

        String currentPrayer = getCurrentPrayerName();
        currentPrayerName.setText(currentPrayer);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        allPrayersRecyclerView = findViewById(R.id.allprayertimes_rcv);
        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        allPrayersRecyclerView.setLayoutManager(layoutManager);
        prayerTimingsAdapter = new PrayerTimingsAdapter(prayerTimingsList);
        allPrayersRecyclerView.setAdapter(prayerTimingsAdapter);

        // Initialize the recyclerView and attach the scroll listener
        recyclerView = findViewById(R.id.allprayertimes_rcv);
        snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        // Start the continuous scrolling animation
        startContinuousScrolling();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // User has finished scrolling, restart the animation
                    if (!isAnimationRunning) {
                        startContinuousScrolling();
                        isAnimationRunning = true;
                    }
                }
            }
        });

        prayerTimingsAdapter = new PrayerTimingsAdapter(prayerTimingsList);
        allPrayersRecyclerView.setAdapter(prayerTimingsAdapter);

        prayerTimingsAdapter.setOnItemClickListener(new PrayerTimingsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                // User clicked on an item, stop the animation
                stopContinuousScrolling();
                isAnimationRunning = false;

                // Optionally, you can implement additional behavior here
                // when the user clicks on a RecyclerView item.
            }
        });


        // Keep the screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Check and request Do Not Disturb permission
        //checkAndRequestDndPermission();

        timeTextView = findViewById(R.id.time_txt);
        dateTextView = findViewById(R.id.dateTextView);
        nextPrayerNameTextView = findViewById(R.id.prayername);
        nextPrayerTimeTextView = findViewById(R.id.prayertime);
        nextPrayerEndTimeTextView = findViewById(R.id.prayerendtime);

        handler = new Handler();

        // Start the initial updates and schedule updates every second
        updateCurrentTime();
        updateDates();
        updatePrayerTimingsForToday(); // Add this line to load prayer timings
        updateNextPrayerTime();
        handler.postDelayed(timeUpdateRunnable, 1000);}

    @Override
    protected void onPause() {
        super.onPause();
        stopContinuousScrolling();
        animationHandler.removeCallbacksAndMessages(null);
    }

    private void startContinuousScrolling() {
        if (isAnimationRunning) {
            scrollPosition = 0; // Start from the first item (item0)
            scrollTimer = new Timer();
            scrollTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(() -> {
                        scrollPosition = (scrollPosition + 1) % layoutManager.getItemCount();
                        recyclerView.smoothScrollToPosition(scrollPosition);
                    });
                }
            }, 0, 4000); // Scroll every 8 seconds
        }
    }


    private Date getNextPrayerTime() {
        Coordinates coordinates = new Coordinates(29.3544, 71.6911); // Replace with your coordinates
        DateComponents dateComponents = DateComponents.from(new Date());
        CalculationMethod calculationMethod = CalculationMethod.KARACHI;
        HighLatitudeRule highLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT;

        PrayerTimes prayerTimes = new PrayerTimes(coordinates, dateComponents, calculationMethod.getParameters());

        Date[] prayerTimesDates = {
                prayerTimes.fajr, prayerTimes.sunrise, prayerTimes.dhuhr,
                prayerTimes.asr, prayerTimes.maghrib, prayerTimes.isha
        };

        Calendar calendar = Calendar.getInstance();
        Date currentTime = new Date();
        calendar.setTime(currentTime);
        int currentMinutesSinceMidnight = (calendar.get(Calendar.HOUR_OF_DAY) * 60) + calendar.get(Calendar.MINUTE);

        for (Date prayerTime : prayerTimesDates) {
            calendar.setTime(prayerTime);
            int prayerMinutesSinceMidnight = (calendar.get(Calendar.HOUR_OF_DAY) * 60) + calendar.get(Calendar.MINUTE);

            if (prayerMinutesSinceMidnight > currentMinutesSinceMidnight) {
                return prayerTime; // This is the next upcoming prayer time
            }
        }

        // If there's no prayer left for the current day, get the next day's Fajr time
        Calendar nextDay = Calendar.getInstance();
        nextDay.add(Calendar.DAY_OF_MONTH, 1);
        dateComponents = DateComponents.from(nextDay.getTime());
        prayerTimes = new PrayerTimes(coordinates, dateComponents, calculationMethod.getParameters());
        return prayerTimes.fajr;
    }

    private void stopContinuousScrolling() {
        if (scrollTimer != null) {
            scrollTimer.cancel();
            scrollTimer = null;
        }
    }

    private String getCurrentPrayerName() {
        // Get the saved time zone from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String savedTimeZone = sharedPreferences.getString("SelectedTimeZone", "");

        // Get the saved latitude and longitude from SharedPreferences
        String savedLatitude = sharedPreferences.getString("SelectedLatitude", "");
        String savedLongitude = sharedPreferences.getString("SelectedLongitude", "");

        // Set the time zone for the formatter
        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm a");
        if (!savedTimeZone.isEmpty()) {
            formatter.setTimeZone(TimeZone.getTimeZone(savedTimeZone));
        } else {
            // Set a default time zone (e.g., "Asia/Karachi") if the saved time zone is empty
            formatter.setTimeZone(TimeZone.getTimeZone("Asia/Karachi"));
        }

        double latitude, longitude;

        if (!savedLatitude.isEmpty() && !savedLongitude.isEmpty()) {
            // Use the saved coordinates if available
            latitude = Double.parseDouble(savedLatitude);
            longitude = Double.parseDouble(savedLongitude);
        } else {
            // Use dummy coordinates if saved coordinates are not available
            latitude = 0.0;  // Replace with your dummy latitude
            longitude = 0.0; // Replace with your dummy longitude
        }

        Coordinates coordinates = new Coordinates(latitude, longitude);
        DateComponents dateComponents = DateComponents.from(new Date());
        CalculationMethod calculationMethod = CalculationMethod.KARACHI;

        PrayerTimes prayerTimes = new PrayerTimes(coordinates, dateComponents, calculationMethod.getParameters());

        Date[] prayerTimesDates = {
                prayerTimes.fajr, prayerTimes.sunrise, prayerTimes.dhuhr,
                prayerTimes.asr, prayerTimes.maghrib, prayerTimes.isha
        };

        String[] prayerNames = {
                "Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"
        };

        // Find the current time and determine the next prayer
        Date currentTime = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentTime);
        int currentMinutesSinceMidnight = (calendar.get(Calendar.HOUR_OF_DAY) * 60) + calendar.get(Calendar.MINUTE);

        // Loop through prayer times to find the current prayer
        for (int i = 0; i < prayerTimesDates.length; i++) {
            Date prayerTime = prayerTimesDates[i];
            calendar.setTime(prayerTime);
            int prayerMinutesSinceMidnight = (calendar.get(Calendar.HOUR_OF_DAY) * 60) + calendar.get(Calendar.MINUTE);

            if (prayerMinutesSinceMidnight > currentMinutesSinceMidnight) {
                if (i > 0) {
                    return prayerNames[i - 1]; // The previous prayer is the current prayer
                } else {
                    return prayerNames[prayerTimesDates.length - 1]; // If it's before Fajr, the current prayer is Isha
                }
            }
        }

        // If no prayer is found (e.g., after Isha), return the last prayer name
        return prayerNames[prayerTimesDates.length - 1];
    }



    private void updateCurrentTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String currentTime = dateFormat.format(new Date());
        timeTextView.setText(currentTime);

        // Apply the animation to the time text
        Animation blinkAnimation = AnimationUtils.loadAnimation(this, R.anim.blink_anim);
        timeTextView.startAnimation(blinkAnimation);
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkAndRequestDndPermission() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (!notificationManager.isNotificationPolicyAccessGranted()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle("Permission Required")
                    .setMessage("Namaz Timings app requires Do Not Disturb access for prayer notifications. Do you want to grant access?")
                    .setPositiveButton("Grant Access", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Redirect user to the permission settings screen
                            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .setCancelable(false);

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == DND_REQUEST_CODE) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager.isNotificationPolicyAccessGranted()) {
                // Do something now that the user granted permission
            } else {
                // Permission not granted
            }
        }
    }


    private Runnable timeUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            updateCurrentTime();
            updateNextPrayerTime();
            handler.postDelayed(this, 1000); // Schedule the runnable again after 1 second
        }
    };





    private void updateDates() {
        // Get the current calendar date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        String currentDate = sdf.format(new Date());

        // Get the current Hijri calendar date
        UmmalquraCalendar hijriCalendar = new UmmalquraCalendar();
        String hijriDate = getFormattedHijriDate(hijriCalendar);

        // Update the dateTextView with both dates
        String combinedDates = currentDate + " / \n " + hijriDate;
        dateTextView.setText(combinedDates);
    }

    private String getFormattedHijriDate(UmmalquraCalendar hijriCalendar) {
        int day = hijriCalendar.get(Calendar.DAY_OF_MONTH);
        int month = hijriCalendar.get(Calendar.MONTH);
        int year = hijriCalendar.get(Calendar.YEAR);

        String monthName = hijriCalendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, new Locale("ar"));
        return String.format(Locale.getDefault(), "%02d %s %04d AH", day, monthName, year);
    }

    private void updatePrayerTimingsForToday() {
        // Get the saved time zone from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String savedTimeZone = sharedPreferences.getString("SelectedTimeZone", "");

        // Get the saved latitude and longitude from SharedPreferences
        String savedLatitude = sharedPreferences.getString("SelectedLatitude", "");
        String savedLongitude = sharedPreferences.getString("SelectedLongitude", "");

        // Set the time zone for the formatter
        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm a");
        if (!savedTimeZone.isEmpty()) {
            formatter.setTimeZone(TimeZone.getTimeZone(savedTimeZone));
        } else {
            // Set a default time zone (e.g., "Asia/Karachi") if the saved time zone is empty
            formatter.setTimeZone(TimeZone.getTimeZone("Asia/Karachi"));
        }

        double latitude, longitude;

        if (!savedLatitude.isEmpty() && !savedLongitude.isEmpty()) {
            // Use the saved coordinates if available
            latitude = Double.parseDouble(savedLatitude);
            longitude = Double.parseDouble(savedLongitude);
        } else {
            // Use dummy coordinates if saved coordinates are not available
            latitude = 0.0;  // Replace with your dummy latitude
            longitude = 0.0; // Replace with your dummy longitude
        }

        Coordinates coordinates = new Coordinates(latitude, longitude);
        DateComponents dateComponents = DateComponents.from(new Date());
        CalculationMethod calculationMethod = CalculationMethod.KARACHI;

        PrayerTimes prayerTimes = new PrayerTimes(coordinates, dateComponents, calculationMethod.getParameters());

        Date[] prayerTimesDates = {
                prayerTimes.fajr, prayerTimes.sunrise, prayerTimes.dhuhr,
                prayerTimes.asr, prayerTimes.maghrib, prayerTimes.isha
        };

        String[] prayerNames = {
                "Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"
        };

        // Clear the existing list and prepare for new data
        prayerTimingsList.clear();

        // Loop through prayer times to load all prayer times for today
        for (int i = 0; i < prayerTimesDates.length; i++) {
            Date prayerTime = prayerTimesDates[i];
            Date prayerEndTime;

            if (i < prayerTimesDates.length - 1) {
                // If there's a next prayer in the list, calculate the end time as 1 hour before that prayer
                prayerEndTime = getPrayerEndTime(prayerTimesDates[i + 1]);
            } else {
                // If it's Isha prayer (the last prayer of the day), calculate the end time based on a fixed interval
                prayerEndTime = getIshaEndTime(prayerTime);
            }

            if (prayerTime != null && prayerEndTime != null) {
                // Add the prayer timing to the list
                prayerTimingsList.add(new PrayerTiming(prayerNames[i], formatter.format(prayerTime), formatter.format(prayerEndTime)));
            } else {
                // Handle the case where prayerTime or prayerEndTime is null
                // You can add some default values or handle the error as needed
            }
        }

        // Notify the adapter that data has changed
        prayerTimingsAdapter.notifyDataSetChanged();
    }

    private Date getPrayerEndTime(Date nextPrayerTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(nextPrayerTime);
        calendar.add(Calendar.HOUR, -1); // Subtracting 1 hour to get the end time
        return calendar.getTime();
    }

    private Date getIshaEndTime(Date ishaStartTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(ishaStartTime);
        calendar.add(Calendar.HOUR, 1); // Adding 1 hour to get the end time for Isha
        return calendar.getTime();
    }

    private void updateNextPrayerTime() {
        // Get the saved time zone from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String savedTimeZone = sharedPreferences.getString("SelectedTimeZone", "");

        // Get the saved latitude and longitude from SharedPreferences
        String savedLatitude = sharedPreferences.getString("SelectedLatitude", "");
        String savedLongitude = sharedPreferences.getString("SelectedLongitude", "");

        // Set the time zone for the formatter
        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm a");
        if (!savedTimeZone.isEmpty()) {
            formatter.setTimeZone(TimeZone.getTimeZone(savedTimeZone));
        } else {
            // Set a default time zone (e.g., "Asia/Karachi") if the saved time zone is empty
            formatter.setTimeZone(TimeZone.getTimeZone("Asia/Karachi"));
        }

        double latitude, longitude;

        if (!savedLatitude.isEmpty() && !savedLongitude.isEmpty()) {
            // Use the saved coordinates if available
            latitude = Double.parseDouble(savedLatitude);
            longitude = Double.parseDouble(savedLongitude);
        } else {
            // Use dummy coordinates if saved coordinates are not available
            latitude = 0.0;  // Replace with your dummy latitude
            longitude = 0.0; // Replace with your dummy longitude
        }

        Coordinates coordinates = new Coordinates(latitude, longitude);
        DateComponents dateComponents = DateComponents.from(new Date());

        // Specify the calculation method you want to use, e.g., CalculationMethod.KARACHI
        CalculationMethod calculationMethod = CalculationMethod.KARACHI;

        PrayerTimes prayerTimes = new PrayerTimes(coordinates, dateComponents, calculationMethod.getParameters());

        Date[] prayerTimesDates = {
                prayerTimes.fajr, prayerTimes.sunrise, prayerTimes.dhuhr,
                prayerTimes.asr, prayerTimes.maghrib, prayerTimes.isha
        };

        String[] prayerNames = {
                "Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"
        };

        // Find the current time and determine the next prayer
        Date currentTime = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentTime);
        int currentMinutesSinceMidnight = (calendar.get(Calendar.HOUR_OF_DAY) * 60) + calendar.get(Calendar.MINUTE);

        // Initialize next prayer time and name
        Date nextPrayerTime = null;
        String nextPrayerName = null;

        // Loop through prayer times to find the next upcoming prayer
        for (int i = 0; i < prayerTimesDates.length; i++) {
            Date prayerTime = prayerTimesDates[i];
            calendar.setTime(prayerTime);
            int prayerMinutesSinceMidnight = (calendar.get(Calendar.HOUR_OF_DAY) * 60) + calendar.get(Calendar.MINUTE);

            if (prayerMinutesSinceMidnight > currentMinutesSinceMidnight) {
                nextPrayerTime = prayerTime;
                nextPrayerName = prayerNames[i];
                break;
            }
        }

        if (nextPrayerTime != null) {
            // Find the index of the next prayer in the list
            int nextPrayerIndex = Arrays.asList(prayerTimesDates).indexOf(nextPrayerTime);

            // Calculate and update prayer end time as 1 hour before the start time of the next prayer
            Date nextPrayerStartTime = prayerTimesDates[nextPrayerIndex];
            Date prayerEndTime = getPrayerEndTime(nextPrayerStartTime, nextPrayerTime);

            nextPrayerNameTextView.setText(nextPrayerName); // Update prayer name
            nextPrayerTimeTextView.setText(formatter.format(nextPrayerTime)); // Update prayer time
            nextPrayerEndTimeTextView.setText(formatter.format(prayerEndTime)); // Update end time

            // Broadcast an intent to trigger the notification
            Intent notificationIntent = new Intent("com.apptic.namaztimings.PRAYER_TIME_NOTIFICATION");
            notificationIntent.putExtra("prayer_name", nextPrayerName);
            sendBroadcast(notificationIntent);
        } else {
            // If there is no next prayer for the current day,
            // calculate the timings for the next day's prayers
            Calendar nextDay = Calendar.getInstance();
            nextDay.add(Calendar.DAY_OF_MONTH, 1);
            dateComponents = DateComponents.from(nextDay.getTime());
            prayerTimes = new PrayerTimes(coordinates, dateComponents, calculationMethod.getParameters());
            nextPrayerTime = prayerTimes.fajr;
            nextPrayerName = "Fajr"; // Assuming the first prayer of the day is Fajr

            nextPrayerNameTextView.setText(nextPrayerName); // Update prayer name
            nextPrayerTimeTextView.setText(formatter.format(nextPrayerTime)); // Update prayer time

            calendar.setTime(nextPrayerTime);
            calendar.add(Calendar.MINUTE, 15); // Adding 15 minutes
            Date prayerEndTime = calendar.getTime();
            nextPrayerEndTimeTextView.setText(formatter.format(prayerEndTime)); // Update end time
        }
    }

    private Date getPrayerEndTime(Date prayerTime, Date nextPrayerTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(nextPrayerTime);
        calendar.add(Calendar.HOUR, (int) +2); // + 2 hour to get the end time
        return calendar.getTime();
    }



    private boolean hasScheduleExactAlarmPermission() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                return alarmManager.canScheduleExactAlarms();
            }
        }
        return false;
    }






    @RequiresApi(api = Build.VERSION_CODES.M)
    private void disableDoNotDisturbMode() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager.isNotificationPolicyAccessGranted()) {
            // Set the interruption filter to allow all notifications
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
        }
    }



    // Schedule alarms for prayer times
// Schedule alarms for prayer times
    private void schedulePrayerTimeAlarms() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Loop through your prayer timings and schedule alarms for each prayer
        for (PrayerTiming timing : prayerTimingsList) {
            String prayerName = timing.getName();
            String prayerTime = timing.getStartTime(); // The time of the prayer

            // Parse the prayer time string into hours and minutes
            String[] timeParts = prayerTime.split(":");
            int hours = Integer.parseInt(timeParts[0]);
            int minutes = Integer.parseInt(timeParts[1]);

            // Create a Calendar instance for the prayer time
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, hours);
            calendar.set(Calendar.MINUTE, minutes);
            calendar.set(Calendar.SECOND, 0);

            // Check if the prayer time is in the future
            if (calendar.getTimeInMillis() > System.currentTimeMillis()) {
                // Create an intent to trigger the notification
                Intent intent = new Intent(this, PrayerNotificationReceiver.class);
                intent.putExtra("prayer_name", prayerName);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
                );

                // Schedule the alarm with AlarmManager
                if (alarmManager != null) {
                    // Use setExact to schedule the alarm precisely at the specified time
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove the callbacks when the activity is destroyed to prevent memory leaks
        handler.removeCallbacks(timeUpdateRunnable);
        stopContinuousScrolling();
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Register a BroadcastReceiver to listen for time zone changes
        IntentFilter filter = new IntentFilter("com.apptic.namaztimings.TIME_ZONE_CHANGED");
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Handle the time zone change event here
                updatePrayerTimingsForToday(); // Update prayer timings when the time zone changes
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }
    }

}
