package com.dev.aproschenko.bluetoothterminal;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Set;

public class MainApplication extends Application
{
    private static final String TAG = "MainApplication";
    private static final boolean D = true;

    public static final int bondedBgColorDefault = 0x2200FF00;
    public static final int sentMessageColorDefault = 0xFF6F00FF;
    public static final int receivedMessageColorDefault = 0xFF0062FF;

    public static final int LINE_ENDING_NONE = 0;
    public static final int LINE_ENDING_CR = 1;
    public static final int LINE_ENDING_LF = 2;
    public static final int LINE_ENDING_CRLF = 3;

    private BluetoothAdapter btAdapter;
    private DeviceConnector connector;

    private SettingsData settings;

    public static final String PREFS_FOLDER_NAME = "com.dev.aproschenko.arduinocontroller";
    public static final String PREFS_DEVICES_FILE = "devices.txt";

    public static final String PREFS_KEY_TERMINAL_COMMAND = "terminalcommand";
    public static final String PREFS_KEY_SORTTYPE = "sorttype";
    public static final String PREFS_KEY_COLLECT_DEVICES = "collectdevices";
    public static final String PREFS_KEY_SHOW_NO_SERVICES = "noservices";
    public static final String PREFS_KEY_SHOW_AUDIOVIDEO = "audiovideo";
    public static final String PREFS_KEY_SHOW_COMPUTER = "computer";
    public static final String PREFS_KEY_SHOW_HEALTH = "health";
    public static final String PREFS_KEY_SHOW_MISC = "misc";
    public static final String PREFS_KEY_SHOW_IMAGING = "imaging";
    public static final String PREFS_KEY_SHOW_NETWORKING = "networking";
    public static final String PREFS_KEY_SHOW_PERIPHERAL = "peripheral";
    public static final String PREFS_KEY_SHOW_PHONE = "phone";
    public static final String PREFS_KEY_SHOW_TOY = "toy";
    public static final String PREFS_KEY_SHOW_WEARABLE = "wearable";
    public static final String PREFS_KEY_SHOW_UNCATEGORIZED = "uncategorized";
    public static final String PREFS_KEY_SHOW_DATETIME_LABELS = "showdatetime";
    public static final String PREFS_KEY_BONDED_COLOR = "bondedcolor";
    public static final String PREFS_KEY_SENT_COLOR = "sentcolor";
    public static final String PREFS_KEY_RECEIVED_COLOR = "receivedcolor";
    public static final String PREFS_KEY_SENT_ENDING = "sentending";
    public static final String PREFS_KEY_RECEIVED_ENDING = "receivedending";

    public boolean showNoServicesDevices = true;
    public boolean showAudioVideo = true;
    public boolean showComputer = true;
    public boolean showHealth = true;
    public boolean showMisc = true;
    public boolean showImaging = true;
    public boolean showNetworking = true;
    public boolean showPeripheral = true;
    public boolean showPhone = true;
    public boolean showToy = true;
    public boolean showWearable = true;
    public boolean showUncategorized = true;

    public boolean collectDevicesStat = false;
    public boolean showDateTimeLabels = false;

    public int bondedBgColor = bondedBgColorDefault;
    public int sentMessageColor = sentMessageColorDefault;
    public int receivedMessageColor = receivedMessageColorDefault;
    public int sentMessageEnding = LINE_ENDING_NONE;
    public int receivedMessageEnding = LINE_ENDING_NONE;

    private ArrayList<String> terminalCommands = new ArrayList<>();
    ArrayList<MacData> macs = new ArrayList<>();

    public enum SortType
    {
        SORT_BY_NAME,
        SORT_BY_TYPE,
        SORT_BY_BONDED_STATE
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        if (D) Log.d(TAG, "onCreate");

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        loadSettings();
    }

    public BluetoothAdapter getAdapter() { return btAdapter; }
    public DeviceConnector getConnector() { return connector; }
    public SettingsData getSettings() { return settings; }
    public ArrayList<String> getTerminalCommands() { return terminalCommands; }
    Set<BluetoothDevice> getBondedDevices() { return btAdapter.getBondedDevices(); }
    public ArrayList<MacData> getMACs() { return macs; }

    public void cancelDiscovery()
    {
        if (btAdapter != null && btAdapter.isDiscovering())
        {
            btAdapter.cancelDiscovery();
        }
    }

    public void createConnector(String address)
    {
        connector = new DeviceConnector(address);
    }

    public void deleteConnector()
    {
        connector = null;
    }

    public int getConnectorState()
    {
        int state = DeviceConnector.STATE_NONE;
        if (connector != null)
        {
            state = connector.getState();
        }
        return state;
    }

    public void disconnect()
    {
        if (connector != null)
        {
            connector.stop();
        }
    }

    public void addHandler(Handler handler)
    {
        if (connector != null)
        {
            connector.getHandlers().add(handler);
        }
    }

    public void removeHandler(Handler handler)
    {
        if (connector != null)
        {
            connector.getHandlers().remove(handler);
        }
    }

