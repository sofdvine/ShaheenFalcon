package com.sid.shaheenfalcon;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.offline.DownloadHelper;
import com.google.android.exoplayer2.offline.DownloadRequest;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.RandomTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.sid.shaheenfalcon.PlayerActivity.ABR_ALGORITHM_DEFAULT;
import static com.sid.shaheenfalcon.PlayerActivity.ABR_ALGORITHM_RANDOM;
import static com.sid.shaheenfalcon.PlayerActivity.PREFER_EXTENSION_DECODERS_EXTRA;

public class SFAudioService extends MediaBrowserServiceCompat {
    private static final int SF_AUDIO_ACTION_STOP = 0;
    private static final int SF_AUDIO_ACTION_PLAY = 1;
    private static final int SF_AUDIO_ACTION_PAUSE = 2;
    private SimpleExoPlayer sfPlayer;
    private  MediaSessionCompat mediaSessionCompat;

    private DataSource.Factory dataSourceFactory;
    private FrameworkMediaDrm mediaDrm;
    private MediaSource mediaSource;
    private DefaultTrackSelector trackSelector;
    private DefaultTrackSelector.Parameters trackSelectorParameters;

    private boolean startAutoPlay = true;
    private Notification.Builder notificationBuilder;
    private BroadcastReceiver sfAudioReceiver;
    private MediaSession.Token mediaSessionToken;

    private ArrayList<SFDBController.AudioMetadata> metadatas = new ArrayList<SFDBController.AudioMetadata>();
    private int currentAudioIdx = 0;
    private boolean doShuffle = false, doReplay = false;

