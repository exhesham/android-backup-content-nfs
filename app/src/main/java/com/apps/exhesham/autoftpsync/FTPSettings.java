package com.apps.exhesham.autoftpsync;

import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;

import com.apps.exhesham.autoftpsync.utils.Constants;
import com.apps.exhesham.autoftpsync.utils.FTPSettingsNode;
import com.apps.exhesham.autoftpsync.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A login screen that offers login via email/password.
 */
public class FTPSettings extends AppCompatActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.activity_ftp_settings);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.settings_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        viewSettings();
    }

    public void storeNewFtp(MenuItem item) {

        TextView username = (TextView) findViewById(R.id.text_username);
        TextView password = (TextView) findViewById(R.id.text_password);
        TextView serverurl = (TextView) findViewById(R.id.text_server);
        TextView defaultpath = (TextView) findViewById(R.id.text_defaultpath);
        TextView port = (TextView) findViewById(R.id.text_port);
        RadioButton passiveMode = (RadioButton) findViewById(R.id.radioPassive);
        CheckBox annonymous = (CheckBox) findViewById(R.id.check_anonymousmode);
        try{
            Utils.getInstance(this).storeConfigString(Constants.DB_FTP_SETTINGS, new FTPSettingsNode(
                    username.getText().toString(),
                    password.getText().toString(),
                    Integer.parseInt(port.getText().toString()),
                    serverurl.getText().toString(),
                    "FTP Server",
                    true,
                    defaultpath.getText().toString(),
                    passiveMode.isChecked(),
                    annonymous.isChecked()).toString()

                    );
                finish();
        }catch (Exception e){
            e.printStackTrace();
        }
        finishActivity(0);

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.ftp_settings_toolbar, menu);
        return true;
    }

    public void viewSettings(){
        String availableFTP = Utils.getInstance(this).getConfigString(Constants.DB_FTP_SETTINGS);

        TextView username = (TextView) findViewById(R.id.text_username);
        TextView password = (TextView) findViewById(R.id.text_password);
        TextView serverurl = (TextView) findViewById(R.id.text_server);
        TextView port = (TextView) findViewById(R.id.text_port);
        TextView defaultpath = (TextView) findViewById(R.id.text_defaultpath);
        JSONObject jo = null;
        if(availableFTP != null && !availableFTP.equals("")) {
            try {
                jo = new JSONObject(availableFTP);
                FTPSettingsNode fn = FTPSettingsNode.parseJSON(jo);
                if(fn == null){
                    return;
                }
                username.setText(fn.getUsername());
                password.setText(fn.getPassword());
                serverurl.setText(fn.getServerurl());
                defaultpath.setText(fn.getDefaultPath());
                port.setText(Integer.toString(fn.getPort()));
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("storeNewFtp", "Failed to parse the ftp repository:" + availableFTP);
                return;
            }
        }
    }

}

