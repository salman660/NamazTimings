package com.apptic.namaztimings.Model;

public class PrayerTiming {
    private String name;
    private String startTime;
    private String endTime;

    public PrayerTiming(String name, String startTime, String endTime) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getName() {
        return name;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }
}
