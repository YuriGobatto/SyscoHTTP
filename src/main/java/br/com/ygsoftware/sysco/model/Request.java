package br.com.ygsoftware.sysco.model;

import android.content.Context;
import android.support.annotation.IntDef;
import android.util.Log;
import android.webkit.URLUtil;
import android.webkit.WebView;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import br.com.ygsoftware.sysco.BuildConfig;
import br.com.ygsoftware.sysco.array.DataArray;
import br.com.ygsoftware.sysco.model.get.GetString;
import br.com.ygsoftware.sysco.model.post.PostFile;
import br.com.ygsoftware.sysco.model.post.PostString;

/**
 * Created by adriana on 25/01/2017.
 */

public class Request {

    /*PRIVATE STATIC CONSTANTS*/
    private static final String lineEnd = "\r\n";
    private static final String twoHyphens = "--";
    private static final String boundary = "*****";


    /*PUBLIC STATIC CONSTANTS*/
    public static final int GET_METHOD = 0;
    public static final int POST_METHOD = 1;
    public static final int FILES_METHOD = 2;

    @IntDef({POST_METHOD, GET_METHOD, FILES_METHOD})
    @Retention(RetentionPolicy.SOURCE)
    private @interface DecodeMethods {
    }

    @IntDef({POST_METHOD, GET_METHOD})
    @Retention(RetentionPolicy.SOURCE)
    private @interface HTTPMethods {
    }

    private String url = "";
    private String method = "POST";

    Map<String, String> arrayHeader = new HashMap<>();
    DataArray data = new DataArray();

    private int getStringsLength = 0;
    private int postStringsLength = 0;
    private int postFilesLength = 0;

    private boolean requested = false;
    private String userAgent = "";

    public Request(String url) {
        this(url, POST_METHOD);
    }

    public Request(String url, DataArray data) {
        this(url, POST_METHOD, data);
    }

    public Request(String url, @HTTPMethods int method) {
        this(url, method, null);
    }

