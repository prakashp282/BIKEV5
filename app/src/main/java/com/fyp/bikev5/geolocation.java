package com.fyp.bikev5;

public class geolocation{

    public String Date;
    public Double Latitude,Longitude;
    public float Speed,Accuracy;

    public geolocation(String date, float accuracy, Double latitude, Double longitude, float speed) {
        Date = date;
        Accuracy = accuracy;
        Latitude = latitude;
        Longitude = longitude;
        Speed = speed;
    }
}
