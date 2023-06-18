package com.example.tennisassist;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class StrokeClassifierClass {

    Interpreter interpreter;

    //コンストラクター
    public StrokeClassifierClass(Context context, String modelName){
        try {
            AssetManager assetManager = context.getAssets();
            Interpreter.Options options = new Interpreter.Options();
            interpreter = new Interpreter(loadModelFile(assetManager, modelName), options);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelName) throws IOException{
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public String predict(float[] inputData){
        float[][] input = {inputData};
        float[][] output = new float[1][3];
        interpreter.run(input, output);
        return getLabel(output);
    }

    public String getLabel(float[][] output){
        float[] probabilities = output[0];
        float maxProbability = 0;
        int predictedLabel = -1;

        for (int i = 0; i < probabilities.length; i++) {
            if (probabilities[i] > maxProbability) {
                maxProbability = probabilities[i];
                predictedLabel = i;
            }
        }

        switch (predictedLabel){
            case 0:
                return "その他";
            case 1:
                return "フォアハンド";
            case 2:
                return "バックハンド";
            default:
                return "不明";
        }
    }

    public void close(){
        interpreter.close();
    }
}
