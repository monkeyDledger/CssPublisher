package com.unionpay.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import com.daniulive.smartpublisher.SmartPublisherJni;
import com.eventhandle.SmartEventCallback;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.util.Log;
import android.view.Surface;

/**
 * 录屏推流
 * 
 * @author lichen2
 */
public class RtmpRecordService extends Thread {

    private static final String TAG = "rtmpRecorder";

    private int mWidth;
    private int mHeight;
    private String rtmpUrl;
    private int mBitRate;
    private int mDpi;
    private MediaProjection mMediaProjection;
    private SmartPublisherJni mPublisherJni;
    private Context mContext;

    // parameters for the encoder
    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced
							 // Video Coding
    private static final int FRAME_RATE = 25; // 25 fps
    private static final int IFRAME_INTERVAL = 1; // 1 seconds between I-frames
    private static final int TIMEOUT_US = 10000;

    private byte[] mPpsSps;
    private int ifSave = 0; // 0：不保存录屏；1：保存录屏
    private int isStarted;
    private String mPath;

    private int mVideoTrackIndex = -1;
    private MediaMuxer mMuxer;
    private boolean mMuxerStarted = false;
    private MediaCodec mEncoder;
    private Surface mSurface;
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private AtomicBoolean mQuit = new AtomicBoolean(false);
    private VirtualDisplay mVirtualDisplay;

    public RtmpRecordService(int width, int height, String url, int bitrate, int dpi, MediaProjection mp,
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

    public RtmpRecordService(int width, int height, String url, int bitrate, int dpi, MediaProjection mp,
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
	ifSave = 1;
    }

    /**
     * stop task
     */
    public final void quit() {
	mQuit.set(true);
    }

    @Override
    public void run() {
	try {
	    try {
		initPublisher();
		prepareEncoder();
		if (ifSave == 1) {
		    mMuxer = new MediaMuxer(mPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
		}
	    } catch (IOException e) {
		throw new RuntimeException(e);
	    }
	    mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG + "-display", mWidth, mHeight, mDpi,
		    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mSurface, null, null);
	    Log.d(TAG, "created virtual display: " + mVirtualDisplay);
	    recordVirtualDisplay();
	} finally {
	    release();
	}
    }

    /**
     * 推流初始化
     */
    private void initPublisher() {
	mPublisherJni.SmartPublisherInit(mContext, 0, 2, mWidth, mHeight);
	mPublisherJni.SmartPublisherSetURL(rtmpUrl);
	mPublisherJni.SetSmartPublisherEventCallback(new EventHande());
	int ifConnect = mPublisherJni.SmartPublisherStart();
	Log.i(TAG, "connect result:" + ifConnect);
    }
    
    private void prepareEncoder() throws IOException {

	MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
	format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
	format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
	format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
	format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

	Log.d(TAG, "created video format: " + format);
	mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
	mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
	mSurface = mEncoder.createInputSurface();
	Log.d(TAG, "created input surface: " + mSurface);
	mEncoder.start();
    }

    /**
     * 录屏
     */
    private void recordVirtualDisplay() {
	while (!mQuit.get()) {
	    int index = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_US);
	    Log.i(TAG, "dequeue output buffer index=" + index);
	    if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
		// 后续输出格式变化
		if(ifSave == 1){
		    resetOutputFormat();
		}
	    } else if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
		// 请求超时
		Log.d(TAG, "retrieving buffers time out!");
		try {
		    // wait 10ms
		    Thread.sleep(10);
		} catch (InterruptedException e) {
		}
	    } else if (index >= 0) {
		// 有效输出
		encodeToVideoTrack(index);

		mEncoder.releaseOutputBuffer(index, false);
	    }
	}
    }
    
    private void resetOutputFormat() {
   	// should happen before receiving buffers, and should only happen
   	// once
   	if (mMuxerStarted) {
   	    throw new IllegalStateException("output format already changed!");
   	}
   	MediaFormat newFormat = mEncoder.getOutputFormat();
   	mVideoTrackIndex = mMuxer.addTrack(newFormat);
   	mMuxer.start();
   	mMuxerStarted = true;
   	Log.i(TAG, "started media muxer, videoIndex=" + mVideoTrackIndex);
       }


    /**
     * 硬解码获取实时帧数据并推流
     * 
     * @param index
     */
    private void encodeToVideoTrack(int index) {
	// 获取到的实时帧视频数据
	ByteBuffer encodedData = mEncoder.getOutputBuffer(index);
	if (encodedData != null) {
	    if (ifSave == 1) {
		mMuxer.writeSampleData(mVideoTrackIndex, encodedData, mBufferInfo);
	    }

	    byte[] outData = new byte[mBufferInfo.size];
	    encodedData.get(outData);
	    // 记录pps和sps
	    int type = outData[4] & 0x07;
	    if (type == 7 || type == 8) {
		mPpsSps = outData;
	    } else if (type == 5) {
		// 在关键帧前面加上pps和sps数据
		if (mPpsSps != null) {
		    byte[] iframeData = new byte[mPpsSps.length + outData.length];
		    System.arraycopy(mPpsSps, 0, iframeData, 0, mPpsSps.length);
		    System.arraycopy(outData, 0, iframeData, mPpsSps.length, outData.length);
		    outData = iframeData;
		}

	    }
	    int ifPush = mPublisherJni.SmartPublisherOnReceivingVideoEncodedData(outData, outData.length, 1,
		    mBufferInfo.presentationTimeUs / 1000);
	}
    }

    private void release() {
	if (mVirtualDisplay != null) {
	    mVirtualDisplay.release();
	}
	if (mMediaProjection != null) {
	    mMediaProjection.stop();
	}
	if (ifSave == 1 && mMuxer != null) {
	    mMuxer.stop();
	    mMuxer.release();
	    mMuxer = null;
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
		txt = "已生成一个录像文件。。";
		break;
	    }

	    String str = "当前回调状态：" + txt;

	    Log.i(TAG, str);

	}
    }
}
