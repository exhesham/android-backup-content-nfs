package com.apps.exhesham.autoftpsync.utils;

import java.io.IOException;
import java.util.ArrayList;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

/**
 * Created by hesham on 6/5/2017.
 */

public class NfsAPI {
    private NFSSettingsNode settings;
    private NfsAPI(){}
    public NfsAPI(NFSSettingsNode settings){
        setSettings(settings);

    }

    public NFSSettingsNode getSettings() {
        return settings;
    }

    public void setSettings(NFSSettingsNode settings) {
        this.settings = settings;
    }

    public boolean doesFileExists(String filepath) {
        //TODO: Implement
        return false;
    }

    public void nfsCreateDirectoryTree(String dstfolder) {
        //TODO: Implement
    }

    public boolean uploadFile(String filepath) {
        //TODO: Implement
        return false;
    }
    public ArrayList<String> nfsGetRootDir(){
        ArrayList<String> res = new ArrayList<>();
        try{
            String path = "smb://" + settings.getServerurl() + "/";
            String user = settings.getUsername() + ":" + settings.getPassword();
            NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(user);

            SmbFile sFile = new SmbFile(path, auth);
            sFile.setConnectTimeout(Constants.TEST_CONNECT_TIMEOUT_MS);
            for (SmbFile child : sFile.listFiles()){
                System.out.println(child.getCanonicalPath());
                if(child.isDirectory() && child.getDiskFreeSpace() > 0){
                    res.add(child.getName());
                }

            }


        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        return  res;
    }
}
