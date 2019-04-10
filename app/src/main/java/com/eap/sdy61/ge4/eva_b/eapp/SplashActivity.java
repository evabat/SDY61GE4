package com.eap.sdy61.ge4.eva_b.eapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 4000;

    private final Handler mHandler   = new Handler();
    private final Launcher mLauncher = new Launcher();

    ImageView eapImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

    }

    @Override
    protected void onStart() {
        super.onStart();
        RotateAnimation anim = new RotateAnimation(0f, 358f, 100f, 50f);
        anim.setInterpolator(new LinearInterpolator());
        anim.setRepeatCount(Animation.INFINITE);
        anim.setDuration(600);
        // Start animating the image
        eapImg = (ImageView) findViewById(R.id.eap_img);
        eapImg.startAnimation(anim);
        mHandler.postDelayed(mLauncher, SPLASH_DELAY);
    }

    @Override
    protected void onStop() {
        mHandler.removeCallbacks(mLauncher);
        super.onStop();
    }

    private void launch() {
        if (!isFinishing()) {
            startActivity(new Intent(this, MainActivity.class));
            eapImg.setAnimation(null);
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