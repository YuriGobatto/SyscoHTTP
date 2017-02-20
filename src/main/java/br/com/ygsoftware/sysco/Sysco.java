package br.com.ygsoftware.sysco;

import android.os.Looper;
import android.support.annotation.IntDef;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import br.com.ygsoftware.sysco.enums.RequestMethods;
import br.com.ygsoftware.sysco.interfaces.FileUploadListener;
import br.com.ygsoftware.sysco.interfaces.RequestListener;
import br.com.ygsoftware.sysco.interfaces.SSEListener;
import br.com.ygsoftware.sysco.model.Request;
import br.com.ygsoftware.sysco.model.Response;
import br.com.ygsoftware.sysco.utils.Check;
import br.com.ygsoftware.sysco.utils.FileUtils;

/**
 * Created by YG Software on 02/12/2016.
 */

public class Sysco {

    public static final int RESULT_ERROR = 0;
    public static final int RESULT_OK = 1;

    public static final int NOT_THREAD = 0;
    public static final int NEW_THREAD = 1;

    @IntDef({NEW_THREAD, NOT_THREAD})
    @Retention(RetentionPolicy.SOURCE)
    private @interface RequestOptions {
    }

    /*PRIVATE STATIC CONSTANTS*/
    private static final String lineEnd = "\r\n";
    private static final String twoHyphens = "--";
    private static final String boundary = "*****";

    /*CONNECTION AND CONFIG CONSTANTS*/
    private HttpURLConnection conn = null;
    private DataOutputStream dos = null;
    private Request request;
    private Response response;
    private FileUploadListener fileListener;
    private boolean SSEActive = false;

    /*BYTES FILE CONSTANTS*/
    private int bytesRead, bytesAvailable, bufferSize;
    private byte[] buffer;
    private int maxBufferSize = 1 * 1024;

    public Sysco(Request request) {
        this(request, null);
    }

    public Sysco(Request request, FileUploadListener listener) {
        this.request = request;
        this.fileListener = listener;
    }

