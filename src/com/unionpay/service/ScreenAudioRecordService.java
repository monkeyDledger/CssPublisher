package com.unionpay.service;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;

import com.daniulive.smartpublisher.SmartPublisherJni;
import com.eventhandle.SmartEventCallback;
import com.unionpay.model.FileInfoBean;
import com.unionpay.util.FileUtil;
import com.voiceengine.NTAudioRecord;
import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.projection.MediaProjection;
import android.os.Looper;
import android.util.Log;

/**
 * 录屏直播，包括音频 通过ImageReader获取视频帧
 * 
 * @author lichen2
 */
@SuppressLint("SdCardPath")
public class ScreenAudioRecordService extends Thread {

    private static final String TAG = "ScreenAudioRecordService";

    private int mWidth;
    private int mHeight;
    private String rtmpUrl;
    private int mBitRate;
    private int mDpi;
    private boolean ifSave = false;
    private String mPath; // 文件保存路径
    private String tmpPath = "/sdcard/csspublisher/tmp";  //文件暂存路径
    private MediaProjection mMediaProjection;
    private Context mContext;

    private VirtualDisplay mVirtualDisplay;
    private ImageReader mReader;

    private SmartPublisherJni mPublisherJni = null;
    // for audio capture
    private NTAudioRecord audioRecord = null;

    public ScreenAudioRecordService(int width, int height, String url, int bitrate, int dpi, MediaProjection mp,
	    Context context) {
	mWidth = width;
	mHeight = height;
	rtmpUrl = url;
	mBitRate = bitrate;
	mDpi = dpi;
	mContext = context;
	mMediaProjection = mp;
	mPublisherJni = new SmartPublisherJni();
	if (width > 480) {
	    mWidth /= 2;
	    mHeight /= 2;
	}
    }

    public ScreenAudioRecordService(int width, int height, String url, int bitrate, int dpi, MediaProjection mp,
	    Context context, String filePath) {
	mWidth = width;
	mHeight = height;
	rtmpUrl = url;
	mBitRate = bitrate;
	mDpi = dpi;
	mContext = context;
	mMediaProjection = mp;
	mPath = filePath;
	mPublisherJni = new SmartPublisherJni();
	if (width > 480) {
	    mWidth /= 2;
	    mHeight /= 2;
	}
	ifSave = true;
    }

    @Override
    public void run() {
	try {
	    initAudioRecord();
	    initPublisher();

	    mReader = ImageReader.newInstance(mWidth, mHeight, 0x1, 2);
	    mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG + "-dispaly", mWidth, mHeight, mDpi,
		    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mReader.getSurface(), null, null);
	    Looper.prepare();
	    mReader.setOnImageAvailableListener(new OnImageAvailableListener() {

		@Override
		public void onImageAvailable(ImageReader reader) {
		    Image image = reader.acquireLatestImage();
		    if (image != null) {
			processScreenImage(image);
			image.close();
		    }
		}
	    }, null);
	    Looper.loop();
	} finally {
	    release();
	}
    }

    /**
     * 音频捕捉初始化
     */
    private void initAudioRecord() {
	if (audioRecord == null) {
	    audioRecord = new NTAudioRecord(mContext, 1);
	} 
	
	if (audioRecord != null){
	    audioRecord.executeAudioRecordMethod();
	}
    }

    /**
     * 推流初始化
     */
    private void initPublisher() {
	mPublisherJni.SmartPublisherInit(mContext, 1, 1, mWidth, mHeight);
	mPublisherJni.SmartPublisherSetURL(rtmpUrl);
	mPublisherJni.SetSmartPublisherEventCallback(new EventHande());

	// 设置保存本地文件
	if (ifSave) {
	    if (mPublisherJni.SmartPublisherCreateFileDirectory(tmpPath) == 0) {
		int i = mPublisherJni.SmartPublisherSetRecorderDirectory(tmpPath);
		mPublisherJni.SmartPublisherSetRecorder(1);
		Log.i(TAG, "set rec dir :" + i);
	    }
	}

	int ifConnect = mPublisherJni.SmartPublisherStart();
	Log.i(TAG, "connect result:" + ifConnect);
    }

    /**
     * 处理ImageReader中的数据
     */
    private void processScreenImage(Image image) {
	int width = image.getWidth();
	int height = image.getHeight();

	final Image.Plane[] planes = image.getPlanes();
	final ByteBuffer buffer = planes[0].getBuffer();

	int rowStride = planes[0].getRowStride();

	mPublisherJni.SmartPublisherOnCaptureVideoRGBAData(buffer, rowStride, width, height);
    }

    /**
     * 释放资源
     */
    public void release() {
	if (mVirtualDisplay != null) {
	    mVirtualDisplay.release();
	    mVirtualDisplay = null;
	}
	if (mMediaProjection != null) {
	    mMediaProjection.stop();
	}
	if (audioRecord != null) {
	    audioRecord.StopRecording();
	    audioRecord = null;
	}
	if (mPublisherJni != null) {
	    mPublisherJni.SmartPublisherStop();
	    mPublisherJni = null;
	}
    }
    
    /**
     * 文件重命名，复制到指定目录，原文件删除
     */
    private void renameRecordFile(){
	List<FileInfoBean> list = FileUtil.getFileInfo(tmpPath+"/");
	if(list.size() > 1 || list == null){
	    Log.e(TAG, "rename record file failed");
	    return ;
	}else {
	    String oldPath = list.get(0).getAbsolutePath();
	    FileUtil.renameFileAndDelete(oldPath, mPath);
	}
    }

    class EventHande implements SmartEventCallback {
	public String txt;

	@Override
	public void onCallback(int code, long param1, long param2, String param3, String param4, Object param5) {
	    switch (code) {
	    case EVENTID.EVENT_DANIULIVE_ERC_PUBLISHER_STARTED:
		txt = "开始。。";
		break;
	    case EVENTID.EVENT_DANIULIVE_ERC_PUBLISHER_CONNECTING:
		txt = "连接中。。";
		break;
	    case EVENTID.EVENT_DANIULIVE_ERC_PUBLISHER_CONNECTION_FAILED:
		txt = "连接失败。。";
		break;
	    case EVENTID.EVENT_DANIULIVE_ERC_PUBLISHER_CONNECTED:
		txt = "连接成功。。";
		break;
	    case EVENTID.EVENT_DANIULIVE_ERC_PUBLISHER_DISCONNECTED:
		txt = "连接断开。。";
		break;
	    case EVENTID.EVENT_DANIULIVE_ERC_PUBLISHER_STOP:
		txt = "关闭。。";
		break;
	    case EVENTID.EVENT_DANIULIVE_ERC_PUBLISHER_RECORDER_START_NEW_FILE:
		Log.i(TAG, "开始一个新的录像文件 : " + param3);
		txt = "开始一个新的录像文件。。";
		break;
	    case EVENTID.EVENT_DANIULIVE_ERC_PUBLISHER_ONE_RECORDER_FILE_FINISHED:
		Log.i(TAG, "已生成一个录像文件 : " + param3);
		renameRecordFile();
		txt = "已生成一个录像文件。。";
		break;
	    }

	    String str = "当前回调状态：" + txt;

	    Log.i(TAG, str);

	}
    }
}
