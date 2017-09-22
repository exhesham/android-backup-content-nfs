package com.apps.exhesham.autoftpsync;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.apps.exhesham.autoftpsync.utils.Constants;
import com.apps.exhesham.autoftpsync.utils.NFSSettingsNode;
import com.apps.exhesham.autoftpsync.utils.NfsAPI;
import com.apps.exhesham.autoftpsync.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

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

        String defaultAddress = Utils.getInstance(context).getConfigString(Constants.DB_SMB_SERVER);
        JSONArray defaultPaths = Utils.getInstance(context).getJsonArrayFromDB(Constants.DB_SMB_DEFAULT_PATH);
        String username = Utils.getInstance(context).getConfigString(Constants.DB_SMB_USERNAME);
        String password = Utils.getInstance(context).getConfigString(Constants.DB_SMB_PASSWORD);
        if(defaultAddress.equals("")){
            defaultAddress = Utils.getInstance(context).getDefaultGatewayAddress();
        }
        // try to guess default data...
        if(username.equals("") && password.equals("")) {
            if (Utils.getInstance(context).validateSmbCredintials(new NFSSettingsNode("admin", "admin", defaultAddress, new JSONArray()))) {
                username = "admin";
                password = "admin";
            }
            if (Utils.getInstance(context).validateSmbCredintials(new NFSSettingsNode("root", "root", defaultAddress, new JSONArray()))) {
                username = "root";
                password = "root";
            }
            if (Utils.getInstance(context).validateSmbCredintials(new NFSSettingsNode("", "", defaultAddress, new JSONArray()))) {
                username = "";
                password = "";
            }
        }
        if (defaultPaths.length() == 0) {
            // UI thread is needed in order to load the categories
            refreshPath(null);
        }else {
            Log.d("fillSettings", "Convert the smb settings json to spinner");
            ArrayList<NfsAPI.DiskNode> routerPathsArray = new ArrayList<>();
            int selectedItem =0;
            for(int i = 0;i < defaultPaths.length();i++){
                try {
                    JSONObject possibleSelection = defaultPaths.getJSONObject(i);
                    if (! possibleSelection.has("size")){
                        // in old version the already stored data doesn't contain the tag and for the app not to crash, i will ignore the size for old versions.
                        routerPathsArray.add(new NfsAPI.DiskNode(possibleSelection.getString("path"), 0, 0 ));
                    }else {
                        routerPathsArray.add(new NfsAPI.DiskNode(possibleSelection.getString("path"), possibleSelection.getLong("free-space"), possibleSelection.getLong("total-space")));
                    }
                    // the is_selected used to check if the user chose this disk to be the default
                    if(possibleSelection.getBoolean("is_selected")){
                        selectedItem = i;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
            fillSpinner(routerPathsArray, selectedItem);
        }
        usernameTV.setText(username);
        passwordTV.setText(password);
        serverurlTV.setText(defaultAddress);



    }
    public void saveSmbSettings(MenuItem item) {
        TextView usernameTV = (TextView) findViewById(R.id.text_username);
        TextView passwordTV = (TextView) findViewById(R.id.text_password);
        TextView serverurlTV = (TextView) findViewById(R.id.text_server);
        Spinner defaultpathTV = (Spinner) findViewById(R.id.text_defaultpath);
        JSONArray allPaths = new JSONArray();
        int pathCount = defaultpathTV.getAdapter() == null ? 0 : defaultpathTV.getAdapter().getCount();
        for(int i=0; i< pathCount; i++){
            JSONObject possiblePathJson = new JSONObject();
            NfsAPI.DiskNode possibleDisk =   ((NfsAPI.DiskNode)defaultpathTV.getAdapter().getItem(i));
            String possibleDiskName =   possibleDisk.diskName;
            long possibleDiskFreeSpace = possibleDisk.freeSize;
            long possibleDiskTotalSpace = possibleDisk.totalSize;
            try {
                possiblePathJson.put("path", possibleDiskName);
                possiblePathJson.put("free-space", possibleDiskFreeSpace);
                possiblePathJson.put("total-space", possibleDiskTotalSpace);
                possiblePathJson.put("is_selected", defaultpathTV.getSelectedItem().equals(possibleDisk));
                allPaths.put(possiblePathJson);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        Utils.getInstance(context).storeConfigString(Constants.DB_SMB_SERVER, serverurlTV.getText().toString());
        Utils.getInstance(context).storeConfigString(Constants.DB_SMB_DEFAULT_PATH, allPaths.toString());
        Utils.getInstance(context).storeConfigString(Constants.DB_SMB_USERNAME,usernameTV.getText().toString());
        Utils.getInstance(context).storeConfigString(Constants.DB_SMB_PASSWORD, passwordTV.getText().toString());
        this.finish();
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
        Spinner defaultpathTV = (Spinner) findViewById(R.id.text_defaultpath);
        //TODO: Should start on thread and do a blocking progress wheel
        if (! Utils.getInstance(context).validateSmbCredintials(new NFSSettingsNode(usernameTV.getText().toString(),
                passwordTV.getText().toString(),
                serverurlTV.getText().toString(),
                spinnerToJsonArray()
                ))) {
            showSnackBar("Your Credintials are INCORRECT!", "help", new MyHelpListener());

            return;
        }
        showSnackBar("Your Credintials are CORRECT!", "help", new MyHelpListener());
    }

    private JSONArray spinnerToJsonArray() {
        Spinner defaultpathTV = (Spinner) findViewById(R.id.text_defaultpath);
        JSONArray allPaths = new JSONArray();
        for(int i=0; i< defaultpathTV.getAdapter().getCount(); i++){
            JSONObject possiblePathJson = new JSONObject();
            String possiblePath =   defaultpathTV.getAdapter().getItem(i).toString();
            try {
                possiblePathJson.put("path", possiblePath);
                possiblePathJson.put("is_selected", defaultpathTV.getSelectedItem().toString().equals(possiblePath));
                allPaths.put(possiblePathJson);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        return allPaths;
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

                    final ArrayList<NfsAPI.DiskNode> possibleDisks = new NfsAPI(new NFSSettingsNode(nfsUsername, nfsPassword, nfsDefaultAddress, new JSONArray())).nfsGetConnectedDisks();
                    if (possibleDisks.size() > 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                fillSpinner(possibleDisks, 0);
                            }
                        });

                    }

                }
            }.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fillSpinner(ArrayList<NfsAPI.DiskNode> possibleRootDir, int selected) {
        Spinner defaultpathTV = (Spinner) findViewById(R.id.text_defaultpath);
        ArrayAdapter<NfsAPI.DiskNode> adp = new ArrayAdapter<NfsAPI.DiskNode>(context, android.R.layout.simple_spinner_item, possibleRootDir);
        adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        defaultpathTV.setAdapter(adp);
        defaultpathTV.setSelection(selected);
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
