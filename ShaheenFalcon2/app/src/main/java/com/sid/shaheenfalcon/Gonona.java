package com.sid.shaheenfalcon;

public class Gonona {
    public static String textBytes(long lbytes, boolean withSuffix){
        String suffix = "";
        if(withSuffix){
            suffix = "Bytes";
        }
        double bytes = lbytes;
        long MB = 1024 * 1024;
        long GB = 1024 * 1024 * 1024;
        if(bytes < MB){
            if (withSuffix) {
                suffix = "KB";
            }
            return String.format("%.2f", bytes / 1024) + suffix;
        }else if(bytes < GB){
            if (withSuffix) {
                suffix = "MB";
            }
            return String.format("%.2f", (bytes / MB)) + suffix;
        }else{
            if (withSuffix) {
                suffix = "GB";
            }
            return String.format("%.2f", (bytes / GB)) + suffix;
        }
    }

    public static String textBytes(long lbytes){
        return textBytes(lbytes, true);
    }

    public static String textMiliseconds(long milisonds){
        long h = 0, m = 0, s = 0;
        s = milisonds / 1000;
        String textTime = s + "s";
        if(s > 59){
            m = s / 60;
            s = s % 60;
            textTime = m + "m " + s + "s";
        }
        if(m > 59){
            h = m / 60;
            m = m % 60;
            textTime = h + "h " + m + "m";
        }
        return textTime;
    }
}
