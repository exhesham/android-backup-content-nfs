package com.apps.exhesham.autoftpsync;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.ActionMenuItemView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.apps.exhesham.autoftpsync.utils.Constants;
import com.apps.exhesham.autoftpsync.utils.FTPNode;
import com.apps.exhesham.autoftpsync.utils.PathDetails;
import com.apps.exhesham.autoftpsync.utils.Utils;

import java.util.ArrayList;


@SuppressLint("NewApi")
public class PhoneFileSystem extends AppCompatActivity {
    private String currPath = Constants.DEFAULT_PATH;
    private Context context;
    static FTPNode ftpnode;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        ftpnode = Utils.getInstance(context).getFTPSettings();

        setContentView( R.layout.activity_ftpsync);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.maintoolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        fillTable(Constants.DEFAULT_PATH);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.phone_file_sys_toolbar, menu);
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

        String status = Utils.getInstance(context).getPathStatus(path);

        ActionMenuItemView iv = (ActionMenuItemView )findViewById(R.id.toolbtn_is_following);
        if (iv == null){
            Log.e("updateToolbarSyncButton","cannot find toolbtn_is_following!");
            return;
        }
        Drawable followIcon= getResources().getDrawable( R.drawable.follow ,null);
        Drawable unfollowIcon= getResources().getDrawable( R.drawable.unfollow ,null);
        try {
            if (status.equals(Constants.FOLLOWING_DIR)) {
                iv.setIcon(followIcon);
                iv.setTitle(Constants.FOLLOWING_DIR);
            } else {
                iv.setIcon(unfollowIcon);
                iv.setTitle(Constants.NOT_FOLLOWING_DIR);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }



    public void handleFollowStatus(MenuItem item) {
        /* Update The database */
        String status = Utils.getInstance(context).getPathStatus(currPath);
        Utils.getInstance(context).convertFollowStateInDB(currPath);
        ActionMenuItemView  iv = (ActionMenuItemView )findViewById(R.id.toolbtn_is_following);
        Drawable followIcon= getResources().getDrawable( R.drawable.follow ,null);
        Drawable unfollowIcon= getResources().getDrawable( R.drawable.unfollow ,null);
        if(status.equals(Constants.FOLLOWING_DIR)){
            /*if you were following the dir and pressed the button then now you are not following it*/
            iv.setTitle(Constants.NOT_FOLLOWING_DIR);
            iv.setIcon(unfollowIcon);
        }else{
            iv.setTitle(Constants.FOLLOWING_DIR);
            iv.setIcon(followIcon);
        }

    }


}
