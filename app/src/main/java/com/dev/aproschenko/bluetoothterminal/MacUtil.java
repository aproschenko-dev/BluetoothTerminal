package com.dev.aproschenko.bluetoothterminal;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MacUtil
{
    public static String serializeMacs(ArrayList<MacData> macs)
    {
        Gson gson = new Gson();
        Type listOfMacObject = new TypeToken<List<MacData>>(){}.getType();
        String s = gson.toJson(macs, listOfMacObject);
        return s;
    }

    public static ArrayList<MacData> deserializeMacs(String s)
    {
        Gson gson = new Gson();
        Type listOfMacObject = new TypeToken<List<MacData>>(){}.getType();
        ArrayList<MacData> macs = gson.fromJson(s, listOfMacObject);
        return macs;
    }

    public static String getVendorByAddress(String address, List<MacData> macs)
    {
        for (MacData mac : macs)
        {
            if (address.replace(":", "").toLowerCase().startsWith(mac.address.toLowerCase()))
                return mac.vendor;
        }

        return "";
    }
}
