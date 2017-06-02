package com.apps.exhesham.autoftpsync;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.apps.exhesham.autoftpsync.utils.Constants;
import com.apps.exhesham.autoftpsync.utils.Utils;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;


public class Rules extends AppCompatActivity {
    private Context context;

    public ArrayMap<String,String> getFollowedRules() {
        if(_followed_rules == null){
            Log.v("read rules", " Reading rules");
            _followed_rules = readRules();
        }
        return _followed_rules;
    }

    private static ArrayMap<String,String> _followed_rules;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_rules);

        Toolbar toolbar = (Toolbar) findViewById(R.id.rules_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog(null, null);
            }
        });
        displayRulesOnTable();

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.rules_toolbar, menu);
        return true;
    }

    private ArrayMap<String,String> readRules(){
        boolean shouldUpdateDB = false;
        JSONArray ja = Utils.getInstance(context).getJsonArrayFromDB("rules");
        JSONObject version =  Utils.getInstance(context).getJsonObjFromDB("following_paths");
        if(version == null || ja.length() == 0){
            /*If the version is not identified then for sure it is not version 3, then reformat the available data*/
            ja = new JSONArray();
            shouldUpdateDB = true;
        }
        if(ja.length() == 0){
            try {
                for(String ext : Constants.PHOTOS_CATERGORY_EXTS){
                    ja.put(new JSONObject().put("extension",ext).put("folder_name","photos").put("status",Constants.FOLLOWING_DIR).put("category","photos"));
                }
                for(String ext : Constants.COMPRESSED_CATERGORY_EXTS){
                    ja.put(new JSONObject().put("extension",ext).put("folder_name","compressed").put("status",Constants.FOLLOWING_DIR).put("category","compressed"));
                }
                for(String ext : Constants.DOCUMENTS_CATERGORY_EXTS){
                    ja.put(new JSONObject().put("extension",ext).put("folder_name","documents").put("status",Constants.FOLLOWING_DIR).put("category","documents"));
                }
                for(String ext : Constants.MUSIC_CATERGORY_EXTS){
                    ja.put(new JSONObject().put("extension",ext).put("folder_name","music").put("status",Constants.FOLLOWING_DIR).put("category","music"));
                }
                for(String ext : Constants.VIDEO_CATERGORY_EXTS){
                    ja.put(new JSONObject().put("extension",ext).put("folder_name","videos").put("status",Constants.FOLLOWING_DIR).put("category","videos"));
                }
                for(String ext : Constants.APPS_CATERGORY_EXTS){
                    ja.put(new JSONObject().put("extension",ext).put("folder_name","apps").put("status",Constants.FOLLOWING_DIR).put("category","apps"));
                }

//                ja.put(new JSONObject().put("extension","*").put("folder_name","others"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        ArrayMap<String, String> rules = new ArrayMap<>();
        for (int i = 0;i < ja.length();i++) {
            try {
                String foldername = ja.getJSONObject(i).getString("folder_name");
                String extension = ja.getJSONObject(i).getString("extension");
                if(!ja.getJSONObject(i).has("status")){
                    ja.getJSONObject(i).put("status",Constants.FOLLOWING_DIR);
                }
                String status = ja.getJSONObject(i).getString("status");
                if(Constants.FOLLOWING_DIR.equals(status)){
                    rules.put(extension,foldername);
                }
            } catch (JSONException e) {
                continue;
            }
        }
        if(shouldUpdateDB){
            Utils.getInstance(context).storeConfigString("rules",ja.toString());
        }
        return  rules;
    }

    public void deleteRules(MenuItem item){
        TableLayout ll = (TableLayout) findViewById(R.id.table_rules);
        boolean showAlert = false;
        for(Integer index : checked_boxes.keySet()){
            if(checked_boxes.get(index) == true) {
                TableRow tr = (TableRow) ll.getChildAt(index);
                String extension = ((TextView) tr.getChildAt(1)).getText().toString();
                if(isFixedExtension(extension)){
                    showAlert = true;
                    continue;
                }
                getFollowedRules().remove(extension);
            }
        }

        saveChanges(null);
        _followed_rules = null;
        displayRulesOnTable();
        if(showAlert){
            Utils.getInstance(context).showAlert("You cannot delete fixed extensions","Delete Fixed Extensions", false);
        }
    }

    private boolean isFixedExtension(String extension) {
        if(Arrays.asList(Constants.COMPRESSED_CATERGORY_EXTS).contains(extension)){
            return true;
        }
        if(Arrays.asList(Constants.DOCUMENTS_CATERGORY_EXTS).contains(extension)){
            return true;
        }
        if(Arrays.asList(Constants.RECORDING_CATERGORY_EXTS).contains(extension)){
            return true;
        }
        if(Arrays.asList(Constants.MUSIC_CATERGORY_EXTS).contains(extension)){
            return true;
        }
        if(Arrays.asList(Constants.PHOTOS_CATERGORY_EXTS).contains(extension)){
            return true;
        }
        if(Arrays.asList(Constants.VIDEO_CATERGORY_EXTS).contains(extension)){
            return true;
        }
        if(Arrays.asList(Constants.APPS_CATERGORY_EXTS).contains(extension)){
            return true;
        }
        return false;
    }

    public void saveChanges(MenuItem item){
        ArrayMap<String,String> rules = getFollowedRules();

        JSONArray ja = new JSONArray();
        for (String key : rules.keySet()) {
            try {
                ja.put(new JSONObject().put("extension",key).put("folder_name",rules.get(key)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Utils.getInstance(context).storeConfigString("rules",ja.toString());
    }
    public void editChanges(MenuItem item){
        if (selected_row <= 0 ){
            return;
        }
        TableLayout ll = (TableLayout) findViewById(R.id.table_rules);
        TableRow tr = (TableRow) ll.getChildAt(selected_row);
        String extension = ((TextView)tr.getChildAt(1)).getText().toString();
        String folder = ((TextView)tr.getChildAt(2)).getText().toString();
        showDialog(extension, folder);
    }
    private static int selected_row = 1;
    private static ArrayMap<Integer, Boolean> checked_boxes = new ArrayMap<Integer, Boolean>();
    private void displayRulesOnTable(){
        ArrayMap<String, String> rules = getFollowedRules();

        final TableLayout ll = (TableLayout) findViewById(R.id.table_rules);
//        ll.removeAllViews();
        while (ll.getChildCount() > 1){ // Remove all except header
            ll.removeViewAt(ll.getChildCount() - 1);
        }
        checked_boxes.clear();
        int i = 1;
        TableRow defaultRow = new TableRow(this);
        for (String key : rules.keySet()) {
            TableRow newRow= new TableRow(this);
            newRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

            CheckBox checkBox = new CheckBox(this);
            checkBox.setGravity(Gravity.NO_GRAVITY);
            checkBox.setTag(i);
            TextView extension = new TextView(this);
            extension.setText(key.equals("*")?"ALL THE REST":key);
            extension.setTextColor(Color.BLACK);
            extension.setTextSize(16f);

            TextView folderName = new TextView(this);
            //folderName.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.7f));
            folderName.setText(rules.get(key));
            folderName.setTextColor(Color.BLACK);
            folderName.setTextSize(16f);

            newRow.setFocusable(true);
            newRow.setFocusableInTouchMode(true);
            newRow.setBackgroundResource(android.R.drawable.list_selector_background);

            final int finalI = i;
            newRow.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    selected_row = finalI;
                }
            });
            newRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TableLayout ll = (TableLayout) findViewById(R.id.table_rules);
                    ll.getChildAt(finalI).setBackgroundResource(android.R.drawable.list_selector_background);
                    selected_row = finalI;
                }
            });
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    checked_boxes.put(finalI,isChecked);
                }
            });

            newRow.addView(checkBox,(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT,0.3f)));
            newRow.addView(extension,(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT,0.8f)));
            //row.addView(arrowImage);
            newRow.addView(folderName,(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT,0.8f)));
            if(key.equals("*")){
                defaultRow = newRow;
                continue;
            }
            ll.addView(newRow,i++);
        }
        defaultRow.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                selected_row = ll.getChildCount()-1;
            }
        });
        defaultRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TableLayout ll = (TableLayout) findViewById(R.id.table_rules);
                ll.getChildAt(ll.getChildCount()-1).setBackgroundResource(android.R.drawable.list_selector_background);
                selected_row = ll.getChildCount()-1;
            }
        });

        ll.addView(defaultRow);
    }

    public String getExtensionFolder(String extension) {
        ArrayMap<String, String> rules = readRules();
        if(rules.containsKey(extension.toLowerCase())){
            return rules.get(extension);
        }
        if(rules.containsKey("*") && rules.get("*").equals("<IGNORE FILE>")){

            return null;
        }
        return rules.get("*");
    }


    private void showDialog(final String extension, final String foldername){
        final Dialog commentDialog = new Dialog(this);
        commentDialog.setContentView(R.layout.edit_rule_layout);
        Button okBtn = (Button) commentDialog.findViewById(R.id.ok);
        final TextView extensionField = (TextView) commentDialog.findViewById(R.id.extension);
        final TextView folderField = (TextView) commentDialog.findViewById(R.id.folder_name);
        final CheckBox ignoreOthers= (CheckBox) commentDialog.findViewById(R.id.ignore_other_extensions);
        if(extension != null && foldername != null){
            extensionField.setText(extension);
            folderField.setText(foldername);
        }
        if("ALL THE REST".equals(extension)){
            extensionField.setEnabled(false);
            ignoreOthers.setVisibility(View.VISIBLE);
        }else{
            extensionField.setEnabled(true);
            ignoreOthers.setVisibility(View.INVISIBLE);
        }
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //do anything you want here before close the dialog

                String newRuleExtension = extensionField.getText().toString();
                String newRuleFolder = folderField.getText().toString();
                if("ALL THE REST".equals(extension)){
                    if(getFollowedRules().containsKey("*")){
                        getFollowedRules().remove("*");
                    }
                    if(ignoreOthers.isChecked()){
                        getFollowedRules().put("*","<IGNORE FILE>");
                    }else{
                        getFollowedRules().put("*",newRuleFolder);
                    }

                }else{
                    if(!newRuleExtension.equals("") && !newRuleFolder.equals("")){
                        if(getFollowedRules().containsKey(extension)){
                            getFollowedRules().remove(extension);
                        }
                        getFollowedRules().put(newRuleExtension,newRuleFolder);
                    }
                }
                saveChanges(null);
                displayRulesOnTable();
                commentDialog.dismiss();
            }
        });
        Button cancelBtn = (Button) commentDialog.findViewById(R.id.cancel);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commentDialog.dismiss();
            }
        });
        commentDialog.show();
    }
    public boolean shouldIgnoreFile(String path) {
        String extension = FilenameUtils.getExtension(path);
        String folderName = getExtensionFolder(extension);
        return folderName == null;
    }
}
