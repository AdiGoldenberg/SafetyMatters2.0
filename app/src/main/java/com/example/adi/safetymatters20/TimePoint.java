package com.example.adi.safetymatters20;

public class TimePoint {
    // states
    private double mLongitute;
    private double mLatitude;
    private int mSafetyTag;

    // Constructor
    public TimePoint(double longitude, double latitude, int safetyTag){
        mLongitute = longitude;
        mLatitude = latitude;
        mSafetyTag = safetyTag;
    }

    // Get methods
    public double getLatitude(){return mLatitude;}
    public double getLongitute(){return mLongitute;}
    public int getSafetyTag(){return mSafetyTag;}
}