    public void createSettings()
    {
        settings = new SettingsData();
    }

    public void loadSettings()
    {
        if (D) Log.d(TAG, "loadSettings");

        createSettings();
        restoreSettings();

        if (collectDevicesStat)
            deserializeDevices();
    }

    private String getPrefsFileName(boolean createFolder, String fileToCreateName)
    {
        String androidFolder = Environment.getExternalStorageDirectory().getPath() + "/Android";
        String dataFolder = androidFolder + "/data";
        String prefsFolder = dataFolder + "/" + PREFS_FOLDER_NAME;
        String fileName = prefsFolder + "/" + fileToCreateName;

        if (!createFolder)
            return fileName;

        boolean success;
        File f = new File(androidFolder);
        if (!f.exists() || !f.isDirectory())
        {
            success = f.mkdir();
        }
        else
            success = true;

        if (!success)
            return null;

        f = new File(dataFolder);
        if (!f.exists() || !f.isDirectory())
        {
            success = f.mkdir();
        }

        if (!success)
            return null;

        f = new File(prefsFolder);
        if (!f.exists() || !f.isDirectory())
        {
            success = f.mkdir();
        }

        if (!success)
            return null;

        return fileName;
    }

    private void restoreSettings()
    {
        SharedPreferences settings = getSharedPreferences(PREFS_FOLDER_NAME, 0);

        String defaultCmd = TerminalActivity.NOT_SET_TEXT;
        for (int i = 0; i < TerminalActivity.BTN_COUNT; i++)
        {
            String key = PREFS_KEY_TERMINAL_COMMAND + i;
            String cmd = settings.getString(key, "");
            if (cmd.isEmpty())
                cmd = defaultCmd;

            if (D)
                Log.d(TAG, "restored terminal key " + key + ":" + cmd);

            terminalCommands.add(cmd);
        }

        String sortValue = settings.getString(PREFS_KEY_SORTTYPE, "SORT_BY_NAME");

        switch (sortValue)
        {
            case "SORT_BY_TYPE":
                this.settings.setSortType(SortType.SORT_BY_TYPE);
                break;
            case "SORT_BY_BONDED_STATE":
                this.settings.setSortType(SortType.SORT_BY_BONDED_STATE);
                break;
            default:
                this.settings.setSortType(SortType.SORT_BY_NAME);
                break;
        }

        showNoServicesDevices = settings.getBoolean(PREFS_KEY_SHOW_NO_SERVICES, true);
        showAudioVideo = settings.getBoolean(PREFS_KEY_SHOW_AUDIOVIDEO, true);
        showComputer = settings.getBoolean(PREFS_KEY_SHOW_COMPUTER, true);
        showHealth = settings.getBoolean(PREFS_KEY_SHOW_HEALTH, true);
        showMisc = settings.getBoolean(PREFS_KEY_SHOW_MISC, true);
        showImaging = settings.getBoolean(PREFS_KEY_SHOW_IMAGING, true);
        showNetworking = settings.getBoolean(PREFS_KEY_SHOW_NETWORKING, true);
        showPeripheral = settings.getBoolean(PREFS_KEY_SHOW_PERIPHERAL, true);
        showPhone = settings.getBoolean(PREFS_KEY_SHOW_PHONE, true);
        showToy = settings.getBoolean(PREFS_KEY_SHOW_TOY, true);
        showWearable = settings.getBoolean(PREFS_KEY_SHOW_WEARABLE, true);
        showUncategorized = settings.getBoolean(PREFS_KEY_SHOW_UNCATEGORIZED, true);

        collectDevicesStat = settings.getBoolean(PREFS_KEY_COLLECT_DEVICES, false);
        showDateTimeLabels = settings.getBoolean(PREFS_KEY_SHOW_DATETIME_LABELS, false);

        bondedBgColor = settings.getInt(PREFS_KEY_BONDED_COLOR, bondedBgColorDefault);
        sentMessageColor = settings.getInt(PREFS_KEY_SENT_COLOR, sentMessageColorDefault);
        receivedMessageColor = settings.getInt(PREFS_KEY_RECEIVED_COLOR, receivedMessageColorDefault);
        sentMessageEnding = settings.getInt(PREFS_KEY_SENT_ENDING, LINE_ENDING_NONE);
        receivedMessageEnding = settings.getInt(PREFS_KEY_RECEIVED_ENDING, LINE_ENDING_NONE);
    }

    public DeviceData getDeviceDataByAddress(String address)
    {
        for (DeviceData data : settings.getDevices())
        {
            if (data.getAddress().equals(address))
                return data;
        }

        return null;
    }

    public void saveSettings()
    {
        saveSettingsInternal();
    }

