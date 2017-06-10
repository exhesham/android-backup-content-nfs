package com.apps.exhesham.autoftpsync.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;

import static com.apps.exhesham.autoftpsync.R.drawable.file;

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

    public boolean uploadFile(String filepathOnDevice, String filepathOnNfs, boolean overrideIfExists) {
        try{
            filepathOnNfs = filepathOnNfs.replace("//", "/");
            String user = settings.getUsername() + ":" + settings.getPassword();
            NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(user);
            //String path = "smb://192.168.1.1/samsung/test.txt";
            String path = "smb://" + settings.getServerurl() +"/"+ filepathOnNfs;
            SmbFile sFile = new SmbFile(path, auth);
            if(sFile.exists() && ! overrideIfExists){
                return true;
            }
            SmbFileOutputStream sfos = new SmbFileOutputStream(sFile);
            // open the file for read and then upload its chunks
            File fileToUpload = new File(filepathOnDevice);
            InputStream is = new FileInputStream(fileToUpload);
            try {
                // Get the size of the file

                long length = fileToUpload.length();

                // You cannot create an array using a long type.
                // It needs to be an int type.
                // Before converting to an int type, check
                // to ensure that file is not larger than Integer.MAX_VALUE.
                if (length > Integer.MAX_VALUE) {
                    // File is too large
                    throw new IOException("File is too large!");
                }

                // Create the byte array to hold the data
                byte[] bytes = new byte[(int)length];

                // Read in the bytes
                int offset = 0;
                int numRead = 0;

                while (offset < bytes.length
                        && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
                    sfos.write(bytes);

                    offset += numRead;
                }
            } finally {
                is.close();
                sfos.close();
            }


            System.out.println("Done!");
            return true;
        } catch (IOException e) {
            // do something
            e.printStackTrace();
            System.out.println(e.getMessage());
            return false;
        }

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
