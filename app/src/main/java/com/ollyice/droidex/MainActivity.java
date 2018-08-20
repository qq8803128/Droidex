package com.ollyice.droidex;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.gson.Gson;
import com.ollyice.droidex.multiapk.AssetsUtils;
import com.ollyice.droidex.multiapk.MultiApk;
import com.ollyice.droidex.statistics.StatisticsManager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.click).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Json json = new Json();

                for (int i = 1;i < 20;i++){
                    json.put("ACTIVITY GSON:" + i,(i + 10000) + "");
                }
                Log.d("JSON",new Gson().toJson(json));
            }
        });

        findViewById(R.id.event1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StatisticsManager.event("event1");
            }
        });

        findViewById(R.id.event2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StatisticsManager.event("event2");
            }
        });

        findViewById(R.id.event3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StatisticsManager.event("event3");
            }
        });

        findViewById(R.id.init).setOnClickListener(new View.OnClickListener() {
            boolean initing = false;
            @Override
            public void onClick(View v) {
                if (initing){
                    return;
                }
                initing = true;
                //你可以根据需求在服务器下载各种插件
                File statistics = AssetsUtils.releaseFile(getApplication(), "plugins/", "statistics.apk");
                MultiApk.install(getApplication(), statistics, true, new Runnable() {
                    @Override
                    public void run() {
                        StatisticsManager.init(getApplicationContext());
                    }
                });
            }
        });
    }
}
