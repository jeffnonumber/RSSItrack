package com.dougan.rssitrack;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * Created by Jeff on 04/05/2015.
 */
public class AudioSetup {

    private static int BufferElements2Rec = 0;

    public static AudioRecord getAudioParms() {

        final int[] intSampleRates = new int[] { 8000, 11025, 22050, 44100 };
        final String TAG = "RSSITrack";  //Tag for debug


        for (int rate : intSampleRates) {
            for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT }) {
                for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO }) {
                    try {
                        Log.d(TAG, "Attempting rate " + rate + ", bits: " + audioFormat + ", channel: " + channelConfig);
                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            Log.d(TAG, "Success at rate "+rate+", bits: "+audioFormat+", channel: "+channelConfig+", format: "+audioFormat+", buffer: "+bufferSize);
                            BufferElements2Rec = bufferSize/audioFormat;
                            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate, channelConfig, audioFormat, bufferSize);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED) return recorder;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, rate + "Invalid parms",e);
                    }
                }
            }
        }
        return null;
    }

    public static int getBuffer(){
        return BufferElements2Rec;
    }
}
