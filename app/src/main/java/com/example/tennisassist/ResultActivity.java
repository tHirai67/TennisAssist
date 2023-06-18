package com.example.tennisassist;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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

    private Interpreter tflite;
    private float[][] inputBuffer;
    private float[][] outputBuffer;

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
                //fileSave("acc");
                //fileSave("gyro");
                //fileSave("acc&gyro");
                fileSave("future");

                Toast.makeText(ResultActivity.this, "計測データを保存しました．", Toast.LENGTH_SHORT).show();
                save_btn.setVisibility(View.INVISIBLE);

                //strokeCount result display

                cTextView.setText(String.valueOf(""));
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
            Log.d("DATA",String.valueOf(gx.get(0)));
            ArrayList<Float> fv = getFutureValues(gx, gy, gz);
            futureValueList.add(fv);
        }

        //strokesClassifier(futureValueList);

    }

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
            if(type == "acc&gyro"){
                //ヘッダー
                writer.print("時刻,X軸加速度,Y軸加速度,Z軸加速度,X軸角速度,Y軸角速度,Z軸角速度\n");
                //データ出力
                int size = xAcc.size();
                for(int i = 0; i < size; i++){
                    String line = String.format(Locale.getDefault(), "%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f\n",
                            time.get(i), xAcc.get(i), yAcc.get(i), zAcc.get(i), xGyro.get(i), yGyro.get(i), zGyro.get(i));
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
                for (int i = 0; i < size; i++) {
                    String line = String.format(Locale.getDefault(), "%.3f,%.3f,%.3f,%.3f\n", tGyro.get(i), xGyro.get(i), yGyro.get(i), zGyro.get(i));
                    writer.print(line);
                }
            } else if (type == "future"){
                //ヘッダー
                writer.print("No.,x最大値,y最大値,z最大値,x最小値,y最小値,z最小値,x範囲,y範囲,z範囲,x分散,y分散,z分散,x標準偏差,y標準偏差,z標準偏差,x尖度,y尖度,z尖度\n");
                //データ出力
                int size = futureValueList.size();

                for (int i = 0; i < size; i++) {
                    ArrayList<Float> stroke = futureValueList.get(i);
                    String line = String.format(Locale.getDefault(), "%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f\n",
                            i+1,stroke.get(0),stroke.get(1),stroke.get(2),stroke.get(3),stroke.get(4),stroke.get(5),stroke.get(6),stroke.get(7),stroke.get(8),stroke.get(9),stroke.get(10),stroke.get(11),stroke.get(12),stroke.get(13),stroke.get(14),stroke.get(15),stroke.get(16),stroke.get(17));
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

//    //ストローク分類メソッド
//    public void strokesClassifier(ArrayList<ArrayList<Float>> futureList){
//        for()
//
//        // 入力と出力のバッファの初期化
//        inputBuffer = new float[1][18]; // 入力は1つのデータポイントで18の特徴量を持つ
//        outputBuffer = new float[1][3]; // 出力は1つのデータポイントで3つのクラスの確率を持つ
//
//        // 特徴量の設定
//        inputBuffer[0][0] = max_x;
//        inputBuffer[0][1] = max_y;
//        inputBuffer[0][2] = max_z;
//        inputBuffer[0][3] = min_x;
//        inputBuffer[0][4] = min_y;
//        inputBuffer[0][5] = min_z;
//        inputBuffer[0][6] = range_x;
//        inputBuffer[0][7] = range_y;
//        inputBuffer[0][8] = range_z;
//        inputBuffer[0][9] = kurtosis_x;
//        inputBuffer[0][10] = kurtosis_y;
//        inputBuffer[0][11] = kurtosis_z;
//        inputBuffer[0][12] = var_x;
//        inputBuffer[0][13] = var_y;
//        inputBuffer[0][14] = var_z;
//        inputBuffer[0][15] = std_x;
//        inputBuffer[0][16] = std_y;
//        inputBuffer[0][17] = std_z;
//    }

    //TensorFlowLiteモデルの読み込み
    private MappedByteBuffer loadModelFile() throws IOException {
        AssetManager assetManager = getAssets();
        AssetFileDescriptor fileDescriptor = assetManager.openFd("SC.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
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

    public ArrayList<Float> getFutureValues(ArrayList<Float> x, ArrayList<Float> y, ArrayList<Float> z){


        //最大値
        float max_x = Collections.max(x);
        float max_y = Collections.max(y);
        float max_z = Collections.max(z);
        //最小値
        float min_x = Collections.min(x);
        float min_y = Collections.min(y);
        float min_z = Collections.min(z);
        //範囲
        float range_x = max_x - min_x;
        float range_y = max_y - min_y;
        float range_z = max_z - min_z;

        //分散
        float var_x = var(x);
        float var_y = var(y);
        float var_z = var(z);

        //標準偏差
        float std_x = (float) Math.sqrt(var_x);
        float std_y = (float) Math.sqrt(var_y);
        float std_z = (float) Math.sqrt(var_z);

        //尖度
        float kurtosis_x = kurtosis(x);
        float kurtosis_y = kurtosis(y);
        float kurtosis_z = kurtosis(z);

        ArrayList<Float> fv = new ArrayList<>(Arrays.asList(max_x, max_y, max_z, min_x, min_y, min_z, range_x, range_y, range_z, var_x, var_y, var_z, std_x, std_y, std_z, kurtosis_x, kurtosis_y, kurtosis_z));

        return fv;
    }

    //分散値を計算するメソッド
    public float var(ArrayList<Float> data){
        int n = data.size();
        float mean = 0;
        float variance = 0;

        for(int i = 0; i < n; i++){
            mean += data.get(i);
        }
        mean /= n;

        for(int i = 0; i < n; i++){
            variance += (data.get(i) - mean)*(data.get(i) - mean);
        }
        variance /= n;

        return variance;
    }

    //尖度を計算するメソッド
    public float kurtosis(ArrayList<Float> data){
        int n = data.size();
        float mean = 0;
        float variance = var(data);
        float kur = 0;

        for(int i = 0; i < n; i++){
            mean += data.get(i);
        }
        mean /= n;

        for(int i = 0; i < n; i++){
            float d = data.get(i);
            kur += (d - mean) * (d - mean) * (d - mean) * (d - mean);
        }
        kur /= n * variance * variance;
        return kur - 3f;
    }
}
