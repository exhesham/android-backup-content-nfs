package com.apps.exhesham.autoftpsync;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.ArraySet;
import android.util.Log;
import android.widget.Toast;

import com.apps.exhesham.autoftpsync.utils.Constants;
import com.apps.exhesham.autoftpsync.utils.NFSSettingsNode;
import com.apps.exhesham.autoftpsync.utils.NfsAPI;
import com.apps.exhesham.autoftpsync.utils.PathDetails;
import com.apps.exhesham.autoftpsync.utils.Utils;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


/**
 * Created by hesham on 6/12/2017.
 */

public class UploadFilesService  extends Service {

    static int totalFilesShouldBeSent =0;
    static int totalFilesFailed =0;
    static int totalFilesAlreadySent =0;
    static int totalHandled =0;


    private NotificationManager mNM;

    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = 123545886;

    private static  boolean lock_sending_requests = false;
    public UploadFilesService() {

    }
    public UploadFilesService(IBinder service) {

    }

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        UploadFilesService getService() {
            return UploadFilesService.this;
        }
    }

    @Override
    public void onCreate() {

        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Display a notification about us starting.  We put an icon in the status bar.

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showNotification("Uploading files...", "Upload Status");
        if(lock_sending_requests == true){
            Log.w("onStartCommand", "Will not start another request because lock_sending_requests is true" );

        }
        Log.d("onStartCommand", "Started the service - will get the files to load" );
        Log.i("onStartCommand", "Received start id " + startId + ": " + intent);

        new Thread() {
            public void run() {
                sendFiles();

            }
        }.start();

        return START_NOT_STICKY;
    }
    private boolean isTimeout(Long epochtime) {
        long diff = Math.abs(epochtime - new Date().getTime());
        long diffDays = diff / Constants.DEFAULT_SENDING_TIMEOUT_MS;
        return diffDays  >= 1;
    }

    /***
     * This is the function that receive the files need to be uploaded and upload them
     * the function is part of the service
     * @return
     */

    private int sendFiles() {
        lock_sending_requests = true;
        ArraySet<PathDetails> filesToSend = new ArraySet<>();
        JSONArray ja = Utils.getInstance(this).getJsonArrayFromDB(Constants.DB_FOLLOWED_DIRS);
        totalFilesShouldBeSent = 0;
        totalFilesAlreadySent = 0;
        totalFilesFailed = 0;
        totalHandled = 0;
        for(int i=0;i<ja.length();i++){
            try {
                JSONObject jo = ja.getJSONObject(i);
                String status = jo.getString("status");
                if(Constants.FOLLOWING_DIR.equals(status)){
                    ArrayList<PathDetails> pda = Utils.FileSysAPI.getFoldersRecursive(jo.getString("path"));

                    for (final PathDetails pd: pda) {
                        if(     pd.isDirectory() ||
                                Utils.getInstance(this).getPathStatus(pd.getFullpath()).equals(Constants.STATUS_SENT)||
                                (
                                        Utils.getInstance(this).getPathStatus(pd.getFullpath()).equals(Constants.STATUS_SENDING) ||
                                                Utils.getInstance(this).getPathStatus(pd.getFullpath()).equals(Constants.STATUS_CONNECTING)
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
        final ArrayList<CharSequence> fullSrcPaths = new ArrayList<>();
        final ArrayList<CharSequence> fullDstPaths = new ArrayList<>();

        NFSSettingsNode nfs_settings = Utils.getInstance(this).getSMBSettings();
        showNotification("Initializing " +filesToSend.size()  + " Files...","Upload Status");

        for(PathDetails pd : filesToSend){

            String calculatedPath = pd.genPathRelativeToDepth();
            if(calculatedPath == null){
                Log.v("SendFile","The file is ignored:"+pd.getFullpath());
                continue;
            }
            fullSrcPaths.add(pd.getFullpath());
            fullDstPaths.add(nfs_settings.getSelectedRootPath() + "/" + calculatedPath);
            totalFilesShouldBeSent++;
        }
        if(fullSrcPaths.size() == 0){
            showNotification("All relevant files already synced!", "Upload Status");
            lock_sending_requests = false;
            return 0;
        }

        try{
            sendWithSmbProtocol(fullSrcPaths, fullDstPaths);

        }catch (Exception e){
            e.printStackTrace();
            lock_sending_requests = false;
            return -2;
        }
        lock_sending_requests = false;
        return 0;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(NOTIFICATION);

        // Tell the user we stopped.
        Toast.makeText(this, "Upload was stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    /**
     * Show a notification while this service is running.
     */
    private void showNotification(String notificationMsg, String notificationTitle) {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        Log.d("showNotification", "Will show the notification " + notificationMsg);
        CharSequence text = notificationMsg;

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent;
        contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.sync_now)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(notificationTitle)  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();

        // Send the notification.
        mNM.notify(NOTIFICATION, notification);
    }

    /***
     * This function will handle and decide what to do with a file status
     * @param filename
     * @param status
     */
    private void updateFileStatus(String filename,String dstfolder, String status) {
        Log.d("updateFileStatus", " will update the status for file " + filename);
        Log.d("updateFileStatus", " totalFilesAlreadySent= " + totalFilesAlreadySent+
                " totalFilesShouldBeSent= " + totalFilesShouldBeSent+" totalHandled= " + totalHandled);
        int percent = (int)(Math.ceil((totalHandled * 100.0f) / totalFilesShouldBeSent));
        showNotification("Uploaded:" + Integer.toString(totalFilesAlreadySent) + " Failed:"  + Integer.toString(totalFilesFailed)+ " Total:"+ Integer.toString(totalFilesShouldBeSent) + " To:" + dstfolder,"Upload Status " + percent + "%");

        // need to update the data in the metadata storage in order to update counters accordingly.
        Log.d("updateFileStatus", "Storing the status " + status + " for the file " + filename);
        Utils.getInstance(this).storeConfigString(filename, Utils.getInstance(this).generateJsonStatus(status, filename, false).toString());
        // store the logs
        JSONArray allLogs = Utils.getInstance(this).getJsonArrayFromDB("sync_logs");
        try {
            Date date = Calendar.getInstance().getTime();

            // Display a date in day, month, year format
            DateFormat formatter = new SimpleDateFormat("dd/MM/yy  hh:mm:ss a");
            String today = formatter.format(date);
            allLogs.put(new JSONObject().put("file_name",filename).
                    put("file_status", status).put("dist_folder", dstfolder).put("sync_date", today)
                    );
            Utils.getInstance(this).storeConfigString("sync_logs", allLogs.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Utils.getInstance(this).storeConfigString(filename, Utils.getInstance(this).generateJsonStatus(status, filename, false).toString());


    }
    private void sendWithSmbProtocol(ArrayList<CharSequence> fileSrcPaths, ArrayList<CharSequence> fileDstPaths) {
        try {
            NFSSettingsNode nfs_settings = Utils.getInstance(this).getSMBSettings();
            System.out.println("sendWithSmbProtocol: " + nfs_settings.toString());

            for (int i = 0; i < fileSrcPaths.size(); i++) {
                totalHandled++;
                boolean isResultSuccess;
                String dstfolder = fileDstPaths.get(i).toString();
                String filepath = fileSrcPaths.get(i).toString();
                Log.v("onHandleIntent", "Will cd to path " + dstfolder);

                String dstfilePath = dstfolder + "/" + FilenameUtils.getName(filepath);
                try {

                    new NfsAPI(nfs_settings).nfsCreateDirectoryTree(dstfolder);
                    Log.v("onHandleIntent", "navigating to dir passed successfully!");


                    isResultSuccess = new NfsAPI(nfs_settings).uploadFile(filepath, dstfilePath, false);
                } catch (Exception ex) {
                    Log.e("sendWithSmbProtocol", "Uploading threw an exception:" + ex.getMessage());
                    ex.printStackTrace();
                    totalFilesFailed++;
                    updateFileStatus(filepath,dstfolder, Constants.STATUS_SENDING_FAILED);
                    continue;
                }
                if (isResultSuccess) {
                    totalFilesAlreadySent++;
                    updateFileStatus(filepath,dstfolder, Constants.STATUS_SENT);
                    Thread.sleep(10);
//                        uploadFailing = false;
                } else {
                    totalFilesFailed++;
                    updateFileStatus(filepath,dstfolder, Constants.STATUS_SENDING_FAILED);
                    Log.e("sendWithSmbProtocol", "Uploading failed");
//                        uploadFailing = true;
                }

            }
            showNotification("Finished - Uploaded:" + Integer.toString(totalFilesAlreadySent) + " Failed:"  + Integer.toString(totalFilesFailed)+ " Total:"+ Integer.toString(totalFilesShouldBeSent), "Upload Status 100%");
            Log.i("sendWithSmbProtocol","Finished uploading " + fileSrcPaths.size() + " Files!!!");
        }
        catch (Exception e)
        {
            Log.e("sendWithSmbProtocol","Failed  uploading all " + fileSrcPaths.size() + " Files!!!. error says:" + e.getMessage());
            for(int i = 0; i<fileSrcPaths.size();i++) {
                String filepath = fileSrcPaths.get(i).toString();
                showNotification("Failed - Connectivity Failure:" + Integer.toString(totalFilesAlreadySent) + " Failed:"  + Integer.toString(totalFilesFailed)+ " Total:"+ Integer.toString(totalFilesShouldBeSent), "Upload Failed");
                totalHandled++;
            }

            e.printStackTrace();
//                uploadFailing = true;
        }
    }

}