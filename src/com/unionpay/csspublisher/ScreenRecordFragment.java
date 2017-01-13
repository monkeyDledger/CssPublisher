package com.unionpay.csspublisher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.unionpay.application.MyApplication;
import com.unionpay.model.FileInfoBean;
import com.unionpay.service.MediaRecordService;
import com.unionpay.service.ScreenAudioRecordService;
import com.unionpay.util.FileUtil;
import com.unionpay.util.PreferenceUtil;
import com.unionpay.util.StatusNotifyTask;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

/**
 * 设备录屏界面
 * 本地录屏 MediaRecordService 
 * 录屏直播 ScreenAudioRecordService
 * @author lichen2
 */
public class ScreenRecordFragment extends Fragment {

    private static final String TAG = "ScreenRecordFragment";

    private LinearLayout urlLayout, fileLayout;
    private RadioGroup publishRadio, localRadio;
    private RadioButton publishBtn1, saveBtn1;
    private EditText fileEdit;
    private Button startBtn;
    private TextView urlText;

    private MediaProjectionManager mProjectionManager;

    private String userName, rtmpUrl, recordFileName;
    private String[] rtmpUrls = {"", ""};
    private String[] httpUrls = {"", ""};
    
    //直播状态通知接口
    private static String statusUrl = null; 

    private ScreenAudioRecordService saRecordService;
    private MediaRecordService mediaRecord;

    // 设备分辨率
    private int displayWidth, displayHeight, dpi;

    // 本地已有的视频文件
    private List<String> videosName;

    // 设备录屏种类（0：本地录屏，1：录屏直播且不保存， 2：录屏直播且保存）
    private int RECORDKIND = 0;
    // 当前是否有直播进程
    private boolean isCamera = false;

    private String defaultPath = "/sdcard/";

