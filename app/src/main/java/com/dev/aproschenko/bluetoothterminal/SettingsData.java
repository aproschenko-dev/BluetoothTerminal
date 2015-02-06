package com.dev.aproschenko.bluetoothterminal;

import java.util.ArrayList;
import com.dev.aproschenko.bluetoothterminal.MainApplication.SortType;

public class SettingsData
{
    private ArrayList<DeviceData> devices = new ArrayList<>();

    private SortType sortType = SortType.SORT_BY_NAME;

    public ArrayList<DeviceData> getDevices()
    {
        return devices;
    }

    public void setSortType(SortType value)
    {
        sortType = value;
    }

    public SortType getSortType()
    {
        return sortType;
    }
}
