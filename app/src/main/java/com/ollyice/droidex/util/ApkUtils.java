package com.ollyice.droidex.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

/**
 * Created by ollyice on 2018/8/20.
 */

public class ApkUtils {
    public static int getApkVersionCode(String absPath,Context context) {

        PackageManager pm = context.getPackageManager();
        PackageInfo pkgInfo = pm.getPackageArchiveInfo(absPath,PackageManager.GET_ACTIVITIES);
        if (pkgInfo != null) {
            ApplicationInfo appInfo = pkgInfo.applicationInfo;
            appInfo.sourceDir = absPath;
            appInfo.publicSourceDir = absPath;
            return pkgInfo.versionCode; // 得到版本信息
        }
        return 0;
    }

    public static String getApkPackageName(String absPath,Context context) {
        PackageManager pm = context.getPackageManager();
        PackageInfo pkgInfo = pm.getPackageArchiveInfo(absPath,PackageManager.GET_ACTIVITIES);
        if (pkgInfo != null) {
            ApplicationInfo appInfo = pkgInfo.applicationInfo;
            appInfo.sourceDir = absPath;
            appInfo.publicSourceDir = absPath;
            return appInfo.packageName; // 得到包名
        }
        return "";
    }
}
