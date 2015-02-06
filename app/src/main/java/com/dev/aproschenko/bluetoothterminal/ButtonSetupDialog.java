package com.dev.aproschenko.bluetoothterminal;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

public class ButtonSetupDialog extends DialogFragment
{
    public static final String PARAM_BUTTON_INDEX = "index";

    public static ButtonSetupDialog newInstance(int buttonIndex)
    {
        ButtonSetupDialog frag = new ButtonSetupDialog();

        Bundle args = new Bundle();
        args.putInt(PARAM_BUTTON_INDEX, buttonIndex);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        final int buttonIndex = getArguments().getInt(PARAM_BUTTON_INDEX);

        LayoutInflater factory = LayoutInflater.from(getActivity());
        final View textEntryView = factory.inflate(R.layout.button_setup_dialog_terminal, null);

        final EditText editor = (EditText)textEntryView.findViewById(R.id.button_message_edit);

        TerminalActivity activity = (TerminalActivity) getActivity();
        String command = activity.getApp().getTerminalCommands().get(buttonIndex);

        if (command.trim().toLowerCase().equals(TerminalActivity.NOT_SET_TEXT.toLowerCase()))
            editor.setText("");
        else
            editor.setText(command.trim());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.button_message);
        builder.setView(textEntryView);
        builder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                String commandToSet = editor.getText().toString().trim();
                if (commandToSet.equals(""))
                    commandToSet = TerminalActivity.NOT_SET_TEXT;

                TerminalActivity activity = (TerminalActivity) getActivity();
                activity.updateButtonCommand(buttonIndex, commandToSet);

                dialog.dismiss();
            }
        });

        builder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                dialog.dismiss();
            }
        });

        final Dialog dlg = builder.create();

        editor.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (hasFocus)
                {
                    dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });

        return dlg;
    }
}
