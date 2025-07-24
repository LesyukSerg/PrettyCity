package com.raziel.prettycity.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class Utils {
    public static String getPath(Context context, Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) return null;

        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(column_index);
        cursor.close();

        return path;
    }

    public static boolean copyFile(File src, File dst) {
        try (InputStream in = new java.io.FileInputStream(src);
             java.io.OutputStream out = new java.io.FileOutputStream(dst)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static double convertToDegree(String stringDMS) {
        if (stringDMS == null) return 0.0;

        String[] DMS = stringDMS.split(",", 3);

        String[] stringD = DMS[0].split("/", 2);
        double D = Double.parseDouble(stringD[0]) / Double.parseDouble(stringD[1]);

        String[] stringM = DMS[1].split("/", 2);
        double M = Double.parseDouble(stringM[0]) / Double.parseDouble(stringM[1]);

        String[] stringS = DMS[2].split("/", 2);
        double S = Double.parseDouble(stringS[0]) / Double.parseDouble(stringS[1]);

        return D + (M / 60) + (S / 3600);
    }

}
