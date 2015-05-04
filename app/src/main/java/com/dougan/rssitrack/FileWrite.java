package com.dougan.rssitrack;

import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

/**
 * Created by Jeff on 04/05/2015.
 */
public class FileWrite {

    public static boolean toFile(ArrayList<DataPoint> lScans, File file){
        try {
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter outWriter = new OutputStreamWriter(fOut);
            outWriter.append(lScans.toString());
            outWriter.close();
            fOut.close();

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

}
