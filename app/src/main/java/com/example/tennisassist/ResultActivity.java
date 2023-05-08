package com.example.tennisassist;

import android.app.Activity;
import android.content.Context;
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

public class ResultActivity extends Activity {

    private TextView cTextView;
    private ArrayList<Float> xAcc = new ArrayList<>();
    private ArrayList<Float> yAcc = new ArrayList<>();
    private ArrayList<Float> zAcc = new ArrayList<>();
    private ArrayList<Double> tAcc = new ArrayList<>();
    private ArrayList<Float> xGyro = new ArrayList<>();
    private ArrayList<Float> yGyro = new ArrayList<>();
    private ArrayList<Float> zGyro= new ArrayList<>();
    private ArrayList<Double> tGyro = new ArrayList<>();

    private int strokesCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        xAcc = (ArrayList<Float>) getIntent().getSerializableExtra("xAcc");
        yAcc = (ArrayList<Float>) getIntent().getSerializableExtra("yAcc");
        zAcc = (ArrayList<Float>) getIntent().getSerializableExtra("zAcc");
        tAcc = (ArrayList<Double>) getIntent().getSerializableExtra("tAcc");
        xGyro = (ArrayList<Float>) getIntent().getSerializableExtra("xGyro");
        yGyro = (ArrayList<Float>) getIntent().getSerializableExtra("yGyro");
        zGyro = (ArrayList<Float>) getIntent().getSerializableExtra("zGyro");
        tGyro = (ArrayList<Double>) getIntent().getSerializableExtra("tGyro");

        cTextView = findViewById(R.id.stroke_count);
        Button save_btn = (Button) findViewById(R.id.b_save);
        Button back_btn = (Button) findViewById(R.id.b_back);
        save_btn.setVisibility(View.VISIBLE);
        back_btn.setVisibility(View.VISIBLE);

        //ストロークの検出
        strokesCount = StrokeDetection();
        //検出回数を表示
        cTextView.setText(String.valueOf("Stroke:"+strokesCount));


        save_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileSave("acc");
                fileSave("gyro");
                synchronization();
                lowpassFilter();
                fileSave("acc&gyro");

