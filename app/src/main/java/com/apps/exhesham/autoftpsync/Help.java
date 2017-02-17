package com.apps.exhesham.autoftpsync;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;

public class Help extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        WebView lWebView = (WebView)findViewById(R.id.webView);
        lWebView.loadUrl("file:///android_asset/help.html");
    }
}
