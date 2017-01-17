package com.unionpay.csspublisher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.unionpay.application.MyApplication;
import com.unionpay.util.PreferenceUtil;
import com.unionpay.util.StatusNotifyTask;
import com.unionpay.view.TopTitleBar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 主界面 
 * ViewPager+Fragment
 * @author lichen2
 */
public class RecordActivity extends FragmentActivity implements OnClickListener {

    private static final String TAG = "RecordActivity";

    private TopTitleBar topTitle;

    private ViewPager vPager;
    private FragmentPagerAdapter mAdapter;

    private List<Fragment> mFragments;

    private LinearLayout screenLayout, cameraLayout, userLayout;
    private ImageButton screenImage, cameraImage, userImage;
    private TextView screenText, cameraText, userText;
    
    private AlertDialog.Builder builder;

    // 设备分辨率
    private DisplayMetrics metrics;
    private int displayWidth, displayHeight, densityDpi;
    
    private String[] titles = { "录屏", "直播", "我的" };
    private String[] RECORDDIR = { "/sdcard/csspublisher/records/", "/sdcard/csspublisher/rtmps/",
	    "/sdcard/csspublisher/cameras/" };
    private String[] dirPreferences = { "local_dir", "rtmp_dir", "camera_dir" };
    
    private String statusUrl, userName;
    
    private boolean isRecord, isCamera;

    static {
	System.load("libSmartPublisher.so");
    }

    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	Log.i(TAG, "onCreate");
	setContentView(R.layout.activity_record);

	MyApplication.getInstance().addActivity(this);
	// 禁止屏幕休眠
	getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
		WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

