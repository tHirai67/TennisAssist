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
import java.util.Collections;
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
    private ArrayList<Double> time = new ArrayList<>();


    private ArrayList<Float> ave = new ArrayList<>();
    private ArrayList<Float> norm = new ArrayList<>();
    private ArrayList<Float> diff = new ArrayList<>();

    private int ForehandCount, BackhandCount, OtherCount;

    private ArrayList<ArrayList<Float>> futureValueList = new ArrayList<>();

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


        save_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileSave("acc");
                fileSave("gyro");
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

    @Override
    protected void onStart() {
        super.onStart();

        StrokeDetectorClass sd = new StrokeDetectorClass();


        //加速度と角速度の同期
        synchronization();
        //ローパスフィルタ
        lowpassFilter();
        //絶対値平均を計算
        ave = averageAbsolute(tAcc, xAcc, yAcc, zAcc);
        //正規化(min-Max)
        norm = normalization(ave);
        //差分値
        diff = difference(norm);

        //ストローク検出範囲のリスト取得
        ArrayList<ArrayList<Integer>> strokesIndexList = StrokeDetection();

        //検出回数を表示
        int strokesCount = strokesIndexList.size();

        //特徴量の計算
        for(int i = 0; i < strokesCount; i++){
            ArrayList<Integer> stroke = strokesIndexList.get(i);

            //ストローク範囲の角速度を取得
            ArrayList<Float> gx = new ArrayList<>();
            ArrayList<Float> gy = new ArrayList<>();
            ArrayList<Float> gz = new ArrayList<>();
            for(int j = stroke.get(0); j < stroke.get(stroke.size()-1); j++){
                gx.add(xGyro.get(j));
                gy.add(yGyro.get(j));
                gz.add(zGyro.get(j));
            }


            //特徴量を取得
            FeatureValueCalculatorClass fvc = new FeatureValueCalculatorClass();
            ArrayList<Float> fvX = fvc.getFeatureValues(gx);
            ArrayList<Float> fvY = fvc.getFeatureValues(gy);
            ArrayList<Float> fvZ = fvc.getFeatureValues(gz);

            ArrayList<Float> featureValues = fvc.integration(fvX, fvY, fvZ);

            //特徴量リストに追加
            futureValueList.add(featureValues);

            Log.d("stroke",strokeClassification(featureValues));

            switch (strokeClassification(featureValues)){
                case "その他":
                    OtherCount += 1;
                    break;
                case "フォアハンド":
                    ForehandCount += 1;
                    break;
                case "バックハンド":
                    BackhandCount += 1;
                    break;
                default:
                    break;
            }
        }

        if(ForehandCount == 0 && BackhandCount == 0 && OtherCount == 0){
            cTextView.setText("Nothing!!");
        }else{
            cTextView.setText("Fore："+ForehandCount+"\nBack："+BackhandCount+"\nOther："+OtherCount);
        }

    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        OtherCount = 0;
        ForehandCount = 0;
        BackhandCount = 0;
    }

    //ストロークの検出メソッド
    public ArrayList<ArrayList<Integer>> StrokeDetection(){
        //検出したストロークのデータのインデックス(100データ)を格納する配列
        ArrayList<ArrayList<Integer>> strokesList = new ArrayList<>();
        //ウィンドウ
        ArrayList<Float> check_list = new ArrayList<>();
        ArrayList<Float> check_list2 = new ArrayList<>();
        ArrayList<Float> check_list3 = new ArrayList<>();

        //スライディングウィンドウ法
        int window_size = 100; //ウィンドウサイズ
        int slide_size = 10; //スライドサイズ

        for(int i = 0; i < tAcc.size(); i++){
            if(check_list.size() == window_size){
                //判定条件のチェック数
                int flag = 0;

                //最大値と最大値のインデックスを取得
                int maxIndex = 0;
                float maxValue = check_list.get(0);
                for(int j = 0; j < check_list.size();j++){
                    if(check_list.get(j) > maxValue){
                        maxValue = check_list.get(j);
                        maxIndex = j;
                    }
                }

                //インパクトの判定
                if(maxValue >= 0.3){
                    flag++;
                }

                //振り始めの判定
                int start_index = 0;
                for(int j = maxIndex; j >= 1; j--){
                    if(check_list.get(j) <= maxValue/2 && check_list2.get(j) >= 0 && check_list2.get(j-1) < 0){
                        flag++;
                        start_index = j;
                        break;
                    }
                }

                //振り終わりの判定
                for(int j = maxIndex; j < check_list.size(); j++){
                    if(check_list.get(j) <= maxValue/2 && check_list2.get(j) >= 0){
                        flag++;
                        break;
                    }
                }

                //テイクバックの判定
                for(int j = start_index; j >= 1; j--){
                    if(check_list3.get(j) - check_list3.get(j-1) > 0 && check_list3.get(j) - check_list3.get(j+1) > 0){
                        flag++;
                        break;
                    }
                }

                if(flag == 4){//検出条件を満たす
                    //検出したストロークデータ(100)を格納
                    ArrayList<Integer> values = new ArrayList<>();
                    for(int j = i - window_size + 1; j <= i; j++){
                        values.add(j);
                    }
                    strokesList.add(values);

                    //初期化
                    check_list.clear();
                    check_list2.clear();
                    check_list3.clear();
                }else{//検出条件を満たさない
                    //スライドサイズ分削除
                    for(int j = 0; j < slide_size; j++){
                        check_list.remove(0);
                        check_list2.remove(0);
                        check_list3.remove(0);
                    }
                }

            }else{
                check_list.add(norm.get(i));
                check_list2.add(diff.get(i));
                check_list3.add(xGyro.get(i));
            }
        }

        return strokesList;
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
            if(type == "acc"){
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
                for (int i = 0; i < size; i++) {
                    String line = String.format(Locale.getDefault(), "%.3f,%.3f,%.3f,%.3f\n", tGyro.get(i), xGyro.get(i), yGyro.get(i), zGyro.get(i));
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

        time.addAll(tAcc);
    }



    //ストローク分類メソッド
    public String strokeClassification(ArrayList<Float> features){
        StrokeClassifierClass classifier = new StrokeClassifierClass(getApplicationContext(),"SC.tflite");
        String predictedLabel = "";

        //特徴量の設定
        float max_x = features.get(0);
        float max_y = features.get(1);
        float max_z = features.get(2);
        float min_x = features.get(3);
        float min_y = features.get(4);
        float min_z = features.get(5);
        float range_x = features.get(6);
        float range_y = features.get(7);
        float range_z = features.get(8);

        //順番がモデルと変わっているので注意
        float var_x = features.get(9);
        float var_y = features.get(10);
        float var_z = features.get(11);
        float std_x = features.get(12);
        float std_y = features.get(13);
        float std_z = features.get(14);
        float kurtosis_x = features.get(15);
        float kurtosis_y = features.get(16);
        float kurtosis_z = features.get(17);

        // 入力と出力のバッファの初期化
        float[] inputData = {
                max_x, max_y, max_z, min_x, min_y, min_z, range_x, range_y, range_z,
                kurtosis_x, kurtosis_y, kurtosis_z, var_x, var_y, var_z, std_x, std_y, std_z
        };

        if(classifier != null){
            predictedLabel = classifier.predict(inputData);
        }

        return predictedLabel;
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

    //絶対値平均の計算メソッド
    public ArrayList<Float> averageAbsolute(ArrayList<Double> t, ArrayList<Float> a, ArrayList<Float> b, ArrayList<Float> c){
        ArrayList<Float> x = new ArrayList<>();
        for(int i = 0; i < t.size(); i++) {
            x.add((Math.abs(a.get(i)) + Math.abs(b.get(i)) + Math.abs(c.get(i))) / 3);
        }
        return x;
    }

    //正規化(Min-Max法)メソッド
    public ArrayList<Float> normalization(ArrayList<Float> values){
        ArrayList<Float> normalizaedValues = new ArrayList<>();
        //正規化後の最小値と最大値
        float newMin = 0;
        float newMax = 1;
        //正規化した配列の最小値と最大値を取得
        float min = Collections.min(values);
        float max = Collections.max(values);
        //範囲
        float range = max - min;

        //正規化
        for(Float value : values){
            float normalizedValue = (value - min) * (newMax - newMin) / range + newMin;
            normalizaedValues.add(normalizedValue);
        }

        return normalizaedValues;
    }

    //差分値の計算メソッド
    public ArrayList<Float> difference(ArrayList<Float> values){
        ArrayList<Float> diffValues = new ArrayList<>();
        for(int i = 0; i < values.size(); i++){
            if(i == values.size() - 1){
                diffValues.add(0.0f);
            }else{
                diffValues.add(values.get(i+1) - values.get(i));
            }

        }
        return diffValues;
    }
}
