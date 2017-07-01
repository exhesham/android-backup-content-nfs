package com.apps.exhesham.autoftpsync.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.text.InputType;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.EditText;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

import static android.content.Context.WIFI_SERVICE;

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
        SharedPreferences sharedPref = myContext.getSharedPreferences(preference_file_key, Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, val);
        editor.commit();
    }
    public String getConfigString(String key){
        SharedPreferences sharedPref = myContext.getSharedPreferences(
                preference_file_key, Context.MODE_MULTI_PROCESS);

        return  sharedPref.getString(key, "");
    }
    public String showAlert(String msg, String title, boolean addTextbox) {

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

            return null;
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
//        JSONArray ja = getJsonArrayFromDB(Constants.DB_FOLLOWED_DIRS)
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
        JSONArray ja = getJsonArrayFromDB(Constants.DB_FOLLOWED_DIRS);

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
        storeConfigString(Constants.DB_FOLLOWED_DIRS,ja.toString());
    }

    public NFSSettingsNode getSMBSettings() {
        String nfsserver = Utils.getInstance(myContext).getConfigString(Constants.DB_SMB_SERVER);
        JSONArray defaultpaths = Utils.getInstance(myContext).getJsonArrayFromDB(Constants.DB_SMB_DEFAULT_PATH);
        String nfsusername = Utils.getInstance(myContext).getConfigString(Constants.DB_SMB_USERNAME);
        String nfspassword = Utils.getInstance(myContext).getConfigString(Constants.DB_SMB_PASSWORD);
        if (nfsserver.equals("") || defaultpaths.length() == 0){
            return null;
        }else{
            return  new NFSSettingsNode(nfsusername,nfspassword,nfsserver,defaultpaths);
        }
    }

    public FTPSettingsNode getFTPSettings() {
        String availableFTPS = Utils.getInstance(myContext).getConfigString(Constants.DB_FTP_SETTINGS);
        JSONObject jo;
        if ( availableFTPS == null || availableFTPS.equals("")){
            return null;
        }else{
            try {
                jo = new JSONObject(availableFTPS);
                return  FTPSettingsNode.parseJSON(jo);
            } catch (JSONException e) {
                e.printStackTrace();
                return  null;
            }
        }
    }
    public String getDefaultGatewayAddress() {
        WifiManager wifiMgr = (WifiManager) myContext.getSystemService(WIFI_SERVICE);
        String ipAddress = Formatter.formatIpAddress(wifiMgr.getDhcpInfo().gateway);
        return ipAddress;

    } // getDefaultAddress

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
        if (key == null || key == "" || joStr == null || joStr == ""){
            return null;
        }
        try {
            ja = new JSONObject(joStr);
        } catch (JSONException e) {

            Log.e("getJsonObjFromDB", " cannot parse the value for the key " + key + "the found value is:" + joStr + " err:" + e.getMessage());
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

    public boolean validateSmbCredintials(NFSSettingsNode smb_settings) {
        if (smb_settings == null){
            return false;
        }
        String username = smb_settings.getUsername();
        String password = smb_settings.getPassword();
        String defaultAddress = smb_settings.getServerurl();
        String dirname = smb_settings.getSelectedRootPath();
        try{
            String path = "smb://"+defaultAddress+"/" + dirname;
            String user = username +":" + password;
            NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(user);

            SmbFile sFile = new SmbFile(path, auth);
            for (SmbFile child : sFile.listFiles()){
                System.out.println(child.getCanonicalPath());
                if(child.isDirectory()){
                    System.out.println("->Directory:" + child.isDirectory());
                    System.out.println("->Can Write:" + child.canWrite());
                    System.out.println("->Name:" + child.getName());
                    System.out.println("------------------------------------");
                }

            }
            System.out.println("Done!");

        } catch (Exception e) {
            // do something
            e.printStackTrace();
            System.out.println("Failed to validate for username " + username + " error: " + e.getMessage());
            return false;
        }
        return true;
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
                    if(!new RulesAPI().shouldIgnoreFile(f.getAbsolutePath())){
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
