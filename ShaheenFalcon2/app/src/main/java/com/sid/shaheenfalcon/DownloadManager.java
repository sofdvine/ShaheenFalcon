package com.sid.shaheenfalcon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.google.android.exoplayer2.util.Log;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2core.DownloadBlock;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DownloadManager extends AppCompatActivity {

    RecyclerView recyclerView;
    DownloadManagerUIReceiver dmuiReceiver;
    private ArrayList<Download> downloads;
    private HashMap<Integer, Integer> downloadMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_manager);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        downloads = new ArrayList<Download>();
        downloadMap = new HashMap<Integer, Integer>();

        recyclerView = (RecyclerView) findViewById(R.id.download_manager_list);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        //llm.setStackFromEnd(true);
        llm.setReverseLayout(true);
        recyclerView.setLayoutManager(llm);

        Intent intent = new Intent("com.sid.ShaheenFalcon.DownloaderService");
        intent.putExtra("QUERY_DOWNLOAD_LIST", true);
        sendBroadcast(intent);
        recyclerView.setAdapter(new DownloadItemAdapter(this, downloads));


        dmuiReceiver = new DownloadManagerUIReceiver(this, new Handler());
        registerReceiver(dmuiReceiver, new IntentFilter("com.sid.ShaheenFalcon.DownloadManager"));


    }

    public class DownloadManagerUIReceiver extends BroadcastReceiver{

        private Context context;
        private Handler handler;

        public DownloadManagerUIReceiver(Context context, Handler handler) {
            this.context = context;
            this.handler = handler;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.hasExtra("DOWNLOAD_LIST")){
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        downloads.clear();
                        ArrayList<Download> result = (ArrayList<Download>)intent.getSerializableExtra("DOWNLOAD_LIST");
                        downloads.addAll(result);
                        for(int i = 0; i < result.size(); i++){
                            downloadMap.put(result.get(i).getId(), i);
                        }
                        recyclerView.getAdapter().notifyDataSetChanged();
                        Log.d("DOWNLOAD_MANAGER", "RECEIVED " + downloads.size());
                    }
                });
            }else if(intent.hasExtra("DOWNLOAD_PROGRESS")){
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Download download = (Download) intent.getSerializableExtra("DOWNLOAD_PROGRESS");
                        if (downloadMap.containsKey(download.getId())) {
                            downloads.set(downloadMap.get(download.getId()), download);
                            //recyclerView.getAdapter().notifyItemChanged(downloadMap.get(download.getId()));
                            ProgressBar pb = recyclerView.getChildAt(downloadMap.get(download.getId())).findViewById(R.id.download_manager_list_item_progressbar);
                            pb.setProgress(download.getProgress());
                            String text = Gonona.textBytes(download.getDownloaded()) + "/" + Gonona.textBytes(download.getTotal()) + "";
                            if(download.getDownloaded() < download.getTotal()){
                                text += " @" + Gonona.textBytes(download.getDownloadedBytesPerSecond()) + "/sec " + Gonona.textMiliseconds(download.getEtaInMilliSeconds());
                            }
                            TextView tv = recyclerView.getChildAt(downloadMap.get(download.getId())).findViewById(R.id.download_manager_list_item_progresstext);
                            tv.setText(text);
                        }
                    }
                });
            }
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        registerReceiver(dmuiReceiver, new IntentFilter("com.sid.ShaheenFalcon.DownloadManager"));
    }

    @Override
    protected void onStop() {
        try {
            unregisterReceiver(dmuiReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onStop();
    }
}
