package br.com.ygsoftware.sysco;

import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.security.InvalidParameterException;

import br.com.ygsoftware.sysco.interfaces.DownloadListener;
import br.com.ygsoftware.sysco.model.RequestDownload;
import br.com.ygsoftware.sysco.utils.Check;

public class DownloadFileFromUrl {

    private Context context;
    private File outputFile;
    private String url = "";
    private DownloadListener listener;
    private RequestDownload request;

    private NotificationManager notifyMng;
    private NotificationCompat.Builder notification;

    private int notificationId = 0;

    public static DownloadFileFromUrl with(Context context) {
        return new DownloadFileFromUrl(context);
    }

    private DownloadFileFromUrl(Context context) {

        this.context = context;

        notificationId = hashCode();
    }

    public DownloadFileFromUrl config(RequestDownload request) {
        if (!request.isDownloading()) {
            Check.isValidURL(request.getUrl(), "");
            if (request.getOutputFile() == null) {
                throw new InvalidParameterException("Output file is null");
            }

            this.url = request.getUrl();
            this.outputFile = request.getOutputFile();
            this.request = request;
        }
        return this;
    }

    public DownloadFileFromUrl listener(DownloadListener listener) {
        if (!request.isDownloading()) {
            this.listener = listener;
        }
        return this;
    }

    public void start() {
        if (!request.isDownloading()) {
            request.start();
            if (listener != null) {
                if (outputFile.exists()) {
                    boolean substitue = this.listener.onFileExists(this.request, outputFile);
                    if (substitue) {
                        outputFile.delete();
                    } else {
                        File[] downDir = outputFile.getParentFile().listFiles();
                        int filesName = 0;
                        for (File check : downDir) {
                            if (check.getName().contains(outputFile.getName())) {
                                filesName++;
                            }
                        }
                        outputFile = new File(outputFile.getParentFile(), outputFile.getName() + "(" + filesName + ")");
                    }
                }
            }
            new AsyncTask<Void, Integer, String>() {
                /**
                 * Before starting background thread
                 * Show Progress Bar Dialog
                 */
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();

                    notifyMng = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                    if (listener != null) {
                        notification = listener.onStartDownload(request, outputFile);
                        DownloadFileFromUrl.this.notify(notification);
                    }
                }

                /**
                 * Downloading file in background thread
                 */
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
                            publishProgress((int) ((total * 100) / lenghtOfFile));

                            // writing data to file
                            output.write(data, 0, count);
                        }

                        // flushing output
                        output.flush();

                        // closing streams
                        output.close();
                        input.close();

                    } catch (Exception e) {
                        if (listener != null) {
                            notification = listener.onErrorDownload(request, notification, e, outputFile);
                            DownloadFileFromUrl.this.notify(notification);
                        }
                    }

                    return null;
                }

                /**
                 * Updating progress bar
                 */
                protected void onProgressUpdate(Integer... progress) {
                    // setting progress percentage
                    if (listener != null) {
                        notification = listener.onProgressUpdate(request, notification, progress[0], outputFile);
                        DownloadFileFromUrl.this.notify(notification);
                    }
                }

                /**
                 * After completing background task
                 * Dismiss the progress dialog
                 **/
                @Override
                protected void onPostExecute(String file_url) {
                    // dismiss the dialog after the file was downloaded
                    if (listener != null) {
                        notification = listener.onFinishDownload(request, notification, outputFile);
                        DownloadFileFromUrl.this.notify(notification);
                    }
                }
            }.execute();
        }
    }

    private void notify(NotificationCompat.Builder notification) {
        if (notification != null) {
            notifyMng.notify(notificationId, notification.build());
        }
    }

}