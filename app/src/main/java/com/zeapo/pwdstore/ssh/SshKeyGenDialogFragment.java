package com.zeapo.pwdstore.ssh;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;
import com.zeapo.pwdstore.R;

import java.io.File;
import java.io.FileOutputStream;

// SSH key generation UI
public class SshKeyGenDialogFragment extends DialogFragment {

    public SshKeyGenDialogFragment() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final Activity callingActivity = getActivity();
        final View v = View.inflate(callingActivity, R.layout.fragment_ssh_keygen, null);
        builder.setView(v);
        Typeface monoTypeface = Typeface.createFromAsset(callingActivity.getAssets(), "fonts/sourcecodepro.ttf");

        Spinner spinner = (Spinner) v.findViewById(R.id.length);
        Integer[] lengths = new Integer[]{2048, 4096};
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(callingActivity,
                android.R.layout.simple_spinner_dropdown_item, lengths);
        spinner.setAdapter(adapter);

        ((EditText) v.findViewById(R.id.passphrase)).setTypeface(monoTypeface);

        CheckBox checkbox = (CheckBox) v.findViewById(R.id.show_passphrase);
        checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                EditText editText = (EditText) v.findViewById(R.id.passphrase);
                int selection = editText.getSelectionEnd();
                if (isChecked) {
                    editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else {
                    editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                editText.setSelection(selection);
            }
        });

        builder.setPositiveButton(getResources().getString(R.string.ssh_keygen_generate), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String sshKeyName = ((EditText) v.findViewById(R.id.name)).getText().toString();
                if (sshKeyName.equals("")) {
                    sshKeyName = ".ssh_key";
                }
                new generateTask(v, callingActivity, sshKeyName).execute();
            }
        });

        builder.setNegativeButton(getResources().getString(R.string.dialog_cancel), null);

        return builder.setTitle("Generate SSH keys").create();
    }

    private class generateTask extends AsyncTask<Void, Void, Exception> {
        private View v;
        private Activity a;
        private ProgressDialog pd;
        private String sshKeyName;

        public generateTask(View v, Activity a, String sshKeyName) {
            this.v = v;
            this.a = a;
            this.sshKeyName = sshKeyName;
        }

        protected Exception doInBackground(Void... voids) {
            InputMethodManager imm = (InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

            Spinner spinner = (Spinner) v.findViewById(R.id.length);
            int length = (Integer) spinner.getSelectedItem();

            EditText editText = (EditText) v.findViewById(R.id.passphrase);
            String passphrase = editText.getText().toString();

            editText = (EditText) v.findViewById(R.id.comment);
            String comment = editText.getText().toString();

            JSch jsch = new JSch();
            try {
                KeyPair kp = KeyPair.genKeyPair(jsch, KeyPair.RSA, length);

                File file = new File(a.getFilesDir() + "/" + sshKeyName);
                FileOutputStream out = new FileOutputStream(file, false);
                if (passphrase.length() > 0) {
                    kp.writePrivateKey(out, passphrase.getBytes());
                } else {
                    kp.writePrivateKey(out);
                }

                file = new File(a.getFilesDir() + "/" + sshKeyName + ".pub");
                out = new FileOutputStream(file, false);
                kp.writePublicKey(out, comment);
                return null;
            } catch (Exception e) {
                System.out.println("Exception caught :(");
                e.printStackTrace();
                return e;
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = ProgressDialog.show(a, "", "Generating keys");

        }

        @Override
        protected void onPostExecute(Exception e) {
            super.onPostExecute(e);
            pd.dismiss();
            if (e == null) {
                Toast.makeText(a, "SSH-key generated", Toast.LENGTH_LONG).show();
                DialogFragment df = new ShowSshKeyDialogFragment();
                Bundle args = new Bundle();
                args.putString("sshKeyName", sshKeyName + ".pub");
                df.setArguments(args);
                df.show(a.getFragmentManager(), "public_key");
                if (SshKeyActivity.getSshKeys(a).size() == 1) {
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(a);
                    settings.edit().putString("ssh_key_name", sshKeyName).apply();
                }
            } else {
                new AlertDialog.Builder(a)
                        .setTitle("Error while trying to generate the ssh-key")
                        .setMessage(a.getResources().getString(R.string.ssh_key_error_dialog_text) + e.getMessage())
                        .setPositiveButton(getResources().getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // pass
                            }
                        }).show();
            }

        }
    }
}