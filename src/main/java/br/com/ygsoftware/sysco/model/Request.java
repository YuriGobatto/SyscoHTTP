package br.com.ygsoftware.sysco.model;

import android.content.Context;
import android.util.Log;
import android.webkit.URLUtil;
import android.webkit.WebView;

import java.io.File;
import java.io.UnsupportedEncodingException;
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
import br.com.ygsoftware.sysco.enums.RequestMethods;
import br.com.ygsoftware.sysco.model.get.GetString;
import br.com.ygsoftware.sysco.model.post.PostFile;
import br.com.ygsoftware.sysco.model.post.PostString;

public class Request {

    /*PRIVATE STATIC CONSTANTS*/
    private static final String lineEnd = "\r\n";
    private static final String twoHyphens = "--";
    private static final String boundary = "*****";


    private String url = "";
    private RequestMethods method = RequestMethods.POST;

    Map<String, String> arrayHeader = new HashMap<>();
    DataArray data = new DataArray();

    private int getStringsLength = 0;
    private int postStringsLength = 0;
    private int postFilesLength = 0;

    private boolean requested = false;
    private String userAgent = "";

    public Request(String url) {
        this(url, RequestMethods.POST);
    }

    public Request(String url, DataArray data) {
        this(url, RequestMethods.POST, data);
    }

    public Request(String url, RequestMethods method) {
        this(url, method, null);
    }

    public Request(String url, RequestMethods method, DataArray data) {
        try {
            setUrl(url);
            this.method = method;
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
        method = RequestMethods.POST;
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
        conn.setRequestMethod(method.getMethod());
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
        Map<String, String> arrayGet = getArray(RequestMethods.GET);
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

    public RequestMethods getMethod() {
        return method;
    }

    public Request setMethod(RequestMethods method) {
        this.method = method;
        return this;
    }

    public ArrayList<String> getKeys(RequestMethods method) {
        ArrayList<String> res = new ArrayList<>();

        for (String s : getArray(method).keySet()) {
            res.add(s);
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

    public String getValue(RequestMethods method, String key) {
        Map<String, String> map = getArray(method);
        if (map.containsKey(key)) {
            return map.get(key);
        } else {
            return "null";
        }
    }

    private String getArrayToString(RequestMethods method) {
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

    public Map<String, String> getArray(RequestMethods method) {
        Map<String, String> res = new HashMap<>();

        for (Method obj : data) {
            if (method == RequestMethods.GET && obj instanceof GetString) {
                res.put(obj.getKey(), ((GetString) obj).getValue());
            } else if (method == RequestMethods.POST && obj instanceof PostString) {
                res.put(obj.getKey(), ((PostString) obj).getValue());
            } else if (method == RequestMethods.FILES && obj instanceof PostFile) {
                res.put(obj.getKey(), ((PostFile) obj).getValue().toString());
            }
        }

        return res;
    }

    public boolean isValidMethod() {
        return (method.getMethod().equals("POST") && haveDataForSend());
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
                ",\n arrayFiles= " + getArrayToString(RequestMethods.FILES) +
                ",\n arrayPost= " + getArrayToString(RequestMethods.POST) +
                ",\n arrayGet= " + getArrayToString(RequestMethods.GET) +
                "\n}";
    }
}
