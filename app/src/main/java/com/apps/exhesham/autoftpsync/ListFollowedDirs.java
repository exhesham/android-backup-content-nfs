package com.apps.exhesham.autoftpsync;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.apps.exhesham.autoftpsync.utils.Constants;
import com.apps.exhesham.autoftpsync.utils.PathDetails;
import com.apps.exhesham.autoftpsync.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ListFollowedDirs extends AppCompatActivity {
    private Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_list_followed_dirs);
        context = this;
        Toolbar myToolbar = (Toolbar) findViewById(R.id.followed_dirs_tb);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        fillTable();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.followed_dirs_toolbar, menu);
        return true;
    }
    public void refreshTable(MenuItem item){
        fillTable();
    }

    private void fillTable() {
        ArrayList<FollowedDirDetails> data = new ArrayList<>();
        final ListView listview = (ListView) findViewById(R.id.followed_dirs_listview);


        JSONArray ja = Utils.getInstance(context).getJsonArrayFromDB("following_paths");

        for (int i = 0; i < ja.length(); i++) {
            try {
                JSONObject jo = ja.getJSONObject(i);
                String status = jo.getString("status");
                if (!Constants.FOLLOWING_DIR.equals(status)) {
                    continue;
                }
                String pathname = jo.getString("path");
                ArrayList<PathDetails> pda = Utils.FileSysAPI.getFoldersRecursive(pathname);
                if (null == pda) {
                    continue;
                }

                boolean isDefault = false;
                if(jo.has("default")) {
                    isDefault = jo.getBoolean("default");
                }
                FollowedDirDetails currPath = new FollowedDirDetails();
                currPath.setFullPath(pathname);
                currPath.setIsDefault(isDefault);

                int total_sent = 0;
                int total = 0;

                for (final PathDetails pd : pda) {
                    total++;
                    total_sent = Utils.getInstance(context).getPathStatus(pd.getFullpath()).equals(Constants.STATUS_SENT) ? total_sent + 1 : total_sent;
                }
                currPath.setTotalRelevantFiles(total);
                currPath.setTotalSentFiles(total_sent);
                data.add(currPath);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        final ListFollowedFoldersAdapter adapter = new ListFollowedFoldersAdapter(this, data.toArray(new FollowedDirDetails[]{}));
        listview.setAdapter(adapter);
    }

    /***********************************************************************************************/
    /*The below code is responsible for decorating the table view*/
    public class ListFollowedFoldersViewHolder {
        TextView path;
        TextView total_relevant_files;
        TextView total_synced_files;
        ImageView imgStat;
    }
    public class ListFollowedFoldersAdapter extends BaseAdapter {
        //Defining the background color of rows. The row will alternate between green light and green dark.
//        private int[] colors = new int[] { 0xFF585B5C, 0xFF7E8182 };
        private int[] colors = new int[] { 0xffffbb33, 0xffff8800 };
        private LayoutInflater mInflater;

        //The variable that will hold our text data to be tied to list.
        private FollowedDirDetails[] data;

        public ListFollowedFoldersAdapter(Context context, FollowedDirDetails[] items) {
            mInflater = LayoutInflater.from(context);
            this.data = items;
        }

        @Override
        public int getCount() {
            return data.length;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        //A view to hold each row in the list
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            // A ViewHolder keeps references to children views to avoid unneccessary calls
            // to findViewById() on each row.
            ListFollowedFoldersViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.followed_dir_row, null);

                holder = new ListFollowedFoldersViewHolder();
                holder.path = (TextView) convertView.findViewById(R.id.firstLine);
                holder.total_relevant_files = (TextView) convertView.findViewById(R.id.total_files);
                holder.total_synced_files = (TextView) convertView.findViewById(R.id.total_synced_files);
                convertView.setTag(holder);
            } else {
                holder = (ListFollowedFoldersViewHolder) convertView.getTag();
            }
            // Bind the data efficiently with the holder.
            final String path = data[position].getFullpath();
            holder.path.setText(path);
            holder.path.setEllipsize(TextUtils.TruncateAt.END);
            holder.total_relevant_files.setText("Total:" + Integer.toString(data[position].getTotalRelevantFiles()));
            holder.total_synced_files.setText("Synced:" + Integer.toString(data[position].getTotalSentFiles()));

            //Set the background color depending of  odd/even colorPos result
            int colorPos = position % colors.length;
            convertView.setBackgroundColor(colors[colorPos]);

            // set the image
            ImageView img = (ImageView) convertView.findViewById(R.id.photos_status);
            if(data[position].isDefault()){
                img.setImageResource(R.drawable.cant_delete);
            }else{
                img.setImageResource(R.drawable.delete);
            }
            img.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Utils.getInstance(context).convertFollowStateInDB(path);
                    fillTable();
                }
            });
            holder.imgStat = img;
            return convertView;
        }
    }


    private class FollowedDirDetails {
        private String fullpath;
        private int totalRelevantFiles;
        private int totalSentFiles;

        public boolean isDefault() {
            return isDefault;
        }

        private boolean isDefault;


        public String getFullpath() {
            return fullpath;
        }

        public void setFullPath(String fullpath) {
            this.fullpath = fullpath;
        }

        public void setTotalRelevantFiles(int totalRelevantFiles) {
            this.totalRelevantFiles = totalRelevantFiles;
        }

        public int getTotalRelevantFiles() {
            return totalRelevantFiles;
        }

        public void setTotalSentFiles(int totalSentFiles) {
            this.totalSentFiles = totalSentFiles;
        }

        public int getTotalSentFiles() {
            return totalSentFiles;
        }

        public void setIsDefault(boolean isDefault) {
            this.isDefault = isDefault;
        }
    }
}
