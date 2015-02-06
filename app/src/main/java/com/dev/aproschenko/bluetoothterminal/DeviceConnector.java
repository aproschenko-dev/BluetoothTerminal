package com.dev.aproschenko.bluetoothterminal;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class DeviceConnector
{
    private static final String TAG = "DeviceConnector";
    private static final boolean D = true;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0; // we're doing nothing
    public static final int STATE_CONNECTING = 1; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 2; // now connected to a remote device

    private int mState;

    private BluetoothAdapter btAdapter;
    private BluetoothDevice connectedDevice;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private ArrayList<Handler> handlers = new ArrayList<>();

    public DeviceConnector(String address)
    {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        connectedDevice = btAdapter.getRemoteDevice(address);
        mState = STATE_NONE;
    }

    public ArrayList<Handler> getHandlers()
    {
        return handlers;
    }

    private void sendMessage(int what, int arg1, int arg2, Object obj)
    {
        for (Handler handler : handlers)
        {
            handler.obtainMessage(what, arg1, arg2, obj).sendToTarget();
        }
    }

    public synchronized void connect()
    {
        if (D)
            Log.d(TAG, "connect to: " + connectedDevice);

        if (mState == STATE_CONNECTING)
        {
            if (mConnectThread != null)
            {
                if (D)
                    Log.d(TAG, "cancel mConnectThread");
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        if (mConnectedThread != null)
        {
            if (D)
                Log.d(TAG, "cancel mConnectedThread");
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(connectedDevice);
        mConnectThread.start();

        setState(STATE_CONNECTING);
    }

    public synchronized void stop()
    {
        if (D)
            Log.d(TAG, "stop");

        if (mConnectThread != null)
        {
            if (D)
                Log.d(TAG, "cancel mConnectThread");
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null)
        {
            if (D)
                Log.d(TAG, "cancel mConnectedThread");
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_NONE);
    }

    private synchronized void setState(int state)
    {
        if (D)
            Log.d(TAG, "setState() " + mState + " -> " + state);

        mState = state;
        sendMessage(Messages.MESSAGE_STATE_CHANGE, state, -1, null);
    }

    public synchronized int getState()
    {
        return mState;
    }

    public synchronized void connected(BluetoothSocket socket)
    {
        if (D)
            Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null)
        {
            if (D)
                Log.d(TAG, "cancel mConnectThread");
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null)
        {
            if (D)
                Log.d(TAG, "cancel mConnectedThread");
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Send the name of the connected device back to the UI Activity
        sendMessage(Messages.MESSAGE_DEVICE_NAME, 0, 0, null);

        setState(STATE_CONNECTED);

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
    }

    public void write(String data)
    {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this)
        {
            if (mState != STATE_CONNECTED)
                return;
            r = mConnectedThread;
        }

        // Perform the write unsynchronized
        r.write(data);
    }

    private void connectionFailed()
    {
        if (D)
            Log.d(TAG, "connectionFailed");

        // Send a failure message back to the Activity
        /*Message msg = mHandler.obtainMessage(Messages.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(DeviceControlActivity.TOAST, String.format(mContext.getResources().getString(R.string.unable_connect_to), mDeviceData.getName()));
        msg.setData(bundle);
        mHandler.sendMessage(msg);*/
        sendMessage(Messages.MESSAGE_CONNECTION_FAILED, 0, 0, null);

        setState(STATE_NONE);
    }

    private void connectionLost()
    {
        // Send a failure message back to the Activity
        /*Message msg = mHandler.obtainMessage(Messages.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(DeviceControlActivity.TOAST, String.format(mContext.getResources().getString(R.string.connection_was_lost), mDeviceData.getName()));
        msg.setData(bundle);
        mHandler.sendMessage(msg);*/

        sendMessage(Messages.MESSAGE_CONNECTION_LOST, 0, 0, null);
        setState(STATE_NONE);
    }

    private class ConnectThread extends Thread
    {
        private static final String TAG = "ConnectThread";
        private static final boolean D = true;

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device)
        {
            if (D)
                Log.d(TAG, "create ConnectThread");
            mmDevice = device;
            mmSocket = BluetoothUtils.createRfcommSocket(mmDevice);
        }

        public void run()
        {
            if (D)
                Log.d(TAG, "ConnectThread run");

            // Always cancel discovery because it will slow down a connection
            btAdapter.cancelDiscovery();

            if (mmSocket == null)
            {
                if (D)
                    Log.d(TAG,
                            "unable to connect to device, socket isn't created");
                connectionFailed();
                return;
            }

            // Make a connection to the BluetoothSocket
            try
            {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            }
            catch (IOException e)
            {
                // Close the socket
                try
                {
                    mmSocket.close();
                }
                catch (IOException e2)
                {
                    if (D)
                        Log.e(TAG,
                                "unable to close() socket during connection failure",
                                e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (DeviceConnector.this)
            {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket);
        }

        public void cancel()
        {
            if (D)
                Log.d(TAG, "ConnectThread cancel");

            if (mmSocket == null)
            {
                if (D)
                    Log.d(TAG, "unable to close null socket");
                return;
            }

            try
            {
                mmSocket.close();
            }
            catch (IOException e)
            {
                if (D)
                    Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread
    {
        private static final String TAG = "ConnectedThread";
        private static final boolean D = true;

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            if (D)
                Log.d(TAG, "create ConnectedThread");

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e)
            {
                if (D)
                    Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run()
        {
            if (D)
                Log.i(TAG, "ConnectedThread run");

            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true)
            {
                try
                {
                    bytes = mmInStream.read(buffer);
                    sendMessage(Messages.MESSAGE_READ, bytes, -1, buffer);
                }
                catch (IOException e)
                {
                    if (D)
                        Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        public void write(String data)
        {
            byte[] buffer = data.getBytes();

            try
            {
                mmOutStream.write(buffer);
                sendMessage(Messages.MESSAGE_WRITE, -1, -1, buffer);
            }
            catch (IOException e)
            {
                if (D)
                    Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel()
        {
            try
            {
                mmSocket.close();
            }
            catch (IOException e)
            {
                if (D)
                    Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
