package com.dev.aproschenko.bluetoothterminal;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.dev.aproschenko.bluetoothterminal.MainApplication.SortType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class DevicesRowAdapter extends ArrayAdapter<DeviceData>
{
    private final MainActivity context;
    private final ArrayList<DeviceData> devices;
    private final MainApplication.SortType sortType;

    private MainApplication getApp() { return (MainApplication) context.getApplication(); }

    public DevicesRowAdapter(MainActivity context, ArrayList<DeviceData> devices, SortType sortBy)
    {
        super(context, R.layout.bt_device_row, devices);

        this.context = context;
        this.devices = devices;
        this.sortType = sortBy;

        Collections.sort(devices, new Comparator<DeviceData>()
        {
            @Override
            public int compare(DeviceData o1, DeviceData o2)
            {
                if (sortType == SortType.SORT_BY_NAME)
                {
                    return o1.getName().compareTo(o2.getName());
                }
                else if (sortType == SortType.SORT_BY_BONDED_STATE)
                {
                    Integer i1 = o1.getBondState();
                    Integer i2 = o2.getBondState();
                    return i1.compareTo(i2) * (-1);
                }
                else //type
                {
                    Integer i1 = o1.getDeviceClass();
                    Integer i2 = o2.getDeviceClass();
                    return i1.compareTo(i2);
                }
            }
        });
    }

    private View.OnClickListener onDeviceInfoButtonClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            String address = (String)v.getTag();
            deviceInfoButtonClick(address);
        }
    };

    private void deviceInfoButtonClick(String address)
    {
        DeviceData deviceData = getApp().getDeviceDataByAddress(address);

        Intent intent = new Intent(context, DeviceInfoActivity.class);
        intent.putExtra(MainActivity.DEVICE_NAME, deviceData.getName());
        intent.putExtra(MainActivity.DEVICE_ADDRESS, deviceData.getAddress());
        context.startActivity(intent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        DeviceData device = devices.get(position);

        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.bt_device_row, parent, false);

        TextView deviceName = (TextView) rowView.findViewById(R.id.deviceName);
        TextView deviceAddress = (TextView) rowView.findViewById(R.id.deviceAddress);
        TextView deviceDesc = (TextView) rowView.findViewById(R.id.deviceDesc);
        TextView deviceState = (TextView) rowView.findViewById(R.id.deviceState);
        ImageView deviceIcon = (ImageView) rowView.findViewById(R.id.deviceIcon);
        TextView deviceServices = (TextView) rowView.findViewById(R.id.deviceServices);

        ImageButton showDeviceInfoButton = (ImageButton) rowView.findViewById(R.id.showDeviceInfoButton);
        showDeviceInfoButton.setTag(device.getAddress());

        showDeviceInfoButton.setOnClickListener(onDeviceInfoButtonClick);

        boolean isBonded = device.getBondState() == BluetoothDevice.BOND_BONDED;
        String bondedState = isBonded ? context.getResources().getString(R.string.bonded) : "";

        if (isBonded)
            rowView.setBackgroundColor(getApp().bondedBgColor);

        deviceName.setText(device.getName());
        deviceAddress.setText(String.format("- %s", device.getAddress()));
        deviceState.setText(bondedState);
        deviceServices.setText(device.getUuids().size() == 0 ? context.getResources().getString(R.string.no_services_string) : "");

        deviceServices.setVisibility(deviceServices.getText().length() == 0 ? View.INVISIBLE : View.VISIBLE);
        deviceState.setVisibility(deviceState.getText().length() == 0 ? View.INVISIBLE : View.VISIBLE);

        if (deviceServices.getVisibility() == View.VISIBLE)
        {
            deviceName.setTextColor(0xff666666);
            deviceDesc.setTextColor(0xff666666);
        }

        int majorClass = device.getMajorDeviceClass();
        int deviceClass = device.getDeviceClass();

        switch (majorClass)
        {
            case BluetoothClass.Device.Major.UNCATEGORIZED:
                deviceDesc.setText(context.getResources().getString(R.string.uncategorized));
                deviceIcon.setImageResource(R.drawable.icomisc);
                break;
            case BluetoothClass.Device.Major.HEALTH:
                deviceDesc.setText(context.getResources().getString(R.string.health));
                deviceIcon.setImageResource(R.drawable.icohealth);
                break;
            case BluetoothClass.Device.Major.TOY:
                deviceDesc.setText(context.getResources().getString(R.string.toy));
                deviceIcon.setImageResource(R.drawable.icotoy);
                break;
            case BluetoothClass.Device.Major.IMAGING:
                deviceDesc.setText(context.getResources().getString(R.string.imaging));
                deviceIcon.setImageResource(R.drawable.icomisc);
                break;
            case BluetoothClass.Device.Major.NETWORKING:
                deviceDesc.setText(context.getResources().getString(R.string.networking));
                deviceIcon.setImageResource(R.drawable.iconetworking);
                break;
            case BluetoothClass.Device.Major.PERIPHERAL:
                deviceDesc.setText(context.getResources().getString(R.string.peripheral));
                deviceIcon.setImageResource(R.drawable.icoperipheral);
                break;
            case BluetoothClass.Device.Major.MISC:
                deviceDesc.setText(context.getResources().getString(R.string.misc));
                deviceIcon.setImageResource(R.drawable.icomisc);
                break;
            case BluetoothClass.Device.Major.WEARABLE:
                deviceDesc.setText(context.getResources().getString(R.string.wearable));
                deviceIcon.setImageResource(R.drawable.icomisc);
                break;

            default:
                switch (deviceClass)
                {
                    case BluetoothClass.Device.PHONE_CELLULAR:
                        deviceDesc.setText(context.getResources().getString(R.string.phone_cellular));
                        deviceIcon.setImageResource(R.drawable.icophone);
                        break;
                    case BluetoothClass.Device.PHONE_CORDLESS:
                        deviceDesc.setText(context.getResources().getString(R.string.phone_cordless));
                        deviceIcon.setImageResource(R.drawable.icophone);
                        break;
                    case BluetoothClass.Device.PHONE_SMART:
                        deviceDesc.setText(context.getResources().getString(R.string.phone_smart));
                        deviceIcon.setImageResource(R.drawable.icosmart);
                        break;
                    case BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE:
                        deviceDesc.setText(context.getResources().getString(R.string.av_handsfree));
                        deviceIcon.setImageResource(R.drawable.icohandsfree);
                        break;
                    case BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET:
                        deviceDesc.setText(context.getResources().getString(R.string.av_headset));
                        deviceIcon.setImageResource(R.drawable.icoheadset);
                        break;
                    case BluetoothClass.Device.COMPUTER_LAPTOP:
                        deviceDesc.setText(context.getResources().getString(R.string.computer_laptop));
                        deviceIcon.setImageResource(R.drawable.icolaptop);
                        break;
                    case BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA:
                        deviceDesc.setText(context.getResources().getString(R.string.computer_palm_pda));
                        deviceIcon.setImageResource(R.drawable.icolaptop);
                        break;
                    case BluetoothClass.Device.COMPUTER_DESKTOP:
                        deviceDesc.setText(context.getResources().getString(R.string.computer_desktop));
                        deviceIcon.setImageResource(R.drawable.icocomputer);
                        break;
                    case BluetoothClass.Device.COMPUTER_SERVER:
                        deviceDesc.setText(context.getResources().getString(R.string.computer_server));
                        deviceIcon.setImageResource(R.drawable.icocomputer);
                        break;
                    case BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO:
                        deviceDesc.setText(context.getResources().getString(R.string.av_portable));
                        deviceIcon.setImageResource(R.drawable.icoportable);
                        break;
                    case BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER:
                        deviceDesc.setText(context.getResources().getString(R.string.av_loudspeaker));
                        deviceIcon.setImageResource(R.drawable.icoloudspeaker);
                        break;
                    case BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED:
                        deviceDesc.setText(context.getResources().getString(R.string.av_uncategorized));
                        deviceIcon.setImageResource(R.drawable.icomisc);
                        break;
                    case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER:
                        deviceDesc.setText(context.getResources().getString(R.string.av_display_loudspeaker));
                        deviceIcon.setImageResource(R.drawable.icoloudspeakerdisplay);
                        break;
                    default:
                        deviceDesc.setText(context.getResources().getString(R.string.unknown));
                        deviceIcon.setImageResource(R.drawable.icomisc);
                        break;
                }
                break;
        }

        return rowView;
    }
}
