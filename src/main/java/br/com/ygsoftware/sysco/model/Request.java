package br.com.ygsoftware.sysco.model;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.webkit.URLUtil;
import android.webkit.WebView;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import br.com.ygsoftware.sysco.BuildConfig;
import br.com.ygsoftware.sysco.array.DataArray;
import br.com.ygsoftware.sysco.enums.RequestMethods;
import br.com.ygsoftware.sysco.model.get.GetArray;
import br.com.ygsoftware.sysco.model.get.GetString;
import br.com.ygsoftware.sysco.model.post.PostArray;
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
    private int getArraysLength = 0;
    private int postStringsLength = 0;
    private int postArraysLength = 0;
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
        getArraysLength = 0;
        postStringsLength = 0;
        postArraysLength = 0;
        postFilesLength = 0;
        this.data = data;
        if(data != null) {
            for (Method method : data.getAll()) {
                if (method instanceof GetString) {
                    getStringsLength++;
                } else if (method instanceof GetArray) {
                    getArraysLength++;
                } else if (method instanceof PostString) {
                    postStringsLength++;
                } else if (method instanceof PostArray) {
                    postArraysLength++;
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
     *
     * Get key=GET:value&key2=GET:value2
     * Get Array key=GET:value::value2
     *
     * Post key=value&key2=value2
     * Post Array key=value::value2
     *
     * @return DataArray contendo os valores para envio
     * */
    public static DataArray prepareParametersFromString(String data){
        DataArray dataArray = new DataArray();
        String[] dataKV = data.split("&");
        for (String kV : dataKV){
            String key = kV.split("=")[0];
            String value = kV.split("=")[1];
            if (key.endsWith("[]")) {
                if (value.startsWith("GET:")) {
                    dataArray.add(new GetArray(key, value.replaceAll("GET:", "").split("::")));
                } else {
                    dataArray.add(new PostArray(key, value.split("::")));
                }
            } else {
                if (URLUtil.isFileUrl(value)) {
                    dataArray.add(new PostFile(key, new File(value)));
                } else if (value.startsWith("GET:")) {
                    dataArray.add(new GetString(key, value.replace("GET:", "")));
                } else {
                    dataArray.add(new PostString(key, value));
                }
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
        return new Request(url, method, data);
    }

    public HttpURLConnection build() throws Exception {
        if (!requested) {
            requested = true;
            for (Method method : data.getAll()) {
                method.setRequested(requested);
            }
        }else{
            throw new UnsupportedOperationException("Você não pode fazer um request com o mesmo objeto. Chame rebuild para poder consertar");
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
        Map<String, String[]> arrayGets = getParameterArrays(RequestMethods.GET);

        URI uri = URI.create(url);
        Uri.Builder builder = new Uri.Builder()
                .scheme(uri.getScheme())
                .authority(uri.getHost())
                .path(uri.getPath());

        if (uri.getQuery() != null) {
            builder.query(uri.getQuery());
        }

        Iterator<String> keys = arrayGet.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = URLEncoder.encode(arrayGet.get(key), "UTF-8");

            builder.appendQueryParameter(key, value);
        }

        Iterator<String> arrayKeys = arrayGets.keySet().iterator();
        while (keys.hasNext()) {
            String key = arrayKeys.next();
            for (String value : arrayGets.get(key)) {
                builder.appendQueryParameter(key, value);
            }
        }
        url = builder.build().toString();
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
        data.removeAll();
        arrayHeader.clear();
        getStringsLength = 0;
        getArraysLength = 0;
        postStringsLength = 0;
        postArraysLength = 0;
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

    public <P> Map<String, P> getArray(RequestMethods method) {
        Map<String, P> res = new HashMap<>();

        for (Method obj : data.getAll()) {
            if (method == RequestMethods.GET && obj instanceof GetString) {
                res.put(obj.getKey(), ((P) ((GetString) obj).getValue()));
            } else if (method == RequestMethods.POST && obj instanceof PostString) {
                res.put(obj.getKey(), ((P) ((PostString) obj).getValue()));
            } else if (method == RequestMethods.FILES && obj instanceof PostFile) {
                res.put(obj.getKey(), ((P) ((PostFile) obj).getValue().toString()));
            }
        }

        return res;
    }

    public Map<String, String[]> getParameterArrays(RequestMethods method) {
        Map<String, String[]> res = new HashMap<>();

        for (Method obj : data.getAll()) {
            if (method == RequestMethods.GET && obj instanceof GetArray) {
                res.put(obj.getKey(), ((GetArray) obj).getValue());
            } else if (method == RequestMethods.POST && obj instanceof PostArray) {
                res.put(obj.getKey(), ((PostArray) obj).getValue());
            } else if (method == RequestMethods.FILES) {
                throw new UnsupportedOperationException("FILES is not supported in the method see Request#getArray(RequestMethods)");
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
