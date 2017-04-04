package com.apps.exhesham.autoftpsync;

import android.Manifest;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.view.menu.ActionMenuItemView;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import com.apps.exhesham.autoftpsync.utils.Constants;
import com.apps.exhesham.autoftpsync.utils.FTPNode;
import com.apps.exhesham.autoftpsync.utils.PathDetails;
import com.apps.exhesham.autoftpsync.utils.Utils;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import static android.R.attr.data;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {


    static int totalFilesShouldBeSent =0;
    static int totalFilesAlreadySent =0;
    static int totalHandled =0;
    static boolean isFtpSettingsCorrect = false;
    private Context context;
    static FTPNode ftpnode;

    /*
    * All Icons are taken from https://www.iconfinder.com/iconsets/circle-icons-1
    * */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        context = this;
        ftpnode = Utils.getInstance(context).getFTPSettings();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // The filter's action is BROADCAST_ACTION
        IntentFilter statusIntentFilter = new IntentFilter(Constants.BROADCAST_ACTION);
        // Instantiates a new DownloadStateReceiver
        ResponseReceiver mDownloadStateReceiver = new ResponseReceiver();
        // Registers the DownloadStateReceiver and its intent filters
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mDownloadStateReceiver,
                statusIntentFilter);
        requestPermissions();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        testFtpConnect(null);
        loadCategoriesContent();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    private void startNotification(String notificationContent) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle("Phone to FTP")
                        .setContentText(notificationContent);
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);

