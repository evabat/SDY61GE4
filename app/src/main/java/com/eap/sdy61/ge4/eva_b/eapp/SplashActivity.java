package com.eap.sdy61.ge4.eva_b.eapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 4000;

    private final Handler mHandler   = new Handler();
    private final Launcher mLauncher = new Launcher();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_up, R.anim.fade_out);
        setContentView(R.layout.activity_splash);

    }

    @Override
    protected void onStart() {
        super.onStart();
        mHandler.postDelayed(mLauncher, SPLASH_DELAY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHandler.postDelayed(mLauncher, SPLASH_DELAY);
    }

    @Override
    protected void onPause() {
        mHandler.removeCallbacks(mLauncher);
        super.onPause();
    }

    @Override
    protected void onStop() {
        mHandler.removeCallbacks(mLauncher);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacks(mLauncher);
        super.onDestroy();
    }

    private void launch() {
        if (!isFinishing()) {
            startActivity(new Intent(this, MainActivity.class));
            overridePendingTransition(R.anim.slide_up, R.anim.fade_out);
            finish();

        }
    }

    private class Launcher implements Runnable {
        @Override
        public void run() {
            launch();
        }
    }
}