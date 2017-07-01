package com.apps.exhesham.autoftpsync.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by hesham on 6/5/2017.
 */

public class NFSSettingsNode {
    public NFSSettingsNode(String username, String password, String serverurl, JSONArray rootdir) {

        setPassword(password);
        setRootPath(rootdir);
        setServerurl(serverurl);
        setUsername(username);
    }

    public String getSelectedRootPath() {
        for(int i=0;i<rootPath.length();i++){
            try {
                if(rootPath.getJSONObject(i).getBoolean("is_selected")){
                    return rootPath.getJSONObject(i).getString("path");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void setRootPath(JSONArray rootPath) {
        this.rootPath = rootPath;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getServerurl() {
        return serverurl;
    }

    public void setServerurl(String serverurl) {
        this.serverurl = serverurl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    private String username;
    private String password;
    private JSONArray rootPath;
    private String serverurl;

    @Override
    public String toString() {
        return "NFSSettingsNode{" +
                "password='" + "********" + '\'' +
                ", username='" + username + '\'' +
                ", rootPath='" + rootPath + '\'' +
                ", serverurl='" + serverurl + '\'' +
                '}';
    }
}
