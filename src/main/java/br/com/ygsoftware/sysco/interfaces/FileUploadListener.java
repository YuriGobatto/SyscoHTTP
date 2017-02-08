package br.com.ygsoftware.sysco.interfaces;

/**
 * Created by adriana on 16/12/2016.
 */

public interface FileUploadListener {

    void onStart(String key, String fileName);

    void onProgressChange(String key, String fileName, int progress);

    void onFinish(String key, String fileName, int result);

    void onError(String key, String fileName, Exception e);

}
