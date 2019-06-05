package com.fyp.bikev5;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {

    Context context;

    PrefManager(Context context) {
        this.context = context;
    }



    public void setSettings(String ipAddress, String bluetoothAddress) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("BIKE", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("IpAddress", ipAddress);
        editor.putString("Bluetooth", bluetoothAddress);
        editor.commit();
    }




    public void setIpAddress(String ipAddress){
        SharedPreferences sharedPreferences = context.getSharedPreferences("BIKE", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("IpAddress", ipAddress);
        editor.commit();

    }

    public void setBluetooth(String Bluetooth){
        SharedPreferences sharedPreferences = context.getSharedPreferences("BIKE", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("Bluetooth", Bluetooth);
        editor.commit();
    }

    public boolean isEmpty(String x){
        SharedPreferences sharedPreferences = context.getSharedPreferences("BIKE", Context.MODE_PRIVATE);
        return sharedPreferences.getString(x, "").isEmpty();
    }

    public String getIpAddress() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("BIKE", Context.MODE_PRIVATE);
        return sharedPreferences.getString("IpAddress", "");
    }

    public String getBluetoothAddress() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("BIKE", Context.MODE_PRIVATE);
        return sharedPreferences.getString("Bluetooth", "");
    }

    public void setData(String Data){
        SharedPreferences sharedPreferences = context.getSharedPreferences("BIKE", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("Data", Data);
        editor.commit();
    }


    public String getData() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("BIKE", Context.MODE_PRIVATE);
        return sharedPreferences.getString("Data", "");
    }
}