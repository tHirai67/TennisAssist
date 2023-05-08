package com.example.tennisassist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MeasureActivity extends Activity implements SensorEventListener {
    private SensorManager accSensorManager, gyroSensorManager;
    private TextView tTextView;
    private double pastime, time;

    private ArrayList<Float> xAcc = new ArrayList<>();
    private ArrayList<Float> yAcc = new ArrayList<>();
    private ArrayList<Float> zAcc = new ArrayList<>();
    private ArrayList<Double> tAcc = new ArrayList<>();
    private ArrayList<Float> xGyro = new ArrayList<>();
    private ArrayList<Float> yGyro = new ArrayList<>();
    private ArrayList<Float> zGyro= new ArrayList<>();
    private ArrayList<Double> tGyro = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        tTextView = (TextView) findViewById(R.id.Time);
        //SensorManagerを取得
        accSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyroSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        Button start_btn = (Button) findViewById(R.id.b_start);
        Button stop_btn = (Button) findViewById(R.id.b_stop);
        Button reset_btn = (Button) findViewById(R.id.b_reset);
        Button result_btn = (Button) findViewById(R.id.b_result);

        start_btn.setVisibility(View.VISIBLE);
        stop_btn.setVisibility(View.INVISIBLE);
        reset_btn.setVisibility(View.INVISIBLE);
        result_btn.setVisibility(View.INVISIBLE);


        start_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //加速度センサと角速度センサを登録
                accSensorManager.registerListener(MeasureActivity.this, accSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
                gyroSensorManager.registerListener(MeasureActivity.this, gyroSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);
                Toast.makeText(MeasureActivity.this, "計測を開始しました．", Toast.LENGTH_SHORT).show();
                start_btn.setVisibility(View.INVISIBLE);
                stop_btn.setVisibility(View.VISIBLE);
            }

        });

        stop_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Toast.makeText(MeasureActivity.this, "計測を停止しています．", Toast.LENGTH_SHORT).show();
                accSensorManager.unregisterListener(MeasureActivity.this);
                gyroSensorManager.unregisterListener(MeasureActivity.this);
                stop_btn.setVisibility(View.INVISIBLE);
                reset_btn.setVisibility(View.VISIBLE);
                result_btn.setVisibility(View.VISIBLE);
            }
        });

        reset_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reset();
                Toast.makeText(MeasureActivity.this, "計測データをリセットしました．", Toast.LENGTH_SHORT).show();
                start_btn.setVisibility(View.VISIBLE);
                reset_btn.setVisibility(View.INVISIBLE);
                result_btn.setVisibility(View.INVISIBLE);
            }
        });

        result_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MeasureActivity.this, ResultActivity.class);
                intent.putExtra("xAcc",xAcc);
                intent.putExtra("yAcc",yAcc);
                intent.putExtra("zAcc",zAcc);
                intent.putExtra("tAcc",tAcc);
                intent.putExtra("xGyro",xGyro);
                intent.putExtra("yGyro",yGyro);
                intent.putExtra("zGyro",zGyro);
                intent.putExtra("tGyro",tGyro);
                startActivity(intent);
            }
        });

    }

    @Override
    public void onStart(){
        super.onStart();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if(pastime == 0){
            pastime = System.currentTimeMillis();
        }

        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            time = ((System.currentTimeMillis() - pastime)/1000);
            xAcc.add(event.values[0]);
            yAcc.add(event.values[1]);
            zAcc.add(event.values[2]);
            tAcc.add(time);

        }else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            time = ((System.currentTimeMillis() - pastime)/1000);
            xGyro.add(event.values[0]);
            yGyro.add(event.values[1]);
            zGyro.add(event.values[2]);
            tGyro.add(time);
        }
        tTextView.setText(String.format("Time:%.3f.s",time));

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //初期化
    public void reset(){
        pastime = 0;
        time = 0;
        xAcc.clear();
        yAcc.clear();
        zAcc.clear();
        tAcc.clear();
        xGyro.clear();
        yGyro.clear();
        zGyro.clear();
        tGyro.clear();
        tTextView.setText(String.format("Time:0 s"));
    }
}
