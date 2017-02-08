package br.com.ygsoftware.sysco.interfaces;

import android.support.v4.app.NotificationCompat;

import java.io.File;

import br.com.ygsoftware.sysco.model.Request;

/**
 * Created by adriana on 08/02/2017.
 */

public interface DownloadListener {

    NotificationCompat.Builder onStartDownload(Request request, File outputFile);

    NotificationCompat.Builder onProgressUpdate(Request request, int progress, File outputFile);

    NotificationCompat.Builder onFinishDownload(Request request, File outputFile);

}
