package com.apps.exhesham.autoftpsync.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.Callable;

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
            alertDialog.show();
            return input.getText().toString();
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }

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
                    pdArray.add(new PathDetails(f,depth));
                }
            }
            return pdArray;
        }
        public static ArrayList<PathDetails> getFoldersRecursive(String path){
            if(!isPathExist(path)){
                return null;
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
    }

}
