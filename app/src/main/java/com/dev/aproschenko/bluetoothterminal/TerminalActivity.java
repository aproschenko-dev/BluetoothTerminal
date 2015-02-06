package com.dev.aproschenko.bluetoothterminal;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.ClipboardManager;
import android.text.Html;
import android.text.format.DateFormat;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dev.aproschenko.bluetoothterminal.colorpicker.ColorPickerPreference;

import java.util.ArrayList;
import java.util.Date;

public class TerminalActivity extends Activity
{
    private static final String TAG = "TerminalActivity";
    private static final boolean D = true;
    public static final String NOT_SET_TEXT = "-";

    private String connectedDeviceName;

    private Button buttonSend;
    private EditText commandBox;
    private TextView commandsView;

    private String commandsCache = "";
    private Integer buttonIds[] = {R.id.button1, R.id.button2, R.id.button3, R.id.button4, R.id.button5};
    private ArrayList<Button> terminalButtons = new ArrayList<>();

    public static final int BTN_COUNT = 5;
    final Context context = this;

    public MainApplication getApp()
    {
        return (MainApplication) getApplication();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (D) Log.d(TAG, "+++ ON CREATE +++");

        Intent intent = getIntent();
        connectedDeviceName = intent.getStringExtra(MainActivity.DEVICE_NAME);

        setContentView(R.layout.terminal_layout);
        setTitle(R.string.bluetooth_terminal);

        buttonSend = (Button) findViewById(R.id.buttonSend);
        buttonSend.setOnClickListener(buttonSendClick);

        commandBox = (EditText) findViewById(R.id.commandBox);
        commandsView = (TextView) findViewById(R.id.commandsView);

        commandsView.setMovementMethod(new ScrollingMovementMethod());
        commandsView.setTextIsSelectable(true);

        for (int i = 0; i < buttonIds.length; i++)
        {
            int id = buttonIds[i];
            Button btn = (Button) findViewById(id);

            btn.setText(getApp().getTerminalCommands().get(i));
            btn.setOnLongClickListener(btnPredefinedCommandLongControlClick);
            btn.setOnClickListener(btnPredefinedCommandControlClick);
            btn.setTag(i);

            terminalButtons.add(btn);
        }

        getApp().addHandler(mHandler);
    }

    public void updateButtonCommand(int buttonIndex, String command)
    {
        Button btn = terminalButtons.get(buttonIndex);
        btn.setText(command);

        getApp().getTerminalCommands().set(buttonIndex, command);
        savePreferences();
    }

