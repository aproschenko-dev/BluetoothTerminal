package com.dev.aproschenko.bluetoothterminal;

import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;

import java.util.ArrayList;
import java.util.Date;

public class DeviceData
{
    private String name = "";
    private String address = "";
    private int bondState = BluetoothDevice.BOND_NONE;
    private ArrayList<ParcelUuid> uuids = null;
    private int deviceClass;
    private int rssi = -1;
    private Date timestamp;
    private int majorDeviceClass;

    public DeviceData(BluetoothDevice device, int rssi, String emptyName)
    {
        address = device.getAddress();
        updateData(device, rssi, emptyName);
    }

    //update all data except address
    public void updateData(BluetoothDevice device, int rssi, String emptyName)
    {
        name = device.getName();
        bondState = device.getBondState();

        if (name == null || name.isEmpty())
            name = emptyName;

        deviceClass = device.getBluetoothClass().getDeviceClass();
        majorDeviceClass = device.getBluetoothClass().getMajorDeviceClass();

        uuids = BluetoothUtils.getDeviceUuids(device);

        this.rssi = rssi;
        timestamp = new Date();
    }

    public int getDeviceClass()
    {
        return deviceClass;
    }

    public int getMajorDeviceClass()
    {
        return majorDeviceClass;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String deviceName)
    {
        name = deviceName;
    }

    public String getAddress()
    {
        return address;
    }

    public ArrayList<ParcelUuid> getUuids()
    {
        return uuids;
    }

    public int getBondState()
    {
        return bondState;
    }

    public int getRssi() { return rssi; }

    public Date getTimestamp() { return timestamp; }
}
