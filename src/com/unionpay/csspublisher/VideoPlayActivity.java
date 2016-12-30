package com.unionpay.csspublisher;

import com.unionpay.application.MyApplication;

import android.app.Activity;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

/**
 * 本地视频播放
 * @author lichen2
 */
public class VideoPlayActivity extends Activity {
    
    private VideoView videoView;
    
    private String videoPath;
    
    public void onCreate(Bundle savedInstanceState){
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_video_play);
	MyApplication.getInstance().addActivity(this);
	
	videoView = (VideoView)findViewById(R.id.video_player);
	
	videoPath = getIntent().getStringExtra("video_path");
	if(videoPath != null && !videoPath.isEmpty()){
	    videoView.setVideoPath(videoPath);
	    videoView.setMediaController(new MediaController(this));
	    videoView.requestFocus();
	    videoView.start();
	}
	
    }

}
