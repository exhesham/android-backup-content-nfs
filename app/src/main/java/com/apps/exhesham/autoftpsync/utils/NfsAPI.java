package com.apps.exhesham.autoftpsync.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Objects;

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
        try {
            filepath = filepath.replace("//", "/");
            String user = settings.getUsername() + ":" + settings.getPassword();
            NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(user);
            //String path = "smb://192.168.1.1/samsung/test.txt";
            String path = "smb://" + settings.getServerurl() + "/" + filepath;
            SmbFile sFile = new SmbFile(path, auth);
            if (sFile.exists()) {
                return true;
            }
        }catch (Exception e){
            e.printStackTrace();

        }
        return  false;
    }

    public void nfsCreateDirectoryTree(String dstfolder) {
        try {
            dstfolder = dstfolder.replace("//", "/");
            String user = settings.getUsername() + ":" + settings.getPassword();
            NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(user);
            //String path = "smb://192.168.1.1/samsung/test.txt";
            String path = "smb://" + settings.getServerurl() + "/" + dstfolder;
            SmbFile sFile = new SmbFile(path, auth);
            if ( !sFile.exists()){
                sFile.mkdirs();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public boolean uploadFile(String filepathOnDevice, String filepathOnNfs, boolean overrideIfExists) {
        try{
            filepathOnNfs = filepathOnNfs.replace("//", "/");
            String user = settings.getUsername() + ":" + settings.getPassword();
            NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(user);
            //String path = "smb://192.168.1.1/samsung/test.txt";
            String path = "smb://" + settings.getServerurl() +"/"+ filepathOnNfs;
            SmbFile sFile = new SmbFile(path, auth);
            if( sFile.exists() && ! overrideIfExists && sFile.length() != 0){
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
                byte[] bytes = new byte[60000];

                // Read in the bytes
                int offset = 0;
                int numRead = 0;

                while ((numRead=is.read(bytes)) != -1) {
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
    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f%sB", bytes / Math.pow(unit, exp), pre);
    }

    /***
     * this class will contain data about the nfs connected disks.
     */
    public static class DiskNode{
        public String diskName;
        public long freeSize;
        public long totalSize;

        public DiskNode(String diskName, long freeSize, long totalSize){
            this.diskName = diskName;
            this.freeSize = freeSize;
            this.totalSize = totalSize;
        }
        @Override
        public String toString(){
            return diskName + " " + humanReadableByteCount(freeSize,false) + "/" +humanReadableByteCount(totalSize,false);
        }

        @Override
        public boolean equals(Object op){
            // compares only the disk name and ignoring the size
            if(! (op instanceof DiskNode)){
                return false;
            }
            DiskNode opDN = (DiskNode)op;
            if(opDN.diskName == null){
                // i did this so i don't get null pointer on the equal at the last return
                if(diskName == null){
                    return true;
                }else{
                    return false;
                }
            }
            return opDN.diskName.equals(diskName);
        }
    }

    /***
     * return info about the nfs connected disks
     * @return
     */
    public ArrayList<DiskNode> nfsGetConnectedDisks(){
        ArrayList<DiskNode> res = new ArrayList<>();
        try{
            String path = "smb://" + settings.getServerurl() + "/";
            String user = settings.getUsername() + ":" + settings.getPassword();
            NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(user);

            SmbFile sFile = new SmbFile(path, auth);
//            sFile.setConnectTimeout(Constants.TEST_CONNECT_TIMEOUT_MS);
            for (SmbFile child : sFile.listFiles()){
                System.out.println(child.getCanonicalPath());
                if(child.isDirectory() && child.getDiskFreeSpace() > 0){
                    res.add(new DiskNode(child.getName(),child.getDiskFreeSpace(), child.length()));

                }

            }


        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        return  res;
    }
}
