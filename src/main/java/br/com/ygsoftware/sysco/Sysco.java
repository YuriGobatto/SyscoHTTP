package br.com.ygsoftware.sysco;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
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

    private static final Signature SIG_RELEASE = new Signature("3082037930820261a00302010202041691595a300d06092a864886f70d01010b0500306c310b3009060355040613023535311430120603550408130b4d61746f2047726f73736f310e300c0603550407130553696e6f70310f300d060355040a13065947536f6674310f300d060355040b13065947536f6674311530130603550403130c5975726920476f626174746f3020170d3135313131393231313534315a180f33303135303332323231313534315a306c310b3009060355040613023535311430120603550408130b4d61746f2047726f73736f310e300c0603550407130553696e6f70310f300d060355040a13065947536f6674310f300d060355040b13065947536f6674311530130603550403130c5975726920476f626174746f30820122300d06092a864886f70d01010105000382010f003082010a028201010085b1045e0a4bb149ea0bdf0d2675b5d5211f151ced025bf2da89e607c0475270b88647c9676aa7f90ab08a18c3301bd8c610e640aedc7909056dcd82c62f199d7c2bda6f57699aef59b379aa9519e87ea5f64f4d41af88746768f351f753bb1321b8b2115b5e90d8889d11ade82adb069b5efb198d4b53e3526dce21c4e8103b0f91be2ab3cf35e68bb9eb2d173afd5bbd1455b634c9623ffcea9cccc59c3faf64e3132bab400fa73e245435916112503643a249d571cb8faef5146636be4621e7fbb561371dd3e5dadcabca2611476a9bab9a708bcf8259ff8c4f80f0bcaae8dc9d325040fed316ac0c7724d1062cec89dce61b013a91d9a8fad4712771d3e70203010001a321301f301d0603551d0e04160414c061ef35bf8ba4cea7ad5cbfefa67447aa2dbeb4300d06092a864886f70d01010b05000382010100499f1b1aa1de3a0d6df770bcecc717d6875850333ca267e1c7768cafa05a74a0739585c4981f5b7046f87a9ac941dd404874c0e63c17b9be9889390bb21389a604113ffd0f71425e6d7d54cbcca0a8b6da80e8c27a9c0c62c89959ea9b3ac68affb92704d5f2da2557c216dbd70bcd787a36df313738b871d4b9be29981146c36f1d62fe77f1b98fe6f32141165c6580703d2bb0a037b1ef762d352104214fa57167d71194e76daef937ef4c6644b128ff16fb77ab53ec7e259b6beae5e1212cf8d1dd19efb254ac961142d65acf58b82930a1c43645289891d7abf131f670a3afc32d0ba4c10568337c6ed2221cf54c06a2cfb39e76b55b917369317d105c59");
    public static Context This = null;
    public static boolean APP_DEBUG = isDebug(This);
    public static String BASE_URL = generateBaseUrl("offlineUrl", "onlineUrl");

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
    private static Config config;

    /*BYTES FILE CONSTANTS*/
    private int bytesRead, bytesAvailable, bufferSize;
    private byte[] buffer;
    private int maxBufferSize = 1 * 1024;

    public static void setConfig(Config config) {
        Sysco.config = config;
        This = config.context;
        BASE_URL = config.baseUrl;
    }

    public static Config getConfig() {
        return config;
    }

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
        Map<String, String> arrayPost = request.getArray(RequestMethods.POST);
        Map<String, String[]> arrayPosts = request.getParameterArrays(RequestMethods.POST);

        for (String key : arrayPost.keySet()) {
            String value = (String) arrayPost.get(key);

            dos.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"" + lineEnd);
            dos.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
            dos.writeBytes("Content-Length: " + value.length() + lineEnd);
            dos.writeBytes(lineEnd);
            dos.writeBytes(value); // mobile_no is String variable
            dos.writeBytes(lineEnd);

            dos.writeBytes(twoHyphens + boundary + lineEnd);
        }

        for (String key : arrayPosts.keySet()) {
            for (String value : arrayPosts.get(key)) {
                dos.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"" + lineEnd);
                dos.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
                dos.writeBytes("Content-Length: " + value.length() + lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes(value); // mobile_no is String variable
                dos.writeBytes(lineEnd);

                dos.writeBytes(twoHyphens + boundary + lineEnd);
            }
        }

    }

    private void preparePOSTSFiles() {
        Map<String, File> arrayFiles = request.getArray(RequestMethods.FILES);
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

    public static boolean isDebug(Context context) {
        if (context != null) {
            try {
                PackageManager pm = context.getPackageManager();
                PackageInfo pi = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
                for (Signature sig : pi.signatures) {
                    if (sig.equals(SIG_RELEASE)) {
                        return false;
                    }
                }
            } catch (Exception e) {
                // Return false if we can't figure it out, just to play it safe
                return true;
            }
        }
        return true;
    }

    public static String getSignature(Context context) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
            return "App Signature: " + pi.signatures[0].toCharsString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "Error";
        }
    }

    private static String generateBaseUrl(String debugUrl, String releaseUrl) {
        return (APP_DEBUG ? debugUrl : releaseUrl);
    }

    public static class Config {

        private Context context;
        protected String baseUrl;
        private String onlineUrl;
        private String offlineUrl;
        private boolean enableBaseUrl;

        public Config(Context context, String offlineUrl, String onlineUrl) {
            this(context, offlineUrl, onlineUrl, true);
        }

        public Config(Context context, String offlineUrl, String onlineUrl, boolean enableBaseUrl) {
            Sysco.APP_DEBUG = isDebug(context);
            this.context = context;
            this.offlineUrl = offlineUrl;
            this.onlineUrl = onlineUrl;
            this.enableBaseUrl = enableBaseUrl;
            baseUrl = generateBaseUrl(offlineUrl, onlineUrl);
        }

        public boolean isEnableBaseUrl() {
            return enableBaseUrl;
        }

        protected Context getContext() {
            return context;
        }
    }

}