	initView();
	initData();
    }

    /**
     * ui初始化
     */
    private void initView() {
	topTitle = (TopTitleBar) findViewById(R.id.title_record);
	topTitle.setTitle("CSS直播");
	topTitle.getLeftButton().setVisibility(View.VISIBLE);
	topTitle.getLeftButton().setOnClickListener(new View.OnClickListener() {

	    @Override
	    public void onClick(View v) {
		showLogOutDialog();
	    }
	});

	vPager = (ViewPager) findViewById(R.id.record_viewpager);

	screenImage = (ImageButton) findViewById(R.id.record_screen);
	cameraImage = (ImageButton) findViewById(R.id.record_camera);
	userImage = (ImageButton) findViewById(R.id.record_user);
	screenText = (TextView) findViewById(R.id.record_screen_text);
	cameraText = (TextView) findViewById(R.id.record_camera_text);
	userText = (TextView) findViewById(R.id.record_user_text);
	
    }

    /**
     * 数据初始化
     */
    private void initData() {
	// fragment list
	mFragments = new ArrayList<>();

	mFragments.add(new ScreenRecordFragment());
	mFragments.add(new CameraFragment());
	mFragments.add(new UserFragment());

	vPager.setCurrentItem(0);

	mAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {

	    @Override
	    public int getCount() {
		// 获取Fragment总数
		return mFragments.size();
	    }

	    @Override
	    public Fragment getItem(int position) {
		// 获取指定Fragment
		return mFragments.get(position);
	    }
	};

	vPager.setAdapter(mAdapter);
	vPager.setOnPageChangeListener(new OnPageChangeListener() {

	    @Override
	    public void onPageSelected(int position) {
		// 根据位置设定fragment
		topTitle.setTitle(titles[position]);
		vPager.setCurrentItem(position);
		resetTab();
		setSelectedTab(position);
	    }

	    @Override
	    public void onPageScrolled(int arg0, float arg1, int arg2) {

	    }

	    @Override
	    public void onPageScrollStateChanged(int arg0) {

	    }
	});

	createRecordDir();

	// 获取并保存设备分辨率
	metrics = new DisplayMetrics();
	getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
	densityDpi = metrics.densityDpi;
	displayWidth = metrics.widthPixels;
	displayHeight = metrics.heightPixels;
	PreferenceUtil.setInt("device_dpi", densityDpi);
	PreferenceUtil.setInt("device_width", displayWidth);
	PreferenceUtil.setInt("device_height", displayHeight);
	Log.i("pixels: ", displayWidth + ", " + displayHeight);
	
	statusUrl = getString(R.string.http_server) + "liveStatus";
	userName = PreferenceUtil.getString("user_name", "");
    }
    
    /**
     * 退出登录确认框
     */
    private void showLogOutDialog() {
	
	isCamera = PreferenceUtil.getBoolean("is_camera", false);
	isRecord = PreferenceUtil.getBoolean("is_record", false);
	
	builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
	if(isCamera || isRecord){
	    builder.setMessage("当前有正在直播的进程，是否继续退出登录");
	}else {
	    builder.setMessage("退出登录");
	}
	builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {

	    @Override
	    public void onClick(DialogInterface dialog, int which) {
		dialog.dismiss();
	    }
	});
	builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {

	    @Override
	    public void onClick(DialogInterface dialog, int which) {
		if(isCamera || isRecord){
		    new StatusNotifyTask(statusUrl, userName, "0").execute();
		    PreferenceUtil.setBoolean("is_camera", false);
		    PreferenceUtil.setBoolean("is_record", false);
		}
		Intent intent = new Intent(RecordActivity.this, MainActivity.class);
		PreferenceUtil.setString("user_pwd", "");
		startActivity(intent);
	    }
	});
	builder.show();
    }

    /**
     * 在设备中创建视频文件存储目录
     */
    private void createRecordDir() {
	File file = null;
	for (int i = 0; i < RECORDDIR.length; i++) {
	    try {
		file = new File(RECORDDIR[i]);
		if (!file.exists()) {
		    file.mkdirs();
		}
		PreferenceUtil.setString(dirPreferences[i], RECORDDIR[i]);
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
    }
    

    @Override
    public void onClick(View v) {
	switch (v.getId()) {
	case R.id.tab_screen_record:
	    setSelectedTab(0);
	    break;
	case R.id.tab_camera_record:
	    setSelectedTab(1);
	    break;
	case R.id.tab_user:
	    setSelectedTab(2);
	    break;
	default:
	    break;
	}
    }

    /**
     * 修改选中tab的样式
     */
    private void setSelectedTab(int i) {
	switch (i) {
	case 0:
	    screenImage.setImageResource(R.drawable.phone_record_light);
	    screenText.setTextColor(getResources().getColor(R.color.tab_blue));
	    break;
	case 1:
	    cameraImage.setImageResource(R.drawable.camera_light);
	    cameraText.setTextColor(getResources().getColor(R.color.tab_blue));
	    break;
	case 2:
	    userImage.setImageResource(R.drawable.user_light);
	    userText.setTextColor(getResources().getColor(R.color.tab_blue));
	    break;
	default:
	    break;
	}

	vPager.setCurrentItem(i);
    }

    /**
     * 将tab重设为灰色
     */
    @SuppressWarnings("deprecation")
    private void resetTab() {
	screenImage.setImageResource(R.drawable.phone_record);
	cameraImage.setImageResource(R.drawable.camera);
	userImage.setImageResource(R.drawable.user);
	screenText.setTextColor(getResources().getColor(R.color.tab_text));
	cameraText.setTextColor(getResources().getColor(R.color.tab_text));
	userText.setTextColor(getResources().getColor(R.color.tab_text));
    }
    
    /**
     * 监听返回键，弹出退出应用提示框
     */
    @Override
    public void onBackPressed() {
	AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
	
	isCamera = PreferenceUtil.getBoolean("is_camera", false);
	isRecord = PreferenceUtil.getBoolean("is_record", false);
	
	if(isCamera || isRecord){
	    builder.setMessage("当前有正在直播的进程，是否继续退出应用");
	}else {
	    builder.setMessage("退出应用");
	}
	builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
	    
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
		dialog.dismiss();
	    }
	});
	builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
	    
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
		if(isCamera || isRecord){
		    new StatusNotifyTask(statusUrl, userName, "0").execute();
		    PreferenceUtil.setBoolean("is_camera", false);
		    PreferenceUtil.setBoolean("is_record", false);
		}
		MyApplication.getInstance().exit();
	    }
	});
	builder.show();
    }
    
    @Override  
    protected void onStart() {  
        super.onStart();  
        Log.e(TAG, "start onStart~~~");  
    }  
      
    @Override  
    protected void onRestart() {  
        super.onRestart();  
        Log.e(TAG, "start onRestart~~~");  
    }  
      
    @Override  
    protected void onResume() {  
        super.onResume();  
        Log.e(TAG, "start onResume~~~");  
    }  
      
    @Override  
    protected void onPause() {  
        super.onPause();  
        Log.e(TAG, "start onPause~~~");  
    }  
      
    @Override  
    protected void onStop() {  
        super.onStop();  
        Log.e(TAG, "start onStop~~~");  
    }  
      
    @Override  
    protected void onDestroy() {  
        super.onDestroy();  
        Log.e(TAG, "start onDestroy~~~");  
    }  

}
