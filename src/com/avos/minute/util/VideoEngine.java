package com.avos.minute.util;

public class VideoEngine {
    static {
        System.loadLibrary("ffmpegutils");
    }
    public native int crop(String inputFile, String outputFile, int targetWidth, int targetHeight);

    public static void main(String[] args) {
        ;
    }
}
