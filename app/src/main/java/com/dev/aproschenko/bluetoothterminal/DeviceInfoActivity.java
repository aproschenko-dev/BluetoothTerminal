package com.dev.aproschenko.bluetoothterminal;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;

public class DeviceInfoActivity extends Activity
{
    private static final String TAG = "DeviceInfoActivity";
    private static final boolean D = true;

    private MainApplication getApp() { return (MainApplication) getApplication(); }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (D) Log.d(TAG, "+++ ON CREATE +++");

        initMACs();

        Intent intent = getIntent();
        String connectedDeviceName = intent.getStringExtra(MainActivity.DEVICE_NAME);
        String connectedDeviceAddress = intent.getStringExtra(MainActivity.DEVICE_ADDRESS);

        DeviceData deviceData = getApp().getDeviceDataByAddress(connectedDeviceAddress);

        setContentView(R.layout.device_info);
        setTitle(String.format("%s - %s", getResources().getString(R.string.device_info), connectedDeviceName));

        String services = getDeviceServices(deviceData);
        if (services.equals(""))
        {
            services = getResources().getString(R.string.no_services);
        }
        final TextView infoView = (TextView) findViewById(R.id.deviceServicesInfo);
        infoView.setText(services);

        final TextView deviceName = (TextView) findViewById(R.id.deviceName);
        deviceName.setText(deviceData.getName());

        final TextView deviceAddress = (TextView) findViewById(R.id.deviceAddress);
        deviceAddress.setText(deviceData.getAddress());

        String classString = String.format("%d [0x%s]", deviceData.getDeviceClass(), Integer.toHexString(deviceData.getDeviceClass()).toUpperCase());
        final TextView deviceClass = (TextView) findViewById(R.id.deviceClass);
        deviceClass.setText(classString);

        String classMajorString = String.format("%d [0x%s]", deviceData.getMajorDeviceClass(), Integer.toHexString(deviceData.getMajorDeviceClass()).toUpperCase());
        final TextView deviceMajorClass = (TextView) findViewById(R.id.deviceMajorClass);
        deviceMajorClass.setText(classMajorString);

        boolean isBonded = deviceData.getBondState() == BluetoothDevice.BOND_BONDED;
        String bondedState = isBonded ? getResources().getString(R.string.bonded) : getResources().getString(R.string.unbonded);
        final TextView deviceState = (TextView) findViewById(R.id.deviceState);
        deviceState.setText(bondedState);
        if (isBonded)
            deviceState.setTextColor(Color.GREEN);

        String vendor = MacUtil.getVendorByAddress(deviceData.getAddress(), getApp().getMACs());
        final TextView deviceVendor = (TextView) findViewById(R.id.deviceVendor);
        deviceVendor.setText(vendor);

        final TextView deviceTimestamp = (TextView) findViewById(R.id.deviceTimestamp);
        deviceTimestamp.setText(deviceData.getTimestamp().toString());

        String rssi = deviceData.getRssi() == -1 ? "-" : String.format("%d dBm", deviceData.getRssi());
        final TextView deviceRSSI = (TextView) findViewById(R.id.deviceRSSI);
        deviceRSSI.setText(rssi);
    }

    private void initMACs()
    {
        if (getApp().getMACs().size() == 0)
        {
            String macJson = MacConverter.readMacsJson(R.raw.vendors, this);
            ArrayList<MacData> macs = MacUtil.deserializeMacs(macJson);
            for (MacData macData : macs)
            {
                getApp().getMACs().add(macData);
            }
        }
    }

    private String getDeviceServices(DeviceData itemData)
    {
        String text = "";

        SortedMap<Integer, String> services = BluetoothUtils.getDeviceServicesMap(itemData.getUuids());
        for (Map.Entry<Integer, String> entry : services.entrySet())
        {
            String key = Integer.toHexString(entry.getKey());
            if (key.length() == 1)
                key = "000" + key;
            else if (key.length() == 2)
                key = "00" + key;
            else if (key.length() == 3)
                key = "0" + key;
            String value = entry.getValue();
            text += String.format("0x%s - %s\r\n", key.toUpperCase(), value);
        }

        return text;
    }
}
