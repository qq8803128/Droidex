package com.ollyice.droidex.statistics;

import android.content.Context;

/**
 * Created by ollyice on 2018/8/20.
 */

public interface IStatistics {
    void init(Context context);
    void event(String methodName,Object... args);
}
