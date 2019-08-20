package com.sid.shaheenfalcon;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;

import com.google.android.exoplayer2.util.Log;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.HttpUrlConnectionDownloader;
import com.tonyodev.fetch2.PrioritySort;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2.Status;
import com.tonyodev.fetch2.database.DownloadInfo;
import com.tonyodev.fetch2.database.FetchDatabaseManager;
import com.tonyodev.fetch2.database.FetchDatabaseManagerImpl;
import com.tonyodev.fetch2.database.FetchDatabaseManagerWrapper;
import com.tonyodev.fetch2.database.migration.Migration;
import com.tonyodev.fetch2.fetch.FetchImpl;
import com.tonyodev.fetch2.fetch.LiveSettings;
import com.tonyodev.fetch2core.DefaultStorageResolver;
import com.tonyodev.fetch2core.DownloadBlock;
import com.tonyodev.fetch2core.Downloader;
import com.tonyodev.fetch2core.Extras;
import com.tonyodev.fetch2core.Func;
import com.tonyodev.fetch2core.Logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kotlin.Pair;

public class DownloaderService extends Service {


    private Fetch sfFetch;

    public DownloaderService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {

        Logger logger = new Logger() {
            @Override
            public boolean getEnabled() {
                return true;
            }

            @Override
            public void setEnabled(boolean b) {

            }

            @Override
            public void d(@NotNull String s) {
                Log.d("DOWNLOADER_SERVICE", s);
            }

            @Override
            public void d(@NotNull String s, @NotNull Throwable throwable) {
                Log.d("DOWNLOADER_SERVICE", s + ": " + throwable.getLocalizedMessage());
            }

            @Override
            public void e(@NotNull String s) {
                Log.d("DOWNLOADER_SERVICE", s);
            }

            @Override
            public void e(@NotNull String s, @NotNull Throwable throwable) {
                Log.d("DOWNLOADER_SERVICE", s + ": " + throwable.getLocalizedMessage());
            }
        };
        LiveSettings settings = new LiveSettings("DownloaderService");

        File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/sfDownloadStorage/");
        if(!f.exists()){
            f.mkdirs();
        }
        DefaultStorageResolver defaultStorageResolver = new DefaultStorageResolver(this, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/sfDownloadStorage/");

        FetchConfiguration configuration = new FetchConfiguration.Builder(this)
                .enableAutoStart(true)
                .enableFileExistChecks(true)
                .setDownloadConcurrentLimit(10)
                .preAllocateFileOnCreation(true)
                .setAutoRetryMaxAttempts(5)
                .enableRetryOnNetworkGain(true)
                .setHttpDownloader(new HttpUrlConnectionDownloader(){
                    @NotNull
                    @Override
                    public FileDownloaderType getRequestFileDownloaderType(@NotNull ServerRequest request, @NotNull Set<? extends FileDownloaderType> supportedFileDownloaderTypes) {
                        return  FileDownloaderType.PARALLEL;
                        //return super.getRequestFileDownloaderType(request, supportedFileDownloaderTypes);
                    }

                    @Override
                    public boolean getHeadRequestMethodSupported(@NotNull ServerRequest request) {
                        return super.getHeadRequestMethodSupported(request);
                    }

                    @Nullable
                    @Override
                    public Integer getFileSlicingCount(@NotNull ServerRequest request, long contentLength) {
                        return super.getFileSlicingCount(request, contentLength);
                    }
                })
                .setDatabaseManager(new FetchDatabaseManagerImpl(this, "DownloaderService", logger, new Migration[]{}, settings, true, defaultStorageResolver))
                .build();

        sfFetch = Fetch.Impl.getInstance(configuration);
        sfFetch.addListener(new FetchListener() {
            @Override
            public void onAdded(@NotNull Download download) {

            }

            @Override
            public void onQueued(@NotNull Download download, boolean b) {
                Log.d("SFFETCH", "QUEUED");
                Intent intent1 = new Intent("com.sid.ShaheenFalcon.DownloadManager");
                intent1.putExtra("DOWNLOAD_QUEUED", (Serializable) download);
                sendBroadcast(intent1);
            }

            @Override
            public void onWaitingNetwork(@NotNull Download download) {

            }

            @Override
            public void onCompleted(@NotNull Download download) {

            }

            @Override
            public void onError(@NotNull Download download, @NotNull Error error, @Nullable Throwable throwable) {
                Log.d("SFFETCH", error.toString());
            }

            @Override
            public void onDownloadBlockUpdated(@NotNull Download download, @NotNull DownloadBlock downloadBlock, int i) {

            }

            @Override
            public void onStarted(@NotNull Download download, @NotNull List<? extends DownloadBlock> list, int i) {
                Log.d("SFFETCH", "STARTED");
            }

            @Override
            public void onProgress(@NotNull Download download, long l, long l1) {
                Log.d("SFFETCH", "PROGRESS: " + l + "/" + l1);
                try {
                    Intent intent1 = new Intent("com.sid.ShaheenFalcon.DownloadManager");
                    intent1.putExtra("DOWNLOAD_PROGRESS", (Serializable) download);
                    sendBroadcast(intent1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onPaused(@NotNull Download download) {

            }

            @Override
            public void onResumed(@NotNull Download download) {

            }

            @Override
            public void onCancelled(@NotNull Download download) {

            }

            @Override
            public void onRemoved(@NotNull Download download) {

            }

            @Override
            public void onDeleted(@NotNull Download download) {

            }
        });

        DownloadBroadcastReceiver broadcastReceiver = new DownloadBroadcastReceiver(this);
        registerReceiver(broadcastReceiver, new IntentFilter("com.sid.ShaheenFalcon.DownloaderService"));

        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;

        //return super.onStartCommand(intent, flags, startId);
    }

    public void addDownload(String url, String filePath, Map<String, String> headers){
        Request request = new Request(url, filePath);
        for(String headerName: headers.keySet()) {
            request.addHeader(headerName, headers.get(headerName));
        }
        sfFetch.enqueue(request, new Func<Request>() {
            @Override
            public void call(@NotNull Request result) {

            }
        }, new Func<Error>() {
            @Override
            public void call(@NotNull Error result) {

            }
        });
    }



    public class DownloadBroadcastReceiver extends BroadcastReceiver{
        //private Handler handler;
        Context context;

        public DownloadBroadcastReceiver(Context context) {
            //this.handler = handler;
            this.context = context;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("DOWNLOADER_SERVICE", "RECEIVED");
            if(intent.hasExtra("ADD_DOWNLOAD")){
                DownloaderService.this.addDownload(intent.getStringExtra("DOWNLOAD_URL"),
                        intent.getStringExtra("DOWNLOAD_FILE")
                , (HashMap<String, String>) intent.getSerializableExtra("DOWNLOAD_HEADERS"));
                Log.d("DOWNLOADER_SERVICE", intent.getStringExtra("DOWNLOAD_URL"));
                Log.d("DOWNLOADER_SERVICE", intent.getStringExtra("DOWNLOAD_FILE"));
            }else if(intent.hasExtra("PAUSE_DOWNLOAD")){
            }else if(intent.hasExtra("QUERY_DOWNLOAD_LIST")){
                sfFetch.getDownloads(new Func<List<Download>>() {
                    @Override
                    public void call(@NotNull List<Download> result) {
                        ArrayList<Download> allDownloads = new ArrayList<Download>();
                        Intent intent1 = new Intent("com.sid.ShaheenFalcon.DownloadManager");
                        allDownloads.addAll(result);
                        intent1.putExtra("DOWNLOAD_LIST", allDownloads);
                        sendBroadcast(intent1);
                    }
                });
            }
        }
    }

}