    private void savePreferences()
    {
        SharedPreferences settings = getSharedPreferences(MainApplication.PREFS_FOLDER_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        for (int i = 0; i < BTN_COUNT; i++)
        {
            String key = MainApplication.PREFS_KEY_TERMINAL_COMMAND + i;
            String cmd = getApp().getTerminalCommands().get(i);
            editor.putString(key, cmd);

            if (D)
                Log.d(TAG, "save terminal key " + key + ":" + cmd);
        }

        editor.commit();
    }

    private View.OnLongClickListener btnPredefinedCommandLongControlClick = new View.OnLongClickListener()
    {
        @Override
        public boolean onLongClick(View v)
        {
            Button btn = (Button)v;
            showButtonActionDialog(btn);
            return true;
        }
    };

    private void showButtonActionDialog(Button btn)
    {
        int buttonIndex = (int)btn.getTag();
        ButtonSetupDialog newFragment = ButtonSetupDialog.newInstance(buttonIndex);
        newFragment.show(getFragmentManager(), "ButtonSetupDialog");
    }

    private View.OnClickListener btnPredefinedCommandControlClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            Button btn = (Button)v;
            String command = btn.getText().toString();

            if (command.equals(TerminalActivity.NOT_SET_TEXT))
                Toast.makeText(context, getResources().getString(R.string.set_command_using_long_tap), Toast.LENGTH_SHORT).show();
            else
                sendCommand(command);
        }
    };

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (D) Log.d(TAG, "--- ON DESTROY ---");
        getApp().removeHandler(mHandler);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        if (D) Log.d(TAG, "onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_terminal, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.menu_close_terminal:
                closeTerminal();
                return true;
            case R.id.menu_copy_terminal:
                copyLog();
                return true;
            case R.id.menu_clear_terminal:
                clearLog();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void clearLog()
    {
        commandsView.setText("");
        commandsCache = "";
        Toast.makeText(this, getResources().getString(R.string.terminal_cleared), Toast.LENGTH_SHORT).show();
    }

    private void copyLog()
    {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setText(commandsView.getText());
        Toast.makeText(this, getResources().getString(R.string.terminal_copied), Toast.LENGTH_SHORT).show();
    }

    private void closeTerminal()
    {
        finish();
    }

    private View.OnClickListener buttonSendClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            String command = commandBox.getText().toString().trim();
            sendCommand(command);
        }
    };

    private String getFormattedDateTime()
    {
        Date date = new Date();
        return String.format("%s %s", DateFormat.getDateFormat(getApplicationContext()).format(date), DateFormat.format("H:mm:ss", date));
    }

    private void appendCommand(String command, int messageType)
    {
        String postfix = "";
        if (command.endsWith("\r\n"))
            postfix = " CR+LF";
        else if (command.endsWith("\r"))
            postfix = " CR";
        else if (command.endsWith("\n"))
            postfix = " LF";

        int commandIntColor = 0;
        String trimmedCommand = command.trim();
        if (trimmedCommand.equals("OK"))
            commandIntColor = Color.GREEN;
        if (trimmedCommand.equals("ERROR"))
            commandIntColor = Color.RED;
        String commandColor = "";
        if (commandIntColor != 0)
            commandColor = ColorPickerPreference.convertToRGB(commandIntColor);

        int intColor = messageType == Messages.MESSAGE_READ ? getApp().receivedMessageColor : getApp().sentMessageColor;
        String color = ColorPickerPreference.convertToRGB(intColor);
        int postfixIntColor = Color.CYAN;
        String postfixColor = ColorPickerPreference.convertToRGB(postfixIntColor);
        String author = messageType == Messages.MESSAGE_READ ? connectedDeviceName : "ME";
        String date = getFormattedDateTime();

        String coloredCommand = command;
        if (!commandColor.equals(""))
            coloredCommand = String.format("<font color='%s'>%s</font>", commandColor, command);
        String textToAdd = String.format("<font color='%s'>%s&gt; </font>%s<font color='%s'>%s</font><br/>", color, author, coloredCommand, postfixColor, postfix);
        if (getApp().showDateTimeLabels)
            textToAdd = String.format("%s %s", date, textToAdd);

        commandsCache = textToAdd + commandsCache;
        commandsView.setText(Html.fromHtml(commandsCache), TextView.BufferType.SPANNABLE);
    }

    private void sendCommand(String command)
    {
        if (getApp().getConnectorState() == DeviceConnector.STATE_CONNECTED)
        {
            if (!command.equals(""))
            {
                switch (getApp().sentMessageEnding)
                {
                    case MainApplication.LINE_ENDING_CR:
                        command = command.concat("\r");
                        break;
                    case MainApplication.LINE_ENDING_LF:
                        command = command.concat("\n");
                        break;
                    case MainApplication.LINE_ENDING_CRLF:
                        command = command.concat("\r\n");
                        break;
                    default: //NONE
                        break;
                }
                getApp().getConnector().write(command);
                appendCommand(command, Messages.MESSAGE_WRITE);
                commandBox.setText("");
            } else
            {
                if (commandBox.requestFocus())
                {
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        }
    }

    private final Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case Messages.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    appendCommand(readMessage, Messages.MESSAGE_READ);
                    break;

                case Messages.MESSAGE_CONNECTION_LOST:
                    buttonSend.setEnabled(false);
                    commandBox.setEnabled(false);
                    appendCommand(String.format(getResources().getString(R.string.connection_was_lost), connectedDeviceName), Messages.MESSAGE_READ);
                    break;
            }
        }
    };
}
