package com.unionpay.csspublisher;

import com.unionpay.util.PreferenceUtil;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class CameraTestActivity extends Activity implements Callback {

    private SurfaceView mSurface;
    private SurfaceHolder mSurfaceHolder;
    private Camera camera;
    private Camera.Parameters parameters;
    private int deviceWidth, deviceHeight;

    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_camera);

	mSurface = (SurfaceView) findViewById(R.id.test_surface);
	deviceHeight = PreferenceUtil.getInt("device_Height", 640);
	deviceWidth = PreferenceUtil.getInt("device_width", 480);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
	camera = Camera.open();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	initCamera();
	try {
	    camera.setPreviewDisplay(holder);
	    camera.startPreview();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
	// TODO Auto-generated method stub

    }

    private void initCamera() {
	parameters = camera.getParameters();
	parameters.setFlashMode("off"); // 无闪光灯
	parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
	parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
	parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
	parameters.setPreviewFormat(ImageFormat.YV12);
	parameters.setPictureSize(deviceWidth, deviceHeight);
	parameters.setPreviewSize(deviceWidth, deviceHeight);

	camera.setParameters(parameters);
	// 横竖屏镜头自动调整
	if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
	    parameters.set("orientation", "portrait"); //
	    parameters.set("rotation", 90); // 镜头角度转90度（默认摄像头是横拍）
	    camera.setDisplayOrientation(90); // 在2.2以上可以使用
	} else// 如果是横屏
	{
	    parameters.set("orientation", "landscape"); //
	    camera.setDisplayOrientation(0); // 在2.2以上可以使用
	}

	byte[] buf = new byte[deviceWidth * deviceHeight * 3 / 2];
	camera.addCallbackBuffer(buf);
	camera.setPreviewCallback((PreviewCallback) this);
    }
}
