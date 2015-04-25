package com.dougan.rssitrack;


public class DataPoint {

    String RSSI, SSID, time;
    double ampMax;

    public DataPoint(String time, String level, String SSID, double ampMax){
        this.RSSI = level;
        this.time = time;
        this.SSID = SSID;
        this.ampMax = ampMax;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(time).append(",").append(SSID).append(",").append(RSSI).append(",").append(ampMax).append("\n");

        return sb.toString();
    }


}
