package com.apps.exhesham.autoftpsync.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;

import com.apps.exhesham.autoftpsync.Rules;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by hesham on 1/28/2017.
 */

public class Utils {
    private Context myContext;
    private String preference_file_key = "1111";
    private static Utils instance = null;

    public Utils(Context myContext) {
        this.myContext = myContext;
    }

    public static Utils getInstance(Context myContext) {
        if(instance == null) {
            instance = new Utils(myContext);
        }
        return instance;
    }
    public void storeConfigString(String key,String val){
        SharedPreferences sharedPref = myContext.getSharedPreferences(
                preference_file_key, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, val);
        editor.commit();
    }
    public String getConfigString(String key){
        SharedPreferences sharedPref = myContext.getSharedPreferences(
                preference_file_key, Context.MODE_PRIVATE);

        return  sharedPref.getString(key, "");
    }
    public String showAlert(String msg,String title,boolean addTextbox) {

        Log.v("-->showAlert",msg);
        try {
            // Set up the input
            final EditText input = new EditText(myContext);
            AlertDialog alertDialog = new AlertDialog.Builder(myContext).create();
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
            alertDialog.setTitle(title);
            alertDialog.setMessage(msg);
            if(addTextbox ){
                alertDialog.setView(input);

                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
//                                dialog.dismiss();
                                return;
                            }
                        });
            }
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Alright",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            try{
                alertDialog.show();
            }catch (Exception e){
                e.printStackTrace();
                return null;
            }

            return input.getText().toString();
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }

    }

    public String getPathStatus(String path) {
        //TODO:Should check if parent path is synced
        if(path.endsWith("/")){
            path = path.substring(0, path.length()-1);
        }
        String following_path = getConfigString(path);
        if(following_path == null || following_path.equals("")){
            return Constants.STATUS_NOTHING;
        }
        JSONObject jo;
//        JSONArray ja = getJsonArrayFromDB("following_paths")
//        for(int i=0;i<ja.length();i++){
//            try {
//                if(ja.getJSONObject(i).getString("path").equals(currPath)){
//                    ja.remove(i);
//                    i= i==0?i:i-1;
//
//                }
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        }

        try {
            jo = new JSONObject(following_path);
            return jo.getString("status");
        } catch (JSONException e) {
            e.printStackTrace();
            return Constants.STATUS_NOTHING;
        }
    }

    public JSONObject generateJsonStatus(String status,String path,boolean isDefault) {
        JSONObject jo = new JSONObject();
        try {
            jo.put("date",System.currentTimeMillis());
            jo.put("status",status);
            jo.put("path",path);
            jo.put("default",isDefault);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo;
    }
//    private String generateStatus(String status,String path) {
//        JSONObject jo = new JSONObject();
//        try {
//            jo.put("date",System.currentTimeMillis());
//            jo.put("status",status);
//            jo.put("path",path);
//
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        return jo.toString();
//    }


    public void convertFollowStateInDB(String currPath) {

        String status = getPathStatus(currPath);
        JSONArray ja = getJsonArrayFromDB("following_paths");

        // First - Delete the path from following_paths
        for(int i=0;i<ja.length();i++){
            try {
                if(ja.getJSONObject(i).getString("path").equals(currPath)){
                    ja.remove(i);
                    i= i==0?i:i-1;

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


        if(status.equals(Constants.FOLLOWING_DIR)){
            storeConfigString(currPath,generateJsonStatus(Constants.NOT_FOLLOWING_DIR,currPath, false).toString());
        }else{
            storeConfigString(currPath, generateJsonStatus(Constants.FOLLOWING_DIR, currPath, false).toString());
            ja.put(generateJsonStatus(Constants.FOLLOWING_DIR, currPath, false));
        }
        storeConfigString("following_paths",ja.toString());
    }

    public FTPNode getFTPSettings() {
        String availableFTPS = Utils.getInstance(myContext).getConfigString("settings");
        JSONObject jo;
        if ( availableFTPS == null || availableFTPS.equals("")){
            return null;
        }else{
            try {
                jo = new JSONObject(availableFTPS);
                return  FTPNode.parseJSON(jo);
            } catch (JSONException e) {
                e.printStackTrace();
                return  null;
            }
        }

    }

    public JSONArray  getJsonArrayFromDB(String key) {
        String following_paths = getConfigString(key);
        JSONArray ja;
        try {
            ja = new JSONArray(following_paths);
        } catch (JSONException e) {
            e.printStackTrace();
            ja = new JSONArray();
        }
        return  ja;
    }
    public JSONObject  getJsonObjFromDB(String key) {
        String  joStr = getConfigString(key);
        JSONObject ja;
        try {
            ja = new JSONObject(joStr);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return  ja;
    }

    /***
     * convert state of category
     * @param categoryKey the category key as appears in the database
     * @return true if the new state of the category is followed. false otherwise
     */
    public boolean convertCategoryStateInDB(String categoryKey) {
        JSONArray jaRules = getJsonArrayFromDB("rules");
        JSONObject jo = getJsonObjFromDB("categories");
        boolean shouldFollow = false;
        if(jo != null){
            try {
                String catStatus  = jo.getJSONObject(categoryKey).getString("status");
                if(catStatus.equals(Constants.FOLLOWING_DIR)){
                    jo.getJSONObject(categoryKey).put("status",Constants.NOT_FOLLOWING_DIR);
                    shouldFollow = false;
                }else{
                    jo.getJSONObject(categoryKey).put("status",Constants.FOLLOWING_DIR);
                    shouldFollow = true;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            storeConfigString("categories", jo.toString());
        }

        /***
         * if the path to convert following status  had rule or category then update them
         */
        for(int i = 0;i<jaRules.length();i++){
            try {
                if(jaRules.getJSONObject(i).getString("category").equals(categoryKey)){
                    jaRules.getJSONObject(i).put("status", shouldFollow ? Constants.FOLLOWING_DIR: Constants.NOT_FOLLOWING_DIR);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        storeConfigString("rules",jaRules.toString());

        return  shouldFollow ;
    }

    public static class FileSysAPI {
        private static ArrayList<PathDetails> getFoldersRecursiveAux(String path, int depth){
            ArrayList pdArray = new ArrayList<>();

            File dir = new File(path);
            File[] files = dir.listFiles();
            if (files == null){
                return pdArray;
            }
            for (File f : files){
                if(f.isDirectory()){
                    pdArray.addAll(getFoldersRecursiveAux(f.getAbsolutePath(),depth+1));
                }else{
                    if(!new Rules().shouldIgnoreFile(f.getAbsolutePath())){
                        pdArray.add(new PathDetails(f,depth));
                    }
                }
            }
            return pdArray;
        }
        public static ArrayList<PathDetails> getFoldersRecursive(String path){
            if(!isPathExist(path)){
                return new ArrayList<>();
            }
            return getFoldersRecursiveAux(path,0);
        }
        public static ArrayList<PathDetails> getFolders(String path){
            ArrayList pdArray = new ArrayList<>();
            if(!isPathExist(path)){
                return null;
            }
            File dir = new File(path);
            File[] files = dir.listFiles();
            if (files == null){
                return pdArray;
            }
            for (File f : files){
                pdArray.add(new PathDetails(f));
            }
            return pdArray;
        }
        private static boolean isPathExist(String path) {
            return  new File(path).exists();
        }

        public static String getFileCategory(String fullpth) {
            String extension = FilenameUtils.getExtension(fullpth).toLowerCase();
            if(Arrays.asList(Constants.PHOTOS_CATERGORY_EXTS).contains(extension)){
                return Constants.PHOTOS_CATERGORY_NAME;
            }
            if(Arrays.asList(Constants.VIDEO_CATERGORY_EXTS).contains(extension)){
                return Constants.VIDEO_CATERGORY_NAME;
            }
            if(Arrays.asList(Constants.MUSIC_CATERGORY_EXTS).contains(extension)){
                return Constants.MUSIC_CATERGORY_NAME;
            }
            if(Arrays.asList(Constants.RECORDING_CATERGORY_EXTS).contains(extension)){
                return Constants.RECORDINGS_CATERGORY_NAME;
            }
            if(Arrays.asList(Constants.DOCUMENTS_CATERGORY_EXTS).contains(extension)){
                return Constants.DOCUMENTS_CATERGORY_NAME;
            }
            if(Arrays.asList(Constants.COMPRESSED_CATERGORY_EXTS).contains(extension)){
                return Constants.COMPRESSED_CATERGORY_NAME;
            }
            if(Arrays.asList(Constants.APPS_CATERGORY_EXTS).contains(extension)){
                return Constants.APPS_CATERGORY_NAME;
            }
            return null;
        }
    }

}
