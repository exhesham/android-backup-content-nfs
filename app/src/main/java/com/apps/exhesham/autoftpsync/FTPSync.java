package com.apps.exhesham.autoftpsync;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
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
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import android.support.v7.view.menu.ActionMenuItemView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.apps.exhesham.autoftpsync.utils.FTPNode;
import com.apps.exhesham.autoftpsync.utils.PathDetails;
import com.apps.exhesham.autoftpsync.utils.Utils;

import org.apache.commons.io.FilenameUtils;
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
import java.util.Iterator;
import java.util.Set;


@SuppressLint("NewApi")
public class FTPSync extends AppCompatActivity {
    private static final  int MY_PERMISSIONS_REQUEST_READ_AND_WRITE_SDK = 1 ;
    private String currPath = "/storage/emulated/0";
    private Context context;
    static FTPNode ftpnode;
    static int totalFilesShouldBeSent =0;
    static int totalFilesAlreadySent =0;
    static int totalHandled =0;
    static boolean isFtpSettingsCorrect = false;
//    static boolean uploadFailing = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        ftpnode = Utils.getInstance(context).getFTPSettings();

        setContentView( R.layout.activity_ftpsync);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

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


    }

    private void startNotification(String notificationContent) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle("Phone to FTP")
                        .setContentText(notificationContent);
// Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, FTPSync.class);

// The stack builder object will contain an artificial back stack for the
// started Activity.
// This ensures that navigating backward from the Activity leads out of
// your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
// Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(FTPSync.class);
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

            ActivityCompat.requestPermissions((FTPSync) this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.INTERNET,
                            Manifest.permission.ACCESS_NETWORK_STATE,
                            Manifest.permission.ACCESS_WIFI_STATE},
                    MY_PERMISSIONS_REQUEST_READ_AND_WRITE_SDK);
        } else {
            fillTable("/storage/emulated/0");
            scanDirectories(null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_AND_WRITE_SDK:

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    fillTable("/storage/emulated/0");
                    scanDirectories(null);
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.maintoolbar, menu);
        return true;
    }
    public void backfolder(View v){
        if( "/".equals(currPath)){
            return;
        }else{
            String prevPath = currPath.substring(0,currPath.replaceAll("/$", "").lastIndexOf('/')+1);
            Log.d("onBackPressed","will back to directory:" + prevPath);
            fillTable(prevPath);
        }
    }
    @Override
    public void onBackPressed() {
        if( "/".equals(currPath)){
            super.onBackPressed();
        }else{
            backfolder(null);
        }
    }
    private static boolean fill_table_lock = false;
    private void fillTable(String path){
        if(fill_table_lock){
            return;
        }
        fill_table_lock = true;
        Log.v("fillTable","Called with path:"+path);
        ArrayList<PathDetails> pda = Utils.FileSysAPI.getFolders(path);
        currPath = path;
        TextView text_currpath = (TextView) findViewById(R.id.text_currpath);
        text_currpath.setText(currPath);
        if(pda == null){
            Log.e("fillTable","Error: fillTable received null pda");
            fill_table_lock = false;
            return;
        }
        updateToolbarSyncButton(path);
        final TableLayout ll = (TableLayout) findViewById(R.id.table_files);
        ll.removeAllViews();
        int i=0;
        for (final PathDetails pd: pda) {
            final TableRow row= new TableRow(this);
            TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
            row.setLayoutParams(lp);
//            CheckBox checkBox = new CheckBox(this);
            TextView tv = new TextView(this);
            tv.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.7f));
            tv.setText(pd.getFilename());
            String filestatus = getPathStatus(pd.getFullpath());
            ImageView statusImg = new ImageView(this);

            ImageView fileTypeBtn = new ImageView(this);
            if(pd.isDirectory()){
                fileTypeBtn.setImageResource(R.drawable.folder);
            }else{
                fileTypeBtn.setImageResource(R.drawable.file);
                statusImg.setImageResource(mapDrawableToStatus(filestatus));
            }
            row.addView(fileTypeBtn);
