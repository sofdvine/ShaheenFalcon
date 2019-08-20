package com.sid.shaheenfalcon;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.frostwire.jlibtorrent.TorrentBuilder;
import com.github.se_bastiaan.torrentstream.StreamStatus;
import com.github.se_bastiaan.torrentstream.Torrent;
import com.github.se_bastiaan.torrentstream.TorrentOptions;
import com.github.se_bastiaan.torrentstream.TorrentStream;
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener;
import com.google.android.exoplayer2.util.Log;

public class TorrentStreamActivity extends AppCompatActivity implements TorrentListener {

    private String streamUrl = "magnet:?xt=urn:btih:88594aaacbde40ef3e2510c47374ec0aa396c08e&dn=bbb%5Fsunflower%5F1080p%5F30fps%5Fnormal.mp4&tr=udp%3A%2F%2Ftracker.openbittorrent.com%3A80%2Fannounce&tr=udp%3A%2F%2Ftracker.publicbt.com%3A80%2Fannounce&ws=http%3A%2F%2Fdistribution.bbb3d.renderfarming.net%2Fvideo%2Fmp4%2Fbbb%5Fsunflower%5F1080p%5F30fps%5Fnormal.mp4";

    private TorrentStream torrentStream;

    private ProgressBar progressBar;

    private TextView txtProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_torrent_stream);

        progressBar = (ProgressBar) findViewById(R.id.torrent_progress_buffering);
        txtProgress = (TextView) findViewById(R.id.torrent_buffering_percent);

        if(getIntent().getData() != null){
            streamUrl = getIntent().getData().toString();
        }

        TorrentOptions torrentOptions = new TorrentOptions.Builder()
                .saveLocation(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
                .removeFilesAfterStop(true)
                .autoDownload(false)
                .build();

        torrentStream = TorrentStream.init(torrentOptions);

        torrentStream.addListener(this);

        torrentStream.startStream(streamUrl);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    @Override
    public void onStreamPrepared(Torrent torrent) {
        Log.d("TORRENTSTREAM", torrent.getVideoFile().getName());
    }

    @Override
    public void onStreamStarted(Torrent torrent) {
        Log.d("TORRENTSTREAM", torrent.getVideoFile().getName() + " stream started!!!");

    }

    @Override
    public void onStreamError(Torrent torrent, Exception e) {
        Log.d("TORRENTSTREAM", torrent.getVideoFile().getName() + " Error: " + e.getLocalizedMessage());
    }

    @Override
    public void onStreamReady(Torrent torrent) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.PREFER_EXTENSION_DECODERS_EXTRA, true);
        intent.putExtra(PlayerActivity.ABR_ALGORITHM_EXTRA, PlayerActivity.ABR_ALGORITHM_DEFAULT);
        intent.setData(Uri.parse(torrent.getVideoFile().toString()));
        this.startActivity(intent);
    }

    @Override
    public void onStreamProgress(Torrent torrent, StreamStatus status) {
        if(status.bufferProgress <= 100 && progressBar.getProgress() < 100 && progressBar.getProgress() != status.bufferProgress) {
            progressBar.setProgress(status.bufferProgress);
            txtProgress.setText(status.bufferProgress + "%");
        }
    }

    @Override
    public void onStreamStopped() {

    }
}