    private static final String CHANNEL_ID = "media_playback_channel";
    private String lastPlayedUrl = "";

    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel() {
        NotificationManager
                mNotificationManager =
                (NotificationManager) this
                        .getSystemService(Context.NOTIFICATION_SERVICE);
        // The id of the channel.
        String id = CHANNEL_ID;
        // The user-visible name of the channel.
        CharSequence name = "Media playback";
        // The user-visible description of the channel.
        String description = "Media playback controls";
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel mChannel = new NotificationChannel(id, name, importance);
        // Configure the notification channel.
        mChannel.setDescription(description);
        mChannel.setShowBadge(false);
        mChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        mNotificationManager.createNotificationChannel(mChannel);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mediaSessionCompat, intent);
        if (intent.hasExtra("SF_AUDIO_URL")) {
            if(sfPlayer == null){
                SFAudioService.this.init();
            }
            SFAudioService.this.setAudio(intent.getStringExtra("SF_AUDIO_URL"), "", "");
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private DataSource.Factory buildDataSourceFactory() {
        return ((DemoApplication) getApplication()).buildDataSourceFactory();
    }

    private DataSource.Factory buildDataSourceFactory(HashMap<String, String> headers) {
        return ((DemoApplication) getApplication()).buildDataSourceFactory(headers);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initNotification();
        initReceiver();
        init();
    }

    private void init(){
        dataSourceFactory = buildDataSourceFactory();
        initExoAudioPlayer();
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        if(TextUtils.equals(clientPackageName, getPackageName())) {
            return new BrowserRoot(getString(R.string.app_name), null);
        }
        return null;
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {

    }

    private void initNotification(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
        }

        MediaSession mediaSession = new MediaSession(this, "SFAudioService");
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mediaSessionToken = mediaSession.getSessionToken();
        Intent i = new Intent("com.sid.ShaheenFalcon.SFAudioService");
        i.putExtra("SF_AUDIO_ACTION", SF_AUDIO_ACTION_PAUSE);
        PendingIntent pi = PendingIntent.getBroadcast(this, 69, i, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder = getNotificationBuilder(new Notification.Action[]{
                new Notification.Action(R.drawable.exo_icon_pause, "Pause", pi)
        });
    }

    private Notification.Builder getNotificationBuilder(Notification.Action[] actions){
        Notification.Builder builder;
        Intent stopIntent = new Intent("com.sid.ShaheenFalcon.SFAudioService");
        stopIntent.putExtra("SF_AUDIO_ACTION", SF_AUDIO_ACTION_STOP);
        PendingIntent pStopIntent = PendingIntent.getBroadcast(this, 89, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);

        }else{
            builder = new Notification.Builder(this);
        }

        builder.setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Track title")
                .setAutoCancel(true)
                .setContentText("Artist - Album").setStyle(new Notification.MediaStyle().setMediaSession(mediaSessionToken).setShowActionsInCompactView(new int[]{0, 1}));
        for(Notification.Action action : actions){
            builder.addAction(action);
        }
        builder.addAction(new Notification.Action(R.drawable.exo_icon_stop, "Stop", pStopIntent));
        return builder;
    }

    private void initReceiver(){
        sfAudioReceiver = new SFAudioBroadcastReceiver(this);
        registerReceiver(sfAudioReceiver, new IntentFilter("com.sid.ShaheenFalcon.SFAudioService"));
    }

    private void setAudio(String audioUrl, String audioTitle, String audioArtist){
//        mediaSource.releaseSource(new MediaSource.SourceInfoRefreshListener() {
//            @Override
//            public void onSourceInfoRefreshed(MediaSource source, Timeline timeline, @Nullable Object manifest) {
//
//            }
//        });
        mediaSource = buildMediaSource(Uri.parse(audioUrl));
        sfPlayer.prepare(mediaSource, true, false);
        notificationBuilder.setContentTitle(audioTitle)
        .setContentText(audioArtist);
        lastPlayedUrl = audioUrl;
    }

    private void initExoAudioPlayer(){
        Uri uris[] = new Uri[]{};
        TrackSelection.Factory trackSelectionFactory;
        String abrAlgorithm = ABR_ALGORITHM_DEFAULT; //For testing only... need to be changed
        if (abrAlgorithm == null || ABR_ALGORITHM_DEFAULT.equals(abrAlgorithm)) {
            trackSelectionFactory = new AdaptiveTrackSelection.Factory();
        } else if (ABR_ALGORITHM_RANDOM.equals(abrAlgorithm)) {
            trackSelectionFactory = new RandomTrackSelection.Factory();
        } else {
            return;
        }
        boolean preferExtensionDecoders = false;
        DemoApplication da = ((DemoApplication) getApplication());
        RenderersFactory renderersFactory =
                da.buildRenderersFactory(preferExtensionDecoders);

//        trackSelector = new DefaultTrackSelector(trackSelectionFactory);
//        trackSelector.setParameters(trackSelectorParameters);
//        sfPlayer = ExoPlayerFactory.newSimpleInstance(this, renderersFactory, trackSelector);
        sfPlayer = ExoPlayerFactory.newSimpleInstance(this);
//        player.addListener(new SFAudioService.PlayerEventListener());
        sfPlayer.setPlayWhenReady(startAutoPlay);
        sfPlayer.addListener(new PlayerEventListener());

        MediaSource[] mediaSources = new MediaSource[uris.length];
        for (int i = 0; i < uris.length; i++) {
            mediaSources[i] = buildMediaSource(uris[i]);
        }
        mediaSource =
                mediaSources.length == 1 ? mediaSources[0] : new ConcatenatingMediaSource(mediaSources);
//        sfPlayer.prepare(mediaSource, true, false);
    }

    private MediaSource buildMediaSource(Uri uri) {
        return buildMediaSource(uri, null);
    }

    private MediaSource buildMediaSource(Uri uri, @Nullable String overrideExtension) {
        DownloadRequest downloadRequest =
                ((DemoApplication) getApplication()).getDownloadTracker().getDownloadRequest(uri);
        if (downloadRequest != null) {
            return DownloadHelper.createMediaSource(downloadRequest, dataSourceFactory);
        }
        @C.ContentType int type = Util.inferContentType(uri, overrideExtension);
        switch (type) {
            case C.TYPE_DASH:
                return new DashMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            case C.TYPE_SS:
                return new SsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            case C.TYPE_OTHER:
                return new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            default:
                throw new IllegalStateException("Unsupported type: " + type);
        }
    }

    private void releasePlayer() {
        if (sfPlayer != null) {
            sfPlayer.release();
            sfPlayer = null;
            mediaSource = null;
            trackSelector = null;
        }
    }

    public boolean playNext(){
        if(doReplay || metadatas.size() > ++currentAudioIdx){
            currentAudioIdx = currentAudioIdx % metadatas.size();
            metadatas.get(currentAudioIdx).getAudioExec();
            return true;
        }
        return false;
    }

    public boolean playPrevious(){
        return false;
    }


    private class PlayerEventListener implements Player.EventListener {


        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            if (playbackState == Player.STATE_ENDED) {

            }else if (playbackState == Player.STATE_READY){
                if(sfPlayer.getPlayWhenReady()){
                    //playing
                    Log.d("READY", "PLAYING");
                    Intent i = new Intent("com.sid.ShaheenFalcon.SFAudioService");
                    i.putExtra("SF_AUDIO_ACTION", SF_AUDIO_ACTION_PAUSE);
                    Log.d("SF_AUDIO_ACTION", SF_AUDIO_ACTION_PAUSE + "");
                    PendingIntent pi = PendingIntent.getBroadcast(SFAudioService.this, 69, i, PendingIntent.FLAG_UPDATE_CURRENT);
                    notificationBuilder = SFAudioService.this.getNotificationBuilder(new Notification.Action[]{
                            new Notification.Action(R.drawable.exo_icon_pause, "Pause", pi)
                    });
                } else {
                    Intent i = new Intent("com.sid.ShaheenFalcon.SFAudioService");
                    i.putExtra("SF_AUDIO_ACTION", SF_AUDIO_ACTION_PLAY);
                    PendingIntent pi = PendingIntent.getBroadcast(SFAudioService.this, 69, i, PendingIntent.FLAG_UPDATE_CURRENT);
                    notificationBuilder = SFAudioService.this.getNotificationBuilder(new Notification.Action[]{
                            new Notification.Action(R.drawable.exo_icon_play, "Play", pi)
                    });
                }
                startForeground(1, notificationBuilder.build());
            }else if (playbackState == Player.STATE_IDLE){
                Intent i = new Intent("com.sid.ShaheenFalcon.SFAudioService");
                i.putExtra("SF_AUDIO_ACTION", SF_AUDIO_ACTION_PLAY);
                PendingIntent pi = PendingIntent.getBroadcast(SFAudioService.this, 69, i, PendingIntent.FLAG_UPDATE_CURRENT);
                notificationBuilder = SFAudioService.this.getNotificationBuilder(new Notification.Action[]{
                        new Notification.Action(R.drawable.exo_icon_play, "Play", pi)
                });
                startForeground(1, notificationBuilder.build());
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException e) {

        }

        @Override
        @SuppressWarnings("ReferenceEquality")
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

        }
    }

    public class SFAudioBroadcastReceiver extends BroadcastReceiver{

        private Context context;

        SFAudioBroadcastReceiver(Context context){
            this.context = context;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String audioUrl = "";
            if (intent.hasExtra("SF_AUDIO_URL")) {
                if(sfPlayer == null){
                    SFAudioService.this.init();
                }
                audioUrl = intent.getStringExtra("SF_AUDIO_URL");
                SFAudioService.this.setAudio(audioUrl, "", "");
            }else if(intent.hasExtra("SF_AUDIO_ACTION")){
                int requestedState = intent.getIntExtra("SF_AUDIO_ACTION", 1);
                switch (requestedState ){
                    case SF_AUDIO_ACTION_STOP:
                        sfPlayer.setPlayWhenReady(false);
                        releasePlayer();
                        stopForeground(true);
                        break;
                    case SF_AUDIO_ACTION_PLAY:
                        sfPlayer.setPlayWhenReady(true);
                        break;
                    case SF_AUDIO_ACTION_PAUSE:
                        sfPlayer.setPlayWhenReady(false);
                }
            }
            Log.d("SF_AUDIO_SERVICE", audioUrl);
        }
    }
}
