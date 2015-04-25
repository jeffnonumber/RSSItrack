package com.dougan.rssitrack;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends ActionBarActivity implements View.OnClickListener{

    private TextView textConnected, textIP, textSSID, textRSSI, textTime;
    private Button btnStop;


    private final Handler myHandler = new Handler(); //Handler for timer
    //private static final String sDir = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static final String TAG = "RSSITrack";  //Tag for debug
    private ArrayList<DataPoint> lScans = new ArrayList<>();
    private static Timer myTimer;
    private static String sRecID;

    //private ConnectivityManager myConnManager;
    private NetworkInfo myNetworkInfo;
    private WifiManager myWifiManager;
    //private WifiInfo myWifiInfo;

    private AudioRecord recorder = null;
    private static final int[] intSampleRates = new int[] { 8000, 11025, 22050, 44100 };
    private static int BufferElements2Rec;
    short sData[];// = new short[BufferElements2Rec];




    //        BUTTON TO CANCEL TIMER
    //        AND WRITE TO FILE
    //        THEN SEND FILE
    //        INTERPRET ON PC
    //
    //        STRING S = PHONE ID + NUMBER
    //
    //        Galaxy - Success at 8000Hz, bits: 2, channel: 16, format: 2, buffer: 1024
    //        One Mini 2 - Success at 8000Hz, bits: 2, channel: 16, format: 2, buffer: 640

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textConnected = (TextView)findViewById(R.id.Connected);
        textIP = (TextView)findViewById(R.id.IP);
        textSSID = (TextView)findViewById(R.id.SSID);
        textRSSI = (TextView)findViewById(R.id.RSSI);
        textTime = (TextView)findViewById(R.id.Time);

        btnStop = (Button) this.findViewById(R.id.btnStop);
        btnStop.setOnClickListener(this);

        try {
            recorder = findAudioRecord();
            //short sData[] = new short[BufferElements2Rec];
            recorder.startRecording();
            Log.d(TAG, "Recorder state " + recorder.getRecordingState());//3 is good
        }catch (IllegalArgumentException e){
            e.printStackTrace();
            recorder = null;
        }


        ConnectivityManager myConnManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        myNetworkInfo = myConnManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        myWifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        //myWifiInfo = myWifiManager.getConnectionInfo();

        String uid = android.os.Build.SERIAL;
        sRecID = "D"+uid+"T"+System.currentTimeMillis();


        myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                myHandler.post(pullData);
                //DisplayWifiState();
            }
        }, 0, 2000);  //Find a way to increase scan rate?


    }


    public void onClick(View v) {
        if (v == btnStop) {
            if(fileWrite(sRecID)){
                myTimer.cancel();
                this.onStop();

            }else Toast.makeText (this, "Save Failed", Toast.LENGTH_SHORT).show();this.onStop();
        }
    }

    /*@Override
    public void onDestroy(){
        if(recorder != null){recorder.stop();recorder.release();recorder=null;}
        Log.e(TAG, "Cleanup complete ---------------------------------------------------.");
        super.onDestroy();
    }*/



    @Override
    public void onStop(){

        if(recorder != null){
            recorder.stop();recorder.release();recorder=null;
            Log.e(TAG, "Cleanup complete ---------------------------------------------------.");
        }
        super.onStop();
    }


   /* private void DisplayWifiState(){

        myHandler.post(pullData);

    }*/

    final Runnable pullData = new Runnable() {
        public void run() {

            if (myNetworkInfo.isConnected()){


                //Log.d(TAG, myWifiInfo.toString());  //Debug print of wifi info
                //Log.d(TAG, myWifiManager.getScanResults().toString());//Multiple hub support

                textConnected.setText("Connected");
                WifiInfo myWifiInfo = myWifiManager.getConnectionInfo();
                int myIp = myWifiInfo.getIpAddress();


                String sIP = Formatter.formatIpAddress(myIp);//Will not work with IPV6
                String sSSID = myWifiInfo.getSSID();
                String sRSSI = String.valueOf(myWifiInfo.getRssi());
                Log.d(TAG,sRSSI);
                long lTime = System.currentTimeMillis();
                String sTime = lTime+"ms";

                textIP.setText(sIP);
                textSSID.setText(sSSID);
                textRSSI.setText(sRSSI);
                textTime.setText(sTime);

                String sDataString = "";
                double dAmpMax = 0;
                sData = new short[BufferElements2Rec];
                recorder.read(sData, 0, BufferElements2Rec);

                if(sData.length != 0){
                    for(int i=0; i<sData.length; i++){
                        if(Math.abs(sData[i])>=dAmpMax){
                            dAmpMax=Math.abs(sData[i]);
                        }
                    }
                }
                else Log.d(TAG, "Buffer empty");


                lScans.add(new DataPoint(lTime, sRSSI, sSSID, dAmpMax));
                Log.d(TAG, lScans.toString());
                Log.d(TAG, sDataString);
            }
            else{
                Log.d(TAG, myNetworkInfo.toString());
                textConnected.setText("Not Connected");
                textIP.setText("Not Connected");
                textSSID.setText("Not Connected");
                textRSSI.setText("Not Connected");
            }
        }
    };

    public boolean fileWrite(String s){

        try {
            File myFile = new File(getExternalFilesDir(null), "Datapoint"+s+".txt");
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(lScans.toString());
            myOutWriter.close();
            fOut.close();

            Toast.makeText(getApplicationContext(),"Saved",Toast.LENGTH_LONG).show();
            return true;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }


    }

    public AudioRecord findAudioRecord() {
        for (int rate : intSampleRates) {
            for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT }) {
                for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO }) {
                    try {
                        Log.d(TAG, "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
                                + channelConfig);
                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            // check if we can instantiate and have a success
                            Log.d(TAG, "Success at " + rate + "Hz, bits: " + audioFormat + ", channel: " + channelConfig + ", format: " + audioFormat + ", buffer: " + bufferSize);
                            BufferElements2Rec = bufferSize/audioFormat;
                            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate, channelConfig, audioFormat, bufferSize);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
                                return recorder;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, rate + "Exception, keep trying.",e);
                    }
                }
            }
        }
        return null;
    }


   /* private boolean externalStorageAvailable() {
        return
                Environment.MEDIA_MOUNTED
                        .equals(Environment.getExternalStorageState());
    }*/





    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
