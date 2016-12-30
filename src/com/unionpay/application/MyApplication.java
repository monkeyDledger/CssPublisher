package com.unionpay.application;

import java.util.LinkedList;
import java.util.List;

import com.unionpay.util.PreferenceUtil;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

/**
 * 
 * @author lichen2
 */
public class MyApplication extends Application {

    public List<Activity> activitieList = new LinkedList<Activity>();

    private static MyApplication instance;

    // 单例模式中获取唯一的MyApplication实例
    public static MyApplication getInstance() {
	if (null == instance) {
	    instance = new MyApplication();
	}
	return instance;
    }

    @Override
    public void onCreate() {
	super.onCreate();
	PreferenceUtil.init("css_publish", this);
    }

    /**
     * activity添加到容器中
     */
    public void addActivity(Activity activity) {
	activitieList.add(activity);
    }

    /**
     * 退出应用
     */
    public void exit() {
	for (Activity activity : activitieList) {
	    activity.finish();
	}
	System.exit(0);
    }
    
    public void showProgressDialog(Context context){
	
    }
    
    
    /**
     * toast提示
     * @param context
     * @param msg
     */
    public void showToast(Context context, String msg){
	Toast toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
	toast.setGravity(Gravity.CENTER, 0, 0);
	toast.show();
    }
    
    

}
