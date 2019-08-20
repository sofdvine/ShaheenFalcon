package com.sid.shaheenfalcon;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.VideoView;

public class BasicVideoPlayer extends AppCompatActivity {

    VideoView basicVideoView = null;
    MediaController mediaController = null;
    Button btnOrientation = null, btnFullscreen = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_video_player);

        basicVideoView = (VideoView) findViewById(R.id.video_player);
        //btnFullscreen = (Button) findViewById(R.id.btn_fullscreen);
        //btnOrientation = (Button) findViewById(R.id.btn_orientation);

        if(mediaController == null){
            mediaController = new MediaController(BasicVideoPlayer.this);
            mediaController.setAnchorView(basicVideoView);
        }

        basicVideoView.setMediaController(mediaController);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);


        if(getIntent().hasExtra("VIDEO_URL")){
            basicVideoView.setVideoURI(Uri.parse(getIntent().getStringExtra("VIDEO_URL")));
        }

        basicVideoView.start();

        /*btnOrientation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        btnFullscreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getSupportActionBar().hide();
                btnOrientation.setVisibility(View.INVISIBLE);
                btnFullscreen.setVisibility(View.INVISIBLE);
            }
        });*/
    }
}
