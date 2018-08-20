package com.ollyice.droidex.statistics;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ollyice on 2018/8/20.
 */

public class StatisticsManager {
    private static Map<String,IStatistics> statisticses = new HashMap<>();

    public static void addStatistics(IStatistics iStatistics){
        if (!statisticses.containsKey(iStatistics.getClass().getName())){
            statisticses.put(iStatistics.getClass().getName(),iStatistics);
        }
    }

    public static void init(Context context){
        for (String key : statisticses.keySet()){
            IStatistics iStatistics = statisticses.get(key);
            try{
                iStatistics.init(context);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public static void event(String methodName,Object... args){
        for (String key : statisticses.keySet()){
            IStatistics iStatistics = statisticses.get(key);
            try{
                iStatistics.event(methodName,args);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
