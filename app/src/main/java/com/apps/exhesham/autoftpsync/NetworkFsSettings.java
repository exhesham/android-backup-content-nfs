package com.apps.exhesham.autoftpsync;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class NetworkFsSettings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_storage_file_sys);
        scanAvailableSharedFS();
    }
    private void scanAvailableSharedFS(){

    }
}
