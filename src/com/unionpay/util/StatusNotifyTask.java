package com.unionpay.util;

import com.unionpay.csspublisher.R;
import com.unionpay.model.ResultBean;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

/**
 * 用户直播状态通知
 * @author lichen2
 */
public class StatusNotifyTask extends AsyncTask<Object, Object, ResultBean>{
    
    private String url;
    
    private String userName;
    
    private String status;
    
    private Context context;
    
    public StatusNotifyTask(String userName, String status, Context context) {
	this.userName = userName;
	this.status = status;
	this.context = context;
	this.url = context.getString(R.string.node_server) + "liveStatus";
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
