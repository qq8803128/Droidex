package droidex.ollyice.com.statistics;

import android.app.Application;

import com.ollyice.droidex.multiapk.MultiApk;
import com.ollyice.droidex.statistics.StatisticsManager;

/**
 * Created by ollyice on 2018/8/20.
 */

public class PackageInvoker implements MultiApk.IPackage{
    @Override
    public void onCreate(Application application) {
        StatisticsManager.addStatistics(new AppStatistics());
    }
}
