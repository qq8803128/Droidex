package com.ollyice.droidex;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.gson.Gson;

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
    }
}