//            row.addView(checkBox);
            row.addView(tv);
            row.addView(statusImg);
            row.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick( View v ) {
                    if(pd.isDirectory()) {

                        fillTable(pd.getFullpath());
                    }else {
                        sendFile(pd);
                    }
                }
            } );
            ll.addView(row,i++);
        }
        fill_table_lock = false;
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

    public String getPathStatus(String path) {
        if(path.endsWith("/")){
            path = path.substring(0, path.length()-1);
        }
        String following_path = Utils.getInstance(context).getConfigString(path);
        if(following_path == null || following_path.equals("")){
            return Constants.STATUS_NOTHING;
        }
        JSONObject jo;
        try {
            jo = new JSONObject(following_path);
            return jo.getString("status");
        } catch (JSONException e) {
            e.printStackTrace();
            return Constants.STATUS_NOTHING;
        }
    }

    private void updateToolbarSyncButton(String path) {
        //TODO:Should check if parent path is synced

//        String status = shouldDirBeFollowed(path);
        String status = getPathStatus(path);

        ActionMenuItemView iv = (ActionMenuItemView )findViewById(R.id.toolbtn_is_following);
        if (iv == null){
            Log.e("updateToolbarSyncButton","cannot find toolbtn_is_following!");
            return;
        }
        Drawable myIcon = getResources().getDrawable( R.drawable.unfollow ,null);
        try {


            if (status.equals(Constants.FOLLOWING_DIR)) {
                myIcon = getResources().getDrawable(R.drawable.follow, null);
                iv.setTitle(Constants.FOLLOWING_DIR);
            } else {

                iv.setTitle(Constants.NOT_FOLLOWING_DIR);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        iv.setIcon(myIcon);
    }

    private String shouldDirBeFollowed(String path) {
        String following_path = Utils.getInstance(context).getConfigString(path);
        String curr_path = path;

        while(curr_path !="/" && (following_path == null || following_path.equals(""))){
            following_path = Utils.getInstance(context).getConfigString(path);
        }
        if(following_path != null &&!following_path.equals("")) {
            JSONObject jo;
            try {
                jo = new JSONObject(following_path);
                return jo.getString("status");
            } catch (JSONException e) {
                e.printStackTrace();
                return Constants.STATUS_NOTHING;
            }
        }
        return Constants.STATUS_NOTHING;
    }

    private int mapDrawableToStatus(String filestatus) {
        if(filestatus.equals(Constants.STATUS_CONNECTING)){
            return  R.drawable.connecting;
        }
        if(filestatus.equals(Constants.STATUS_SENT)){
            return  R.drawable.sent;
        }
        if(filestatus.equals(Constants.STATUS_SENDING)){
            return  R.drawable.sending;
        }
        if(filestatus.equals(Constants.STATUS_FAILED_LOGIN)){
            return  R.drawable.sending_failed;
        }
        if(filestatus.equals(Constants.STATUS_FAILED_CONNECTING)){
            return  R.drawable.sending_failed;
        }
        if(filestatus.equals(Constants.STATUS_SENDING_FAILED)){
            return  R.drawable.sending_failed;
        }
        return  R.drawable.no_sync;
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case (1) : {
                ftpnode = Utils.getInstance(context).getFTPSettings();
                testFtpConnect();
            }
        }
    }

    private void testFtpConnect() {
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
            return;
        }
        final ProgressDialog progressDialog = ProgressDialog.show(this, "Checking FTP Settings", "Please wait. Checking ftp connectivity and settings correctness...");
        new Thread() {
            public void run() {
                try  {

                    try
                    {
                        final FTPClient con = new FTPClient();
                        con.setConnectTimeout(5000);
                        con.setDefaultTimeout(5000);
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
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Utils.getInstance(context).showAlert(
                                        "faulty connectivity...Failed to connect to server because ",
                                        "FTP Configuration error",
                                        false);
                            }
                        });

                        isFtpSettingsCorrect = false;
                    }
                    isFtpSettingsCorrect = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    isFtpSettingsCorrect = false;
                }
                // dismiss the progress dialog
                progressDialog.dismiss();
            }
        }.start();

