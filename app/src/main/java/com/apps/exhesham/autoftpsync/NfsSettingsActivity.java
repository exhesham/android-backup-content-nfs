package com.apps.exhesham.autoftpsync;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.apps.exhesham.autoftpsync.utils.Constants;
import com.apps.exhesham.autoftpsync.utils.NFSSettingsNode;
import com.apps.exhesham.autoftpsync.utils.NfsAPI;
import com.apps.exhesham.autoftpsync.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;

public class NfsSettingsActivity extends AppCompatActivity {
    private Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_nfs);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.smb_settings_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        fillSettings();

    }
    private void fillSettings(){
        TextView usernameTV = (TextView) findViewById(R.id.text_username);
        TextView passwordTV = (TextView) findViewById(R.id.text_password);
        TextView serverurlTV = (TextView) findViewById(R.id.text_server);
        TextView defaultpathTV = (TextView) findViewById(R.id.text_defaultpath);

        String defaultAddress = Utils.getInstance(context).getConfigString(Constants.DB_SMB_SERVER);
        String defaultPath = Utils.getInstance(context).getConfigString(Constants.DB_SMB_DEFAULT_PATH);
        String username = Utils.getInstance(context).getConfigString(Constants.DB_SMB_USERNAME);
        String password = Utils.getInstance(context).getConfigString(Constants.DB_SMB_PASSWORD);
        if(defaultAddress.equals("")){
            defaultAddress = Utils.getInstance(context).getDefaultGatewayAddress();
        }

        if(username.equals("") && password.equals("")) {
            if (Utils.getInstance(context).validateSmbCredintials(new NFSSettingsNode("admin", "admin", defaultAddress, ""))) {
                username = "admin";
                password = "admin";
            }
            if (Utils.getInstance(context).validateSmbCredintials(new NFSSettingsNode("root", "root", defaultAddress, ""))) {
                username = "root";
                password = "root";
            }
            if (Utils.getInstance(context).validateSmbCredintials(new NFSSettingsNode("", "", defaultAddress, ""))) {
                username = "";
                password = "";
            }
        }
        if ("".equals(defaultPath)) {
            // UI thread is needed in order to load the categories
            refreshPath(null);
        }else {
            defaultpathTV.setText(defaultPath);
        }
        usernameTV.setText(username);
        passwordTV.setText(password);
        serverurlTV.setText(defaultAddress);



    }
    public void saveSmbSettings(MenuItem item) {
        TextView usernameTV = (TextView) findViewById(R.id.text_username);
        TextView passwordTV = (TextView) findViewById(R.id.text_password);
        TextView serverurlTV = (TextView) findViewById(R.id.text_server);
        TextView defaultpathTV = (TextView) findViewById(R.id.text_defaultpath);

        Utils.getInstance(context).storeConfigString(Constants.DB_SMB_SERVER, serverurlTV.getText().toString());
        Utils.getInstance(context).storeConfigString(Constants.DB_SMB_DEFAULT_PATH, defaultpathTV.getText().toString());
        Utils.getInstance(context).storeConfigString(Constants.DB_SMB_USERNAME,usernameTV.getText().toString());
        Utils.getInstance(context).storeConfigString(Constants.DB_SMB_PASSWORD, passwordTV.getText().toString());
    }
    private void showSnackBar(String msg, String undo, View.OnClickListener listener){
        //TODO: Move to utils
        Snackbar mySnackbar = Snackbar.make(findViewById(R.id.smb_settings_coord_layout),
                        msg, Snackbar.LENGTH_SHORT);
        TextView tv = (TextView) mySnackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        tv.setTextColor(Color.WHITE);
        mySnackbar.setAction(undo, listener);
        mySnackbar.show();
    }
    public void validateNfsSettings(MenuItem item) {
        TextView usernameTV = (TextView) findViewById(R.id.text_username);
        TextView passwordTV = (TextView) findViewById(R.id.text_password);
        TextView serverurlTV = (TextView) findViewById(R.id.text_server);
        TextView defaultpathTV = (TextView) findViewById(R.id.text_defaultpath);
        //TODO: Should start on thread and do a blocking progress wheel
        if (! Utils.getInstance(context).validateSmbCredintials(new NFSSettingsNode(usernameTV.getText().toString(),
                passwordTV.getText().toString(),
                serverurlTV.getText().toString(),
                defaultpathTV.getText().toString()
                ))) {
            showSnackBar("Your Credintials are INCORRECT!", "help", new MyHelpListener());

            return;
        }
        showSnackBar("Your Credintials are CORRECT!", "help", new MyHelpListener());
    }
    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
    public static void createFile(String filename){
        try{
            String user = "exhesham:fenderHrod11";
            NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(user);
            String path = "smb://192.168.1.1/samsung/test.txt";
            SmbFile sFile = new SmbFile(path, auth);
            SmbFileOutputStream sfos = new SmbFileOutputStream(sFile);
            sfos.write("Test".getBytes());
            sfos.close();
            System.out.println("Done!");
        } catch (IOException e) {
            // do something
            e.printStackTrace();
            System.out.println(e.getMessage());
        }

    }

    public void refreshPath(View view) {
        try {

            TextView usernameTV = (TextView) findViewById(R.id.text_username);
            TextView passwordTV = (TextView) findViewById(R.id.text_password);
            TextView serverurlTV = (TextView) findViewById(R.id.text_server);
            final String nfsUsername = usernameTV.getText().toString();
            final String nfsPassword = passwordTV.getText().toString();
            final String nfsDefaultAddress = serverurlTV.getText().toString();
            new Thread() {

                @Override
                public void run() {

                    ArrayList<String> possibleRootDir = new NfsAPI(new NFSSettingsNode(nfsUsername, nfsPassword, nfsDefaultAddress, "")).nfsGetRootDir();
                    if (possibleRootDir.size() > 0) {
                        final String availablePath = possibleRootDir.get(0);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView defaultpathTV = (TextView) findViewById(R.id.text_defaultpath);
                                defaultpathTV.setText(availablePath);
                            }
                        });

                    }

                }
            }.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.nfs_settings_toolbar, menu);
        return true;
    }
    public class MyHelpListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {

            // Code to undo the user's last action
        }
    }

}
