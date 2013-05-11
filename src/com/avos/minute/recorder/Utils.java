package com.avos.minute.recorder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.TypedValue;

public class Utils {
    private static String dirPath;
    private static int fileSeq;

    public static Boolean isNullOrEmptyString(String s) {
        return s == null || s.length() == 0;
    }

    public static <T> Boolean isNullOrEmptyArray(T[] arr) {
        return arr == null || arr.length == 0;
    }

    public static <T> Boolean isNullOrEmptyList(List<T> list) {
        return list == null || list.size() == 0;
    }

    public static <T> List<T> asList(T... rest) {
        List<T> list = new ArrayList<T>();

        if (rest != null) {
            for (T t : rest) {
                if (t != null) {
                    list.add(t);
                }
            }
        }

        return list;
    }

    public static String toStringList(Object[] args) {
        StringBuilder sb = new StringBuilder();

        if (args != null && args.length > 0) {
            for (Object arg : args) {
                sb.append(arg);
                sb.append(",");
            }

            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
    }

    public static <T> String toStringList(List<T> list) {
        return toStringList(list, ",");
    }

    public static <T> String toStringList(List<T> list, String separator) {
        StringBuilder sb = new StringBuilder();

        if (list != null && list.size() > 0) {
            for (T t : list) {
                sb.append(t.toString());

                sb.append(separator);
            }

            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
    }

    public static Double safeParseDouble(String s) {
        Double d = null;

        try {
            if (!Utils.isNullOrEmptyString(s)) {
                d = Double.parseDouble(s);
            }
        } catch (NumberFormatException e) {
            d = null;
        }

        return d;
    }

    public static String getNextVideoPath() {
        File mediaStorageDir = new File(Environment
                .getExternalStorageDirectory().getPath(), "wanpai");
        if (dirPath == null) {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                    .format(new Date());
            fileSeq = 1;
            dirPath = mediaStorageDir + File.separator + "TEMP_" + timeStamp;
            File dir = new File(dirPath);
            dir.mkdirs();
        } else {
            fileSeq++;
        }
        return dirPath + File.separator + fileSeq + ".mp4";
    }

    public static String getFinalVideoPath() {
        File mediaStorageDir = new File(Environment
                .getExternalStorageDirectory().getPath(), "wanpai");
        if (dirPath == null) {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                    .format(new Date());
            fileSeq = 1;
            dirPath = mediaStorageDir + File.separator + "TEMP_" + timeStamp;
            File dir = new File(dirPath);
            dir.mkdirs();
        }
        return dirPath + File.separator + fileSeq + "-final.mp4";
    }
    public static String getNextSnapshotPath() {
        File mediaStorageDir = new File(Environment
                .getExternalStorageDirectory().getPath(), "wanpai");
        if (dirPath == null) {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                    .format(new Date());
            fileSeq = 1;
            dirPath = mediaStorageDir + File.separator + "TEMP_" + timeStamp;
            File dir = new File(dirPath);
            dir.mkdirs();
        }
        return dirPath + File.separator + fileSeq + ".jpg";
    }

    /**
     * Get the Android UDID and then convert it to 40 characters to be
     * compatible to iOS UDID
     * 
     */
    public static String getAndroidUDID(Context context) {
        String androidUDID = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();

        return androidUDID.toLowerCase(Locale.US);
    }

    // http://stackoverflow.com/questions/9525075/detect-android-os-in-java?lq=1
    // http://stackoverflow.com/questions/4519556/how-to-determine-if-my-app-is-running-on-android
    public static String getAndroidOsString() {
        String os = null;

        if (!Utils.isNullOrEmptyString(os = System.getProperty("java.runtime.name"))) {
            return os;
        } else if (!Utils.isNullOrEmptyString(os = System.getProperty("java.vm.name"))) {
            return os;
        } else if (!Utils.isNullOrEmptyString(os = System.getProperty("java.specification.vendor"))) {
            return os;
        } else if (!Utils.isNullOrEmptyString(os = System.getProperty("java.vm.specification.vendor"))) {
            return os;
        } else {
            // ok...that's enough.
            return null;
        }
    }

    public static int convertDipsToPixels(Context context, int dips) {
        Resources r = context.getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dips, r.getDisplayMetrics());
        return Math.round(px);
    }

    public static boolean isExternalStorageAvailable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }
}
