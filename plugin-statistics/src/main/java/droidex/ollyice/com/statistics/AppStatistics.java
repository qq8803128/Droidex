package droidex.ollyice.com.statistics;

import android.content.Context;
import android.util.Log;

import com.ollyice.droidex.statistics.IStatistics;

/**
 * Created by ollyice on 2018/8/20.
 */

public class AppStatistics implements IStatistics{
    private static final String TAG = "AppStatistics";
    @Override
    public void init(Context context) {
        Log.e(TAG,"init");
    }

    @Override
    public void event(String s, Object... objects) {
        Log.e(TAG,"event:" + s);
    }
}