//        Thread thread = new Thread(new Runnable() {
//
//            @Override
//            public void run() {
//
//
//            }
//        });
//        thread.start();
    }

    public void viewSettings(MenuItem item) {
        try{
            Intent k = new Intent(FTPSync.this, FTPSettings.class);
            startActivityForResult(k,1);
        }catch (Exception ex){
            Log.getStackTraceString(ex);
        }
    }
    public void help(MenuItem item) {
        try{
            Intent k = new Intent(FTPSync.this, Help.class);
            startActivity(k);
        }catch (Exception ex){
            Log.getStackTraceString(ex);
        }
    }

    public void handleFollowStatus(MenuItem item) {
        if(ftpnode == null){
            Utils.getInstance(context).showAlert(
                    "Please go to Settings and configure your FTP",
                    "No FTP Configured",
                    false);
            return;
        }
        String status = getPathStatus(currPath);
        String following_paths = Utils.getInstance(context).getConfigString("following_paths");
        JSONArray ja;
        try {
            ja = new JSONArray(following_paths);
        } catch (JSONException e) {
            e.printStackTrace();
            ja = new JSONArray();
        }
        ActionMenuItemView  iv = (ActionMenuItemView )findViewById(R.id.toolbtn_is_following);
        Drawable myIcon = getResources().getDrawable( R.drawable.follow ,null);
        if(status.equals(Constants.FOLLOWING_DIR)){
            /*if you were following the dir and pressed the button then now you are not following it*/
            myIcon = getResources().getDrawable( R.drawable.unfollow ,null);
            Utils.getInstance(context).storeConfigString(currPath,generateStatus(Constants.NOT_FOLLOWING_DIR,currPath));
            iv.setTitle(Constants.NOT_FOLLOWING_DIR);
            for(int i=0;i<ja.length();i++){
                try {
                    if(ja.getJSONObject(i).getString("path").equals(currPath)){
                        ja.remove(i);
                        break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }else{
            iv.setTitle(Constants.FOLLOWING_DIR);
            Utils.getInstance(context).storeConfigString(currPath,
                    generateStatus(Constants.FOLLOWING_DIR, currPath));
            try {
                JSONObject jo = new JSONObject(generateStatus(Constants.FOLLOWING_DIR, currPath));

                ja.put(jo);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        iv.setIcon(myIcon);
        Utils.getInstance(context).storeConfigString("following_paths",ja.toString());


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

    public final class Constants {

        // Defines a custom Intent action
        public static final String BROADCAST_ACTION =
                "com.example.android.threadsample.BROADCAST";
        public static final String STATUS_CONNECTING = "Connecting...";
        public static final String STATUS_SENT = "Sent";
        public static final String STATUS_SENDING = "Sending...";
        public static final String STATUS_FAILED_LOGIN = "Login Failed!";
        public static final String STATUS_FAILED_CONNECTING = "Connecting Failed!";
        public static final String STATUS_SENDING_FAILED = "Sending Failed";
        public static final String FOLLOWING_DIR = "Following Directory";
        public static final String NOT_FOLLOWING_DIR = "Not Following Directory";
        public static final String STATUS_NOTHING = "Nothing";
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
                //TODO: Stop Rotating waiting logo
                shouldStartRotatingIcon(false);
            }
            if(totalFilesShouldBeSent != 0 && totalHandled == totalFilesShouldBeSent && totalFilesAlreadySent == 0){
                startNotification("Failed to upload all "+Integer.toString(totalFilesShouldBeSent) +  " Files! Check FTP Configuration or connectivity");
                //TODO: Stop Rotating waiting logo
                shouldStartRotatingIcon(false);
            }
//            if(uploadFailing){
//                startNotification("Failed to upload. Check FTP directory, space, connectivity");
//            }
            while (iter.hasNext()) {
                String filename = iter.next();
                Utils.getInstance(context).storeConfigString(filename, generateStatus(b.getString(filename),filename));
            }
            fillTable(currPath);
        }
    }
    private void shouldStartRotatingIcon(boolean start){
        ActionMenuItemView iv = (ActionMenuItemView )findViewById(R.id.follow_status);
//            iv.startAnimation(rotation);
        ObjectAnimator imageViewObjectAnimator = ObjectAnimator.ofFloat(iv ,
                "rotation", 0f, 360f);
        imageViewObjectAnimator.setStartDelay(140);
        imageViewObjectAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        imageViewObjectAnimator.setRepeatMode(ObjectAnimator.RESTART);
        imageViewObjectAnimator.setInterpolator(new AccelerateInterpolator());

        if(start){
            iv.setEnabled(false);
            imageViewObjectAnimator.start();
        }else{
            iv.setEnabled(true);
            imageViewObjectAnimator.cancel();

        }
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
//            if(uploadFailing){
//                return;
//            }

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

    public void viewFollowedDirs(MenuItem item){
        try{
            Intent k = new Intent(FTPSync.this, ListFollowedDirs.class);
            startActivityForResult(k,1);
        }catch (Exception ex){
            Log.getStackTraceString(ex);
        }
    }
    public void viewRules(MenuItem item){
        try{
            Intent k = new Intent(FTPSync.this, Rules.class);
            startActivityForResult(k,1);
        }catch (Exception ex){
            Log.getStackTraceString(ex);
        }
    }

    public void scanDirectories(MenuItem item){
        ftpnode = Utils.getInstance(context).getFTPSettings();
//        uploadFailing = false;
        testFtpConnect();
        if(isFtpSettingsCorrect == false){
            return;
        }
        shouldStartRotatingIcon(true);
        //TODO: Start Rotating waiting logo
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
                        if((getPathStatus(pd.getFullpath()).equals(Constants.STATUS_SENDING) ||
                                getPathStatus(pd.getFullpath()).equals(Constants.STATUS_CONNECTING) ||
                                getPathStatus(pd.getFullpath()).equals(Constants.STATUS_SENT) ||
                                pd.isDirectory()) && ! isTimeout(jo.getLong("date"))){
                            continue;
                        }else{
                            int res = sendFile(pd);
                            if(res == -1){
                                Utils.getInstance(context).showAlert(
                                        "Please go to Settings and configure your FTP",
                                        "No FTP Configured",
                                        false);
                                //TODO: Stop Rotating waiting logo
                                shouldStartRotatingIcon(false);
                                return;
                            }
                            if(res == 0){
                                totalFilesShouldBeSent++;
                            }
//                            if(uploadFailing){
//                                Utils.getInstance(context).showAlert(
//                                        "Failed to upload. make sure that the path exists or you have permission to write to this path. also make sure that the ftp disk is not full",
//                                        "Uploading fails",
//                                        false);
//                                return;
//                            }
                        }
                    }

                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    private boolean isTimeout(Long epochtime) {
        long diff = Math.abs(epochtime - new Date().getTime());
        long diffDays = diff / (2*24 * 60 * 60 * 1000);
        return diffDays  >= 1;
    }

}
