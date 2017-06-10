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
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.ActionMenuItemView;
import android.support.v7.widget.Toolbar;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.apps.exhesham.autoftpsync.utils.Constants;
import com.apps.exhesham.autoftpsync.utils.PathDetails;
import com.apps.exhesham.autoftpsync.utils.NFSSettingsNode;
import com.apps.exhesham.autoftpsync.utils.NfsAPI;
import com.apps.exhesham.autoftpsync.utils.Utils;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {


    static int totalFilesShouldBeSent =0;
    static int totalFilesAlreadySent =0;
    static int totalHandled =0;
    private Context context;

    static NFSSettingsNode nfs_settings;
    HashMap<String,Integer> total = new HashMap<>();
    HashMap<String,Integer> total_sent = new HashMap<>();

    /*
    * All Icons are taken from https://www.iconfinder.com/iconsets/circle-icons-1
    * */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set public params
        context = this;

        nfs_settings = Utils.getInstance(context).getSMBSettings();


        initializeUI();

        // start
        resetCounters();
        testSmbConnect(null);
        setDefaultPaths();
//        loadCategoriesContent();
    }

    private void scanDirectoriesOnDemand(){
        ArraySet<PathDetails> filesToSend = new ArraySet<>();
        nfs_settings = Utils.getInstance(context).getSMBSettings();
        if(nfs_settings == null){
            showSnackBar("Network Settings Incorrect", "Fix", new MyHelpListener(), false);
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                shouldStartRotatingIcon(true);
            }
        });
        //
        //-loadCategoriesContent();
        if (totalFilesShouldBeSent !=0 && totalHandled < totalFilesShouldBeSent){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Utils.getInstance(context).showAlert(
                            "Sync process already running. please wait until it finishes",
                            "Second request",
                            false);
                }
            });
            return;
        }

        JSONArray ja = Utils.getInstance(context).getJsonArrayFromDB(Constants.DB_FOLLOWED_DIRS);

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
                            filesToSend.add(pd);
                        }
                    }

                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        sendFiles(filesToSend);
        if(totalFilesShouldBeSent == 0){
            shouldStartRotatingIcon(false);
        }

    }
    private int sendFiles(ArraySet<PathDetails> pdArr) {
        final ArrayList<CharSequence> fullSrcPaths = new ArrayList<>();
        final ArrayList<CharSequence> fullDstPaths = new ArrayList<>();
        boolean useSmbAsDefault = ! "false".equals(Utils.getInstance(context).getConfigString("use-ftp-as-default"));
        nfs_settings = nfs_settings == null?Utils.getInstance(context).getSMBSettings():nfs_settings;
        if(nfs_settings == null){
            return -1;
        }

        for(PathDetails pd : pdArr){

            String calculatedPath = pd.genPathRelativeToDepth();
            if(calculatedPath == null){
                Log.v("SendFile","The file is ignored:"+pd.getFullpath());
                continue;
            }
            fullSrcPaths.add(pd.getFullpath());
            fullDstPaths.add(nfs_settings.getRootPath() + "/" + calculatedPath);
            totalFilesShouldBeSent++;
        }
        try{
            new Thread() {
                @Override
                public void run() {
                    sendWithSmbProtocol(fullSrcPaths, fullDstPaths);
                }
            }.start();


        }catch (Exception e){
            e.printStackTrace();
            return -2;
        }
        return 0;
    }
    private void updateStatus(String filepath,String status) {

        Intent localIntent = new Intent(Constants.BROADCAST_ACTION).putExtra(filepath, status);
        // Broadcasts the Intent to receivers in this app.
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }
    private void sendWithSmbProtocol(ArrayList<CharSequence> fileSrcPaths, ArrayList<CharSequence> fileDstPaths) {
        try
        {

            if (Utils.getInstance(context).validateSmbCredintials(nfs_settings))
            {
                for(int i = 0; i<fileSrcPaths.size();i++) {
                    totalHandled++;
                    boolean isResultSuccess = false;
                    String dstfolder = fileDstPaths.get(i).toString();
                    String filepath = fileSrcPaths.get(i).toString();
                    Log.v("onHandleIntent", "Will cd to path " + dstfolder);

                    String dstfilePath = dstfolder+"/"+ FilenameUtils.getName(filepath);
                    try {

                        new NfsAPI(nfs_settings).nfsCreateDirectoryTree(dstfolder);
                        Log.v("onHandleIntent", "navigating to dir passed successfully!");


                        isResultSuccess = new NfsAPI(nfs_settings).uploadFile(filepath, dstfilePath, false);
                    }catch (Exception ex){
                        updateStatus(filepath, Constants.STATUS_SENDING_FAILED);
                        continue;
                    }
                    if (isResultSuccess) {
                        updateStatus(filepath, Constants.STATUS_SENT);
                        totalFilesAlreadySent++;
                        Thread.sleep(10);
//                        uploadFailing = false;
                    } else {
                        updateStatus(filepath, Constants.STATUS_SENDING_FAILED);
//                        uploadFailing = true;
                    }

                }
            }else{
                for(int i = 0; i<fileSrcPaths.size();i++) {
                    String filepath = fileSrcPaths.get(i).toString();
                    updateStatus(filepath, Constants.STATUS_FAILED_LOGIN);
                    totalHandled++;
                }
            }
        }
        catch (Exception e)
        {
            for(int i = 0; i<fileSrcPaths.size();i++) {
                String filepath = fileSrcPaths.get(i).toString();
                updateStatus(filepath,Constants.STATUS_FAILED_CONNECTING);
                totalHandled++;
            }

            e.printStackTrace();
//                uploadFailing = true;
        }
    }