    public Request(String url, @HTTPMethods int method, DataArray data) {
        try {
            setUrl(url);
            this.method = (method == GET_METHOD ? "GET" : "POST");
            setData(data);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public Request setData(DataArray data) {
        getStringsLength = 0;
        postStringsLength = 0;
        postFilesLength = 0;
        this.data = data;
        if(data != null) {
            for (Method method : data) {
                if (method instanceof GetString) {
                    getStringsLength++;
                } else if (method instanceof PostString) {
                    postStringsLength++;
                } else if (method instanceof PostFile) {
                    postFilesLength++;
                }
            }
        }
        return this;
    }

    public DataArray getData() {
        return data;
    }

    public Request addHeaderField(String field, String value) {
        if (!arrayHeader.containsKey(field)) {
            arrayHeader.put(field, value);
        } else {
            changeHeaderField(field, value);
        }
        return this;
    }

    public Request removeHeaderField(String field) {
        if (arrayHeader.containsKey(field)) {
            arrayHeader.remove(field);
        }

        return this;
    }

    public Request changeHeaderField(String field, String newValue) {
        if (arrayHeader.containsKey(field)) {
            removeHeaderField(field);
        }
        addHeaderField(field, newValue);
        return this;
    }

    /**
     * @param data String example:
     * File key=file://path&key2=file://path
     * Get key=GET:value&key2=GET:value2
     * Post key=value&key2=value2
     *
     * @return DataArray contendo os valores para envio
     * */
    public static DataArray prepareParametersFromString(String data){
        DataArray dataArray = new DataArray();
        String[] dataKV = data.split("&");
        for (String kV : dataKV){
            String key = kV.split("=")[0];
            String value = kV.split("=")[1];
            if(URLUtil.isFileUrl(value)){
                dataArray.add(new PostFile(key, new File(value)));
            }else if(value.startsWith("GET:")){
                dataArray.add(new GetString(key, value.replace("GET:", "")));
            }else {
                dataArray.add(new PostString(key, value));
            }
        }

        return dataArray;
    }

    public static String getChromeUserAgent(Context context){
        return new WebView(context).getSettings().getUserAgentString();
    }

    public static String getSyscoUserAgent(Context context){
        return "Sysco-"+BuildConfig.APPLICATION_ID+"/"+BuildConfig.VERSION_NAME+"-"+getChromeUserAgent(context);
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Request rebuild(){
        url = "";
        method = "POST";
        requested = false;
        clearAll();

        return this;
    }

    public HttpURLConnection build() throws Exception {
        if (!requested) {
            requested = true;
            for (Method method : data) {
                method.setRequested(requested);
            }
        }else{
            throw new UnsupportedOperationException("Você não pode fazer um request com o mesmo objeto. Chame rebuld para poder consertar");
        }
        prepareGETStrings();
        URL Url = new URL(url);
        Log.d("URL", Url.toString());
        HttpURLConnection conn = (HttpURLConnection) Url.openConnection();
        conn.setDoInput(true); // Allow Inputs
        conn.setDoOutput(true); // Allow Outputs
        conn.setUseCaches(false); // Don't use a Cached Copy
        conn.setRequestMethod(method);
        conn.setRequestProperty("User-Agent", userAgent);
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("ENCTYPE", "multipart/form-data");
        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

        conn = setHeader(conn);

        return conn;
    }

    private HttpURLConnection setHeader(HttpURLConnection connection) {
        if (arrayHeader.size() > 0 && !arrayHeader.isEmpty()) {
            for (String key : arrayHeader.keySet()) {
                String value = arrayHeader.get(key);
                connection.setRequestProperty(key, value);
            }
        }
        return connection;
    }

    public boolean isRequested() {
        return requested;
    }

    private void prepareGETStrings() throws UnsupportedEncodingException {
        Map<String, String> arrayGet = getArray(GET_METHOD);
        String prepareUrlSplit = url.replace("?", "_QST_");
        String prepareUrl = prepareUrlSplit.split("_QST_")[0];
        if (prepareUrlSplit.split("_QST_").length - 1 > 0) {
            String getStr = prepareUrlSplit.split("_QST_")[1];
            String[] gets = getStr.split("&");
            for (String get : gets) {
                String[] getKV = get.split("=");
                String key = getKV[0];
                String value = getKV[1];
                if (!data.contains(key)) {
                    data.add(new GetString(key, value));
                }
            }
        }
        boolean isFirst = true;
        Iterator<String> keys = arrayGet.keySet().iterator();
        StringBuilder sb = new StringBuilder();
        sb.append(prepareUrl);
        sb.append("?");
        while (keys.hasNext()) {
            String key = keys.next();
            String value = URLEncoder.encode(arrayGet.get(key), "UTF-8");
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append("&");
            }
            sb.append(key);
            sb.append("=");
            sb.append(value);
        }
        url = sb.toString();
    }

    public String getUrl() {
        return url;
    }

    public Request setUrl(String url) throws MalformedURLException {
        if (url == null || url.isEmpty()) {
            throw new MalformedURLException("The url is null or empty");
        }
        if (!URLUtil.isValidUrl(url)) {
            throw new MalformedURLException("The url is not valid");
        }
        this.url = url;
        return this;
    }

    public String getFileKey() {
        return "file_" + postFilesLength;
    }

    public String getMethod() {
        return method;
    }

    public Request setMethod(String method) {
        this.method = method;
        return this;
    }

    public ArrayList<String> getKeys(@DecodeMethods int method) {
        ArrayList<String> res = new ArrayList<>();
        Iterator<String> keys = getArray(method).keySet().iterator();

        while (keys.hasNext()) {
            res.add(keys.next());
        }

        return res;
    }

    public void clearAll() {
        data.clear();
        arrayHeader.clear();
        getStringsLength = 0;
        postStringsLength = 0;
        postFilesLength = 0;
    }

    public String getValue(@DecodeMethods int method, String key) {
        Map<String, String> map = getArray(method);
        if (map.containsKey(key)) {
            return map.get(key);
        } else {
            return "null";
        }
    }

    private String getArrayToString(@DecodeMethods int method) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> keys = getArray(method).keySet().iterator();
        if (!keys.hasNext()) {
            return "null";
        }
        boolean isFirst = true;
        while (keys.hasNext()) {
            String key = keys.next();

            if (isFirst) {
                isFirst = false;
            } else {
                sb.append("&");
            }

            sb.append(key);
            sb.append(" = ");
            sb.append(data.get(key).getValue().toString());
        }
        return sb.toString();
    }

    public Map<String, String> getArray(@DecodeMethods int method) {
        Map<String, String> res = new HashMap<>();

        for (Method obj : data) {
            if (method == GET_METHOD && obj instanceof GetString) {
                res.put(obj.getKey(), ((GetString) obj).getValue());
            } else if (method == POST_METHOD && obj instanceof PostString) {
                res.put(obj.getKey(), ((PostString) obj).getValue());
            } else if (method == FILES_METHOD && obj instanceof PostFile) {
                res.put(obj.getKey(), ((PostFile) obj).getValue().toString());
            }
        }

        return res;
    }

    public boolean isValidMethod() {
        return (GET_METHOD == (method.equals("GET") ? 0 : 1) && haveDataForSend());
    }

    public boolean haveDataForSend() {
        return (postStringsLength > 0 || postFilesLength > 0);
    }

    @Override
    public String toString() {
        return "Request{" +
                "\n url= '" + url + '\'' +
                ",\n method= '" + method + '\'' +
                ",\n arrayHeader= " + arrayHeader +
                ",\n arrayFiles= " + getArrayToString(FILES_METHOD) +
                ",\n arrayPost= " + getArrayToString(POST_METHOD) +
                ",\n arrayGet= " + getArrayToString(GET_METHOD) +
                "\n}";
    }
}