    private void saveSettingsInternal()
    {
        if (D) Log.d(TAG, "saveSettingsInternal");

        SharedPreferences settings = getSharedPreferences(PREFS_FOLDER_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        String sortValue = "SORT_BY_NAME";
        switch (this.settings.getSortType())
        {
            case SORT_BY_TYPE:
                sortValue = "SORT_BY_TYPE";
                break;
            case SORT_BY_BONDED_STATE:
                sortValue = "SORT_BY_BONDED_STATE";
                break;
            default: //name
                break;
        }
        editor.putString(PREFS_KEY_SORTTYPE, sortValue);

        editor.putBoolean(PREFS_KEY_SHOW_NO_SERVICES, showNoServicesDevices);
        editor.putBoolean(PREFS_KEY_SHOW_AUDIOVIDEO, showAudioVideo);
        editor.putBoolean(PREFS_KEY_SHOW_COMPUTER, showComputer);
        editor.putBoolean(PREFS_KEY_SHOW_HEALTH, showHealth);
        editor.putBoolean(PREFS_KEY_SHOW_MISC, showMisc);
        editor.putBoolean(PREFS_KEY_SHOW_IMAGING, showImaging);
        editor.putBoolean(PREFS_KEY_SHOW_NETWORKING, showNetworking);
        editor.putBoolean(PREFS_KEY_SHOW_PERIPHERAL, showPeripheral);
        editor.putBoolean(PREFS_KEY_SHOW_PHONE, showPhone);
        editor.putBoolean(PREFS_KEY_SHOW_TOY, showToy);
        editor.putBoolean(PREFS_KEY_SHOW_WEARABLE, showWearable);
        editor.putBoolean(PREFS_KEY_SHOW_UNCATEGORIZED, showUncategorized);

        editor.putBoolean(PREFS_KEY_COLLECT_DEVICES, collectDevicesStat);
        editor.putBoolean(PREFS_KEY_SHOW_DATETIME_LABELS, showDateTimeLabels);

        editor.putInt(PREFS_KEY_BONDED_COLOR, bondedBgColor);
        editor.putInt(PREFS_KEY_SENT_COLOR, sentMessageColor);
        editor.putInt(PREFS_KEY_RECEIVED_COLOR, receivedMessageColor);
        editor.putInt(PREFS_KEY_SENT_ENDING, sentMessageEnding);
        editor.putInt(PREFS_KEY_RECEIVED_ENDING, receivedMessageEnding);

        editor.commit();
    }

    public String readFile(String fileName)
    {
        String fileNamePath = getPrefsFileName(false, fileName);
        String jsonData = "";

        try
        {
            File myFile = new File(fileNamePath);
            if (!myFile.exists())
            {
                if (D) Log.e(TAG, "readFile(): file " + fileNamePath + " not found.");
                return "";
            }

            if (D)
                Log.d(TAG, "readFile(): try load from " + fileNamePath);

            FileInputStream fIn = new FileInputStream(myFile);
            BufferedReader myReader = new BufferedReader(new InputStreamReader(fIn));
            String aDataRow;
            String aBuffer = "";
            while ((aDataRow = myReader.readLine()) != null)
            {
                aBuffer += aDataRow + "\n";
            }
            jsonData = aBuffer;
            myReader.close();

            if (D)
                Log.d(TAG, "readFile(): loaded from " + fileNamePath);
        }
        catch (Exception e)
        {
            if (D) Log.e(TAG, "readFile() failed", e);
        }

        return jsonData;
    }

    private void deserializeDevices()
    {
        String jsonData = readFile(PREFS_DEVICES_FILE);

        String emptyName = getResources().getString(R.string.empty_device_name);
        SettingsData tmp = DeviceSerializer.deserialize(jsonData);

        SortType st = SortType.SORT_BY_NAME;
        if (settings != null)
        {
            st = settings.getSortType();
        }

        if (tmp != null)
        {
            settings = tmp;
            settings.setSortType(st);

            for (DeviceData item : settings.getDevices())
            {
                String deviceName = item.getName();
                if (deviceName == null || deviceName.isEmpty())
                    item.setName(emptyName);
            }
        }
        else
        {
            createSettings();
            settings.setSortType(st);
        }
    }

    public void serializeDevices()
    {
        serializeDevicesInternal();
    }

    private void serializeDevicesInternal()
    {
        String jsonData = DeviceSerializer.serialize(settings);
        writeFile(jsonData, PREFS_DEVICES_FILE);
    }

    public void writeFile(String jsonData, String fileName)
    {
        String fileNamePath = getPrefsFileName(true, fileName);

        if (fileNamePath == null || fileNamePath.isEmpty())
        {
            if (D)
                Log.d(TAG, "writeFile(): unable to prepare prefs folder.");
            return;
        }

        if (D)
            Log.d(TAG, "writeFile(): try save to " + fileNamePath);

        try
        {
            File myFile = new File(fileNamePath);

            FileWriter filewriter = new FileWriter(myFile);
            BufferedWriter out = new BufferedWriter(filewriter);

            out.write(jsonData);

            out.close();
        }
        catch (Exception e)
        {
            if (D) Log.e(TAG, "writeFile() failed", e);
        }

        if (D)
            Log.d(TAG, "writeFile(): save to " + fileNamePath + " successful");
    }
}
