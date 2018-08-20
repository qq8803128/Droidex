package com.ollyice.droidex.multiapk;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by ollyice on 2018/8/9.
 */

public class AssetsUtils {
    public static File releaseFile(Context context, String dir, String name){
        String outputDir = context.getDir(MultiApk.DEX_RELEASE_DIR,Context.MODE_PRIVATE).getAbsolutePath();
        File file = new File(outputDir,name);
        if (file.exists() && file.isFile()){
            return file;
        }
        File tempFile = new File(outputDir,name + ".temp");
        if (tempFile.exists() && tempFile.isFile()){
            tempFile.delete();
        }
        InputStream in = null;
        OutputStream out = null;
        try {
            in = context.getAssets().open(dir + name);
            out = new FileOutputStream(tempFile.getAbsolutePath());
            byte[] bytes = new byte[1024];
            int i;
            while ((i = in.read(bytes)) != -1) {
                out.write(bytes, 0, i);
            }
        } catch (IOException e) {
            e.printStackTrace();
            file = null;
        } finally {
            try {
                if (in != null)
                    in.close();
                if (out != null)
                    out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (tempFile != null && tempFile.exists() && tempFile.isFile()){
            tempFile.renameTo(file);
        }
        return file;
    }
}
