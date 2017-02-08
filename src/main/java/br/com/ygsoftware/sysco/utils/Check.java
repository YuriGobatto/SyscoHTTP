package br.com.ygsoftware.sysco.utils;

import android.webkit.URLUtil;

/**
 * Created by adriana on 16/12/2016.
 */

public class Check {

    public static boolean isValidKey(String key) {
        return key != null && !(key.length() <= 0 || key.isEmpty());
    }

    public static boolean isValidValue(String value) {
        return value != null && !(value.length() <= 0 || value.isEmpty());
    }

    public static void isValidURL(String url, String... a) {
        if (url == null || url.isEmpty()) {
            throw new NullPointerException("The url is null or empty");
        }
        if (!URLUtil.isValidUrl(url)) {
            throw new IllegalArgumentException("The url is not valid");
        }
    }

    public static String isValidURL(String url) {
        try {
            isValidURL(url, "");
            return "";
        }catch (Exception e){
            return e.getMessage();
        }
    }

    public static String prepareKey(String key) {
        String tmpKey = key;
        tmpKey.trim();
        tmpKey = tmpKey.replaceAll(" ", "_");
        return tmpKey;
    }

}
