package com.apps.exhesham.autoftpsync.utils;

import android.support.v4.util.ArrayMap;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by hesham on 6/9/2017.
 */

public class RulesAPI {

    public ArrayMap<String,String> readRules(){
        boolean shouldUpdateDB = false;
        JSONArray ja = Utils.getInstance(null).getJsonArrayFromDB("rules");
        JSONObject version =  Utils.getInstance(null).getJsonObjFromDB(Constants.DB_FOLLOWED_DIRS);
        if(version == null || ja.length() == 0){
            /*If the version is not identified then for sure it is not version 3, then reformat the available data*/
            ja = new JSONArray();
            shouldUpdateDB = true;
        }
        if(ja.length() == 0){
            try {
                for(String ext : Constants.PHOTOS_CATERGORY_EXTS){
                    ja.put(new JSONObject().put("extension",ext).put("folder_name","photos").put("status",Constants.FOLLOWING_DIR).put("category","photos"));
                }
                for(String ext : Constants.COMPRESSED_CATERGORY_EXTS){
                    ja.put(new JSONObject().put("extension",ext).put("folder_name","compressed").put("status",Constants.FOLLOWING_DIR).put("category","compressed"));
                }
                for(String ext : Constants.DOCUMENTS_CATERGORY_EXTS){
                    ja.put(new JSONObject().put("extension",ext).put("folder_name","documents").put("status",Constants.FOLLOWING_DIR).put("category","documents"));
                }
                for(String ext : Constants.MUSIC_CATERGORY_EXTS){
                    ja.put(new JSONObject().put("extension",ext).put("folder_name","music").put("status",Constants.FOLLOWING_DIR).put("category","music"));
                }
                for(String ext : Constants.VIDEO_CATERGORY_EXTS){
                    ja.put(new JSONObject().put("extension",ext).put("folder_name","videos").put("status",Constants.FOLLOWING_DIR).put("category","videos"));
                }
                for(String ext : Constants.APPS_CATERGORY_EXTS){
                    ja.put(new JSONObject().put("extension",ext).put("folder_name","apps").put("status",Constants.FOLLOWING_DIR).put("category","apps"));
                }

//                ja.put(new JSONObject().put("extension","*").put("folder_name","others"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        ArrayMap<String, String> rules = new ArrayMap<>();
        for (int i = 0;i < ja.length();i++) {
            try {
                String foldername = ja.getJSONObject(i).getString("folder_name");
                String extension = ja.getJSONObject(i).getString("extension");
                if(!ja.getJSONObject(i).has("status")){
                    ja.getJSONObject(i).put("status",Constants.FOLLOWING_DIR);
                }
                String status = ja.getJSONObject(i).getString("status");
                if(Constants.FOLLOWING_DIR.equals(status)){
                    rules.put(extension,foldername);
                }
            } catch (JSONException e) {
                continue;
            }
        }
        if(shouldUpdateDB){
            Utils.getInstance(null).storeConfigString("rules",ja.toString());
        }
        return  rules;
    }
    public String getExtensionFolder(String extension) {
        ArrayMap<String, String> rules = new RulesAPI().readRules();
        if(rules.containsKey(extension.toLowerCase())){
            return rules.get(extension);
        }
        if(rules.containsKey("*") && rules.get("*").equals("<IGNORE FILE>")){

            return null;
        }
        return rules.get("*");
    }

    public boolean shouldIgnoreFile(String path) {
        String extension = FilenameUtils.getExtension(path);
        String folderName = getExtensionFolder(extension);
        return folderName == null;
    }

}
