package com.unionpay.util;

import com.unionpay.model.ResultBean;

import android.os.AsyncTask;
import android.util.Log;

/**
 * 用户直播状态通知
 * 网络请求不能放在UI进程中
 * @author lichen2
 */
public class StatusNotifyTask extends AsyncTask<Object, Object, ResultBean>{
    
    private String url;
    
    private String userName;
    
    private String status;
    
    public StatusNotifyTask(String url, String userName, String status) {
	this.userName = userName;
	this.status = status;
	this.url = url;
    }

    @Override
    protected ResultBean doInBackground(Object... params) {
	ResultBean result = null;
	try{
	    result = HttpUtil.sendPublishStatus(url, userName, status);
	}catch(Exception e){
	    e.printStackTrace();
	}
	return result;
    }
    
    protected void onPostExecute(ResultBean resultBean) {
	if(resultBean != null){
	    if(resultBean.getSuccess().equals("true")){
		    Log.i("publish status notify", "success");
		}else {
		    Log.e("publish status notify", "failed");
		}
	}
    }

}
