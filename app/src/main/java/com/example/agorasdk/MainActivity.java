package com.example.agorasdk;

import static io.agora.rtc2.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15;
import static io.agora.rtc2.video.VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE;
import static io.agora.rtc2.video.VideoEncoderConfiguration.STANDARD_BITRATE;
import static io.agora.rtc2.video.VideoEncoderConfiguration.VD_640x360;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.video.VideoEncoderConfiguration;


public class MainActivity extends AppCompatActivity {

    // Fill the App ID of your project generated on Agora Console.
    private final String appId = "ba182c0cdda4421bad5406e2fd613622";
    // Fill the channel name.
    private String channelName = "ar";
    // Fill the temp token generated on Agora Console.
    private String token = "007eJxTYJBVZWptvPbEQKRkVsiZE5Hf3qy6UZB+IcuReZ2K+LwdHdoKDEmJhhZGyQbJKSmJJiZGhkmJKaYmBmapRmkpZobGZkZGOsWqKQ2BjAxKtwwZGRkgEMRnYkgsYmAAANj1HSk=";
    // An integer that identifies the local user.
    private int uid = 0;
    private boolean isJoined = false;

    private RtcEngine agoraEngine;
    //SurfaceView to render local video in a Container.
    private SurfaceView localSurfaceView;
    //SurfaceView to render Remote video in a Container.
    private SurfaceView remoteSurfaceView;

    private static final int PERMISSION_REQ_ID = 22;
    private static final  String[] REQUESTED_PERMISSIONS=
            {
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA
            };
    private boolean checkSelfPermission()
    {
        if(ContextCompat.checkSelfPermission(this,REQUESTED_PERMISSIONS[0])!= PackageManager.PERMISSION_GRANTED||
                ContextCompat.checkSelfPermission(this,REQUESTED_PERMISSIONS[1])!= PackageManager.PERMISSION_GRANTED)
        {
            return false;
        }
        return true;
    }
    void showMessage(String message){
        runOnUiThread(() ->
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }
    private void setupVideSDKEngine(){
        try{
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getBaseContext();
            config.mAppId = appId;
            config.mEventHandler = mRtcEventHandler;
            agoraEngine= RtcEngine.create(config);
            agoraEngine.enableVideo();
            agoraEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                    VD_640x360,
                    FRAME_RATE_FPS_15,
                    STANDARD_BITRATE,
                    ORIENTATION_MODE_ADAPTIVE
            ));
        } catch (Exception e){
            showMessage(e.toString());
        }
    }
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
            public void onUserJoined(int uid, int elapsed){
            showMessage("Remote user joined"+uid);
            runOnUiThread(()->setupRemoteVideo(uid));
        }
        @Override
            public  void onJoinChannelSuccess(String channel, int uid, int elapsed){
            isJoined = true;
            showMessage("Joined Channel"+channel);
        }
        @Override
            public  void onUserOffline(int uid, int reason){
            showMessage("Remote user offline"+uid+" "+reason);
            runOnUiThread(()-> remoteSurfaceView.setVisibility(View.GONE));
        }

    };
    private  void setupRemoteVideo(int uid) {
        FrameLayout container = findViewById(R.id.remote_video_view_container);
        remoteSurfaceView = new SurfaceView(getBaseContext());
        remoteSurfaceView.setZOrderMediaOverlay(true);
        container.addView(remoteSurfaceView);
        agoraEngine.setupRemoteVideo(new VideoCanvas(remoteSurfaceView,VideoCanvas.RENDER_MODE_FIT,uid));
        remoteSurfaceView.setVisibility(View.VISIBLE);
    }
    private  void setupLocalVideo(){
        FrameLayout container = findViewById(R.id.local_video_view_container);
        // create a surfaceview object and add it as a child to the Framelayout
        localSurfaceView = new SurfaceView(getBaseContext());
        container.addView(localSurfaceView);
        // call setuploacalvideo with a videocanvas having uid set to 0
        agoraEngine.setupLocalVideo(new VideoCanvas(localSurfaceView,VideoCanvas.RENDER_MODE_HIDDEN,0));

    }
    public void joinChannel(View view){
        if(checkSelfPermission()){
            ChannelMediaOptions options = new ChannelMediaOptions();
            //for a video call, set the channel profile as COMMUNICATION.
            options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
            //set the client role as BROADCASTER OR AUDIENCE according to the scenario
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
            // display localsurfaceview
            setupLocalVideo();
            localSurfaceView.setVisibility(View.VISIBLE);
            //start local preview
            agoraEngine.startPreview();
            //join the chanel with a temp token
            // you need to specify the user ID yourself, and ensure that it is unique in the channel
            agoraEngine.joinChannel(token,channelName,uid,options);
        }
            else {
            Toast.makeText(getApplicationContext(), "Permissions was not granted", Toast.LENGTH_SHORT).show();
            }
        }
    public void leaveChannel(View view){
        if(!isJoined){
            showMessage("Join a channel first");
        }else {
            agoraEngine.leaveChannel();
            showMessage("You left the channel");
            //stop remote video rendering
            if (remoteSurfaceView!=null) remoteSurfaceView.setVisibility(View.GONE);
            //top local video rendering
            if(localSurfaceView!= null) localSurfaceView.setVisibility(View.GONE);
            isJoined = false;
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //if all the permissions are granted, initialized the RtcEngine object and join a channel
        if (!checkSelfPermission()){
            ActivityCompat.requestPermissions(this,REQUESTED_PERMISSIONS,PERMISSION_REQ_ID);
            }
        setupVideSDKEngine();
    }
    protected void onDestroy() {
        super.onDestroy();
        agoraEngine.stopPreview();
        agoraEngine.leaveChannel();

        // Destroy the engine in a sub-thread to avoid congestion
        new Thread(() -> {
            RtcEngine.destroy();
            agoraEngine = null;
        }).start();
    }

}