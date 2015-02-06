package com.dev.aproschenko.bluetoothterminal;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends Activity
{
    private static final String TAG = "MainActivity";
    private static final boolean D = true;

    final Context context = this;

    private ListView devicesView;
    private EditText searchBox;
    private Button btnSearchDevices;
    private boolean isReceiverRegistered = false;
    private ProgressDialog loadingDialog;
    private String searchFilter = "";
    private BroadcastReceiver broadcastReceiver;

    private IntentFilter actionFoundFilter;
    private IntentFilter actionServicesFoundFilter;
    private IntentFilter actionDiscoveryStartedFilter;
    private IntentFilter actionDiscoveryFinishedFilter;

    public static int REQUEST_ENABLE_BT = 1;
    public static String DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static String DEVICE_NAME = "DEVICE_NAME";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (D) Log.d(TAG, "+++ ON CREATE +++");

        setContentView(R.layout.activity_main);

        devicesView = (ListView)findViewById(R.id.devicesView);
        btnSearchDevices = (Button)findViewById(R.id.btnSearchDevices);

        ImageButton btnClearSearchBox = (ImageButton) findViewById(R.id.btnClearSearchBox);
        btnClearSearchBox.setOnClickListener(btnClearSearchBoxClick);

        ImageButton btnFilter = (ImageButton) findViewById(R.id.btnFilter);
        btnFilter.setOnClickListener(btnFilterClick);
        registerForContextMenu(btnFilter);

        searchBox = (EditText)findViewById(R.id.searchBox);
        searchBox.addTextChangedListener(searchBoxTextChangedListener);

        devicesView.setOnItemClickListener(devicesViewItemClick);
        registerForContextMenu(devicesView);

        btnSearchDevices.setEnabled(false);
        btnSearchDevices.setOnClickListener(btnSearchDevicesClick);

        createLoadingDialog();
        loadSettings();

        if (getApp().getAdapter() == null)
        {
            showAlert(getResources().getString(R.string.no_bt_support));
        }
    }

    private MainApplication getApp() { return (MainApplication) getApplication(); }

    private void loadSettings()
    {
        getApp().loadSettings();
    }

    private void createLoadingDialog()
    {
        loadingDialog = new ProgressDialog(context);
        loadingDialog.setMessage(getResources().getString(R.string.searching));
        loadingDialog.setCancelable(false);
        loadingDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.stop), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
                getApp().cancelDiscovery();
            }
        });
    }

    private void searchDevices()
    {
        getPairedDevices();

        // Create a BroadcastReceiver for ACTION_FOUND
        broadcastReceiver = new BroadcastReceiver()
        {
            public void onReceive(Context context, Intent intent)
            {
                String action = intent.getAction();
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action))
                {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                    if (device != null)
                    {
                        addBluetoothDevice(device, rssi);
                        fillDevicesView();
                    }
                }
                else if (BluetoothDevice.ACTION_UUID.equals(action))
                {
                    if (D) Log.d(TAG, "action uuid");
                }
                else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action))
                {
                    if (D) Log.d(TAG, "action discovery started");
                }
                else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
                {
                    loadingDialog.dismiss();
                    Toast.makeText(context, R.string.search_finished, Toast.LENGTH_SHORT).show();
                    fillDevicesView();
                }
            }
        };

        // Register the BroadcastReceiver
        actionFoundFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, actionFoundFilter);

        actionDiscoveryStartedFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(broadcastReceiver, actionDiscoveryStartedFilter);

        actionDiscoveryFinishedFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(broadcastReceiver, actionDiscoveryFinishedFilter);

        actionServicesFoundFilter = new IntentFilter(BluetoothDevice.ACTION_UUID);
        registerReceiver(broadcastReceiver, actionServicesFoundFilter);

        isReceiverRegistered = true;
    }

    private void getPairedDevices()
    {
        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = getApp().getBondedDevices();

        if (D)
            Log.d(TAG, "getPairedDevices: getBondedDevices returned " + pairedDevices.size() + " devices");

        for (BluetoothDevice device : pairedDevices)
        {
            addBluetoothDevice(device, -1);
        }

        fillDevicesView();
    }

    private boolean needAddToFiltered(DeviceData item)
    {
        if (item.getMajorDeviceClass() == BluetoothClass.Device.Major.AUDIO_VIDEO && getApp().showAudioVideo)
            return true;
        if (item.getMajorDeviceClass() == BluetoothClass.Device.Major.COMPUTER && getApp().showComputer)
            return true;
        if (item.getMajorDeviceClass() == BluetoothClass.Device.Major.HEALTH && getApp().showHealth)
            return true;
        if (item.getMajorDeviceClass() == BluetoothClass.Device.Major.IMAGING && getApp().showImaging)
            return true;
        if (item.getMajorDeviceClass() == BluetoothClass.Device.Major.MISC && getApp().showMisc)
            return true;
        if (item.getMajorDeviceClass() == BluetoothClass.Device.Major.NETWORKING && getApp().showNetworking)
            return true;
        if (item.getMajorDeviceClass() == BluetoothClass.Device.Major.PERIPHERAL && getApp().showPeripheral)
            return true;
        if (item.getMajorDeviceClass() == BluetoothClass.Device.Major.PHONE && getApp().showPhone)
            return true;
        if (item.getMajorDeviceClass() == BluetoothClass.Device.Major.TOY && getApp().showToy)
            return true;
        if (item.getMajorDeviceClass() == BluetoothClass.Device.Major.UNCATEGORIZED && getApp().showUncategorized)
            return true;
        if (item.getMajorDeviceClass() == BluetoothClass.Device.Major.WEARABLE && getApp().showWearable)
            return true;

        return false;
    }

    private void fillDevicesView()
    {
        ArrayList<DeviceData> filtered = new ArrayList<>();
        for (DeviceData item : getApp().getSettings().getDevices())
        {
            if (!needAddToFiltered(item))
                continue;

            String name = item.getName() + "";
            if (searchFilter == null || searchFilter.isEmpty() || name.toLowerCase().contains(searchFilter.toLowerCase()))
            {
                if (getApp().showNoServicesDevices)
                {
                    filtered.add(item);
                }
                else
                {
                    boolean hasServices = item.getUuids().size() > 0;
                    if (hasServices)
                    {
                        filtered.add(item);
                    }
                }
            }
        }

        DevicesRowAdapter adapter = new DevicesRowAdapter(this, filtered, getApp().getSettings().getSortType());
        devicesView.setAdapter(adapter);

        String title = String.format("%s, %d/%d", getResources().getString(R.string.app_name), filtered.size(), getApp().getSettings().getDevices().size());
        setTitle(title);

        getApp().saveSettings();
    }

    private void scanForDevices()
    {
        // If we're already discovering, stop it
        if (getApp().getAdapter().isDiscovering())
        {
            getApp().getAdapter().cancelDiscovery();
            loadingDialog.dismiss();
            return;
        }

        getApp().getAdapter().startDiscovery();
        loadingDialog.show();
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (D) Log.d(TAG, "onActivityResult " + resultCode);

        if (requestCode == REQUEST_ENABLE_BT)
        {
            if (resultCode == RESULT_OK)
            {
                btnSearchDevices.setEnabled(true);
                searchDevices();
            }
            else
            {
                Toast.makeText(context, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onStart()
    {
        super.onStart();
        if (D) Log.d(TAG, "++ ON START ++");

        if (!getApp().getAdapter().isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }

        btnSearchDevices.setEnabled(true);
        searchDevices();
    }

    @Override
    public synchronized void onResume()
    {
        super.onResume();
        if (D) Log.d(TAG, "+ ON RESUME +");
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (D) Log.d(TAG, "--- ON DESTROY ---");

        getApp().cancelDiscovery();
        unregisterBroadcastReceiver();
    }

    @Override
    public synchronized void onPause()
    {
        super.onPause();
        if (D) Log.d(TAG, "- ON PAUSE -");
        unregisterBroadcastReceiver();
        getApp().saveSettings();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if (D) Log.d(TAG, "-- ON STOP --");
        unregisterBroadcastReceiver();
    }

    private void unregisterBroadcastReceiver()
    {
        if (isReceiverRegistered)
        {
            unregisterReceiver(broadcastReceiver);
            isReceiverRegistered = false;
        }
    }

    private TextWatcher searchBoxTextChangedListener = new TextWatcher()
    {
        public void afterTextChanged(Editable s)
        {
            searchFilter = searchBox.getText().toString();
            fillDevicesView();
        }
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
    };

    private OnClickListener btnClearSearchBoxClick = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            searchFilter = "";
            searchBox.clearFocus();
            searchBox.setText("");

            InputMethodManager keyboard = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.hideSoftInputFromWindow(searchBox.getWindowToken(), 0);

            fillDevicesView();
        }
    };

    private OnClickListener btnFilterClick = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            openContextMenu(v);
        }
    };

    private OnClickListener btnSearchDevicesClick = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            scanForDevices();
        }
    };

    private void showAlert(String message)
    {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setTitle(message);

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (v.getId() == R.id.btnFilter)
        {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.filter_popup, menu);

            menu.findItem(R.id.menu_show_without_services).setChecked(getApp().showNoServicesDevices);
            menu.findItem(R.id.menu_filter_av).setChecked(getApp().showAudioVideo);
            menu.findItem(R.id.menu_filter_computer).setChecked(getApp().showComputer);
            menu.findItem(R.id.menu_filter_health).setChecked(getApp().showHealth);
            menu.findItem(R.id.menu_filter_imaging).setChecked(getApp().showImaging);
            menu.findItem(R.id.menu_filter_misc).setChecked(getApp().showMisc);
            menu.findItem(R.id.menu_filter_networking).setChecked(getApp().showNetworking);
            menu.findItem(R.id.menu_filter_peripheral).setChecked(getApp().showPeripheral);
            menu.findItem(R.id.menu_filter_phone).setChecked(getApp().showPhone);
            menu.findItem(R.id.menu_filter_toy).setChecked(getApp().showToy);
            menu.findItem(R.id.menu_filter_uncategorized).setChecked(getApp().showUncategorized);
            menu.findItem(R.id.menu_filter_wearable).setChecked(getApp().showWearable);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.menu_show_without_services)
        {
            getApp().showNoServicesDevices = !getApp().showNoServicesDevices;
            fillDevicesView();
            return true;
        }
        if (item.getItemId() == R.id.menu_filter_av)
        {
            getApp().showAudioVideo = !getApp().showAudioVideo;
            fillDevicesView();
            return true;
        }
        if (item.getItemId() == R.id.menu_filter_computer)
        {
            getApp().showComputer = !getApp().showComputer;
            fillDevicesView();
            return true;
        }
        if (item.getItemId() == R.id.menu_filter_health)
        {
            getApp().showHealth = !getApp().showHealth;
            fillDevicesView();
            return true;
        }
        if (item.getItemId() == R.id.menu_filter_imaging)
        {
            getApp().showImaging = !getApp().showImaging;
            fillDevicesView();
            return true;
        }
        if (item.getItemId() == R.id.menu_filter_misc)
        {
            getApp().showMisc = !getApp().showMisc;
            fillDevicesView();
            return true;
        }
        if (item.getItemId() == R.id.menu_filter_networking)
        {
            getApp().showNetworking = !getApp().showNetworking;
            fillDevicesView();
            return true;
        }
        if (item.getItemId() == R.id.menu_filter_peripheral)
        {
            getApp().showPeripheral = !getApp().showPeripheral;
            fillDevicesView();
            return true;
        }
        if (item.getItemId() == R.id.menu_filter_phone)
        {
            getApp().showPhone = !getApp().showPhone;
            fillDevicesView();
            return true;
        }
        if (item.getItemId() == R.id.menu_filter_toy)
        {
            getApp().showToy = !getApp().showToy;
            fillDevicesView();
            return true;
        }
        if (item.getItemId() == R.id.menu_filter_uncategorized)
        {
            getApp().showUncategorized = !getApp().showUncategorized;
            fillDevicesView();
            return true;
        }
        if (item.getItemId() == R.id.menu_filter_wearable)
        {
            getApp().showWearable = !getApp().showWearable;
            fillDevicesView();
            return true;
        }

        return super.onContextItemSelected(item);
    }

    private void connectToDevice(DeviceData itemData)
    {
        // If we're already discovering, stop it
        if (getApp().getAdapter().isDiscovering())
        {
            getApp().getAdapter().cancelDiscovery();
            loadingDialog.dismiss();
        }

        Intent intent = new Intent(context, TerminalActivity.class);
        intent.putExtra(DEVICE_NAME, itemData.getName());
        intent.putExtra(DEVICE_ADDRESS, itemData.getAddress());
        startActivity(intent);
    }

    private OnItemClickListener devicesViewItemClick = new OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)
        {
            ListAdapter adapter = devicesView.getAdapter();
            DeviceData data = (DeviceData) adapter.getItem(position);
            connectToDevice(data);
        }
    };

    @Override
    public boolean onPrepareOptionsMenu (Menu menu)
    {
        menu.findItem(R.id.menu_enable_bt).setEnabled(!getApp().getAdapter().isEnabled());

        if (getApp().getSettings().getSortType() == MainApplication.SortType.SORT_BY_NAME)
            menu.findItem(R.id.menu_sort_by_name).setChecked(true);
        if (getApp().getSettings().getSortType() == MainApplication.SortType.SORT_BY_TYPE)
            menu.findItem(R.id.menu_sort_by_type).setChecked(true);
        if (getApp().getSettings().getSortType() == MainApplication.SortType.SORT_BY_BONDED_STATE)
            menu.findItem(R.id.menu_sort_by_bonded_state).setChecked(true);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.menu_sort_by_name:
                getApp().getSettings().setSortType(MainApplication.SortType.SORT_BY_NAME);
                fillDevicesView();
                return true;

            case R.id.menu_sort_by_type:
                getApp().getSettings().setSortType(MainApplication.SortType.SORT_BY_TYPE);
                fillDevicesView();
                return true;

            case R.id.menu_sort_by_bonded_state:
                getApp().getSettings().setSortType(MainApplication.SortType.SORT_BY_BONDED_STATE);
                fillDevicesView();
                return true;

            case R.id.menu_enable_bt:

                if (!getApp().getAdapter().isEnabled())
                {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }

                return true;

            case R.id.menu_settings:
                showSettings();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showSettings()
    {
        Intent intent = new Intent(context, SettingsActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    private void addBluetoothDevice(BluetoothDevice device, int rssi)
    {
        String deviceAddress = null;

        try
        {
            deviceAddress = device.getAddress();
        }
        catch (Exception e)
        {
            if (D) Log.e(TAG, "addBluetoothDevice() failed", e);
            Toast.makeText(context, String.format(getResources().getString(R.string.error_adding_device), e.getMessage()), Toast.LENGTH_SHORT).show();
        }

        if (deviceAddress == null || deviceAddress.isEmpty())
            return;

        String emptyName = getResources().getString(R.string.empty_device_name);
        for (DeviceData item : getApp().getSettings().getDevices())
        {
            String address = item.getAddress() + "";
            if (address.equals(deviceAddress))
            {
                item.updateData(device, rssi, emptyName);
                return;
            }
        }

        DeviceData dataToAdd = new DeviceData(device, rssi, emptyName);
        getApp().getSettings().getDevices().add(dataToAdd);

        if (getApp().collectDevicesStat)
            getApp().serializeDevices();
    }

    @Override
    public boolean onSearchRequested()
    {
        searchBox.requestFocus();
        searchBox.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                InputMethodManager keyboard = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.showSoftInput(searchBox, 0);
            }
        }, 200);

        return false;
    }
}
