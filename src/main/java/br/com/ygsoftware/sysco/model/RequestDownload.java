package br.com.ygsoftware.sysco.model;

import android.util.Log;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by adriana on 14/02/2017.
 */

public class RequestDownload {

    private String url;
    private File outputFile;
    private String userAgent;

    private boolean downloading = false;

    public RequestDownload(String url, File outputFile) {
        this(url, outputFile, "");
    }

    public RequestDownload(String url, File outputFile, String userAgent) {
        this.url = url;
        this.outputFile = outputFile;
        this.userAgent = userAgent;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        if (!downloading) {
            this.url = url;
        }else {
            throw new UnsupportedOperationException("Download in run");
        }
    }

    public File getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(File outputFile) {
        if (!downloading) {
            this.outputFile = outputFile;
        }else {
            throw new UnsupportedOperationException("Download in run");
        }
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        if (!downloading) {
            this.userAgent = userAgent;
        }else {
            throw new UnsupportedOperationException("Download in run");
        }
    }

    public HttpURLConnection build() throws Exception {
        URL Url = new URL(url);
        Log.d("URL", Url.toString());
        HttpURLConnection conn = (HttpURLConnection) Url.openConnection();
        conn.setDoInput(true); // Allow Inputs
        conn.setDoOutput(true); // Allow Outputs
        conn.setUseCaches(false); // Don't use a Cached Copy
        conn.setRequestMethod("POST");
        conn.setRequestProperty("User-Agent", userAgent);
        conn.setRequestProperty("Connection", "Keep-Alive");

        return conn;
    }
}
