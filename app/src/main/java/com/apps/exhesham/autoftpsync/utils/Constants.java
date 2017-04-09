package com.apps.exhesham.autoftpsync.utils;

/**
 * Created by hesham on 4/2/2017.
 */

public final class Constants {

    // Defines a custom Intent action
    public static final String BROADCAST_ACTION =
            "com.example.android.threadsample.BROADCAST";
    public static final String STATUS_CONNECTING = "Connecting...";
    public static final String STATUS_SENT = "Sent";
    public static final String STATUS_SENDING = "Sending...";
    public static final String STATUS_FAILED_LOGIN = "Login Failed!";
    public static final String STATUS_FAILED_CONNECTING = "Connecting Failed!";
    public static final String STATUS_SENDING_FAILED = "Sending Failed";
    public static final String FOLLOWING_DIR = "Following Directory";
    public static final String NOT_FOLLOWING_DIR = "Not Following Directory";
    public static final String STATUS_NOTHING = "Nothing";
    public static final String DEFAULT_PATH = "/";
    public static final int TEST_CONNECT_TIMEOUT_MS = 5000;
    public static final  int MY_PERMISSIONS_REQUEST_READ_AND_WRITE_SDK = 1 ;

    public static final String[] MUSIC_CATERGORY_EXTS = {"mp3","wmv"};
    public static final String[] VIDEO_CATERGORY_EXTS = {"mp4","avi","mpeg"};
    public static final String[] RECORDING_CATERGORY_EXTS = {"m4a","wav"};
    public static final String[] PHOTOS_CATERGORY_EXTS = {"jpeg","jpg","png","gif"};
    public static final String[] DOCUMENTS_CATERGORY_EXTS = {"xlsx","xls","docx","doc","txt","pdf","ppt","pptx"};
    public static final String[] COMPRESSED_CATERGORY_EXTS = {"rar","zip","tar.gz"};
    public static final String[] APPS_CATERGORY_EXTS = {"apk","exe"};


    public static final String MUSIC_CATERGORY_NAME = "Music";
    public static final String VIDEO_CATERGORY_NAME = "Video";
    public static final String RECORDINGS_CATERGORY_NAME = "Recordings";
    public static final String PHOTOS_CATERGORY_NAME = "Photos";
    public static final String DOCUMENTS_CATERGORY_NAME = "Documents";
    public static final String COMPRESSED_CATERGORY_NAME = "Compressed";
    public static final String APPS_CATERGORY_NAME = "Apps";


    public static final String VERSION = "3";
    public static final long DEFAULT_SENDING_TIMEOUT_MS = 5 * 60 * 1000;
}
