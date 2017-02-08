package br.com.ygsoftware.sysco.interfaces;

import android.app.Notification;

import java.io.File;

import br.com.ygsoftware.sysco.model.Request;

/**
 * Created by adriana on 08/02/2017.
 */

public interface DownloadListener {

    Notification onStartDownload(Request request, File outputFile);

    Notification onProgressUpdate(Request request, int progress, File outputFile);

    Notification onFinishDownload(Request request, File outputFile);

    Notification onErrorDownload(Request request, Exception error, File outputFile);

}
