package com.example.miniprojectapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        new Handler().postDelayed(() -> {
            SessionManager session = new SessionManager(SplashScreen.this);
            if (session.isLoggedIn()) {
                startActivity(new Intent(SplashScreen.this, DashboardActivity.class));
            } else {
                startActivity(new Intent(SplashScreen.this, MainActivity.class));
            }
            finish();
        }, 2000);
    }
}
