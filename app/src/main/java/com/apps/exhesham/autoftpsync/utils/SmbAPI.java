package com.apps.exhesham.autoftpsync.utils;

/**
 * Created by hesham on 6/5/2017.
 */

public class SmbAPI {
    private SMBSettingsNode settings;
    private SmbAPI(){}
    public SmbAPI(SMBSettingsNode settings){
        setSettings(settings);

    }

    public SMBSettingsNode getSettings() {
        return settings;
    }

    public void setSettings(SMBSettingsNode settings) {
        this.settings = settings;
    }

    public boolean doesFileExists(String filepath) {
        //TODO: Implement
        return false;
    }

    public void smbCreateDirectoryTree(String dstfolder) {
        //TODO: Implement
    }

    public boolean uploadFile(String filepath) {
        //TODO: Implement
        return false;
    }
}
