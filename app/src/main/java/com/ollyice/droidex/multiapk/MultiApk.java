package com.ollyice.droidex.multiapk;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

/**
 * Created by ollyice on 2018/8/9.
 */

public class MultiApk {
    private static final String FILE_NAME = "YouGameSdk_Settings";//自己根据你们SDK名称修改吧
    public static final String DEX_RELEASE_DIR = "dex_cache";//dex释放路径
    public static final String JNI_RELEASE_DIR = "jni_cache";//jni加载与释放路径

    //是否设置了jni加载路径
    private static boolean sHasInsertedNativeLibrary = false;

    //判断app里面是否已经加载了当前插件
    public static boolean isInstalled(Context context, File apk) {
        try {
            Object baseDexElements = getDexElements(getPathList(getPathClassLoader()));
            int length = Array.getLength(baseDexElements);
            for (int i = 0; i < length; i++) {
                Object object = Array.get(baseDexElements, i);
                File src = null;

                if (Build.VERSION.SDK_INT >= 28) {
                    src = getInjectedApk(object,"path");
                } else {
                    src = getInjectedApk(object,"zip");
                }

                if (src != null && src.getAbsolutePath().equals(apk.getAbsolutePath())) {
                    Log.d("MultiApk","插件已经加载过:" + apk.getAbsolutePath());
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
    * 反射获取PathClassLoader里面dexElements 的文件路径
    */
    private static File getInjectedApk(Object object, String name) {
        try {
            Field field = object.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (File) field.get(object);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 安装一个插件
     * @param context  app的 application
     * @param apk  插件app文件路径
     */
    public static void install(Context context, File apk) {
        if (apk == null || !apk.isFile()){
            return;
        }
        if (isInstalled(context,apk)){
            return;
        }

        ClassLoader parent = MultiApk.class.getClassLoader();//获取 app classloader

        String dexDir = getDexReleaseDir(context)
                .getAbsolutePath();//获取dex释放路径
        String jniDir = getJniReleaseDir(context)
                .getAbsolutePath();//获取jni加载与释放路径

        //利用DexClassLoader加载外部插件apk文件
        DexClassLoader dexClassLoader = new DexClassLoader(
                apk.getAbsolutePath(),
                dexDir,
                jniDir,
                parent
        );

        try {
            //获取app中的dexElements
            Object baseDexElements = getDexElements(getPathList(getPathClassLoader()));
            //获取plugin中的dexElements
            Object newDexElements = getDexElements(getPathList(dexClassLoader));
            //合并app与plugin中的dexElements
            Object allDexElements = combineArray(baseDexElements, newDexElements);

            Object pathList = getPathList(getPathClassLoader());
            //将新的dexElements反射设置到app中替换原来的dexElements
            setField(pathList.getClass(), pathList, "dexElements", allDexElements);

            //设置so文件加载目录
            insertNativeLibrary(context,dexClassLoader);

            //从插件中查找符合cpu架构的so文件释放到so库加载目录
            tryToCopyNativeLib(context,apk);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取jni加载与释放路径
     */
    private static File getJniReleaseDir(Context context) {
        return context.getDir(JNI_RELEASE_DIR,Context.MODE_PRIVATE);
    }

    /**
     * 获取dex缓存路径
     */
    private static File getDexReleaseDir(Context context) {
        return context.getDir(DEX_RELEASE_DIR,Context.MODE_PRIVATE);
    }

    /**
     * 设置so加载目录
     */
    private static void insertNativeLibrary(Context context,DexClassLoader dexClassLoader) throws Exception {
        //jni加载目录只需要设置一次
        if (sHasInsertedNativeLibrary) {
            return;
        }
        sHasInsertedNativeLibrary = true;

        Object basePathList = getPathList(getPathClassLoader());
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {//5.1
            List<File> nativeLibraryDirectories = (List<File>) getField(basePathList.getClass(),
                    basePathList, "nativeLibraryDirectories"); //获取pathList里面的jni加载目录
            nativeLibraryDirectories.add(getJniReleaseDir(context));//将我们插件so加载目录写到里面去

            //5.1以上新增的需要反射设置so库的路径
            Object baseNativeLibraryPathElements = getField(basePathList.getClass(), basePathList, "nativeLibraryPathElements");
            final int baseArrayLength = Array.getLength(baseNativeLibraryPathElements);

            Object newPathList = getPathList(dexClassLoader);
            Object newNativeLibraryPathElements = getField(newPathList.getClass(), newPathList, "nativeLibraryPathElements");
            Class<?> elementClass = newNativeLibraryPathElements.getClass().getComponentType();
            Object allNativeLibraryPathElements = Array.newInstance(elementClass, baseArrayLength + 1);
            System.arraycopy(baseNativeLibraryPathElements, 0, allNativeLibraryPathElements, 0, baseArrayLength);

            Field soPathField;
            if (Build.VERSION.SDK_INT >= 26) {
                soPathField = elementClass.getDeclaredField("path");
            } else {
                soPathField = elementClass.getDeclaredField("dir");
            }
            soPathField.setAccessible(true);
            final int newArrayLength = Array.getLength(newNativeLibraryPathElements);
            for (int i = 0; i < newArrayLength; i++) {
                Object element = Array.get(newNativeLibraryPathElements, i);
                String dir = ((File) soPathField.get(element)).getAbsolutePath();
                if (dir.contains(DEX_RELEASE_DIR)) {
                    Array.set(allNativeLibraryPathElements, baseArrayLength, element);
                    break;
                }
            }

            setField(basePathList.getClass(), basePathList, "nativeLibraryPathElements", allNativeLibraryPathElements);
        } else {
            File[] nativeLibraryDirectories = (File[]) getFieldNoException(basePathList.getClass(),
                    basePathList, "nativeLibraryDirectories");
            final int N = nativeLibraryDirectories.length;
            File[] newNativeLibraryDirectories = new File[N + 1];
            System.arraycopy(nativeLibraryDirectories, 0, newNativeLibraryDirectories, 0, N);
            newNativeLibraryDirectories[N] = getJniReleaseDir(context);
            setField(basePathList.getClass(), basePathList, "nativeLibraryDirectories", newNativeLibraryDirectories);
        }
    }

    /**
     * 获取PathList里面的dexElements对象
     */
    private static Object getDexElements(Object pathList) throws Exception {
        return getField(pathList.getClass(), pathList, "dexElements");
    }

    /**
     * 获取ClassLoader里面的pathList对象
     */
    private static Object getPathList(Object baseDexClassLoader) throws Exception {
        return getField(Class.forName("dalvik.system.BaseDexClassLoader"), baseDexClassLoader, "pathList");
    }

    /**
     * 获取PathClassLoader
     */
    private static PathClassLoader getPathClassLoader() {
        PathClassLoader pathClassLoader = (PathClassLoader) MultiApk.class.getClassLoader();
        return pathClassLoader;
    }

    /**
     * 合并数组
     */
    private static Object combineArray(Object firstArray, Object secondArray) {
        Class<?> localClass = firstArray.getClass().getComponentType();

        //modify to sure plugin jar class is first use
        int firstArrayLength = Array.getLength(secondArray);
        int allLength = firstArrayLength + Array.getLength(firstArray);
        Object result = Array.newInstance(localClass, allLength);
        for (int k = 0; k < allLength; ++k) {
            if (k < firstArrayLength) {
                Array.set(result, k, Array.get(secondArray, k));
            } else {
                Array.set(result, k, Array.get(firstArray, k - firstArrayLength));
            }
        }
        return result;
    }

    /**
     * 释放so
     */
    private static void tryToCopyNativeLib(Context context,File apk) throws Exception {
        long startTime = System.currentTimeMillis();
        ZipFile zipfile = new ZipFile(apk.getAbsolutePath());//apk就是一个zip文件

        String packageName = getPackageName(context,apk);
        int versionCode = getPackageVersion(context,apk);
        File nativeLibDir = getJniReleaseDir(context);

        try {
            //查找插件zip的文件目录
            //根据手机cpu架构释放对应目录的so文件
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                for (String cpuArch : Build.SUPPORTED_ABIS) {
                    if (findAndCopyNativeLib(zipfile, context, cpuArch, packageName, versionCode, nativeLibDir)) {
                        return;
                    }
                }

            } else {
                if (findAndCopyNativeLib(zipfile, context, Build.CPU_ABI, packageName, versionCode, nativeLibDir)) {
                    return;
                }
            }

            findAndCopyNativeLib(zipfile, context, "armeabi", packageName, versionCode, nativeLibDir);

        } finally {
            zipfile.close();
            Log.d("NativeLib", "Done! +" + (System.currentTimeMillis() - startTime) + "ms");
        }
    }

    /**
     * 获取插件app版本号
     */
    private static int getPackageVersion(Context context, File apk) {
        String apkPath = apk.getAbsolutePath();
        PackageManager pm = context.getPackageManager();
        PackageInfo pkgInfo = pm.getPackageArchiveInfo(apkPath,PackageManager.GET_ACTIVITIES);
        if (pkgInfo != null) {
            ApplicationInfo appInfo = pkgInfo.applicationInfo;
            appInfo.sourceDir = apkPath;
            appInfo.publicSourceDir = apkPath;
            return pkgInfo.versionCode; // 得到版本信息
        }
        return 0;
    }

    /**
     * 获取插件app的包名
     */
    private static String getPackageName(Context context, File apk) {
        String apkPath = apk.getAbsolutePath();
        PackageManager pm = context.getPackageManager();
        PackageInfo pkgInfo = pm.getPackageArchiveInfo(apkPath,PackageManager.GET_ACTIVITIES);
        if (pkgInfo != null) {
            ApplicationInfo appInfo = pkgInfo.applicationInfo;
            appInfo.sourceDir = apkPath;
            appInfo.publicSourceDir = apkPath;
            return pkgInfo.packageName; // 得到版本信息
        }
        return null;
    }

    /**
     * 遍历插件app的lib/xxxx文件夹  释放对应的so库
     */
    private static boolean findAndCopyNativeLib(ZipFile zipfile, Context context, String cpuArch, String packageName, int versionCode, File nativeLibDir) throws Exception {
        Log.d("NativeLib", "Try to copy plugin's cup arch: " + cpuArch);
        boolean findLib = false;
        boolean findSo = false;
        byte buffer[] = null;
        String libPrefix = "lib/" + cpuArch + "/";
        ZipEntry entry;
        Enumeration e = zipfile.entries();

        //遍历zip文件
        while (e.hasMoreElements()) {
            entry = (ZipEntry) e.nextElement();
            String entryName = entry.getName();

            if (entryName.charAt(0) < 'l') {
                continue;
            }
            if (entryName.charAt(0) > 'l') {
                break;
            }
            if (!findLib && !entryName.startsWith("lib/")) {
                continue;
            }
            findLib = true;
            if (!entryName.endsWith(".so") || !entryName.startsWith(libPrefix)) {
                continue;
            }

            if (buffer == null) {
                findSo = true;
                Log.d("NativeLib", "Found plugin's cup arch dir: " + cpuArch);
                buffer = new byte[8192];
            }

            String libName = entryName.substring(entryName.lastIndexOf('/') + 1);
            Log.d("NativeLib", "verify so " + libName);
            File libFile = new File(nativeLibDir, libName);
            String key = packageName + "_" + libName;
            if (libFile.exists()) {
                int VersionCode = getSoVersion(context, key);
                if (VersionCode == versionCode) {
                    Log.d("NativeLib", "skip existing so : " + entry.getName());
                    continue;
                }
            }
            FileOutputStream fos = new FileOutputStream(libFile);
            Log.d("NativeLib", "copy so " + entry.getName() + " of " + cpuArch);
            copySo(buffer, zipfile.getInputStream(entry), fos);
            setSoVersion(context, key, versionCode);
        }

        if (!findLib) {
            Log.d("NativeLib", "Fast skip all!");
            return true;
        }

        return findSo;
    }

    /**
     * 缓存so库版本信息
     */
    private static void setSoVersion(Context context, String name, int version) {
        SharedPreferences preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(name, version);
        editor.commit();
    }

    /**
     * 获取缓存的so库版本信息
     */
    private static int getSoVersion(Context context, String name) {
        SharedPreferences preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        return preferences.getInt(name, 0);
    }

    /**
     * 从插件apk文件中释放so文件
     */
    private static void copySo(byte[] buffer, InputStream input, OutputStream output) throws IOException {
        BufferedInputStream bufferedInput = new BufferedInputStream(input);
        BufferedOutputStream bufferedOutput = new BufferedOutputStream(output);
        int count;

        while ((count = bufferedInput.read(buffer)) > 0) {
            bufferedOutput.write(buffer, 0, count);
        }
        bufferedOutput.flush();
        bufferedOutput.close();
        output.close();
        bufferedInput.close();
        input.close();
    }

    /**
     * 反射获取field的值
     */
    private static Object getField(Class clazz, Object target, String name) throws Exception {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    /**
     * 反射设置field的值
     */
    private static void setField(Class clazz, Object target, String name, Object value) throws Exception {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * 反射获取field的值
     */
    private static Object getFieldNoException(Class clazz, Object target, String name) {
        try {
            return getField(clazz, target, name);
        } catch (Exception e) {
            //ignored.
        }

        return null;
    }
}