    private static final int LOCAL_REQUEST_CODE = 0;
    private static final int RTMP_REQUEST_CODE = 1;
    private static final int RTMPWITHSAVE_REQUEST_CODE = 2;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	View view = inflater.inflate(R.layout.fragment_screen_record, null);
	initView(view);
	initEvent();
	initData();
	Log.i(TAG, "onCreateView");
	return view;
    }

    private void initView(View v) {
	urlLayout = (LinearLayout) v.findViewById(R.id.screen_url);
	fileLayout = (LinearLayout) v.findViewById(R.id.screen_file);
	publishRadio = (RadioGroup) v.findViewById(R.id.screen_if_publish);
	localRadio = (RadioGroup) v.findViewById(R.id.screen_if_save);
	urlText = (TextView) v.findViewById(R.id.screen_publish_url);
	startBtn = (Button) v.findViewById(R.id.screen_start_btn);
	publishBtn1 = (RadioButton) v.findViewById(R.id.screen_publish1);
	saveBtn1 = (RadioButton) v.findViewById(R.id.screen_save1);
	fileEdit = (EditText) v.findViewById(R.id.screen_file_path);

	// 默认选择本地录屏
	publishBtn1.setChecked(true);

	publishRadio.setOnCheckedChangeListener(new OnCheckedChangeListener() {

	    @Override
	    public void onCheckedChanged(RadioGroup group, int checkedId) {
		if (checkedId == publishBtn1.getId()) {
		    urlLayout.setVisibility(View.GONE);
		    localRadio.setVisibility(View.GONE);
		    fileLayout.setVisibility(View.VISIBLE);
		    if (mediaRecord == null && saRecordService == null) {
			RECORDKIND = 0;
		    }
		} else {
		    // 默认选择不保存录像
		    saveBtn1.setChecked(true);
		    urlLayout.setVisibility(View.VISIBLE);
		    localRadio.setVisibility(View.VISIBLE);
		    fileLayout.setVisibility(View.GONE);
		    if (mediaRecord == null && saRecordService == null) {
			RECORDKIND = 1;
		    }
		}
	    }
	});

	localRadio.setOnCheckedChangeListener(new OnCheckedChangeListener() {

	    @Override
	    public void onCheckedChanged(RadioGroup group, int checkedId) {
		if (checkedId == saveBtn1.getId()) {
		    fileLayout.setVisibility(View.GONE);
		    if (mediaRecord == null && saRecordService == null) {
			RECORDKIND = 1;
		    }
		} else {
		    fileLayout.setVisibility(View.VISIBLE);
		    if (mediaRecord == null && saRecordService == null) {
			RECORDKIND = 2;
		    }
		}
	    }
	});
	
	//点击地址切换直播
	urlText.setOnClickListener(new OnClickListener() {
	    
	    @Override
	    public void onClick(View v) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), 
			AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
		builder.setTitle("切换推流地址");
		builder.setSingleChoiceItems(rtmpUrls, -1, new DialogInterface.OnClickListener() {
		    
		    @Override
		    public void onClick(DialogInterface dialogInterface, int i) {
			rtmpUrl = rtmpUrls[i];
		    }
		});
		builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
		    
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
			dialog.dismiss();
		    }
		});
		builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
		    
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
			urlText.setText(rtmpUrl);
		    }
		});
		builder.show();
	    }
	});

	startBtn.setOnClickListener(recordListener);
    }

    private void initEvent() {
	displayWidth = PreferenceUtil.getInt("device_width", 480);
	displayHeight = PreferenceUtil.getInt("device_height", 640);
	dpi = PreferenceUtil.getInt("device_dpi", 1);

	mProjectionManager = (MediaProjectionManager) getActivity().getSystemService("media_projection");
    }

    private void initData() {
	userName = PreferenceUtil.getString("user_name", "");
	rtmpUrl = getString(R.string.rtmp_server_url) + userName;
	urlText.setText(rtmpUrl);
	rtmpUrls[0] = rtmpUrl;
	rtmpUrls[1] = getString(R.string.rtmp_local_url) + userName;
	
	statusUrl = getString(R.string.http_server) + "liveStatus";
    }

    /**
     * 开始录屏按钮监听事件
     */
    private OnClickListener recordListener = new OnClickListener() {

	@Override
	public void onClick(View v) {

	    hideSoftKeyBoard();
	    if (RECORDKIND == 0 || RECORDKIND == 2) {
		recordFileName = fileEdit.getText().toString();

		if (recordFileName.equals("")) {
		    MyApplication.getInstance().showToast(getActivity(), "请先输入视频的文件名");
		    return;
		}
		if (mediaRecord == null && saRecordService == null) {
		    recordFileName = recordFileName.replaceAll(" ", "");
		    List<String> existedName = getExistedFiles();
		    if (existedName != null) {
			String tmp = recordFileName + ".mp4";
			if (existedName.contains(tmp)) {
			    MyApplication.getInstance().showToast(getActivity(), "该文件名已存在");
			    fileEdit.requestFocus();
			    return;
			}
		    }
		}
	    }

	    isCamera = PreferenceUtil.getBoolean("is_camera", false);
	    if (isCamera) {
		MyApplication.getInstance().showToast(getActivity(), "您已开启摄像头直播，请关闭后再开启录屏");
		return;
	    }
	    switch (RECORDKIND) {
	    case 0:
		startLocalRecord();
		break;
	    case 1:
		startRtmpRecord(false);
		break;
	    case 2:
		startRtmpRecord(true);
		break;
	    default:
		break;
	    }
	}
    };

    /**
     * 本地录屏
     */
    private void startLocalRecord() {
	if (mediaRecord != null) {
	    mediaRecord.release();
	    mediaRecord = null;
	    PreferenceUtil.setBoolean("is_record", false);
	    MyApplication.getInstance().showToast(getActivity(), "录屏结束");
	    startBtn.setText("开始录屏");
	    
	    notifyStatus("0");
	} else {
	    Intent captureIntent = mProjectionManager.createScreenCaptureIntent();
	    startActivityForResult(captureIntent, LOCAL_REQUEST_CODE);
	}
    }

    /**
     * 录屏直播
     * @param ifSave 是否保存到本地
     */
    private void startRtmpRecord(boolean ifSave) {
	if (saRecordService == null) {
	    Intent captureIntent = mProjectionManager.createScreenCaptureIntent();
	    if (ifSave) {
		// 录屏直播且保存
		startActivityForResult(captureIntent, RTMPWITHSAVE_REQUEST_CODE);
	    } else {
		// 不保存
		startActivityForResult(captureIntent, RTMP_REQUEST_CODE);
	    }
	} else {
	    saRecordService.release();
	    saRecordService = null;
	    PreferenceUtil.setBoolean("is_record", false);
	    MyApplication.getInstance().showToast(getActivity(), "录屏结束");
	    startBtn.setText("开始录屏");
	    notifyStatus("0");
	}
    }

    /**
     * mediaprojection回调
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

	MediaProjection mediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
	if (mediaProjection == null) {
	    Log.e(TAG, "media projection is null");
	    return;
	}
	PreferenceUtil.setBoolean("is_record", true);
	startBtn.setText("录屏ing...点击停止");
	switch (requestCode) {
	case 0:
	    // 本地录屏
	    // 标示当前已有录制进程
	    File file = new File(PreferenceUtil.getString("local_dir", defaultPath) + recordFileName + ".mp4");
//	    screenRecord = new ScreenRecordService(displayWidth, displayHeight, 6000000, 1, mediaProjection,
//		    file.getAbsolutePath());
//	    screenRecord.start();
	    mediaRecord = new MediaRecordService(displayWidth, displayHeight, 6000000, dpi, 
		    mediaProjection, file.getAbsolutePath());
	    mediaRecord.start();
	    break;
	case 1:
	    // 录屏直播且不保存
	    // recordWithImageService = new
	    // ScreenRecordWithImageService(displayWidth, displayHeight,
	    // rtmpUrl, 2000000, 1, mediaProjection,
	    // getActivity());
	    // recordWithImageService.start();

	    saRecordService = new ScreenAudioRecordService(displayWidth, displayHeight, rtmpUrl, dpi, mediaProjection,
		    getActivity());
	    saRecordService.start();
	    break;
	case 2:
	    // 录屏直播且保存
	    File rtmpFile = new File(PreferenceUtil.getString("rtmp_dir", defaultPath) + recordFileName + ".mp4");
	    // recordWithImageService = new
	    // ScreenRecordWithImageService(displayWidth, displayHeight,
	    // rtmpUrl, 2000000, 1, mediaProjection,
	    // getActivity());
	    // recordWithImageService.start();

	    saRecordService = new ScreenAudioRecordService(displayWidth, displayHeight, rtmpUrl, dpi, mediaProjection,
		    getActivity(), rtmpFile.getAbsolutePath());
	    saRecordService.start();
	    break;
	default:
	    break;
	}
	getActivity().moveTaskToBack(true);
	
	notifyStatus("1");
    }
    
    /**
     * 通知直播状态
     * @param status
     */
    private void notifyStatus(String status){
	Log.i(TAG, "status url " + statusUrl);
	new StatusNotifyTask(statusUrl, userName, status).execute();
    }
    
    /**
     * 结束
     */
    private void stop(){
	if (mediaRecord != null) {
	    mediaRecord.release();
	}
	if (saRecordService != null) {
	    notifyStatus("0");
	    saRecordService.release();
	}
    }

    /**
     * 查询已有文件的文件名
     */
    private List<String> getExistedFiles() {
	videosName = new ArrayList<String>();
	videosName.clear();
	List<FileInfoBean> localList = FileUtil.getFileInfo(PreferenceUtil.getString("local_dir", defaultPath));
	List<FileInfoBean> rtmpList = FileUtil.getFileInfo(PreferenceUtil.getString("rtmp_dir", defaultPath));
	if (localList != null && rtmpList != null) {
	    for (FileInfoBean f : localList) {
		videosName.add(f.getName());
	    }
	    for (FileInfoBean f : rtmpList) {
		videosName.add(f.getName());
	    }
	}
	Log.i(TAG, "local files" + videosName.toString());

	return videosName;
    }

    /**
     * 隐藏软键盘
     */
    public void hideSoftKeyBoard() {
	if (getActivity() != null) {
	    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
	    imm.hideSoftInputFromWindow(getActivity().getWindow().getDecorView().getWindowToken(), 0);
	}
    }

    public void onStart() {
	super.onStart();
	Log.i(TAG, "onStart");
    }

    public void onResume() {
	super.onResume();
	if(RECORDKIND == 0){
	    fileLayout.setVisibility(View.VISIBLE);
	}
	
	if (mediaRecord != null || saRecordService != null) {
	    startBtn.setText("录屏ing...点击停止");
	}
	Log.i(TAG, "onResume");
    }

    public void onPause() {
	super.onPause();
	Log.i(TAG, "onPause");
    }

    public void onStop() {
	super.onStop();
	Log.i(TAG, "onStop");
    }

    public void onDestroyView() {
	super.onDestroyView();
	Log.i(TAG, "onDestroyView");
    }

    public void onDestroy() {
	super.onDestroy();
	Log.i(TAG, "onDestroy");
	stop();
    }

}
