package com.ollyice.droidex;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ollyice on 2018/8/9.
 */

public class Json {
    private Map<String,String> map = new HashMap<>();

    public Json put(String key,String value){
        map.put(key,value);
        return this;
    }
}
