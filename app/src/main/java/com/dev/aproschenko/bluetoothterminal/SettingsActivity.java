package com.dev.aproschenko.bluetoothterminal;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.util.Log;

import com.dev.aproschenko.bluetoothterminal.colorpicker.ColorPickerPreference;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener
{
    private static final String TAG = "SettingsActivity";
    private static final boolean D = true;

    SwitchPreference collectDevicesPreference;
    SwitchPreference showDatetimeLabelPreference;
    ColorPickerPreference bondedDeviceBgPreference;
    ColorPickerPreference sentMessagePreference;
    ColorPickerPreference receivedMessagePreference;
    ListPreference sentMessageEndingPreference;
    ListPreference receivedMessageEndingPreference;

    private MainApplication getApp() { return (MainApplication) getApplication(); }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (D) Log.d(TAG, "+++ ON CREATE +++");

        setTitle(R.string.settings);
        addPreferencesFromResource(R.xml.preferences);

        collectDevicesPreference = (SwitchPreference)findPreference("collect_devices");
        showDatetimeLabelPreference = (SwitchPreference)findPreference("show_datetime_label");
        bondedDeviceBgPreference = (ColorPickerPreference)findPreference("bonded_device_color");
        sentMessagePreference = (ColorPickerPreference)findPreference("terminal_self_message");
        receivedMessagePreference = (ColorPickerPreference)findPreference("terminal_their_message");
        sentMessageEndingPreference = (ListPreference)findPreference("terminal_self_ending");
        receivedMessageEndingPreference = (ListPreference)findPreference("terminal_their_ending");

        setPreferenceDefaults();
    }

    private void setPreferenceDefaults()
    {
        // Setup the initial values
        collectDevicesPreference.setChecked(getApp().collectDevicesStat);
        showDatetimeLabelPreference.setChecked(getApp().showDateTimeLabels);

        bondedDeviceBgPreference.setColor(getApp().bondedBgColor);
        bondedDeviceBgPreference.setOnPreferenceChangeListener(this);
        bondedDeviceBgPreference.setSummary(ColorPickerPreference.convertToARGB(getApp().bondedBgColor).toUpperCase());

        sentMessagePreference.setColor(getApp().sentMessageColor);
        sentMessagePreference.setOnPreferenceChangeListener(this);
        sentMessagePreference.setSummary(ColorPickerPreference.convertToRGB(getApp().sentMessageColor).toUpperCase());

        receivedMessagePreference.setColor(getApp().receivedMessageColor);
        receivedMessagePreference.setOnPreferenceChangeListener(this);
        receivedMessagePreference.setSummary(ColorPickerPreference.convertToRGB(getApp().receivedMessageColor).toUpperCase());

        sentMessageEndingPreference.setValueIndex(getApp().sentMessageEnding);
        sentMessageEndingPreference.setSummary(getCRLFDescription(getApp().sentMessageEnding));

        receivedMessageEndingPreference.setValueIndex(getApp().receivedMessageEnding);
        receivedMessageEndingPreference.setSummary(getCRLFDescription(getApp().receivedMessageEnding));

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    private String getCRLFDescription(int setting)
    {
        switch (setting)
        {
            case MainApplication.LINE_ENDING_CR:
                return getString(R.string.cr);
            case MainApplication.LINE_ENDING_LF:
                return getString(R.string.lf);
            case MainApplication.LINE_ENDING_CRLF:
                return getString(R.string.crlf);
            default:
                return getResources().getString(R.string.none);
        }
    }

    @Override
    public void onStart()
    {
        super.onStart();
        if (D) Log.d(TAG, "++ ON START ++");
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (D) Log.d(TAG, "+ ON RESUME +");
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if (D) Log.d(TAG, "- ON PAUSE -");
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (D) Log.d(TAG, "--- ON DESTROY ---");
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if (D) Log.d(TAG, "-- ON STOP --");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        if (D) Log.d(TAG, "onSharedPreferenceChanged " + key);

        if (key.equals("collect_devices"))
        {
            getApp().collectDevicesStat = collectDevicesPreference.isChecked();
        }

        if (key.equals("show_datetime_label"))
        {
            getApp().showDateTimeLabels = showDatetimeLabelPreference.isChecked();
        }

        if (key.equals("terminal_self_ending"))
        {
            String value = sentMessageEndingPreference.getValue();
            getApp().sentMessageEnding = Integer.parseInt(value);
            sentMessageEndingPreference.setSummary(getCRLFDescription(getApp().sentMessageEnding));
        }

        if (key.equals("terminal_their_ending"))
        {
            String value = receivedMessageEndingPreference.getValue();
            getApp().receivedMessageEnding = Integer.parseInt(value);
            receivedMessageEndingPreference.setSummary(getCRLFDescription(getApp().receivedMessageEnding));
        }

        getApp().saveSettings();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue)
    {
        String key = preference.getKey();
        if (D)
            Log.d(TAG, "onPreferenceChange " + key);

        if (key.equals("bonded_device_color"))
        {
            getApp().bondedBgColor = bondedDeviceBgPreference.getColor();
            String color = ColorPickerPreference.convertToARGB(getApp().bondedBgColor).toUpperCase();
            bondedDeviceBgPreference.setSummary(color);
            if (D)
                Log.d(TAG, "set " + key + " to " + color);
        }
        if (key.equals("terminal_self_message"))
        {
            getApp().sentMessageColor = sentMessagePreference.getColor();
            String color = ColorPickerPreference.convertToRGB(getApp().sentMessageColor).toUpperCase();
            sentMessagePreference.setSummary(color);
            if (D)
                Log.d(TAG, "set " + key + " to " + color);
        }
        if (key.equals("terminal_their_message"))
        {
            getApp().receivedMessageColor = receivedMessagePreference.getColor();
            String color = ColorPickerPreference.convertToRGB(getApp().receivedMessageColor).toUpperCase();
            receivedMessagePreference.setSummary(color);
            if (D)
                Log.d(TAG, "set " + key + " to " + color);
        }

        return false;
    }
}
