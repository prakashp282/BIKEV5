package com.fyp.bikev5;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ViewFlipper;

// home page activity, login and signin views are displayed and accessed from here
// flipper for animating images
public class HomePageActivity extends AppCompatActivity {
    ViewFlipper flipper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        flipper = (ViewFlipper)findViewById(R.id.flip);
        flipper.setFlipInterval(1000);
        Animation in = AnimationUtils.loadAnimation(getApplicationContext(),android.R.anim.slide_in_left);
        Animation out = AnimationUtils.loadAnimation(getApplicationContext(),android.R.anim.slide_out_right);
        flipper.setInAnimation(in);
        flipper.setOutAnimation(out);
        flipper.startFlipping();

        Intent intent = new Intent(HomePageActivity.this,SignUpActivity.class);
        startActivity(intent);
    }
}
