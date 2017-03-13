package com.apps.exhesham.autoftpsync;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.apps.exhesham.autoftpsync.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



public class Rules extends AppCompatActivity {
    private Context context;

    public ArrayMap<String,String> getRules() {
        if(rules == null){
            Log.v("read rules", " Reading rules");
            rules = readRules();
        }
        return rules;
    }

    private static ArrayMap<String,String> rules;
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
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                addNewRule();
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

    private void deleteRule(String extension, String foldername){
        ArrayMap<String,String> rules = readRules();
        rules.remove(extension);
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
    private void saveChanges(String extension, String foldername){

    }
    private void editChanges(String extension, String foldername){

    }
    private void displayRulesOnTable(){
        ArrayMap<String, String> rules = readRules();

        TableLayout ll = (TableLayout) findViewById(R.id.table_rules);
//        ll.removeAllViews();
        while (ll.getChildCount() > 1){ // Remove all except header
            ll.removeViewAt(ll.getChildCount() - 1);
        }
        int i = 1;
        TableRow defaultRow = null;
        for (String key : rules.keySet()) {
            final TableRow row= new TableRow(this);
            TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
            row.setLayoutParams(lp);
            final CheckBox checkBox = new CheckBox(this);

            checkBox.setText(key.equals("*")?"ALL THE REST":key);

            checkBox.setGravity(Gravity.NO_GRAVITY);
            TextView extension = new TextView(this);
            extension.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.7f));
            extension.setText(key.equals("*")?"ALL THE REST":key);
            TextView folderName = new TextView(this);
            folderName.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.7f));
            folderName.setText(rules.get(key));
            ImageView arrowImage = new ImageView(this);
            arrowImage.setImageResource(R.drawable.folder);

            row.setFocusable(true);
            row.setFocusableInTouchMode(true);
            final int finalI = i;
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TableLayout ll = (TableLayout) findViewById(R.id.table_rules);
                    ll.getChildAt(finalI).setBackgroundResource(android.R.drawable.list_selector_background);
                }
            });

            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TableLayout ll = (TableLayout) findViewById(R.id.table_rules);
                    if(!checkBox.isChecked()){

                        ll.getChildAt(finalI).setBackgroundResource(android.R.drawable.list_selector_background);

                    }else{
                        ll.getChildAt(finalI).setBackgroundResource(android.R.drawable.list_selector_background);
                        ll.getChildAt(finalI).setBackgroundColor(0xFFe1e9ce);

                    }

                }
            });
            row.addView(checkBox);
///            row.addView(extension);
            //row.addView(arrowImage);
            row.addView(folderName);
            if(key.equals("*")){
                defaultRow = row;
            }else{
                ll.addView(row,i++);
            }
        }
        ll.addView(defaultRow,i++);
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
    private String newRuleExtension = "";
    private String newRuleFolder = "";

    private void addNewRule(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Title");

        // Set up the input
        final EditText inputExtension = new EditText(this);
        final EditText inputFolder = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        inputExtension.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        inputFolder.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(inputExtension);
        builder.setView(inputFolder);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                newRuleExtension = inputExtension.getText().toString();
                newRuleFolder = inputFolder.getText().toString();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}
