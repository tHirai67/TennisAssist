package com.example.tennisassist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MeasureActivity extends Activity implements SensorEventListener {
    private ImageView backgroundImageView;
    private SensorManager sensorManager;
    private TextView tTextView;
    private double pastime, time;
    public long post_acc = 0;
    public long post_gyro = 0;

    private ArrayList<Float> accelerationX = new ArrayList<>();
    private ArrayList<Float> accelerationY = new ArrayList<>();
    private ArrayList<Float> accelerationZ = new ArrayList<>();
    private ArrayList<Double> accelerationT = new ArrayList<>();
    private ArrayList<Float> angularVelocityX = new ArrayList<>();
    private ArrayList<Float> angularVelocityY = new ArrayList<>();
    private ArrayList<Float> angularVelocityZ= new ArrayList<>();
    private ArrayList<Double> angularVelocityT = new ArrayList<>();

    private boolean MeasureFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        backgroundImageView = findViewById(R.id.background);
        tTextView = (TextView) findViewById(R.id.Time);
        //SensorManagerを取得
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

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
                MeasureFlag = true;
                start_btn.setVisibility(View.INVISIBLE);
                stop_btn.setVisibility(View.VISIBLE);
            }

        });

        stop_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                MeasureFlag = false;
                stop_btn.setVisibility(View.INVISIBLE);
                reset_btn.setVisibility(View.VISIBLE);
                result_btn.setVisibility(View.VISIBLE);
            }
        });

        reset_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reset();
                start_btn.setVisibility(View.VISIBLE);
                reset_btn.setVisibility(View.INVISIBLE);
                result_btn.setVisibility(View.INVISIBLE);
            }
        });

        result_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 重い処理を非同期で実行する
                AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                    //非同期処理より先に実行する
                    @Override
                    protected void onPreExecute() {
                        // 処理開始前にToastを表示
                        Toast.makeText(MeasureActivity.this, "処理中です．", Toast.LENGTH_LONG).show();
                    }

                    //非同期処理
                    @Override
                    protected Void doInBackground(Void... voids) {
                        // テスト用データを使う場合は、このメソッドを呼び出す
                        TestData();

                        // 処理が完了した後にActivityを遷移する
                        Intent intent = new Intent(MeasureActivity.this, ResultActivity.class);
                        intent.putExtra("accelerationX", accelerationX);
                        intent.putExtra("accelerationY", accelerationY);
                        intent.putExtra("accelerationZ", accelerationZ);
                        intent.putExtra("accelerationT", accelerationT);
                        intent.putExtra("angularVelocityX", angularVelocityX);
                        intent.putExtra("angularVelocityY", angularVelocityY);
                        intent.putExtra("angularVelocityZ", angularVelocityZ);
                        intent.putExtra("angularVelocityT", angularVelocityT);
                        startActivity(intent);

                        return null;
                    }

                };

                // AsyncTaskを実行する
                task.execute();
            }
        });


    }

    @Override
    public void onStart(){
        super.onStart();
        sensorManager.registerListener(MeasureActivity.this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(MeasureActivity.this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);
        Log.i("SensorManager","Start");
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(MeasureActivity.this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(MeasureActivity.this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);
        Log.i("SensorManager","Start");
    }


    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        Log.i("SensorManager","Stop");
    }


    @Override
    public void onDestroy(){
        super.onDestroy();
        sensorManager.unregisterListener(this);
        Log.i("SensorManager","Stop");

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long now = event.timestamp;
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            if((now - post_acc)/1000 <= 21000){
                backgroundImageView.setImageResource(R.drawable.bg_measure);
            }else{
                backgroundImageView.setImageResource(R.drawable.bg_measure_error);
            }
            post_acc = now;

        }else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            if((now - post_gyro)/1000 <= 21000){
                backgroundImageView.setImageResource(R.drawable.bg_measure);
            }else{
                backgroundImageView.setImageResource(R.drawable.bg_measure_error);
            }
            post_gyro = now;
        }

        if(MeasureFlag == true){
            if(pastime == 0){
                pastime = System.currentTimeMillis();
            }

            if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                time = ((System.currentTimeMillis() - pastime)/1000);
                accelerationX.add(event.values[0]);
                accelerationY.add(event.values[1]);
                accelerationZ.add(event.values[2]);
                accelerationT.add(time);

            }else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
                time = ((System.currentTimeMillis() - pastime)/1000);
                angularVelocityX.add(event.values[0]);
                angularVelocityY.add(event.values[1]);
                angularVelocityZ.add(event.values[2]);
                angularVelocityT.add(time);
            }
            DecimalFormat df = new DecimalFormat("00.00");
            tTextView.setText(df.format(time)+" s");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //初期化
    public void reset(){
        MeasureFlag = false;
        pastime = 0;
        time = 0;
        accelerationX.clear();
        accelerationY.clear();
        accelerationZ.clear();
        accelerationT.clear();
        angularVelocityX.clear();
        angularVelocityY.clear();
        angularVelocityZ.clear();
        angularVelocityT.clear();
        tTextView.setText(String.format("00.00 s"));
    }

    //テスト用データを使用して検証する
    public void TestData(){
        //計測したデータをリセット
        accelerationX.clear();
        accelerationY.clear();
        accelerationZ.clear();
        accelerationT.clear();
        angularVelocityX.clear();
        angularVelocityY.clear();
        angularVelocityZ.clear();
        angularVelocityT.clear();
        //ファイルから検証用のデータを格納
        String filename = "152945";
        fileReader(filename+"_a.csv",1);
        fileReader(filename+"_g.csv",2);
    }

    public void fileReader(String filename, int type){
        try{
            InputStream is = getAssets().open(filename);
            InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);

            String line;
            while((line = br.readLine()) != null){
                String[] values = line.split(",");
                ArrayList<Float> data = new ArrayList<>();
                for (String value : values) {
                    data.add(Float.parseFloat(value));
                }

                Double time = (double)data.get(0);
                float x = data.get(1);
                float y = data.get(2);
                float z = data.get(3);

                if(type == 1){
                    accelerationT.add(time);
                    accelerationX.add(x);
                    accelerationY.add(y);
                    accelerationZ.add(z);
                }else if(type == 2){
                    angularVelocityT.add(time);
                    angularVelocityX.add(x);
                    angularVelocityY.add(y);
                    angularVelocityZ.add(z);
                }


            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
