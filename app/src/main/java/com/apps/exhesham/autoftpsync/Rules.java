package com.apps.exhesham.autoftpsync;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.ArraySet;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.apps.exhesham.autoftpsync.utils.Utils;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.Set;


public class Rules extends AppCompatActivity {
    private Context context;

    public ArrayMap<String,String> getRules() {
        if(_rules == null){
            Log.v("read rules", " Reading rules");
            _rules = readRules();
        }
        return _rules;
    }

    private static ArrayMap<String,String> _rules;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_rules);
        Toolbar toolbar = (Toolbar) findViewById(R.id.rules_toolbar);
        setSupportActionBar(toolbar);

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
        String rules_str = Utils.getInstance(context).getConfigString("rules");
        JSONArray ja;
        try {
            ja = new JSONArray(rules_str);
        } catch (JSONException e) {
            e.printStackTrace();
            ja = new JSONArray();
        }
        if(ja.length() == 0){
            try {
                ja.put(new JSONObject().put("extension","png").put("folder_name","photos"));
                ja.put(new JSONObject().put("extension","jpeg").put("folder_name","photos"));
                ja.put(new JSONObject().put("extension","jpg").put("folder_name","photos"));
                ja.put(new JSONObject().put("extension","mp3").put("folder_name","music"));
                ja.put(new JSONObject().put("extension","mp4").put("folder_name","videos"));
                ja.put(new JSONObject().put("extension","pdf").put("folder_name","documents"));
                ja.put(new JSONObject().put("extension","*").put("folder_name","others"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        ArrayMap<String, String> rules = new ArrayMap<>();
        for (int i = 0;i < ja.length();i++) {
            try {
                String foldername = ja.getJSONObject(i).getString("folder_name");
                String extension = ja.getJSONObject(i).getString("extension");
                rules.put(extension,foldername);
            } catch (JSONException e) {
                continue;
            }
        }
        return  rules;
    }

    public void deleteRules(MenuItem item){
        TableLayout ll = (TableLayout) findViewById(R.id.table_rules);
        for(Integer index : checked_boxes.keySet()){
            if(checked_boxes.get(index) == true) {
                TableRow tr = (TableRow) ll.getChildAt(index);
                String extension = ((TextView) tr.getChildAt(1)).getText().toString();
                getRules().remove(extension);
            }
        }
        saveChanges(null);
        displayRulesOnTable();
    }
    public void saveChanges(MenuItem item){
        ArrayMap<String,String> rules = getRules();

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
        ArrayMap<String, String> rules = getRules();

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
                    if(getRules().containsKey("*")){
                        getRules().remove("*");
                    }
                    if(ignoreOthers.isChecked()){
                        getRules().put("*","<IGNORE FILE>");
                    }else{
                        getRules().put("*",newRuleFolder);
                    }

                }else{
                    if(!newRuleExtension.equals("") && !newRuleFolder.equals("")){
                        if(getRules().containsKey(extension)){
                            getRules().remove(extension);
                        }
                        getRules().put(newRuleExtension,newRuleFolder);
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
