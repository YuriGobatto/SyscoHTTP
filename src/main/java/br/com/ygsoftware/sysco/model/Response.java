package br.com.ygsoftware.sysco.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by adriana on 03/01/2017.
 */

public class Response implements Parcelable {

    private int responseCode;
    private String responseMessage;
    private InputStream responseStream;
    private String responseString;
    private String contentType;
    private int contentLength;
    private Map<String, List<String>> headerFields = null;

    private String[] VALID_IMAGE_TYPES = {"image/jpeg", "image/bmp", "image/gif", "image/jpg", "image/png"};
    private String[] VALID_AUDIO_TYPES = {"audio/mpeg", "audio/ogg", "audio/x-wav"};
    private String[] VALID_VIDEO_TYPES = {"video/wav", "video/mp4"};

    public Response(Map<String, List<String>> headerFields, int responseCode, String responseMessage, InputStream responseStream, String contentType, int contentLength) {
        this.headerFields = headerFields;
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.contentType = contentType;
        this.responseStream = responseStream;
        this.responseString = StreamToString();
        this.contentLength = contentLength;
    }

    protected Response(Parcel in) {
        responseCode = in.readInt();
        responseMessage = in.readString();
        responseString = in.readString();
        contentType = in.readString();
        contentLength = in.readInt();
    }

    public static final Creator<Response> CREATOR = new Creator<Response>() {
        @Override
        public Response createFromParcel(Parcel in) {
            return new Response(in);
        }

        @Override
        public Response[] newArray(int size) {
            return new Response[size];
        }
    };

    public Map<String, List<String>> getHeaderFields() {
        return headerFields;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public InputStream getResponseStream() {
        return responseStream;
    }

    public boolean isMediaType(){
        String[] types = concatenate(VALID_IMAGE_TYPES, VALID_AUDIO_TYPES, VALID_VIDEO_TYPES);
        for(String type : types){
            if(contentType.toLowerCase().contains(type.toLowerCase())){
                return true;
            }
        }
        return false;
    }

    private  <T> T[] concatenate (T[]... a) {
        int allLength = 0;

        int dstPos = 0;

        for(int i = 0; i < a.length; i++){
            allLength += a[i].length;
        }

        @SuppressWarnings("unchecked")
        T[] to = (T[]) Array.newInstance(a[0].getClass().getComponentType(), allLength);
        for(T[] from : a) {
            System.arraycopy(from, 0, to, dstPos, from.length);
            dstPos += from.length;
            Log.d("Length", to.length+"-"+dstPos);
            Log.d("Array From", Arrays.toString(from));
            Log.d("Array To", Arrays.toString(to));
        }

        return to;
    }

    public String getResponseString() {
        return responseString;
    }

    public String getContentType() {
        return contentType;
    }

    public int getContentLength() {
        return contentLength;
    }

    private String StreamToString() {
        if (contentType.contains("image/")) {
            return "Image File";
        } else {
            try {
                if (responseStream == null) {
                    return "null";
                } else {
                    InputStream tmpRes = responseStream;
                    StringBuilder sb = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(tmpRes));
                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    return sb.toString();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "Exception: " + e.getMessage();
            }
        }
    }

    public String getHeaderFieldsToString() {
        if (headerFields != null) {
            StringBuilder sb = new StringBuilder();

            Iterator<String> keys = headerFields.keySet().iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                List<String> values = headerFields.get(key);
                sb.append("\"");
                sb.append(key);
                sb.append("\"");
                sb.append("\n{\n");
                for (String value : values) {
                    sb.append("\t\t");
                    sb.append(value);
                    sb.append(",\n");
                }
                int virgule = sb.lastIndexOf(",");
                sb.replace(virgule, (virgule + 1), "");
                sb.append("},\n");
            }

            return sb.toString().substring(0, (sb.length() - 2));
        } else {
            return "null";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Response response = (Response) o;

        if (responseCode != response.responseCode) return false;
        if (responseMessage != null ? !responseMessage.equals(response.responseMessage) : response.responseMessage != null)
            return false;
        if (responseStream != null ? !responseStream.equals(response.responseStream) : response.responseStream != null)
            return false;
        return responseString != null ? responseString.equals(response.responseString) : response.responseString == null;

    }

    @Override
    public int hashCode() {
        int result = responseCode;
        result = 31 * result + super.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Response{" +
                "\n headerFields='" + getHeaderFieldsToString() + '\'' +
                "\n, responseCode=" + responseCode +
                "\n, responseMessage='" + responseMessage + '\'' +
                "\n, responseString='" + responseString + '\'' +
                "\n, contentType='" + contentType + '\'' +
                "\n, contentLength=" + contentLength +
                "\n}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(responseCode);
        dest.writeString(responseMessage);
        dest.writeString(responseString);
        dest.writeString(contentType);
        dest.writeInt(contentLength);
    }
}
