package com.example.tennisassist;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class GyroscopeActivity extends Activity implements SensorEventListener {

    private TextView dTextView;
    private SensorManager gyroSensorManager;
    private float gx,gy,gz;
    private Button back_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gyroscope);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        dTextView = findViewById(R.id.text);
        gyroSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        back_btn = findViewById(R.id.b_back);
        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }


    @Override
    public void onStart(){
        super.onStart();
        Sensor gyro_sensor = gyroSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        gyroSensorManager.registerListener(this, gyro_sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        gyroSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            gx = event.values[0];
            gy = event.values[1];
            gz = event.values[2];

            if(dTextView != null){
                dTextView.setText(String.format("X:%f\nY:%f\nZ:%f\n",gx,gy,gz));
                dTextView.setTextSize(20.0f);
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}