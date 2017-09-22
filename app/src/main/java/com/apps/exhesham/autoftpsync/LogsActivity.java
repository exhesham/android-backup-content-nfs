package com.apps.exhesham.autoftpsync;

import android.content.Context;
import android.support.test.espresso.core.deps.guava.io.CharStreams;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

import com.apps.exhesham.autoftpsync.utils.Constants;
import com.apps.exhesham.autoftpsync.utils.NfsAPI;
import com.apps.exhesham.autoftpsync.utils.PathDetails;
import com.apps.exhesham.autoftpsync.utils.Utils;

import org.apache.commons.net.io.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

public class LogsActivity extends AppCompatActivity {
    public int totalFailures = 0;
    public int totalSuccess = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.activity_logs);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.logs_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        try {
            editHTML();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        setContentView(R.layout.activity_logs);

    }
    public void clearLogs(MenuItem item){
        JSONArray ja = Utils.getInstance(this).getJsonArrayFromDB(Constants.DB_FOLLOWED_DIRS);
        // first delete each log with filename as key
        try {
            for(int i=0;i<ja.length();i++){

                // get the status of the directory. may the user disabled it from being followed
                JSONObject jo = ja.getJSONObject(i);
                String status = jo.getString("status");
                if (Constants.FOLLOWING_DIR.equals(status)) {
                    // get all the files in the folder recursivly
                    ArrayList<PathDetails> pda = Utils.FileSysAPI.getFoldersRecursive(jo.getString("path"));
                    Log.d("Sending files", "The files that going to be filtered from first scan are " + pda.size());
                    for (final PathDetails pd : pda) {
                        // if the file is already sent or it is sending but no timeout or a directory then ignore.
                        Utils.getInstance(this).storeConfigString(pd.getFullpath(), "");
                    }

                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }


        // now clear the logs
        Utils.getInstance(this).storeConfigString("sync_logs", "[]");
    }
    public void refreshLogs(MenuItem item){
        try {
           editHTML();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.logs_toolbar, menu);
        return true;
    }
    private void editHTML() throws IOException {
        InputStream is = null;
        WebView lWebView = (WebView)findViewById(R.id.webView);
        lWebView.getSettings().setJavaScriptEnabled(true);
        // register class containing methods to be exposed to JavaScript https://developer.android.com/guide/webapps/webview.html#UsingJavaScript

//        JSInterface = new JavaScriptInterface(this);
//        wv.addJavascriptInterface(JSInterface, "JSInterface");
        try {
            is = getApplicationContext().getAssets().open("logs_template.html");
        } catch (IOException e) {
            e.printStackTrace();
            //TODO:Display an appropriate message.
            return;
        }
        Reader r = new InputStreamReader(is);
        String details = CharStreams.toString(r);
        String logsRecords = logsToHTML();
        String summary = generateSummary();
        details = details.replace("LOGS_HERE",logsRecords );
        details = details.replace("SUMMARY_HERE",summary );

        lWebView.getSettings().setJavaScriptEnabled(true);
        lWebView.loadData(details, "text/html",  "utf-8");

        lWebView.setWebChromeClient(new WebChromeClient());
    }

    private String generateSummary() {
        StringBuffer res = new StringBuffer();
        res.append("<p>").append("Total Succeeded:").append(totalSuccess).append("</p>");
        res.append("<p>").append("Total Failed:").append(totalFailures).append("</p>");
        return res.toString();
    }

    private String reformatDate(long epoch){
        DateFormat format = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);

        Date date = new Date(epoch);

        // Display a date in day, month, year format
        DateFormat formatter = new SimpleDateFormat("dd/MM/yy  hh:mm:ss a");

        String formattedDate = formatter.format(date);
        return formattedDate;
    }
    private String logsToHTML() {
        totalSuccess=0;
        totalFailures=0;
        JSONArray res = new JSONArray();
        HashMap<String,String> files = new HashMap<>();
        JSONArray updatedLogs = new JSONArray(); // contain no duplicates
        try {
            JSONArray ja = Utils.getInstance(this).getJsonArrayFromDB("sync_logs");
            for(int i = 0 ; i < ja.length();i++){
                String date;
                JSONObject currFile = ja.getJSONObject(i);
                try{
                    date =  reformatDate(currFile.getLong("sync_date"));
                }catch (Exception ex){
                    date =  currFile.getString("sync_date");
                }

                String filename = currFile.getString("file_name");

                String filestatus = currFile.getString("file_status");
                String distfolder = currFile.getString("dist_folder");
                // remove duplicate files and take the last status
                if(files.containsKey(filename)){
                    if(parseDate(files.get(filename)).compareTo(parseDate(date))<=0){
                        ja.remove(i);
                    }
                }
                File fd = new File(filename);
                updatedLogs.put(Utils.getInstance(this).createLogEntry(date,filename,filestatus,distfolder));
                res.put(generateRow(date,filename,distfolder,filestatus,fd.length()));
                if(filestatus.equals(Constants.STATUS_SENT)) {
                    totalSuccess++;
                }else{
                    totalFailures++;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // store the updated logs into the metadata - this updated logs has no duplicates
        Utils.getInstance(this).storeConfigString("sync_logs", updatedLogs.toString());
        return res.toString();
    }

    private Date parseDate(String s) {
        return new Date();
    }

    private JSONObject generateRow(String date, String filename, String dist, String status, long size){
//        String colorClass = status.equals(Constants.STATUS_SENT)?"success": "danger";
//        String template = String.format("<tr  class=\"%s\">\n" +
//                "            <td>%s</td>\n" +
//                "            <td>%s</td>\n" +
//                "            <td>%s</td>\n" +
//                "            <td>%s</td>\n" +
//                "            <td>%s</td>\n" +
//                "        </tr>",colorClass, date, NfsAPI.humanReadableByteCount(size,false), filename, dist,status);
//        return  template;
        JSONObject res = new JSONObject();
        try {
            res.put("sync_date",date).
                    put("file_size",size).
                    put("file_name",filename).
                    put("file_status",status).
                    put("dist_folder",dist);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return res;
    }
}
