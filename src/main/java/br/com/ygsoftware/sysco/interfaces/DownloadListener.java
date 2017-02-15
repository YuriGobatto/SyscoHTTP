package br.com.ygsoftware.sysco.interfaces;

import android.support.v4.app.NotificationCompat;

import java.io.File;

import br.com.ygsoftware.sysco.model.RequestDownload;

/**
 * Created by adriana on 08/02/2017.
 */

public interface DownloadListener {

    NotificationCompat.Builder onStartDownload(RequestDownload request, File outputFile);

    NotificationCompat.Builder onProgressUpdate(RequestDownload request, NotificationCompat.Builder notification, int progress, File outputFile);

    NotificationCompat.Builder onFinishDownload(RequestDownload request, NotificationCompat.Builder notification, File outputFile);

    NotificationCompat.Builder onErrorDownload(RequestDownload request, NotificationCompat.Builder notification, Exception error, File outputFile);

    boolean onFileExists(RequestDownload request, File outputFile);

}
