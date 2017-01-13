package com.unionpay.csspublisher;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.daniulive.smartpublisher.SmartPublisherJni;
import com.eventhandle.SmartEventCallback;
import com.unionpay.application.MyApplication;
import com.unionpay.util.HttpUtil;
import com.unionpay.util.PreferenceUtil;
import com.unionpay.util.StatusNotifyTask;
import com.voiceengine.NTAudioRecord;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

/**
 * 摄像头直播
 * @author lichen2
 */
@SuppressWarnings("deprecation")
public class CameraFragment extends Fragment implements Callback, PreviewCallback {

    private static final String TAG = "CmeraFragment";

    private SurfaceView mSurfaceView;
    private ImageView switchCameraBtn;
    private TextView publishUrl;
    private Spinner resolutionSpinner;
    private Switch preSwitch, saveSwitch;
    private LinearLayout mLayout;
    private EditText fileEidt;
    private Button startBtn;

    private SurfaceHolder mHolder = null;
    private Camera camera = null;
    private AutoFocusCallback mAutoFocusCallback = null;
    
    private NTAudioRecord audioRecord;

    private String rtmpUrl, statusUrl;
    private String userName;
    private int displayWidth = 640, displayHeight = 480;

    private boolean isRecord; // 当前是否有正在录屏的进程
    private boolean isPreview = false; // 是否开启预览
    private boolean isSave = false; // 是否保存录像
    private boolean isStartLive = false; // 是否开启直播
    private static final int FRONT = 1; // 前置摄像头标记
    private static final int BACK = 2; // 后置摄像头标记
    private int currentCameraType = BACK; // 当前打开的摄像头标记
    private static final int PORTRAIT = 1; // 竖屏
    private static final int LANDSCAPE = 2; // 横屏
    private int currentOrigentation = PORTRAIT;
    private int curCameraIndex = -1;
    private int frameCount = 0;

