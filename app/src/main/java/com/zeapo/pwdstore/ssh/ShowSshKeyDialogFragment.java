package com.zeapo.pwdstore.ssh;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zeapo.pwdstore.R;

import org.apache.commons.io.FileUtils;

import java.io.File;

// Displays the generated public key .ssh_key.pub
public class ShowSshKeyDialogFragment extends DialogFragment {
    public ShowSshKeyDialogFragment() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final View view = View.inflate(getActivity(), R.layout.fragment_show_ssh_key, null);
        builder.setView(view);

        TextView textView = (TextView) view.findViewById(R.id.public_key);
        String filename = getArguments().getString("filename");
        File file = new File(getActivity().getFilesDir() + "/" + filename);
        try {
            textView.setText(FileUtils.readFileToString(file));
        } catch (Exception e) {
            System.out.println("Exception caught :(");
            e.printStackTrace();
        }

        builder.setPositiveButton(getResources().getString(R.string.dialog_ok), null);
        builder.setNegativeButton(getResources().getString(R.string.dialog_cancel), null);
        builder.setNeutralButton(getResources().getString(R.string.ssh_keygen_copy), null);

        final AlertDialog ad = builder.setTitle("Your public key").create();
        ad.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button b = ad.getButton(AlertDialog.BUTTON_NEUTRAL);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        TextView textView = (TextView) view.findViewById(R.id.public_key);
                        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("public key", textView.getText().toString());
                        clipboard.setPrimaryClip(clip);
                    }
                });
            }
        });
        return ad;
    }
}