// The stack builder object will contain an artificial back stack for the
// started Activity.
// This ensures that navigating backward from the Activity leads out of
// your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
// Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
// Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
// mId allows you to update the notification later on.
        int mId = 10;
        mNotificationManager.notify(mId, mBuilder.build());
    }

    private void requestPermissions() {
        if ((Build.VERSION.SDK_INT >= 23) &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.INTERNET,
                            Manifest.permission.ACCESS_NETWORK_STATE,
                            Manifest.permission.ACCESS_WIFI_STATE},
                    Constants.MY_PERMISSIONS_REQUEST_READ_AND_WRITE_SDK);
        } else {
            // If no permissions were granted dont display the directories content
            loadCategoriesContent();
        }
    }
    private void loadCategoriesContent(){

//        ArrayList<ListFollowedDirs.FollowedDirDetails> data = new ArrayList<>();
        final ListView listview = (ListView) findViewById(R.id.followed_dirs_listview);
        HashMap<String,Integer> total = new HashMap<>();
        HashMap<String,Integer> total_sent = new HashMap<>();
        total.put(Constants.COMPRESSED_CATERGORY_NAME,0);
        total.put(Constants.VIDEO_CATERGORY_NAME,0);
        total.put(Constants.PHOTOS_CATERGORY_NAME,0);
        total.put(Constants.DOCUMENTS_CATERGORY_NAME,0);
        total.put(Constants.MUSIC_CATERGORY_NAME,0);
        total.put(Constants.RECORDINGS_CATERGORY_NAME,0);

        total_sent.put(Constants.COMPRESSED_CATERGORY_NAME,0);
        total_sent.put(Constants.VIDEO_CATERGORY_NAME,0);
        total_sent.put(Constants.PHOTOS_CATERGORY_NAME,0);
        total_sent.put(Constants.DOCUMENTS_CATERGORY_NAME,0);
        total_sent.put(Constants.MUSIC_CATERGORY_NAME,0);
        total_sent.put(Constants.RECORDINGS_CATERGORY_NAME,0);

//        String following_paths = Utils.getInstance(context).getConfigString("following_paths");
//        JSONArray ja;
//        try {
//            ja = new JSONArray(following_paths);
//        } catch (JSONException e) {
//            e.printStackTrace();
//            ja = new JSONArray();
//        }
//        for (int i = 0; i < ja.length(); i++) {
//            try {
//                JSONObject jo = ja.getJSONObject(i);
//                String status = jo.getString("status");
//                if (!Constants.FOLLOWING_DIR.equals(status)) {
//                    continue;
//                }
//                String pathname = jo.getString("path");
//                Long lastUpdate = jo.getLong("date");
//
//
//                ArrayList<PathDetails> pda = Utils.FileSysAPI.getFoldersRecursive(pathname);
//
//                for (final PathDetails pd : pda) {
//
//                    total[Utils.FileSysAPI.getFileCategory(pathname)]++;
//                    int total_sent_ctr = total_sent[Utils.FileSysAPI.getFileCategory(pathname)];
//                    total_sent[Utils.FileSysAPI.getFileCategory(pathname)] = Utils.getInstance(context).getPathStatus(pd.getFullpath()).equals(Constants.STATUS_SENT) ? total_sent_ctr  + 1 : total_sent_ctr ;
//                }
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        }


            String pathname = Constants.DEFAULT_PATH;



            ArrayList<PathDetails> pda = Utils.FileSysAPI.getFoldersRecursive(pathname);

            for (final PathDetails pd : pda) {
                try {
                    String categoryName = Utils.FileSysAPI.getFileCategory(pd.getFullpath());
                    if(null == categoryName){
                        continue;
                    }
                    total.put(categoryName,total.get(categoryName)+1);
                    int total_sent_ctr = total_sent.get(categoryName);
                    total_sent.put(categoryName, Utils.getInstance(context).getPathStatus(pd.getFullpath()).equals(Constants.STATUS_SENT) ? total_sent_ctr  + 1 : total_sent_ctr) ;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        //TODO:After calculating the results, assign them to the fields
        TextView video_total_files = (TextView) findViewById(R.id.video_total_files);
        TextView video_total_sent_files = (TextView) findViewById(R.id.video_total_synced_files);
        video_total_files.setText("Total: " + total.get(Constants.VIDEO_CATERGORY_NAME));
        video_total_sent_files.setText("Synced: " + total_sent.get(Constants.VIDEO_CATERGORY_NAME));

        TextView recordings_total_files = (TextView) findViewById(R.id.recordings_total_files);
        TextView recordings_total_sent_files = (TextView) findViewById(R.id.recordings_total_synced_files);
        recordings_total_files.setText("Total: " + total.get(Constants.RECORDINGS_CATERGORY_NAME));
        recordings_total_sent_files.setText("Synced: " + total_sent.get(Constants.RECORDINGS_CATERGORY_NAME));

        TextView documents_total_files = (TextView) findViewById(R.id.documents_total_files);
        TextView documents_total_sent_files = (TextView) findViewById(R.id.documents_total_synced_files);
        documents_total_files.setText("Total: " + total.get(Constants.DOCUMENTS_CATERGORY_NAME));
        documents_total_sent_files.setText("Synced: " + total_sent.get(Constants.DOCUMENTS_CATERGORY_NAME));

        TextView photos_total_files = (TextView) findViewById(R.id.photos_total_files);
        TextView photos_total_sent_files = (TextView) findViewById(R.id.photos_total_synced_files);
        photos_total_files.setText("Total: " + total.get(Constants.PHOTOS_CATERGORY_NAME));
        photos_total_sent_files.setText("Synced: " + total_sent.get(Constants.PHOTOS_CATERGORY_NAME));

        TextView music_total_files = (TextView) findViewById(R.id.music_total_files);
        TextView music_total_sent_files = (TextView) findViewById(R.id.music_total_synced_files);
        music_total_files.setText("Total: " + total.get(Constants.MUSIC_CATERGORY_NAME));
        music_total_sent_files.setText("Synced: " + total_sent.get(Constants.MUSIC_CATERGORY_NAME));


        TextView compressed_total_files = (TextView) findViewById(R.id.compressed_total_files);
        TextView compressed_total_sent_files = (TextView) findViewById(R.id.compressed_total_synced_files);
        compressed_total_files.setText("Total: " + total.get(Constants.COMPRESSED_CATERGORY_NAME));
        compressed_total_sent_files.setText("Synced: " + total_sent.get(Constants.COMPRESSED_CATERGORY_NAME));

//        TextView apk_total_files = (TextView) findViewById(R.id.apk_total_files);
//        TextView apk_total_sent_files = (TextView) findViewById(R.id.apk_total_synced_files);
//        apk_total_files.setText("Total: " + total.get(Constants.));
//        apk_total_sent_files.setText("Synced: " + total_sent.get(Constants.VIDEO_CATERGORY_NAME));

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case Constants.MY_PERMISSIONS_REQUEST_READ_AND_WRITE_SDK:

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadCategoriesContent();
                }
                break;
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case (1) : {
                ftpnode = Utils.getInstance(context).getFTPSettings();

            }
        }
    }
    interface Callback {
        void callback(); // would be in any signature
    }

    private void testFtpConnect(final Callback callback) {
        if(ftpnode == null){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Utils.getInstance(context).showAlert(
                            "Please go to settings and configure an FTP Server.",
                            "FTP Configuration missing",
                            false);
                }
            });
            if(callback != null){
                callback.callback();
            }
            return;
        }
        final ProgressDialog progressDialog = ProgressDialog.show(this, "Checking FTP Settings", "Please wait. Checking ftp connectivity and settings correctness...");
        new Thread() {
            public void run() {
                try  {

                    try
                    {
                        final FTPClient con = new FTPClient();
                        con.setConnectTimeout(Constants.TEST_CONNECT_TIMEOUT_MS);
                        con.setDefaultTimeout(Constants.TEST_CONNECT_TIMEOUT_MS);
                        con.connect(ftpnode.getServerurl(),ftpnode.getPort());

                        if (con.login(ftpnode.getUsername(), ftpnode.getPassword()))
                        {
                            if(ftpnode.isPassive()) {
                                con.enterLocalPassiveMode(); // important!
                            }else {
                                con.enterLocalActiveMode();
                            }
                            con.setFileType(FTP.BINARY_FILE_TYPE);

                            if(!con.changeWorkingDirectory(ftpnode.getDefaultPath())){
                                String dirTree = ftpnode.getDefaultPath();
                                try {
                                    boolean dirExists = true;

                                    //tokenize the string and attempt to change into each directory level.  If you cannot, then start creating.
                                    String[] directories = dirTree.split("/");
                                    for (String dir : directories ) {
                                        if (!dir.isEmpty() ) {
                                            if (dirExists) {
                                                dirExists = con.changeWorkingDirectory(dir);
                                            }
                                            if (!dirExists) {
                                                if (!con.makeDirectory(dir)) {
                                                    throw new IOException("Unable to create remote directory '" + dir + "'.  error='" + con.getReplyString()+"'");
                                                }
                                                if (!con.changeWorkingDirectory(dir)) {
                                                    throw new IOException("Unable to change into newly created remote directory '" + dir + "'.  error='" + con.getReplyString()+"'");
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Utils.getInstance(context).showAlert(
                                                    "The FTP default path " +  ftpnode.getDefaultPath() + " Cannot be created. please choose available root path",
                                                    "FTP Configuration Error",
                                                    false);
                                        }
                                    });
                                    isFtpSettingsCorrect = false;
                                }

                            }
                            isFtpSettingsCorrect = true;
                            con.logout();
                            con.disconnect();
                        }else{
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Utils.getInstance(context).showAlert(
                                            "The user name or password are incorrect!",
                                            "FTP Configuration error",
                                            false);
                                }
                            });

                            isFtpSettingsCorrect = false;
                        }

                    }
                    catch (final Exception e)
                    {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Utils.getInstance(context).showAlert(
                                        "faulty connectivity...Failed to connect to server - ftp cannot be reached",
                                        "FTP Configuration error",
                                        false);
                            }
                        });

                        isFtpSettingsCorrect = false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    isFtpSettingsCorrect = false;
                }
                // dismiss the progress dialog
                progressDialog.dismiss();
                // DO Any Callback
                if(callback != null){
                    callback.callback();
                }
            }
        }.start();
    }

    public void viewSettings(MenuItem item) {
        try{
            Intent k = new Intent(MainActivity.this, FTPSettings.class);
            startActivityForResult(k,1);
        }catch (Exception ex){
            Log.getStackTraceString(ex);
        }
    }
    public void help(MenuItem item) {
        try{
            Intent k = new Intent(MainActivity.this, Help.class);
            startActivity(k);
        }catch (Exception ex){
            Log.getStackTraceString(ex);
        }
    }
    private String generateStatus(String status,String path) {
        JSONObject jo = new JSONObject();
        try {
            jo.put("date",System.currentTimeMillis());
            jo.put("status",status);
            jo.put("path",path);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString();
    }


    public class ResponseReceiver extends BroadcastReceiver
    {
        // Prevents instantiation
        private ResponseReceiver() {}
        // Called when the BroadcastReceiver gets an Intent it's registered to receive
        @Override
        public void onReceive(Context context, Intent intent) {
        /*
         * Handle Intents here.
         */
            Log.v ("Receive","On receive");
            /*
             * Gets the status from the Intent's extended data, and chooses the appropriate action
             */
            Bundle b = intent.getExtras();
            Set<String> allfiles = b.keySet();
            Iterator<String> iter = allfiles.iterator();
            if (totalFilesAlreadySent < totalFilesShouldBeSent && totalHandled < totalFilesShouldBeSent) {
                startNotification("Uploaded " + Integer.toString(totalFilesAlreadySent) + " Out of " + Integer.toString(totalFilesShouldBeSent));
            }
            if(totalFilesShouldBeSent != 0 && totalHandled == totalFilesShouldBeSent && totalFilesAlreadySent != 0){
                startNotification("Finished uploading "+Integer.toString(totalFilesAlreadySent) + " Files");
                shouldStartRotatingIcon(false);
            }
            if(totalFilesShouldBeSent != 0 && totalHandled == totalFilesShouldBeSent && totalFilesAlreadySent == 0){
                startNotification("Failed to upload all "+Integer.toString(totalFilesShouldBeSent) +  " Files! Check FTP Configuration or connectivity");
                shouldStartRotatingIcon(false);
            }
            while (iter.hasNext()) {
                String filename = iter.next();
                Utils.getInstance(context).storeConfigString(filename, generateStatus(b.getString(filename),filename));
            }
            //TODO: Select to update counters only....
            loadCategoriesContent();
        }
    }
    private void shouldStartRotatingIcon(final boolean start){
        int color = Color.parseColor("#00E506");
        if(!start){
            color = Color.parseColor("#FFF1F9");
        }
        final PorterDuffColorFilter colorFilter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        ActionMenuItemView iv = (ActionMenuItemView )findViewById(R.id.follow_status);
        iv.getCompoundDrawables()[0].setColorFilter(colorFilter);
    }
    public static class FTPAPI extends IntentService {
        public FTPAPI() {
            super("ReminderService");
        }

        private void updateStatus(String filepath,String status){

            Intent localIntent = new Intent(Constants.BROADCAST_ACTION).putExtra(filepath, status);
            // Broadcasts the Intent to receivers in this app.
            LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
        }
        private static void ftpCreateDirectoryTree( FTPClient client, String dirTree ) throws IOException {

            boolean dirExists = true;

            //tokenize the string and attempt to change into each directory level.  If you cannot, then start creating.
            String[] directories = dirTree.split("/");
            for (String dir : directories ) {
                if (!dir.isEmpty() ) {
                    if (dirExists) {
                        dirExists = client.changeWorkingDirectory(dir);
                    }
                    if (!dirExists) {
                        if (!client.makeDirectory(dir)) {
                            throw new IOException("Unable to create remote directory '" + dir + "'.  error='" + client.getReplyString()+"'");
                        }
                        if (!client.changeWorkingDirectory(dir)) {
                            throw new IOException("Unable to change into newly created remote directory '" + dir + "'.  error='" + client.getReplyString()+"'");
                        }
                    }
                }
            }
        }

        @Override
        protected void onHandleIntent(Intent workIntent)  {
            final  String filepath = workIntent.getStringExtra("full-path");
            final  String dstfolder = workIntent.getStringExtra("dst-dir");

            FTPClient con = null;
            updateStatus(filepath,Constants.STATUS_CONNECTING);
            try
            {
                con = new FTPClient();
                con.setControlEncoding("UTF-8");
                con.connect(ftpnode.getServerurl(),ftpnode.getPort());

                if (con.login(ftpnode.getUsername(), ftpnode.getPassword()))
                {
                    if(ftpnode.isPassive()) {
                        con.enterLocalPassiveMode(); // important!
                    }else {
                        con.enterLocalActiveMode();
                    }
                    con.setFileType(FTP.BINARY_FILE_TYPE);
                    Log.v("onHandleIntent","Will cd to path " + dstfolder);
                    con.setControlKeepAliveTimeout(600);
                    con.setDataTimeout(1600);
                    con.setConnectTimeout(1600);
                    ftpCreateDirectoryTree(con,dstfolder);
                    Log.v("onHandleIntent","navigating to dir passed successfully!");
                    con.changeWorkingDirectory(dstfolder);
                    String data = filepath;
                    Log.v("FTPAPI",con.getStatus());
                    FileInputStream in = new FileInputStream(new File(data));
                    updateStatus(filepath,Constants.STATUS_SENDING);
                    boolean result = con.storeFile(new File(data).getName(), in);
                    Log.v("send",Integer.toString(con.getReplyCode()));
                    Log.v("send",con.getReplyString());
                    in.close();
                    if (result){
                        updateStatus(filepath,Constants.STATUS_SENT);
                        totalFilesAlreadySent++;
//                        uploadFailing = false;
                    }else{
                        updateStatus(filepath,Constants.STATUS_SENDING_FAILED);
//                        uploadFailing = true;
                    }
                    con.logout();
                    con.disconnect();
                }else{
                    updateStatus(filepath,Constants.STATUS_FAILED_LOGIN);
                }
            }
            catch (Exception e)
            {
                updateStatus(filepath,Constants.STATUS_FAILED_CONNECTING);
                e.printStackTrace();
//                uploadFailing = true;
            }
            totalHandled++;
        } // onHandleIntent

    }

    public void viewFileSystem(MenuItem item){
        try{
            Intent k = new Intent(MainActivity.this, FTPSync.class);
            startActivityForResult(k,1);
        }catch (Exception ex){
            Log.getStackTraceString(ex);
        }
    }
    public void viewFollowedDirs(MenuItem item){
        try{
            Intent k = new Intent(MainActivity.this, ListFollowedDirs.class);
            startActivityForResult(k,1);
        }catch (Exception ex){
            Log.getStackTraceString(ex);
        }
    }
    public void viewRules(MenuItem item){
        try{
            Intent k = new Intent(MainActivity.this, Rules.class);
            startActivityForResult(k,1);
        }catch (Exception ex){
            Log.getStackTraceString(ex);
        }
    }

    public void scanDirectories(MenuItem item){
        testFtpConnect(new Callback() {
            @Override
            public void callback() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        scanDirectoriesOnDemand();
                    }
                });
            }
        });
    }
    private int sendFile(PathDetails pd) {
        if(ftpnode == null){
            ftpnode = Utils.getInstance(context).getFTPSettings();
        }
        if(ftpnode == null){
            return -1;
        }
        try{
            Intent mServiceIntent = new Intent(context, FTPAPI.class);
            mServiceIntent.putExtra("full-path",pd.getFullpath());
            String calculatedPath = pd.genPathRelativeToDepth();
            if(calculatedPath == null){
                Log.v("SendFile","The file is ignored:"+pd.getFullpath());
                return -3;
            }
            mServiceIntent.putExtra("dst-dir",ftpnode.getDefaultPath() + "/"
                    + calculatedPath);
            context.startService(mServiceIntent);

        }catch (Exception e){
            e.printStackTrace();
            return -2;
        }
        return 0;
    }

    private void scanDirectoriesOnDemand(){
        ftpnode = Utils.getInstance(context).getFTPSettings();
        if(isFtpSettingsCorrect == false){
            return;
        }
        shouldStartRotatingIcon(true);
        if (totalFilesShouldBeSent !=0 && totalHandled < totalFilesShouldBeSent){
            Utils.getInstance(context).showAlert(
                    "Sync process already running. please wait until it finishes",
                    "Second request",
                    false);
        }
        String following_paths = Utils.getInstance(context).getConfigString("following_paths");
        JSONArray ja;
        try {
            ja = new JSONArray(following_paths);
        } catch (JSONException e) {
            e.printStackTrace();
            ja = new JSONArray();
        }
        totalFilesShouldBeSent = 0;
        totalFilesAlreadySent = 0;
        totalHandled = 0;
        for(int i=0;i<ja.length();i++){
            try {
                JSONObject jo = ja.getJSONObject(i);
                String status = jo.getString("status");
                if(Constants.FOLLOWING_DIR.equals(status)){
                    ArrayList<PathDetails> pda = Utils.FileSysAPI.getFoldersRecursive(jo.getString("path"));
                    for (final PathDetails pd: pda) {
                        if(     pd.isDirectory() ||
                                Utils.getInstance(context).getPathStatus(pd.getFullpath()).equals(Constants.STATUS_SENT)||
                                (
                                        Utils.getInstance(context).getPathStatus(pd.getFullpath()).equals(Constants.STATUS_SENDING) ||
                                                Utils.getInstance(context).getPathStatus(pd.getFullpath()).equals(Constants.STATUS_CONNECTING)
                                ) && ! isTimeout(jo.getLong("date"))){
                            continue;
                        }else{
                            int res = sendFile(pd);
                            if(res == -1){
                                Utils.getInstance(context).showAlert(
                                        "Please go to Settings and configure your FTP",
                                        "No FTP Configured",
                                        false);
                                shouldStartRotatingIcon(false);
                                return;
                            }
                            if(res == 0){
                                totalFilesShouldBeSent++;
                            }
                        }
                    }

                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if(totalFilesShouldBeSent == 0){
            shouldStartRotatingIcon(false);
        }

    }

    private boolean isTimeout(Long epochtime) {
        long diff = Math.abs(epochtime - new Date().getTime());
        long diffDays = diff / (2*24 * 60 * 60 * 1000);
        return diffDays  >= 1;
    }



}