    private SmartPublisherJni publisherJni;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	View view = inflater.inflate(R.layout.fragment_camera, null);
	initView(view);
	initData();
	return view;
    }

    private void initView(View v) {
	mSurfaceView = (SurfaceView) v.findViewById(R.id.camera_surface);
	switchCameraBtn = (ImageView) v.findViewById(R.id.camera_switch);
	publishUrl = (TextView) v.findViewById(R.id.camera_publish_url);
	resolutionSpinner = (Spinner) v.findViewById(R.id.camera_size_spinner);
	preSwitch = (Switch) v.findViewById(R.id.camera_switch_preview);
	saveSwitch = (Switch) v.findViewById(R.id.camera_if_save);
	mLayout = (LinearLayout) v.findViewById(R.id.camera_file);
	fileEidt = (EditText) v.findViewById(R.id.camera_file_name);
	startBtn = (Button) v.findViewById(R.id.camera_start_btn);

	// 分辨率选择
	final String[] resolutionSel = new String[] { "低分辨率", "中分辨率", "高分辨率" };
	ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item,
		resolutionSel);
	resolutionSpinner.setAdapter(arrayAdapter);
	resolutionSpinner.setSelection(1);
	resolutionSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

	    @Override
	    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		if (isStartLive) {
		    MyApplication.getInstance().showToast(getActivity(), "直播中...无法更改分辨率");
		    return ;
		}

		SwitchResolution(position);
	    }

	    @Override
	    public void onNothingSelected(AdapterView<?> parent) {
		resolutionSpinner.setSelection(1);
	    }
	});

	// 预览选择按钮
	preSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

	    @Override
	    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (isChecked) {
		    startBtn.setBackgroundColor(getResources().getColor(R.color.transparent));
		    mSurfaceView.setVisibility(View.VISIBLE);
		    switchCameraBtn.setVisibility(View.VISIBLE);
		    isPreview = true;
		} else {
		    mSurfaceView.setVisibility(View.GONE);
		    switchCameraBtn.setVisibility(View.GONE);
		    startBtn.setBackgroundColor(getResources().getColor(R.color.blue_text));
		    isPreview = false;
		}
	    }
	});

	saveSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

	    @Override
	    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (isChecked) {
		    isSave = true;
		} else {
		    isSave = false;
		    publisherJni.SmartPublisherSetRecorder(0);
		}
	    }
	});

	switchCameraBtn.setOnClickListener(new OnClickListener() {

	    @Override
	    public void onClick(View v) {
		try {
		    switchCamera();
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }
	});

	startBtn.setOnClickListener(startPublishListener);

	mHolder = mSurfaceView.getHolder();
	mHolder.addCallback(this);
	mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

	// 自动聚焦变量回调
	mAutoFocusCallback = new AutoFocusCallback() {
	    public void onAutoFocus(boolean success, Camera camera) {
		if (success)// success表示对焦成功
		{
		    Log.i(TAG, "onAutoFocus succeed...");
		} else {
		    Log.i(TAG, "onAutoFocus failed...");
		}
	    }
	};
    }

    private void initData() {
	userName = PreferenceUtil.getString("user_name", "");
	rtmpUrl = getString(R.string.rtmp_server_url) + userName;
	publishUrl.setText(rtmpUrl);
	
	statusUrl = getString(R.string.http_server) + "liveStatus";

	publisherJni = new SmartPublisherJni();
    }

    /**
     * 开始按钮监听事件
     */
    View.OnClickListener startPublishListener = new View.OnClickListener() {

	@Override
	public void onClick(View v) {

	    isRecord = PreferenceUtil.getBoolean("is_record", false);
	    if (isRecord) {
		MyApplication.getInstance().showToast(getActivity(), "您正在录屏，请关闭后再进行直播");
		return;
	    }
	    if (isStartLive) {
		stop();
		PreferenceUtil.setBoolean("is_camera", false);
		return;
	    }
	    if(!isPreview){
		MyApplication.getInstance().showToast(getActivity(), "请先开启摄像头预览");
		return; 
	    }

	    isStartLive = true;
	    PreferenceUtil.setBoolean("is_camera", true);
	    startBtn.setText("正在直播...点击停止");
	    if (publisherJni != null) {
		// 推送音视频
		publisherJni.SmartPublisherInit(getActivity(), 1, 1, displayWidth,
			displayHeight);
		publisherJni.SetSmartPublisherEventCallback(new EventHande());
		if (isSave) {
		    publisherJni.SmartPublisherSetRecorder(1);
		    String path = PreferenceUtil.getString("camera_dir", getString(R.string.default_camera_dir));
		    publisherJni.SmartPublisherSetRecorderDirectory(path);
		}
		publisherJni.SmartPublisherSetURL(rtmpUrl);

		if (camera == null) {
		    camera = openCamera(currentCameraType);
		}

		initAudioRecord();
		
		int result = publisherJni.SmartPublisherStart();
		if (result == 0) {
		    new StatusNotifyTask(statusUrl, userName, "1").execute();
		    Log.i(TAG, "camera publish success");
		} else {
		    Log.i(TAG, "camera publish error");
		}
	    }

	}
    };
    
    public void onDestroy() {
	super.onDestroy();
	Log.i(TAG, "onDestroy");
	stop();
    }
    
    /**
     * 检查音频捕捉
     */
    private void initAudioRecord(){
	if(audioRecord == null){
	    audioRecord = new NTAudioRecord(getActivity(), 1);
	}
	if(audioRecord != null){
	    audioRecord.executeAudioRecordMethod();
	}
    }

    /**
     * 结束直播
     */
    private void stop() {
	if(audioRecord != null){
	    audioRecord.StopRecording();
	    audioRecord = null;
	}
	if (publisherJni != null) {
	    new StatusNotifyTask(statusUrl, userName, "0").execute();
	    publisherJni.SmartPublisherStop();
	}
	isStartLive = false;
	startBtn.setText("开始直播");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
	Log.i(TAG, "surfaceCreated..");
	try {

	    int CammeraIndex = findBackCamera();
	    Log.i(TAG, "BackCamera: " + CammeraIndex);

	    if (CammeraIndex == -1) {
		CammeraIndex = findFrontCamera();
		currentCameraType = FRONT;
		switchCameraBtn.setEnabled(false);
		if (CammeraIndex == -1) {
		    Log.i(TAG, "NO camera!!");
		    return;
		}
	    } else {
		currentCameraType = BACK;
	    }

	    if (camera == null) {
		camera = openCamera(currentCameraType);
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	}

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	Log.i(TAG, "surfaceChanged..");
	initCamera(holder);
    }

    /**
     * 摄像头初始化
     * 
     * @param holder
     */
    private void initCamera(SurfaceHolder holder) {
	Log.i(TAG, "initCamera..");

	if (isPreview && camera != null){
	    camera.stopPreview();
	}
	    

	Camera.Parameters parameters;
	try {
	    parameters = camera.getParameters();
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    return;
	}

	parameters.setPreviewSize(displayWidth, displayHeight);
	parameters.setPictureFormat(PixelFormat.JPEG);
	parameters.setPreviewFormat(PixelFormat.YCbCr_420_SP);

	SetCameraFPS(parameters);

	setCameraDisplayOrientation(getActivity(), curCameraIndex, camera);

	camera.setParameters(parameters);

	int bufferSize = (((displayWidth | 0xf) + 1) * displayHeight
		* ImageFormat.getBitsPerPixel(parameters.getPreviewFormat())) / 8;

	camera.addCallbackBuffer(new byte[bufferSize]);

	camera.setPreviewCallbackWithBuffer(this);
	try {
	    camera.setPreviewDisplay(holder);
	} catch (Exception ex) {
	    // TODO Auto-generated catch block
	    if (null != camera) {
		camera.release();
		camera = null;
	    }
	    ex.printStackTrace();
	}
	camera.startPreview();
	camera.autoFocus(mAutoFocusCallback);
	isPreview = true;
    }

    private Camera openCamera(int type) {
	int frontIndex = -1;
	int backIndex = -1;
	int cameraCount = Camera.getNumberOfCameras();
	Log.i(TAG, "cameraCount: " + cameraCount);

	CameraInfo info = new CameraInfo();
	for (int cameraIndex = 0; cameraIndex < cameraCount; cameraIndex++) {
	    Camera.getCameraInfo(cameraIndex, info);

	    if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
		frontIndex = cameraIndex;
	    } else if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
		backIndex = cameraIndex;
	    }
	}

	currentCameraType = type;
	if (type == FRONT && frontIndex != -1) {
	    curCameraIndex = frontIndex;
	    return Camera.open(frontIndex);
	} else if (type == BACK && backIndex != -1) {
	    curCameraIndex = backIndex;
	    return Camera.open(backIndex);
	}
	return null;
    }

    private void switchCamera() throws IOException {
	camera.setPreviewCallback(null);
	camera.stopPreview();
	camera.release();
	if (currentCameraType == FRONT) {
	    camera = openCamera(BACK);
	} else if (currentCameraType == BACK) {
	    camera = openCamera(FRONT);
	}

	initCamera(mHolder);
    }

    // 检查设备是否有前置摄像头
    private int findFrontCamera() {
	int cameraCount = 0;
	Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
	cameraCount = Camera.getNumberOfCameras();

	for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
	    Camera.getCameraInfo(camIdx, cameraInfo);
	    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
		return camIdx;
	    }
	}
	return -1;
    }

    // 检查设备是否有后置摄像头
    private int findBackCamera() {
	int cameraCount = 0;
	Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
	cameraCount = Camera.getNumberOfCameras();

	for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
	    Camera.getCameraInfo(camIdx, cameraInfo);
	    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
		return camIdx;
	    }
	}
	return -1;
    }

    private void SetCameraFPS(Camera.Parameters parameters) {
	if (parameters == null)
	    return;

	int[] findRange = null;

	int defFPS = 20 * 1000;

	List<int[]> fpsList = parameters.getSupportedPreviewFpsRange();
	if (fpsList != null && fpsList.size() > 0) {
	    for (int i = 0; i < fpsList.size(); ++i) {
		int[] range = fpsList.get(i);
		if (range != null && Camera.Parameters.PREVIEW_FPS_MIN_INDEX < range.length
			&& Camera.Parameters.PREVIEW_FPS_MAX_INDEX < range.length) {
		    Log.i(TAG,
			    "Camera index:" + i + " support min fps:" + range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX]);

		    Log.i(TAG,
			    "Camera index:" + i + " support max fps:" + range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);

		    if (findRange == null) {
			if (defFPS <= range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]) {
			    findRange = range;

			    Log.i(TAG,
				    "Camera found appropriate fps, min fps:"
					    + range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] + " ,max fps:"
					    + range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
			}
		    }
		}
	    }
	}

	if (findRange != null) {
	    parameters.setPreviewFpsRange(findRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
		    findRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
	}
    }

    private void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
	android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
	android.hardware.Camera.getCameraInfo(cameraId, info);
	int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
	int degrees = 0;
	switch (rotation) {
	case Surface.ROTATION_0:
	    degrees = 0;
	    break;
	case Surface.ROTATION_90:
	    degrees = 90;
	    break;
	case Surface.ROTATION_180:
	    degrees = 180;
	    break;
	case Surface.ROTATION_270:
	    degrees = 270;
	    break;
	}
	int result;
	if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
	    result = (info.orientation + degrees) % 360;
	    result = (360 - result) % 360;
	} else {
	    // back-facing
	    result = (info.orientation - degrees + 360) % 360;
	}

	Log.i(TAG, "curDegree: " + result);

	camera.setDisplayOrientation(result);
    }

    /**
     * 更改分辨率
     * 
     * @param position
     */
    public void SwitchResolution(int position) {
	Log.i(TAG, "Current Resolution position: " + position);

	switch (position) {
	case 0:
	    displayWidth = 320;
	    displayHeight = 240;
	    break;
	case 1:
	    displayWidth = 640;
	    displayHeight = 480;
	    break;
	case 2:
	    displayWidth = 1280;
	    displayHeight = 720;
	    break;
	default:
	    displayWidth = 640;
	    displayHeight = 480;
	}

	if (camera != null) {
	    camera.stopPreview();
	    initCamera(mHolder);
	}
    }

    public void onConfigurationChanged(Configuration newConfig) {
	try {
	    super.onConfigurationChanged(newConfig);
	    Log.i(TAG, "onConfigurationChanged, start:" + isStartLive);
	    if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
		if (!isStartLive) {
		    currentOrigentation = LANDSCAPE;
		}
	    } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
		if (!isStartLive) {
		    currentOrigentation = PORTRAIT;
		}
	    }
	} catch (Exception ex) {
	}
    }

    /**
     * 获取视频帧，并推流
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
	if (frameCount % 3000 == 0) {
	    System.gc();
	}

	if (data == null) {
	    Parameters params = camera.getParameters();
	    Size size = params.getPreviewSize();
	    int bufferSize = (((size.width | 0x1f) + 1) * size.height
		    * ImageFormat.getBitsPerPixel(params.getPreviewFormat())) / 8;
	    camera.addCallbackBuffer(new byte[bufferSize]);
	} else {
	    if (isStartLive) {
		publisherJni.SmartPublisherOnCaptureVideoData(data, data.length, currentCameraType,
			currentOrigentation);
	    }
	    camera.addCallbackBuffer(data);
	}
    }

    public void getSupportPreviewSizeList() {
	List<Camera.Size> sizeList = camera.getParameters().getSupportedPreviewSizes();

	// 如果sizeList只有一个我们也没有必要做什么了，因为就他一个别无选择
	if (sizeList.size() > 1) {
	    Iterator<Camera.Size> itor = sizeList.iterator();
	    while (itor.hasNext()) {
		Camera.Size cur = itor.next();
		System.out.println("size==" + cur.width + " " + cur.height);
	    }
	}
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
	// TODO Auto-generated method stub
	Log.i(TAG, "Surface destroyed");
    }
    
    class EventHande implements SmartEventCallback {

	String txt;

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