//    public class SmbSenderServiceAPI extends IntentService {
//        public SmbSenderServiceAPI() {
//            super("ReminderService");
//        }
//
//        private void updateStatus(String filepath,String status){
//
//            Intent localIntent = new Intent(Constants.BROADCAST_ACTION).putExtra(filepath, status);
//            // Broadcasts the Intent to receivers in this app.
//            LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
//        }
//
//
//        @Override
//        protected void onHandleIntent(Intent workIntent)  {
//            final  ArrayList<CharSequence> fileSrcPaths = workIntent.getCharSequenceArrayListExtra("full-src-paths");
//            final  ArrayList<CharSequence> fileDstPaths = workIntent.getCharSequenceArrayListExtra("full-dst-paths");
//            sendWithSmbProtocol(fileSrcPaths,fileDstPaths);
//
//        } // onHandleIntent
//
//
//
//    }

    private void loadCategoriesContent() {
//        new Thread() {
//            public void run() {
        //Show loading...
        final ProgressDialog progressDialog = ProgressDialog.show(context, "Loading Categories", "Please wait.");
        resetCounters();
        new Thread() {

            @Override
            public void run() {

                JSONArray ja = Utils.getInstance(context).getJsonArrayFromDB(Constants.DB_FOLLOWED_DIRS);
                for (int i = 0; i < ja.length(); i++) {
                    try {
                        JSONObject jo = ja.getJSONObject(i);
                        String status = jo.getString("status");
                        if (!Constants.FOLLOWING_DIR.equals(status)) {
                            continue;
                        }
                        String pathname = jo.getString("path");
                        Long lastUpdate = jo.getLong("date");
                        ArrayList<PathDetails> pda = Utils.FileSysAPI.getFoldersRecursive(pathname);
                        if(pda == null){
                            continue;
                        }
                        for (final PathDetails pd : pda) {
                            try {
                                String categoryName = Utils.FileSysAPI.getFileCategory(pd.getFullpath());
                                if (null == categoryName) {
                                    continue;
                                }
                                total.put(categoryName, total.get(categoryName) + 1);
                                int total_sent_ctr = total_sent.get(categoryName);
                                boolean shouldInc = Utils.getInstance(context).getPathStatus(pd.getFullpath()).equals(Constants.STATUS_SENT);
                                if(!shouldInc)
                                    Log.v("loadCategoriesContent", "shouldInc for file "  +pd.getFullpath()+ " is " +(shouldInc?"True":"False"));

                                total_sent.put(categoryName, shouldInc ? total_sent_ctr + 1 : total_sent_ctr);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                progressDialog.dismiss();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateCounters();
                    }
                });

            }
        }.start();


//            }
//        }.start();
    }


    public void refreshCategory(MenuItem item){
        loadCategoriesContent();
    }
    public void followCategory(View view) {
        ImageView iv = (ImageView)view;
        boolean isFollowing = Utils.getInstance(context).convertCategoryStateInDB(iv.getTag().toString());
        Drawable followIcon = getResources().getDrawable(R.drawable.follow, null);
        Drawable unfollowIcon = getResources().getDrawable(R.drawable.unfollow, null);
        if(isFollowing){
            iv.setImageDrawable(followIcon);
        }else{
            iv.setImageDrawable(unfollowIcon);
        }
        //loadCategoriesContent();

    }

    interface Callback {
        void callback(); // would be in any signature
    }


    private void testSmbConnect(final Callback successCallback) {
        nfs_settings = Utils.getInstance(context).getSMBSettings();
        if(nfs_settings == null){
            showSnackBar("Please Configure Network Storage Settings", "Config", new MyHelpListener(), true);
            return;
        }
        final ProgressDialog progressDialog = ProgressDialog.show(this, "Checking Network Storage Settings", "Please wait. Checking network connectivity and settings correctness...");
        new Thread() {
            public void run() {
                if(! Utils.getInstance(context).validateSmbCredintials(nfs_settings)){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Utils.getInstance(context).showAlert(
                                    "The network settings is not correct",
                                    "Network Configuration Error",
                                    false);
                        }
                    });
                }else{
                    if(successCallback != null){
                        successCallback.callback();
                    }
                }

                progressDialog.dismiss();



            }
        }.start();
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
                startNotification("Failed to upload all "+Integer.toString(totalFilesShouldBeSent) +  " Files! Check Network Configuration or connectivity");
                shouldStartRotatingIcon(false);
            }
            while (iter.hasNext()) {
                String filename = iter.next();
                String status = b.getString(filename);
                String previous_status = Utils.getInstance(context).getPathStatus(filename);
                Utils.getInstance(context).storeConfigString(filename, Utils.getInstance(context).generateJsonStatus(status,filename,false).toString());
                if(Constants.STATUS_SENT.equals(status)){
                    //Update Counters
                    String categoryName = Utils.FileSysAPI.getFileCategory(filename);
                    /*i added !status.equals(previous_status) in order to ban updating the counter twice on a resent file*/
                    if(null != categoryName && !status.equals(previous_status)) {
                        int total_sent_ctr = total_sent.get(categoryName);
                        total_sent.put(categoryName, Utils.getInstance(context).getPathStatus(filename).equals(Constants.STATUS_SENT) ? total_sent_ctr + 1 : total_sent_ctr);
                    }
                }
            }
            updateCounters();
        }
    }
    private void shouldStartRotatingIcon(final boolean start){
        final int color = !start ? Color.parseColor("#FFF1F9"): Color.parseColor("#00E506");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final PorterDuffColorFilter colorFilter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                ActionMenuItemView iv = (ActionMenuItemView )findViewById(R.id.follow_status);
                iv.getCompoundDrawables()[0].setColorFilter(colorFilter);
            }
        });

    }

    public void viewFileSystem(MenuItem item){
        try{
            Intent k = new Intent(MainActivity.this, DeviceFileSystemActivity.class);
            startActivityForResult(k,1);
        }catch (Exception ex){
            Log.getStackTraceString(ex);
        }
    }

    public void viewSharedFS(MenuItem item){
        try{
            Intent k = new Intent(MainActivity.this, NfsSettingsActivity.class);
            startActivityForResult(k,1);
        }catch (Exception ex){
            Log.getStackTraceString(ex);
        }
    }
    public void viewFollowedDirs(MenuItem item){
        try{
            Intent k = new Intent(MainActivity.this, ListFollowedDirsActivity.class);
            startActivityForResult(k,1);
        }catch (Exception ex){
            Log.getStackTraceString(ex);
        }
    }
    public void viewRules(MenuItem item){
        try{
            Intent k = new Intent(MainActivity.this, RulesActivity.class);
            startActivityForResult(k,1);
        }catch (Exception ex){
            Log.getStackTraceString(ex);
        }
    }

    public void scanDirectories(MenuItem item){
        testSmbConnect(new Callback() {
            @Override
            public void callback() {
                new Thread() {
                    @Override
                    public void run() {
                        scanDirectoriesOnDemand();
                    }
                }.start();
            }
        });
    }


    private void setCategoriesStatus(){
        JSONObject jo = Utils.getInstance(context).getJsonObjFromDB("categories");
        if(jo == null){
            /* the categories are not in the database - create them with default values */
            jo = new JSONObject();
            try {
                jo.put("music", new JSONObject().put("status",Constants.FOLLOWING_DIR).put("name","Music"));
                jo.put("videos", new JSONObject().put("status",Constants.FOLLOWING_DIR).put("name","Videos"));
                jo.put("photos", new JSONObject().put("status",Constants.FOLLOWING_DIR).put("name","Photos"));
                jo.put("recordings", new JSONObject().put("status",Constants.FOLLOWING_DIR).put("name","Recordings"));
                jo.put("compressed", new JSONObject().put("status",Constants.FOLLOWING_DIR).put("name","Compressed"));
                jo.put("documents", new JSONObject().put("status",Constants.FOLLOWING_DIR).put("name","Documents"));
                jo.put("apps", new JSONObject().put("status",Constants.FOLLOWING_DIR).put("name","Apps"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Utils.getInstance(context).storeConfigString("categories",jo.toString());
        }
        try {
            Drawable followIcon = getResources().getDrawable(R.drawable.follow, null);
            Drawable unfollowIcon = getResources().getDrawable(R.drawable.unfollow, null);
            ImageView iv = (ImageView)findViewById(R.id.music_status);
            if(jo.getJSONObject("music").getString("status").equals(Constants.FOLLOWING_DIR)){
                iv.setImageDrawable(followIcon);
            }else{
                iv.setImageDrawable(unfollowIcon);
            }
            iv = (ImageView)findViewById(R.id.video_status);
            if(jo.getJSONObject("videos").getString("status").equals(Constants.FOLLOWING_DIR)){
                iv.setImageDrawable(followIcon);
            }else{
                iv.setImageDrawable(unfollowIcon);
            }
            iv = (ImageView)findViewById(R.id.photos_status);
            if(jo.getJSONObject("photos").getString("status").equals(Constants.FOLLOWING_DIR)){
                iv.setImageDrawable(followIcon);
            }else{
                iv.setImageDrawable(unfollowIcon);
            }
            iv = (ImageView)findViewById(R.id.recordings_status);
            if(jo.getJSONObject("recordings").getString("status").equals(Constants.FOLLOWING_DIR)){
                iv.setImageDrawable(followIcon);
            }else{
                iv.setImageDrawable(unfollowIcon);
            }
            iv = (ImageView)findViewById(R.id.compressed_status);
            if(jo.getJSONObject("compressed").getString("status").equals(Constants.FOLLOWING_DIR)){
                iv.setImageDrawable(followIcon);
            }else{
                iv.setImageDrawable(unfollowIcon);
            }
            iv = (ImageView)findViewById(R.id.documents_status);
            if(jo.getJSONObject("documents").getString("status").equals(Constants.FOLLOWING_DIR)){
                iv.setImageDrawable(followIcon);
            }else{
                iv.setImageDrawable(unfollowIcon);
            }
            iv = (ImageView)findViewById(R.id.apk_status);
            if(jo.getJSONObject("apps").getString("status").equals(Constants.FOLLOWING_DIR)){
                iv.setImageDrawable(followIcon);
            }else{
                iv.setImageDrawable(unfollowIcon);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }


    }


    private boolean isTimeout(Long epochtime) {
        long diff = Math.abs(epochtime - new Date().getTime());
        long diffDays = diff / Constants.DEFAULT_SENDING_TIMEOUT_MS;
        return diffDays  >= 1;
    }
    private void updateCounters() {

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

        TextView apk_total_files = (TextView) findViewById(R.id.apk_total_files);
        TextView apk_total_sent_files = (TextView) findViewById(R.id.apk_total_synced_files);
        apk_total_files.setText("Total: " + total.get(Constants.APPS_CATERGORY_NAME));
        apk_total_sent_files.setText("Synced: " + total_sent.get(Constants.APPS_CATERGORY_NAME));

        setCategoriesStatus();
    }
    public void viewNfsSettings(MenuItem item) {
        try{
            Intent k = new Intent(MainActivity.this, NfsSettingsActivity.class);
            startActivityForResult(k,1);
        }catch (Exception ex){
            Log.getStackTraceString(ex);
        }
    }
    public void help(MenuItem item) {
        try{
            Intent k = new Intent(MainActivity.this, HelpActivity.class);
            startActivity(k);
        }catch (Exception ex){
            Log.getStackTraceString(ex);
        }
    }

    private void setDefaultPaths(){
        JSONArray ja = Utils.getInstance(context).getJsonArrayFromDB(Constants.DB_FOLLOWED_DIRS);
        ArrayMap<String,Boolean> am = new ArrayMap<>();

        String version =  Utils.getInstance(context).getConfigString("version");
        if(version == null ){
            /*If the version is not identified then for sure it is not version 3, then reformat the available data*/
            ja = new JSONArray();
            Utils.getInstance(context).storeConfigString("version",Constants.VERSION);
        }
        for(int i=0;i<ja.length();i++){
            try{
                am.put(ja.getJSONObject(i).getString("path"),true);
            }catch (Exception e){}
        }

        // use smb as the default
        if(Utils.getInstance(context).getConfigString("use-smb-as-default") == null){
            Utils.getInstance(context).storeConfigString("use-smb-as-default", "true");
        }

        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
        if(!am.containsKey(path)){
            JSONObject pathNode = Utils.getInstance(context).generateJsonStatus(Constants.FOLLOWING_DIR, path, true);
            ja.put(pathNode);
            Utils.getInstance(context).storeConfigString(path, pathNode.toString());
        }
        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
        if(!am.containsKey(path)){
            JSONObject pathNode = Utils.getInstance(context).generateJsonStatus(Constants.FOLLOWING_DIR, path, true);
            ja.put(pathNode);
            Utils.getInstance(context).storeConfigString(path, pathNode.toString());

        }
        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        if(!am.containsKey(path)){
            JSONObject pathNode = Utils.getInstance(context).generateJsonStatus(Constants.FOLLOWING_DIR, path, true);
            ja.put(pathNode);
            Utils.getInstance(context).storeConfigString(path, pathNode.toString());
        }
        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath();
        if(!am.containsKey(path)){
            JSONObject pathNode = Utils.getInstance(context).generateJsonStatus(Constants.FOLLOWING_DIR, path, true);
            ja.put(pathNode);
            Utils.getInstance(context).storeConfigString(path, pathNode.toString());
        }
        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
        if(!am.containsKey(path)){
            JSONObject pathNode = Utils.getInstance(context).generateJsonStatus(Constants.FOLLOWING_DIR, path, true);
            ja.put(pathNode);
            Utils.getInstance(context).storeConfigString(path, pathNode.toString());
        }
        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS).getAbsolutePath();
        if(!am.containsKey(path)){
            JSONObject pathNode = Utils.getInstance(context).generateJsonStatus(Constants.FOLLOWING_DIR, path, true);
            ja.put(pathNode);
            Utils.getInstance(context).storeConfigString(path, pathNode.toString());
        }
        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
        if(!am.containsKey(path)){
            JSONObject pathNode = Utils.getInstance(context).generateJsonStatus(Constants.FOLLOWING_DIR, path, true);
            ja.put(pathNode);
            Utils.getInstance(context).storeConfigString(path, pathNode.toString());
        }
        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS).getAbsolutePath();
        if(!am.containsKey(path)){
            JSONObject pathNode = Utils.getInstance(context).generateJsonStatus(Constants.FOLLOWING_DIR, path, true);
            ja.put(pathNode);
            Utils.getInstance(context).storeConfigString(path, pathNode.toString());
        }
        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES).getAbsolutePath();
        if(!am.containsKey(path)){
            JSONObject pathNode = Utils.getInstance(context).generateJsonStatus(Constants.FOLLOWING_DIR, path, true);
            ja.put(pathNode);
            Utils.getInstance(context).storeConfigString(path, pathNode.toString());
        }
        Utils.getInstance(context).storeConfigString(Constants.DB_FOLLOWED_DIRS,ja.toString());
    }

    private  void initializeUI(){
        // init toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // init ui
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
    }
    private void resetCounters() {
        total.put(Constants.COMPRESSED_CATERGORY_NAME,0);
        total.put(Constants.VIDEO_CATERGORY_NAME,0);
        total.put(Constants.PHOTOS_CATERGORY_NAME,0);
        total.put(Constants.DOCUMENTS_CATERGORY_NAME,0);
        total.put(Constants.MUSIC_CATERGORY_NAME,0);
        total.put(Constants.RECORDINGS_CATERGORY_NAME,0);
        total.put(Constants.APPS_CATERGORY_NAME,0);

        total_sent.put(Constants.COMPRESSED_CATERGORY_NAME,0);
        total_sent.put(Constants.VIDEO_CATERGORY_NAME,0);
        total_sent.put(Constants.PHOTOS_CATERGORY_NAME,0);
        total_sent.put(Constants.DOCUMENTS_CATERGORY_NAME,0);
        total_sent.put(Constants.MUSIC_CATERGORY_NAME,0);
        total_sent.put(Constants.RECORDINGS_CATERGORY_NAME,0);
        total_sent.put(Constants.APPS_CATERGORY_NAME,0);
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

        if (id == R.id.file_sys_browse) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        }  else if (id == R.id.shared_fs_scan) {

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
                nfs_settings = Utils.getInstance(context).getSMBSettings();

            }
        }
    }
    public class MyHelpListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            try{
                Intent k = new Intent(MainActivity.this, NfsSettingsActivity.class);
                startActivityForResult(k,1);
            }catch (Exception ex){
                Log.getStackTraceString(ex);
            }
        }
    }
    private void showSnackBar(String msg, String undo, View.OnClickListener listener, boolean indifinite){
        //TODO: Move to utils
        Snackbar mySnackbar;
        if(indifinite){
            mySnackbar = Snackbar.make(findViewById(R.id.main_activity_coord_layout),
                    msg, Snackbar.LENGTH_INDEFINITE);
        }else{
            mySnackbar = Snackbar.make(findViewById(R.id.main_activity_coord_layout),
                    msg, Snackbar.LENGTH_LONG);
        }

        TextView tv = (TextView) mySnackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        tv.setTextColor(Color.WHITE);
        mySnackbar.setAction(undo, listener);
        mySnackbar.show();
    }

}