                Toast.makeText(ResultActivity.this, "計測データを保存しました．", Toast.LENGTH_SHORT).show();
                save_btn.setVisibility(View.INVISIBLE);
            }
        });

        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    public int StrokeDetection(){
        //検出回数
        int count = 0;

        //加速度と角速度の同期
        synchronization();
        //ローパスフィルタ
        lowpassFilter();
        //絶対値平均を計算
        //正規化
        //差分値を計算
        //ウィンドウサイズやスライドサイズを初期化
        //スライディングウィンドウ法を実装

        return count;
    }

    public void fileSave(String type){

        //ファイル名を作成
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String filename = sdf.format(new Date())  +String.valueOf("_"+type)+  ".csv";

        //CSVファイルを出力
        try{
            FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            PrintWriter writer = new PrintWriter(osw);
            if(type == "acc&gyro"){
                //ヘッダー
                writer.print("時刻,X軸加速度,Y軸加速度,Z軸加速度,時刻,X軸角速度,Y軸角速度,Z軸角速度\n");
                //データ出力
                int size = xAcc.size();
                for(int i = 0; i < size; i++){
                    String line = String.format(Locale.getDefault(), "%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f\n",
                            tAcc.get(i), xAcc.get(i), yAcc.get(i), zAcc.get(i),tGyro.get(i), xGyro.get(i), yGyro.get(i), zGyro.get(i));
                    writer.print(line);
                }
            }else if(type == "acc"){
                //ヘッダー
                writer.print("時刻,X軸加速度,Y軸加速度,Z軸加速度\n");
                //データ出力
                int size = xAcc.size();
                for(int i = 0; i < size; i++){
                    String line = String.format(Locale.getDefault(), "%.3f,%.3f,%.3f,%.3f\n",tAcc.get(i), xAcc.get(i), yAcc.get(i), zAcc.get(i));
                    writer.print(line);
                }
            } else if (type == "gyro") {
                //ヘッダー
                writer.print("時刻,X軸角速度,Y軸角速度,Z軸角速度\n");
                //データ出力
                int size = xGyro.size();
                for(int i = 0; i < size; i++){
                    String line = String.format(Locale.getDefault(), "%.3f,%.3f,%.3f,%.3f\n",tGyro.get(i), xGyro.get(i), yGyro.get(i), zGyro.get(i));
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
    public void synchronization(){
        //取得した加速度と角速度をリサンプリング
        resample("Acc");
        resample("Gyro");

        //最初の時刻を合わせる
        if(tAcc.get(0) <= tGyro.get(0)){ //角速度を基準
            int slide_count = 0;
            //スライドする数をカウント
            for(int i = 0; i < tAcc.size(); i++){
                if(tGyro.get(0) - tAcc.get(i) <= 0.02){
                    slide_count = i;
                    break;
                }
            }


            //スライド
            for(int i = 0; i < tAcc.size() - slide_count; i++){
                tAcc.set(i,tAcc.get(i+slide_count));
                xAcc.set(i,xAcc.get(i+slide_count));
                yAcc.set(i,yAcc.get(i+slide_count));
                zAcc.set(i,zAcc.get(i+slide_count));
            }
            //スライドした分の後ろの要素を削除
            for(int i = tAcc.size() - slide_count; i < tAcc.size(); i++){
                tAcc.remove(tAcc.size() - 1);
                xAcc.remove(tAcc.size() - 1);
                yAcc.remove(tAcc.size() - 1);
                zAcc.remove(tAcc.size() - 1);
            }

        }else { //加速度を基準
            int slide_count = 0;
            //スライドする数をカウント
            for(int i = 0; i < tGyro.size(); i++){
                if(tAcc.get(0) - tGyro.get(i) < 0.02){
                    slide_count = i;
                    break;
                }
            }
            //スライド
            for(int i = 0; i < tGyro.size() - slide_count; i++){
                tGyro.set(i, tGyro.get(i + slide_count));
                xGyro.set(i, xGyro.get(i + slide_count));
                yGyro.set(i, yGyro.get(i + slide_count));
                zGyro.set(i, zGyro.get(i + slide_count));
            }
            //スライドした分の後ろの要素を削除
            for (int i = tGyro.size() - slide_count; i < tGyro.size(); i++) {
                tGyro.remove(tGyro.size() - 1);
                xGyro.remove(xGyro.size() - 1);
                yGyro.remove(yGyro.size() - 1);
                zGyro.remove(zGyro.size() - 1);
            }
        }

        //余った分の削除
        if(tAcc.size() <= tGyro.size()){ //加速度を基準
            int sizeDiff = tGyro.size() - tAcc.size();
            for(int i=0; i<sizeDiff; i++) {
                tGyro.remove(tGyro.size()-1);
                xGyro.remove(tGyro.size()-1);
                yGyro.remove(tGyro.size()-1);
                zGyro.remove(tGyro.size()-1);
            }
        } else { //角速度を基準
            int sizeDiff = tAcc.size() - tGyro.size();
            for(int i=0; i<sizeDiff; i++) {
                tAcc.remove(tAcc.size()-1);
                xAcc.remove(tAcc.size()-1);
                yAcc.remove(tAcc.size()-1);
                zAcc.remove(tAcc.size()-1);
            }
        }


    }

    //リサンプリングメソッド
    public void resample(String type){
        ArrayList<Double> t = new ArrayList<>();
        ArrayList<Float> x = new ArrayList<>();
        ArrayList<Float> y = new ArrayList<>();
        ArrayList<Float> z = new ArrayList<>();
        if(type == "Acc"){
            t = tAcc;
            x = xAcc;
            y = yAcc;
            z = zAcc;
        }else if(type == "Gyro"){
            t = tGyro;
            x = xGyro;
            y = yGyro;
            z = zGyro;
        }
        //リサンプリング後の格納用配列を用意
        ArrayList<Double> resampledTime = new ArrayList<>();
        ArrayList<Float> resampledX = new ArrayList<>();
        ArrayList<Float> resampledY = new ArrayList<>();
        ArrayList<Float> resampledZ = new ArrayList<>();

        //開始時間と終了時間
        double startTime = t.get(0);
        double endTime = t.get(t.size() - 1 );
        //リサンプリングの間隔
        double interval = 1.0 / 50;

        double currentTime = startTime;
        while(currentTime < endTime){
            resampledTime.add(currentTime);
            currentTime += interval;
        }

        for(int i = 0; i <= resampledTime.size() - 1; i++){
            Log.d("i",String.valueOf(i+":OK"));
            for(int j = 0; j <= t.size() - 1; j++){
                if(Math.abs(t.get(j) - resampledTime.get(i)) < 0.00001){
                    resampledX.add(x.get(j));
                    resampledY.add(y.get(j));
                    resampledZ.add(z.get(j));
                    break;
                }
                if(t.get(j) < resampledTime.get(i) && resampledTime.get(i) < t.get(j+1)){
                    //傾きを計算
                    float kx = (float) ((x.get(j+1) - x.get(j)) / (t.get(j+1) - t.get(j)));
                    float ky = (float) ((y.get(j+1) - y.get(j)) / (t.get(j+1) - t.get(j)));
                    float kz = (float) ((z.get(j+1) - z.get(j)) / (t.get(j+1) - t.get(j)));
                    //線形補間の計算
                    resampledX.add((float) (kx * (resampledTime.get(i) - t.get(j)) + x.get(j)));
                    resampledY.add((float) (ky * (resampledTime.get(i) - t.get(j)) + y.get(j)));
                    resampledZ.add((float) (kz * (resampledTime.get(i) - t.get(j)) + z.get(j)));
                    break;
                }
            }
        }

        if(type == "Acc"){
            xAcc.clear();
            xAcc.addAll(resampledX);
            yAcc.clear();
            yAcc.addAll(resampledY);
            zAcc.clear();
            zAcc.addAll(resampledZ);
            tAcc.clear();
            tAcc.addAll(resampledTime);
        }else if(type == "Gyro"){
            xGyro.clear();
            xGyro.addAll(resampledX);
            yGyro.clear();
            yGyro.addAll(resampledY);
            zGyro.clear();
            zGyro.addAll(resampledZ);
            tGyro.clear();
            tGyro.addAll(resampledTime);
        }
    }

    public void lowpassFilter(){
        double sf = 0.8; //平滑化係数

        for(int i = 0; i < tAcc.size(); i++){
            if(i == 0){

            }else{
                xAcc.set(i, (float) (sf * xAcc.get(i - 1) + (1 - sf) * xAcc.get(i)));
                yAcc.set(i, (float) (sf * yAcc.get(i - 1) + (1 - sf) * yAcc.get(i)));
                zAcc.set(i, (float) (sf * zAcc.get(i - 1) + (1 - sf) * zAcc.get(i)));
                xGyro.set(i, (float) (sf * xGyro.get(i - 1) + (1 - sf) * xGyro.get(i)));
                yGyro.set(i, (float) (sf * yGyro.get(i - 1) + (1 - sf) * yGyro.get(i)));
                zGyro.set(i, (float) (sf * zGyro.get(i - 1) + (1 - sf) * zGyro.get(i)));
            }
        }
    }
}
