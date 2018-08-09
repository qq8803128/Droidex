package com.ollyice.droidex;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ollyice on 2018/8/9.
 */

public class App extends Application{

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        File gson = AssetsUtils.releaseFile(this, "plugins/", "gson.apk");
        MultiApk.install(this,gson);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        testGson1();
    }

    /**
     * 5.0以下会被error捕获 提示找不到这个类 这个应该是系统加载class的时机问题吧  具体需要去问google爸爸吧
     * 5.1  7.0  9.0测试全部可以log
     */
    void testGson1(){
        try {
            Json json = new Json();

            for (int i = 1; i < 20; i++) {
                json.put("APP GSON1:" + i, (i + 10000) + "");
            }
            Log.d("APPJSON1", new Gson().toJson(json));
        }catch (Exception e){
            e.printStackTrace();
        }catch (Error e){
            e.printStackTrace();
        }
    }

    public class Json {
        private Map<String,String> map = new HashMap<>();

        public Json put(String key, String value){
            map.put(key,value);
            return this;
        }
    }
}
