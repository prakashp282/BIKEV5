package com.fyp.bikev5;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SaveSettings extends AppCompatActivity {

    TextView txtipaddress,txtbluetooth;
    Button btnsave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getReference();

        setReference();



        btnsave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String bluetoothAddress=txtbluetooth.getText().toString();
                String ipAddress=txtipaddress.getText().toString();
                savedetails(ipAddress,bluetoothAddress);

                Intent i = new Intent(SaveSettings.this,MapsActivity.class);
                startActivity(i);
                finish();
            }
        });

    }

    private void setReference() {
        PrefManager pre=new PrefManager(this);
        if(!pre.isEmpty("IpAddress"))
            txtipaddress.setText(pre.getIpAddress());
        if(!pre.isEmpty("Bluetooth"))
            txtbluetooth.setText(pre.getBluetoothAddress());

    }


    public void getReference(){
        txtipaddress=(TextView) findViewById(R.id.ipaddress);
        txtbluetooth=(TextView) findViewById(R.id.bluetooth);
        btnsave=(Button) findViewById(R.id.save);
    }


    public  void savedetails(String ipaddress,String bluetoothAddress){
        new PrefManager(this).setSettings(ipaddress,bluetoothAddress);

    }

}
