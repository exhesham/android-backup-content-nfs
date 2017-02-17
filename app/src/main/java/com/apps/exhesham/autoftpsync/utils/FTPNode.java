package com.apps.exhesham.autoftpsync.utils;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by hesham on 1/28/2017.
 */

public class FTPNode {
    public FTPNode(String username,
                   String password,
                   int port,
                   String serverurl,
                   String ftpname,
                   boolean isMainFtp,
                   String defaultPath,
                   boolean isPassive,
                   boolean isAnonymous){
        setPassword(password);
        setUsername(username);
        setPort(port);
        setServerurl(serverurl);
        setFtpname(ftpname);
        setMainFtp(isMainFtp);
        setDefaultPath(defaultPath);
        setAnonymous(isAnonymous);
        setPassive(isPassive);
    }
    public FTPNode(){}

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getServerurl() {
        return serverurl;
    }

    public void setServerurl(String serverurl) {
        this.serverurl = serverurl;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private String username;

    public String getDefaultPath() {
        return defaultPath;
    }

    public void setDefaultPath(String defaultPath) {
        this.defaultPath = defaultPath;
    }

    private String defaultPath;
    private String serverurl;
    private int port;

    public boolean isPassive() {
        return isPassive;
    }

    public void setPassive(boolean passive) {
        isPassive = passive;
    }

    boolean isPassive;

    public boolean isAnonymous() {
        return isAnonymous;
    }

    public void setAnonymous(boolean anonymous) {
        isAnonymous = anonymous;
    }

    boolean isAnonymous;
    public boolean isMainFtp() {
        return isMainFtp;
    }

    public void setMainFtp(boolean mainFtp) {
        isMainFtp = mainFtp;
    }

    private boolean isMainFtp;
    private String password;

    public String getFtpname() {
        return ftpname;
    }

    public void setFtpname(String ftpname) {
        this.ftpname = ftpname;
    }

    private String ftpname;

    public JSONObject toJSON() {
        JSONObject jo = new JSONObject();
        try {
            jo.put("name",getFtpname());
            jo.put("username",getUsername());
            jo.put("password",getPassword());
            jo.put("port",getPort());
            jo.put("server",getServerurl());
            jo.put("default_path",getDefaultPath());
            jo.put("is_main_ftp",isMainFtp());
            jo.put("is_passive",isPassive());
            jo.put("is_anonymous",isAnonymous());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo;
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }

    public static FTPNode parseJSON(JSONObject jo) {
        try {
            FTPNode res = new FTPNode(
                    jo.has("username")?jo.getString("username"):"",
                    jo.has("password")?jo.getString("password"):"",
                    jo.has("port")?jo.getInt("port"):21,
                    jo.has("server")?jo.getString("server"):"192.168.1.1",
                    jo.has("name")?jo.getString("name"):"main ftp",
                    jo.has("is_main_ftp")?jo.getBoolean("is_main_ftp"):true,
                    jo.has("default_path")?jo.getString("default_path"):"/",
                    jo.has("is_passive")?jo.getBoolean("is_passive"):true,
                    jo.has("is_anonymous")?jo.getBoolean("is_anonymous"):false);
            return  res;
        } catch (JSONException e) {
            Log.e("parseJSON","Failed to parse the ftp node");
            return null;
        }
    }
}
