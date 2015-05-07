package com.dougan.rssitrack;

import android.media.AudioRecord;
import android.util.Log;

/**
 * Created by Jeff on 05/05/2015.
 */
public class AmpMax {

    public static double getAmpMax(AudioRecord recorder, int BufferElements2Rec){
        final String TAG = "RSSITrack";  //Tag for debug
        double dAmpMax = 0;
        short[] sData = new short[BufferElements2Rec];

        recorder.read(sData, 0, BufferElements2Rec);

        if(sData.length != 0){
            for(int i=0; i<sData.length; i++){
                if(Math.abs(sData[i])>=dAmpMax){
                    dAmpMax=Math.abs(sData[i]);
                }
            }
        }
        else Log.d(TAG, "Buffer empty");
        return dAmpMax;
    }
}
