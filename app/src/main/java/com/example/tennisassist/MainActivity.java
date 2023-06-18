package com.example.tennisassist;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager accSensorManager, gyroSensorManager;
    public long post_acc = 0;
    public long post_gyro = 0;
    private Button ready_btn;
    private Button fixAcc_btn;
    private Button fixGyro_btn;
    private int errorFlag_acc = 10, errorFlag_gyro = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        accSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyroSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        ready_btn = findViewById(R.id.b_ready);
        fixAcc_btn = findViewById(R.id.b_fixAcc);
        fixGyro_btn = findViewById(R.id.b_fixGyro);

        ready_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MeasureActivity.class);
                startActivity(intent);
            }
        });

        fixAcc_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AccelerometerActivity.class);
                startActivity(intent);
            }
        });

        fixGyro_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GyroscopeActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onStart(){
        super.onStart();
        accSensorManager.registerListener(MainActivity.this, accSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        gyroSensorManager.registerListener(MainActivity.this, gyroSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    public void onPause(){
        super.onPause();
        accSensorManager.unregisterListener(this);
        gyroSensorManager.unregisterListener(this);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        accSensorManager.unregisterListener(this);
        gyroSensorManager.unregisterListener(this);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long now = event.timestamp;
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            if((now - post_acc)/1000 <= 21000){
                fixAcc_btn.setBackgroundResource(R.drawable.green_btn);
                errorFlag_acc = 0;
            }else{
                fixAcc_btn.setBackgroundResource(R.drawable.red_btn);
                errorFlag_acc++;
            }
            post_acc = now;

        }else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            if((now - post_gyro)/1000 <= 21000){
                fixGyro_btn.setBackgroundResource(R.drawable.green_btn);
                errorFlag_gyro = 0;
            }else{
                fixGyro_btn.setBackgroundResource(R.drawable.red_btn);
                errorFlag_gyro++;
            }
            post_gyro = now;
        }

        if(errorFlag_acc < 10 && errorFlag_gyro < 10){
            ready_btn.setEnabled(true);
            ready_btn.setVisibility(View.VISIBLE);
            fixAcc_btn.setEnabled(false);
            fixGyro_btn.setEnabled(false);
            fixAcc_btn.setVisibility(View.INVISIBLE);
            fixGyro_btn.setVisibility(View.INVISIBLE);
        }else{
            ready_btn.setEnabled(false);
            ready_btn.setVisibility(View.INVISIBLE);
            fixAcc_btn.setEnabled(true);
            fixGyro_btn.setEnabled(true);
            fixAcc_btn.setVisibility(View.VISIBLE);
            fixGyro_btn.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}
