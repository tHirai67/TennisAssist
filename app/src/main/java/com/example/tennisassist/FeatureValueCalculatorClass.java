package com.example.tennisassist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class FeatureValueCalculatorClass {

    //特徴量を計算する
    public ArrayList<Float> getFeatureValues(ArrayList<Float> data){

        //最大値
        float max = Collections.max(data);
        //最小値
        float min = Collections.min(data);
        //範囲
        float range = max - min;

        //分散
        float var = var(data);

        //標準偏差
        float std = (float) Math.sqrt(var);

        //尖度
        float kurtosis = kurtosis(data);

        ArrayList<Float> featureValues = new ArrayList<>(Arrays.asList(max, min, range, var, std, kurtosis));

        return featureValues;
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

    public ArrayList<Float> integration(ArrayList<Float> x, ArrayList<Float> y, ArrayList<Float> z){
        ArrayList<Float> featureValues = new ArrayList<>(18);

        for(int i = 0; i < 6; i++){
            featureValues.add(x.get(i));
            featureValues.add(y.get(i));
            featureValues.add(z.get(i));
        }
        return featureValues;

    }
}
