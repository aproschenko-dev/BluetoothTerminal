package com.dev.aproschenko.bluetoothterminal;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MacConverter
{
    private static final String TAG = "MacConverter";
    private static final boolean D = true;

    public static String readMacsJson(int macId, Context context)
    {
        String jsonData = "";

        try
        {
            InputStream inputStream = context.getResources().openRawResource(macId);
            BufferedReader myReader = new BufferedReader(new InputStreamReader(inputStream));
            String aDataRow;
            String aBuffer = "";
            while ((aDataRow = myReader.readLine()) != null)
            {
                aBuffer += aDataRow + "\n";
            }
            jsonData = aBuffer;
            myReader.close();
        }
        catch (Exception e)
        {
            if (D) Log.e(TAG, "readMacsJson() failed", e);
        }

        return jsonData;
    }

    public static String convertFile(int macId, Context context, MainApplication application)
    {
        if (D) Log.d(TAG, "convertFile");

        ArrayList<MacData> macs = new ArrayList<>();
        InputStream inputStream = context.getResources().openRawResource(macId);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;

        try
        {
            line = reader.readLine();

            while (line != null)
            {
                if (line.startsWith("  ")) //skip vendor address
                {
                    if (line.length() >= 3)
                    {
                        if (line.charAt(2) != '\t')
                        {
                            line = line.trim();

                            if (line.charAt(2) != '-') //skip 00-00-00 lines
                            {
                                MacData mac = new MacData();
                                mac.address = line.substring(0, 6);

                                int lastTab = line.lastIndexOf("\t");
                                mac.vendor = line.substring(lastTab + 1);

                                macs.add(mac);
                            }
                        }
                    }
                }

                line = reader.readLine();
            }
        }
        catch (IOException e)
        {
            if (D) Log.e(TAG, "convertFile", e);
        }

        try
        {
            inputStream.close();
        }
        catch (IOException e)
        {
            if (D) Log.e(TAG, "convertFile", e);
        }

        String json = MacUtil.serializeMacs(macs);
        application.writeFile(json, "vendors.txt");
        return json;
    }
}
