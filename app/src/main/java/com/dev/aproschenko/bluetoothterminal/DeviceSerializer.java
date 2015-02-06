package com.dev.aproschenko.bluetoothterminal;

import com.google.gson.Gson;

public class DeviceSerializer
{
    public static String serialize(SettingsData settings)
    {
        Gson gson = new Gson();
        String s = gson.toJson(settings);
        return s;
    }

    public static SettingsData deserialize(String s)
    {
        Gson gson = new Gson();
        SettingsData settings = gson.fromJson(s, SettingsData.class);
        return settings;
    }
}
