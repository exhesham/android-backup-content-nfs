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
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;

import android.support.v7.view.menu.ActionMenuItemView;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
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
import java.util.Iterator;
import java.util.Set;


@SuppressLint("NewApi")
public class FTPSync extends AppCompatActivity {
    private String currPath = Constants.DEFAULT_PATH;
    private Context context;
    static FTPNode ftpnode;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        ftpnode = Utils.getInstance(context).getFTPSettings();

        setContentView( R.layout.activity_ftpsync);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.maintoolbar, menu);
        return true;
    }
    public void backFolder(View v){
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
            backFolder(null);
        }
    }

    /* We dont want customers to wait on locks so we make it custom */
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
            String filestatus = Utils.getInstance(context).getPathStatus(pd.getFullpath());
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
                     //   sendFile(pd);
                    }
                }
            } );
            ll.addView(row,i++);
        }
        fill_table_lock = false;
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




    private void updateToolbarSyncButton(String path) {
        //TODO:Should check if parent path is synced
        String status = Utils.getInstance(context).getPathStatus(path);

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



    public void handleFollowStatus(MenuItem item) {
        if(ftpnode == null){
            Utils.getInstance(context).showAlert(
                    "Please go to Settings and configure your FTP",
                    "No FTP Configured",
                    false);
            return;
        }
        /* Update The database */
        Utils.getInstance(context).convertFollowStateInDB(currPath);
        String status = Utils.getInstance(context).getPathStatus(currPath);
        ActionMenuItemView  iv = (ActionMenuItemView )findViewById(R.id.toolbtn_is_following);
        Drawable myIcon = getResources().getDrawable( R.drawable.follow ,null);
        if(status.equals(Constants.FOLLOWING_DIR)){
            /*if you were following the dir and pressed the button then now you are not following it*/
            myIcon = getResources().getDrawable( R.drawable.unfollow ,null);
            iv.setTitle(Constants.NOT_FOLLOWING_DIR);
        }else{
            iv.setTitle(Constants.FOLLOWING_DIR);
        }
        iv.setIcon(myIcon);
    }


}
