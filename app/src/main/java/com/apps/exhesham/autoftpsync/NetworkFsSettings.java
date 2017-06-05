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
import com.apps.exhesham.autoftpsync.utils.SMBSettingsNode;
import com.apps.exhesham.autoftpsync.utils.Utils;

import java.io.IOException;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;

public class NetworkFsSettings extends AppCompatActivity {
    private Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_shared_storage_file_sys);

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
        if(! defaultPath.equals("")){
            defaultpathTV.setText(defaultPath);
        }
        if(username.equals("") && password.equals("")) {
            if (Utils.getInstance(context).validateSmbCredintials(new SMBSettingsNode("admin", "admin", defaultAddress, ""))) {
                username = "admin";
                password = "admin";
            }
            if (Utils.getInstance(context).validateSmbCredintials(new SMBSettingsNode("root", "root", defaultAddress, ""))) {
                username = "root";
                password = "root";
            }
            if (Utils.getInstance(context).validateSmbCredintials(new SMBSettingsNode("", "", defaultAddress, ""))) {
                username = "";
                password = "";
            }
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
    public void validateSmbSettings(MenuItem item) {
        TextView usernameTV = (TextView) findViewById(R.id.text_username);
        TextView passwordTV = (TextView) findViewById(R.id.text_password);
        TextView serverurlTV = (TextView) findViewById(R.id.text_server);
        TextView defaultpathTV = (TextView) findViewById(R.id.text_defaultpath);

        if (! Utils.getInstance(context).validateSmbCredintials(new SMBSettingsNode(usernameTV.getText().toString(),
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
    public static void readDir(String dirname){
        try{
            String path = "smb://192.168.1.1/" + dirname;
            String user = "exhesham:fenderHrod11";
            NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(user);

            SmbFile sFile = new SmbFile(path, auth);
            for (SmbFile child : sFile.listFiles()){
                System.out.println(child.getCanonicalPath());
                if(child.isDirectory()){
                    System.out.println("->Directory:" + child.isDirectory());
                    System.out.println("->Free Space:" + humanReadableByteCount(child.getDiskFreeSpace(),false));
                    System.out.println("->Can Write:" + child.canWrite());
                    System.out.println("->Name:" + child.getName());
                    System.out.println("------------------------------------");
                }

            }

            System.out.println("Done!");
        } catch (IOException e) {
            // do something
            e.printStackTrace();
            System.out.println(e.getMessage());
        }

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.smb_settings_toolbar, menu);
        return true;
    }
    public class MyHelpListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {

            // Code to undo the user's last action
        }
    }
}