    public void sendRequest(final RequestListener requestListener) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            sendRequest(NEW_THREAD, requestListener);
        } else {
            sendRequest(NOT_THREAD, requestListener);
        }
    }

    public void sendRequest(@RequestOptions int options, final RequestListener requestListener) {
        if (options == NOT_THREAD && Looper.myLooper() == Looper.getMainLooper()) {
            if (requestListener != null) {
                requestListener.onError(request.getUrl(), new UnsupportedOperationException("Operação de rede na thread principal não é permitido"));
            } else {
                throw new UnsupportedOperationException("Operação de rede na thread principal não é permitido");
            }
        }
        if (options == NEW_THREAD) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    sendCore(requestListener);
                }
            }).start();
        } else {
            sendCore(requestListener);
        }
    }

    private void sendCore(RequestListener requestListener) {
        if (requestListener == null) {
            throw new NullPointerException("requestListener = null");
        }
        String exception = Check.isValidURL(request.getUrl());
        if (!exception.equals("")) {
            requestListener.onError(request.getUrl(), new Exception(exception));
            return;
        }
        if (request.isValidMethod()) {
            requestListener.onError(request.getUrl(), new UnsupportedOperationException("You can not send POST or FILES for GET method"));
            return;
        }
        try {
            conn = request.build();

            if (request.haveDataForSend()) {
                dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(twoHyphens + boundary + lineEnd);

                preparePOSTStrings();
                preparePOSTSFiles();

                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            }

            Map headerFields = conn.getHeaderFields();
            SSEActive = conn.getContentType().contains("text/event-stream");

            int responseCode = conn.getResponseCode();
            String responseMessage = conn.getResponseMessage();

            if (SSEActive) {
                requestListener.onError(request.getUrl(), new UnsupportedOperationException("SSE unsupported in sendRequest usage sendSSE"));
                disconnect();
                return;
            }

            InputStream responseStream;
            if (responseCode == HttpURLConnection.HTTP_OK) {
                responseStream = conn.getInputStream();
            } else {
                responseStream = conn.getErrorStream();
            }

            response = new Response(headerFields, responseCode, responseMessage, responseStream, conn.getContentType(), conn.getContentLength());

            requestListener.onSuccess(request.getUrl(), response);

            if (dos != null) {
                dos.flush();
                dos.close();
            }
        } catch (Exception e) {
            requestListener.onError(request.getUrl(), e);
        }

        disconnect();
    }

    public void sendSSE(final SSEListener sseListener) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            sendSSE(NEW_THREAD, sseListener);
        } else {
            sendSSE(NOT_THREAD, sseListener);
        }
    }

    public void sendSSE(@RequestOptions int options, final SSEListener sseListener) {
        if (options == NOT_THREAD && Looper.myLooper() == Looper.getMainLooper()) {
            if (sseListener != null) {
                sseListener.onError(new UnsupportedOperationException("Operação de rede na thread principal não é permitido"));
            } else {
                throw new UnsupportedOperationException("Operação de rede na thread principal não é permitido");
            }
        }
        if (options == NEW_THREAD) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    sendSSECore(sseListener);
                }
            }).start();
        } else {
            sendSSECore(sseListener);
        }
    }

    private void sendSSECore(SSEListener sseListener) {
        if (sseListener == null) {
            throw new NullPointerException("sseListener = null");
        }
        String exception = Check.isValidURL(request.getUrl());
        if (!exception.equals("")) {
            sseListener.onError(new Exception(exception));
            return;
        }
        if (request.isValidMethod()) {
            sseListener.onError(new UnsupportedOperationException("You can not send POST or FILES for GET method"));
            return;
        }
        try {
            URL Url = new URL(request.getUrl());
            Log.d("URL", Url.toString());
            conn = (HttpURLConnection) Url.openConnection();

            Map headerFields = conn.getHeaderFields();
            SSEActive = conn.getContentType().contains("text/event-stream");

            int responseCode = conn.getResponseCode();
            String responseMessage = conn.getResponseMessage();

            if (!SSEActive) {
                sseListener.onError(new UnsupportedOperationException("SSE not available in connection in usage sendRequest"));
                disconnect();
                return;
            }

            InputStream responseStream;
            if (responseCode == HttpURLConnection.HTTP_OK) {
                responseStream = conn.getInputStream();
            } else {
                responseStream = conn.getErrorStream();
            }

            response = new Response(headerFields, responseCode, responseMessage, responseStream, conn.getContentType(), conn.getContentLength());
            getResponse(sseListener);

            if (dos != null) {
                dos.flush();
                dos.close();
            }
        } catch (Exception e) {
            sseListener.onError(e);
        }

        disconnect();
    }

    public int disconnect() {
        if (conn != null) {
            conn.disconnect();
        }
        return 0;
    }

    private void preparePOSTStrings() throws IOException {
        Map<String, ?> arrayPost = request.getArray(RequestMethods.POST);
        for (String key : arrayPost.keySet()) {
            String value = URLEncoder.encode((String)arrayPost.get(key), "UTF-8");

            dos.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"" + lineEnd);
            dos.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
            dos.writeBytes("Content-Length: " + value.length() + lineEnd);
            dos.writeBytes(lineEnd);
            dos.writeBytes(value); // mobile_no is String variable
            dos.writeBytes(lineEnd);

            dos.writeBytes(twoHyphens + boundary + lineEnd);
        }

    }

    private void preparePOSTSFiles() {
        Map<String, ?> arrayFiles = request.getArray(RequestMethods.FILES);
        for (String key : arrayFiles.keySet()) {
            File file = (File)arrayFiles.get(key);
            try {
                if (fileListener != null) {
                    fileListener.onStart(key, file.getName());
                }
                FileInputStream fileInputStream = new FileInputStream(file);

                dos.writeBytes("Content-Disposition: form-data; name=\"" + key + "\";filename=\"" + file.getName() + "\"" + lineEnd);
                dos.writeBytes("Content-Type: " + FileUtils.getMimeType(file.getPath()) + "; charset=UTF-8" + lineEnd);
                dos.writeBytes(lineEnd);
                // create a buffer of maximum size
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];
                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);

                    int progress = (bytesRead * 100 / bytesAvailable);

                    if (fileListener != null) {
                        fileListener.onProgressChange(key, file.getName(), progress);
                    }

                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    if (progress > 5 && fileListener != null) {
                        try {
                            Thread.sleep(800);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                fileInputStream.close();
                dos.writeBytes(lineEnd);
                if (fileListener != null) {
                    fileListener.onFinish(key, file.getName(), RESULT_OK);
                }
                if (arrayFiles.size() > 1) {
                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                }
            } catch (Exception e) {
                fileListener.onError(key, file.getName(), e);
                return;
            }
        }
    }

    public void setFileUploadListener(FileUploadListener listener) {
        this.fileListener = listener;
    }

    private String getResponse(SSEListener sseListener) throws IOException {
        if (response == null) {
            return "null";
        } else {
            InputStream tmpRes = response.getResponseStream();
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(tmpRes));
            String line = "";
            while ((line = reader.readLine()) != null) {
                if (sseListener != null && SSEActive) {
                    sseListener.onReadLine(response, line + "\n");
                }
            }
            return sb.toString();
        }
    }

    @Override
    public String toString() {
        try {
            return "REQUEST: "+request.toString() + ",\n HEADER: " + response.getHeaderFieldsToString() + ",\n RESPONSE: " + response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return Log.getStackTraceString(e.fillInStackTrace());
        }
    }

}
