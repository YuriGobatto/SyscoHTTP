package br.com.ygsoftware.sysco;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;

import br.com.ygsoftware.sysco.interfaces.DownloadListener;
import br.com.ygsoftware.sysco.model.Request;
import br.com.ygsoftware.sysco.model.post.PostFile;
import br.com.ygsoftware.sysco.utils.Check;

public class DownloadFileFromUrl extends AsyncTask<Void, Integer, String> {

    private Context context;
    private File outputFile;
    private String url = "";
    private DownloadListener listener;
    private Request request;

    private NotificationManager notifyMng;
    private NotificationCompat.Builder notification;

    private int notificationId = 0;

    public final static String FILE_OUTPUT_KEY = "br.com.ygsoftware.sysco.FILE_OUTPUT";

    public DownloadFileFromUrl(Context context, Request request, DownloadListener listener) {

        Check.isValidURL(request.getUrl(), "");

        this.context = context;
        this.outputFile = ((PostFile)request.getData().get(FILE_OUTPUT_KEY)).getValue();
        this.url = request.getUrl();
        this.request = request;

        notificationId = hashCode();
    }

    /**
     * Before starting background thread
     * Show Progress Bar Dialog
     * */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        notifyMng = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

        if(listener != null) {
            notification = listener.onStartDownload(request, outputFile);
        }
        if(notification == null) {
            notification = new NotificationCompat.Builder(context);
            notification.setContentTitle("Preparando Download");
            notification.setContentText("De: " + url);
        }
        notification.setProgress(0, 0, true);

        notifyMng.notify(notificationId, notification.build());
    }

    /**
     * Downloading file in background thread
     * */
    @Override
    protected String doInBackground(Void... p) {
        int count;
        try {
            URLConnection conection = request.build();
            // this will be useful so that you can show a tipical 0-100% progress bar
            int lenghtOfFile = conection.getContentLength();

            // download the file
            InputStream input = new BufferedInputStream(conection.getInputStream(), 8192);

            // Output stream
            OutputStream output = new FileOutputStream(outputFile);

            byte data[] = new byte[1024];

            long total = 0;

            while ((count = input.read(data)) != -1) {
                total += count;
                // publishing the progress....
                // After this onProgressUpdate will be called
                publishProgress((int)((total*100)/lenghtOfFile));

                // writing data to file
                output.write(data, 0, count);
            }

            // flushing output
            output.flush();

            // closing streams
            output.close();
            input.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Updating progress bar
     * */
    protected void onProgressUpdate(Integer... progress) {
        // setting progress percentage
        if(listener != null){
            notification = listener.onProgressUpdate(request, progress[0], outputFile);
            notifyMng.notify(notificationId, notification.build());
        }else{
            notification.setProgress(100, progress[0], false);
            notifyMng.notify(notificationId, notification.build());
        }
    }

    /**
     * After completing background task
     * Dismiss the progress dialog
     * **/
    @Override
    protected void onPostExecute(String file_url) {
        // dismiss the dialog after the file was downloaded
        if(listener != null){
            notification = listener.onFinishDownload(request, outputFile);
        }else {
            notification.setContentTitle("Donwload Concluido");
            notification.setContentText("Abrir " + outputFile.getName());

            Intent res = new Intent(Intent.ACTION_VIEW, Uri.parse(outputFile.getPath()));

            notification.setContentIntent(PendingIntent.getActivity(context, 0, res, PendingIntent.FLAG_UPDATE_CURRENT));
        }
        notifyMng.notify(notificationId, notification.build());
    }

}