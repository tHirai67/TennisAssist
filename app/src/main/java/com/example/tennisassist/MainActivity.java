package com.example.tennisassist;

import android.app.Activity;
import android.content.Context;
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
import android.widget.Toast;


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager accSensorManager, gyroSensorManager;
    private TextView tTextView, sTextView, caTextView, cgTextView;
    private double pastime, time;
    private int button_flag = 0;
    public long post_acc = 0;
    public long post_gyro = 0;

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
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        sTextView = (TextView) findViewById(R.id.Status);
        tTextView = (TextView) findViewById(R.id.Time);
        caTextView = (TextView) findViewById(R.id.Condition_Acc);
        cgTextView = (TextView) findViewById(R.id.Condition_Gyro);
        accSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyroSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        Button start_btn = (Button) findViewById(R.id.b1);
        Button stop_btn = (Button) findViewById(R.id.b2);
        Button reset_btn = (Button) findViewById(R.id.b3);
        Button save_btn = (Button) findViewById(R.id.b4);
        Button file_btn = (Button) findViewById(R.id.b5);

        start_btn.setVisibility(View.VISIBLE);
        stop_btn.setVisibility(View.INVISIBLE);
        reset_btn.setVisibility(View.INVISIBLE);
        save_btn.setVisibility(View.INVISIBLE);
        file_btn.setVisibility(View.INVISIBLE);

        start_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                button_flag = 1;
                start_btn.setVisibility(View.INVISIBLE);
                stop_btn.setVisibility(View.VISIBLE);
            }

        });

        stop_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                button_flag = 2;
                stop_btn.setVisibility(View.INVISIBLE);
                reset_btn.setVisibility(View.VISIBLE);
                save_btn.setVisibility(View.VISIBLE);
                file_btn.setVisibility(View.VISIBLE);
            }
        });

        reset_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button_flag = 3;
                start_btn.setVisibility(View.VISIBLE);
                stop_btn.setVisibility(View.INVISIBLE);
                reset_btn.setVisibility(View.INVISIBLE);
                save_btn.setVisibility(View.INVISIBLE);
                file_btn.setVisibility(View.INVISIBLE);
            }
        });

        save_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button_flag = 4;
                reset_btn.setVisibility(View.VISIBLE);
                save_btn.setVisibility(View.INVISIBLE);
                file_btn.setVisibility(View.VISIBLE);
            }
        });

        file_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplication(), FileActivity.class);
                startActivity(intent);
            }
        });



    }

    @Override
    public void onStart(){
        super.onStart();
        Sensor acc_sensor = accSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor gyro_sensor = gyroSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accSensorManager.registerListener(this, acc_sensor, SensorManager.SENSOR_DELAY_NORMAL);
        gyroSensorManager.registerListener(this, gyro_sensor, SensorManager.SENSOR_DELAY_NORMAL);
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
                caTextView.setText(String.format("ACC:○"));
            }else{
                caTextView.setText(String.format("ACC:×"));
            }
            post_acc = now;

        }else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            if((now - post_gyro)/1000 <= 21000){
                cgTextView.setText(String.format("GYRO:○"));
            }else{
                cgTextView.setText(String.format("GYRO:×"));
            }
            post_gyro = now;
        }


        if(button_flag == 0){
            sTextView.setText(String.format("未計測"));
            reset();

        }else if(button_flag == 1){
            sTextView.setText(String.format("計測中"));
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
                yGyro.add(event.values[0]);
                zGyro.add(event.values[0]);
                tGyro.add(time);
            }
            tTextView.setText(String.format("Time:%.3f.s",time));

        }else if(button_flag == 2){
            sTextView.setText(String.format("停止中"));
        }else if(button_flag == 3){
            button_flag = 0;
        }else if(button_flag == 4){
            sTextView.setText(String.format("保存済み"));
            resample("Acc");
            resample("Gyro");
            fileSave("Acc");
            fileSave("Gyro");
            button_flag = 5;
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public static String getNowDate(){
        final DateFormat df = new SimpleDateFormat("HHmmss");
        final Date date = new Date(System.currentTimeMillis());
        return df.format(date);
    }

    public void fileSave(String type){


        //ファイル名を作成
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String filename = sdf.format(new Date()) +"_" + type +  ".csv";

        //CSVファイルを出力
        try{
            FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            PrintWriter writer = new PrintWriter(osw);

            if(type == "Acc"){
                //ヘッダー
                writer.print("時刻,X軸加速度,Y軸加速度,Z軸加速度\n");
                //データ出力
                int size = xAcc.size();
                for(int i = 0; i < size; i++){
                    String line = String.format(Locale.getDefault(), "%.3f,%.3f,%.3f,%.3f\n",
                            tAcc.get(i), xAcc.get(i), yAcc.get(i), zAcc.get(i));
                    writer.print(line);
                }
            }else if(type == "Gyro"){
                //ヘッダー
                writer.print("時刻,X軸角速度,Y軸角速度,Z軸角速度\n");
                //データ出力
                int size = xGyro.size();
                for(int i = 0; i < size; i++){
                    String line = String.format(Locale.getDefault(), "%.3f,%.3f,%.3f,%.3f\n",
                            tGyro.get(i), xGyro.get(i), yGyro.get(i), zGyro.get(i));
                    writer.print(line);
                }
            }

            writer.close();
            osw.close();
            fos.close();

            Toast.makeText(this, "取得データを保存しました", Toast.LENGTH_SHORT).show();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void resample(String type){

        //リサンプリング後の格納用配列を用意
        ArrayList<Double> resampledTime = new ArrayList<>();
        ArrayList<Float> resampledX = new ArrayList<>();
        ArrayList<Float> resampledY = new ArrayList<>();
        ArrayList<Float> resampledZ = new ArrayList<>();

        //開始時間と終了時間
        double startTime = tAcc.get(0);
        double endTime = tAcc.get(tAcc.size() - 1 );
        //リサンプリングの間隔
        double interval = 1.0 / 50;

        double currentTime = startTime;
        while(currentTime < endTime){
            resampledTime.add(currentTime);
            currentTime += interval;
        }

        for(int i = 0; i <= resampledTime.size() - 1; i++){
            for(int j = 0; j < tAcc.size() - 1; j++){
                if(tAcc.get(j) == i){
                    resampledX.add(xAcc.get(j));
                    resampledY.add(yAcc.get(j));
                    resampledZ.add(zAcc.get(j));
                    break;
                }if(tAcc.get(j) < resampledTime.get(i) && resampledTime.get(i) < tAcc.get(j+1)){
                    //傾きを計算
                    float kx = (float) ((xAcc.get(j+1) - xAcc.get(j)) / (tAcc.get(j+1) - tAcc.get(j)));
                    float ky = (float) ((yAcc.get(j+1) - yAcc.get(j)) / (tAcc.get(j+1) - tAcc.get(j)));
                    float kz = (float) ((zAcc.get(j+1) - zAcc.get(j)) / (tAcc.get(j+1) - tAcc.get(j)));
                    //線形補間の計算
                    resampledX.add((float) (kx * (resampledTime.get(i) - tAcc.get(j)) + xAcc.get(j)));
                    resampledY.add((float) (ky * (resampledTime.get(i) - tAcc.get(j)) + yAcc.get(j)));
                    resampledZ.add((float) (kz * (resampledTime.get(i) - tAcc.get(j)) + zAcc.get(j)));
                    break;
                }
            }
        }

        xAcc.clear();
        xAcc.addAll(resampledX);
        yAcc.clear();
        yAcc.addAll(resampledY);
        zAcc.clear();
        zAcc.addAll(resampledZ);
        tAcc.clear();
        tAcc.addAll(resampledTime);

    }

    public void reset(){
        pastime = 0;
        time = 0;
        tTextView.setText(String.format("Time:%.3f s",time));
    }
}