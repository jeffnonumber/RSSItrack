package com.dougan.rssitrack;

import android.content.Context;
import android.media.AudioRecord;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends ActionBarActivity implements View.OnClickListener{

    private TextView textConnected, textIP, textSSID, textRSSI, textTime, textVolume;
    private Button btnStop, btnStart, btnQuit;

    private final Handler hTime = new Handler(); //Handler for timer
    private final String TAG = "RSSITrack";  //Tag for debug
    private ArrayList<DataPoint> lScans = new ArrayList<>();
    private Timer myTimer;
    private String sRecID;

    private NetworkInfo nInfo;
    private WifiManager wManager;

    private AudioRecord recorder = null;
    private int BufferElements2Rec;
    short sData[];

    boolean bStart = false;
    boolean bStop = false;

    //        Galaxy S2 - Success at 8000Hz, bits: 2, channel: 16, format: 2, buffer: 1024
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
        textVolume = (TextView)findViewById(R.id.Volume);

        btnStart = (Button) this.findViewById(R.id.btnStart);
        btnStop = (Button) this.findViewById(R.id.btnStop);
        btnQuit = (Button) this.findViewById(R.id.btnQuit);

        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnQuit.setOnClickListener(this);

        ConnectivityManager cManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        nInfo = cManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        wManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
    }


    public void onClick(View v) {

        if(v == btnStart && !bStart){
            try {
                lScans.add(new DataPoint(getGoodTime(System.currentTimeMillis()), "", "", 0));
                recorder = AudioSetup.getAudioParms();
                BufferElements2Rec = AudioSetup.getBuffer();
                if(recorder != null){
                    recorder.startRecording();
                    bStart = true;
                    bStop = false;
                    sRecID = "Dev"+android.os.Build.SERIAL+"Time"+System.currentTimeMillis();
                }
                //Log.d(TAG, "Recorder state " + recorder.getRecordingState());//3 is good
            }catch (IllegalArgumentException e){
                e.printStackTrace();
                recorder = null;
                Toast.makeText (this, "Incompatible Device", Toast.LENGTH_LONG).show();
            }

            myTimer = new Timer();
            myTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    hTime.post(pullData);
                }
            }, 0, 2000);  //Find a way to increase scan rate?
        }
        else if (v == btnStop && bStart) {
            bStart = false;
            bStop = true;
            //if(fileWrite(sRecID))
            if(FileWrite.toFile(lScans, new File(getExternalFilesDir(null), sRecID + ".csv"))){
                Toast.makeText(getApplicationContext(),"Saved",Toast.LENGTH_LONG).show();
                if(recorder != null){
                    recorder.stop();recorder.release();recorder=null;myTimer.cancel();
                }

                textIP.setText("STOPPED");
                textSSID.setText("STOPPED");
                textRSSI.setText("STOPPED");
                textTime.setText("STOPPED");
                textVolume.setText("STOPPED");

            }else Toast.makeText (this, "Save Failed", Toast.LENGTH_SHORT).show();this.onPause();
        }
        else if (v == btnQuit){
            this.onPause();
            android.os.Process.killProcess(android.os.Process.myPid()) ;
        }
    }

    @Override
    public void onPause(){

        if(recorder != null){
            recorder.stop();recorder.release();recorder=null;myTimer.cancel();
            Log.e(TAG, "Cleanup complete ---------------------------------------------------.");
        }
        super.onPause();
    }

    final Runnable pullData = new Runnable() {
        public void run() {

            if (nInfo.isConnected()&&!bStop){


                //Log.d(TAG, wInfo.toString());  //Debug print of wifi info
                //Log.d(TAG, wManager.getScanResults().toString());//Multiple hub support?

                textConnected.setText("Connected");
                WifiInfo wInfo = wManager.getConnectionInfo();
                int myIp = wInfo.getIpAddress();



                String sIP = Formatter.formatIpAddress(myIp);//Will not work with IPV6, not essential
                String sSSID = wInfo.getSSID();
                String sRSSI = String.valueOf(wInfo.getRssi());
                //Log.d(TAG,sRSSI);
                long lTime = System.currentTimeMillis();
                String sTime = getGoodTime(lTime);//Not here for a long time

                textIP.setText(sIP);
                textSSID.setText(sSSID);
                textRSSI.setText(sRSSI);
                textTime.setText(sTime);

                double dAmpMax = AmpMax.getAmpMax(recorder, BufferElements2Rec);
                String sAmpMax = String.valueOf(dAmpMax);

                textVolume.setText(sAmpMax);

                /*sData = new short[BufferElements2Rec];
                recorder.read(sData, 0, BufferElements2Rec);

                if(sData.length != 0){
                    for(int i=0; i<sData.length; i++){
                        if(Math.abs(sData[i])>=dAmpMax){
                            dAmpMax=Math.abs(sData[i]);
                        }
                    }
                }
                else Log.d(TAG, "Buffer empty");*/


                lScans.add(new DataPoint(sTime, sRSSI, sSSID, dAmpMax));
                //Log.d(TAG, lScans.toString());
                //Log.d(TAG, sDataString);
            }
            else{
                Log.d(TAG, nInfo.toString());
                textConnected.setText("Not Connected");
                textIP.setText("Not Connected");
                textSSID.setText("Not Connected");
                textRSSI.setText("Not Connected");
            }
        }
    };



    public String getGoodTime(long millis)
    {
        if(millis < 0) throw new IllegalArgumentException("Invalid duration");

        return (new SimpleDateFormat("D:mm:ss:SSS")).format(new Date(millis));
    }


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
