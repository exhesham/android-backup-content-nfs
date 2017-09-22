package com.apps.exhesham.autoftpsync;

import android.content.Context;
import android.support.test.espresso.core.deps.guava.io.CharStreams;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

import com.apps.exhesham.autoftpsync.utils.Constants;
import com.apps.exhesham.autoftpsync.utils.NfsAPI;
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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

public class LogsActivity extends AppCompatActivity {
    public int totalFailures = 0;
    public int totalSuccess = 0;
    public class WebAppInterface {
        Context mContext;

        /** Instantiate the interface and set the context */
        WebAppInterface(Context c) {
            mContext = c;
        }

        /** Show a toast from the web page */
        @JavascriptInterface
        public void showToast(String toast) {
            Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);



        try {
            editHTML();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private String reformatDate(String inputString){
        DateFormat format = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);

        Date date = new Date(inputString);

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

                JSONObject currFile = ja.getJSONObject(i);

                String date =  reformatDate(currFile.getString("sync_date"));
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
