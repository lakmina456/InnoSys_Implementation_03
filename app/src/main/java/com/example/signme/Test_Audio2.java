package com.example.signme;

import android.app.Activity;
import android.os.Bundle;


public class Test_Audio2 extends Activity {

    private TrafficSignAlertSystem alertSystem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test1);

        alertSystem = new TrafficSignAlertSystem(this);

        // Simulate detected signs
        simulateSignDetection();
    }

    private void simulateSignDetection() {
        alertSystem.alertBasedOnContext("Road Work Ahead", "Road Narrows Ahead");
        //alertSystem.processTrafficSignDetections("Road Work Ahead");
        //alertSystem.processTrafficSignDetections("Road Narrows Ahead");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        alertSystem.shutdown();
    }
}
