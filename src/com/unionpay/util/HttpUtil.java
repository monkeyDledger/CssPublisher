package com.unionpay.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.unionpay.application.MyApplication;
import com.unionpay.model.FileInfoBean;
import com.unionpay.model.ResultBean;

import android.content.Context;
import android.util.Log;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Http通信工具类
 * @author lichen2
 */
public class HttpUtil {

    private static final int TIME_OUT = 30000; // 超时时间

    protected int BUFFER_SIZE = 8192;

    private static final String CHARSET = "UTF-8";
    private static final String CONTENT_TYPE = "application/json";

    private static Gson gson = new Gson();
    
    private static MediaType MEDIA_TYPE_MP4 = MediaType.parse("application/octet-stream");
    
    /**
     * 发送post请求
     * @param url
     * @param data
     * @return
     */
    public static String postData(String url, String data){
	InputStream is = null;
	HttpURLConnection urlConnection = null;
	try {
	    URL serverUrl = new URL(url);
	    urlConnection = (HttpURLConnection)serverUrl.openConnection();
	    
	    urlConnection.setConnectTimeout(TIME_OUT);
	    urlConnection.setRequestProperty("Content-Type", CONTENT_TYPE);
	    urlConnection.setDoOutput(true);
	    urlConnection.setDefaultUseCaches(true);
	    urlConnection.setRequestMethod("POST");
	    
	    DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
	    wr.writeBytes(data);
	    wr.flush();
	    wr.close();
	    
	    int statuCode = urlConnection.getResponseCode();
	    if(statuCode == HttpsURLConnection.HTTP_OK){
		is = new BufferedInputStream(urlConnection.getInputStream());
		String response = dealResponseResult(is);
		return response;
	    }
	    
	} catch (Exception e) {
	    e.printStackTrace();
	}
	return null;
    }
    
    private static String dealResponseResult(InputStream inputStream) {
	String resultData = null;
	ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	byte[] data = new byte[1024];
	int len = 0;
	try {
	    while ((len = inputStream.read(data)) != -1) {
		byteArrayOutputStream.write(data, 0, len);
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	}
	resultData = new String(byteArrayOutputStream.toByteArray());
	System.out.println("resultData: "+ resultData);
	return resultData;
    }
    
    public static String getCurrentTime(){
	Date date = new Date();
	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	return format.format(date);
    }
    
    /**
     * 验证登录
     * @param url
     * @param userName
     * @param password
     * @return
     */
    public static ResultBean login(String url, String userName, String password){
	JsonObject json = new JsonObject();
	json.addProperty("username", userName);
	json.addProperty("password", password);
	System.out.println("jsonData: "+ json.toString());
	String result = postData(url, json.toString());
	ResultBean resultBean = gson.fromJson(result, ResultBean.class);
	return resultBean;
    }
    
    /**
     * 根据用户名通知该用户是否开启直播
     * @param url
     * @param userName
     * @param isPublish， 0：未开启直播， 1：正在直播
     * @return
     */
    public static ResultBean sendPublishStatus(String url, String userName, String isPublish){
	JsonObject json = new JsonObject();
	json.addProperty("username", userName);
	json.addProperty("isPublish", isPublish);
	String result = postData(url, json.toString());
	System.out.println("jsonData: "+ json.toString());
	
	ResultBean resultBean = gson.fromJson(result, ResultBean.class);
	return resultBean;
    }
    
    
    /**
     * okhttp批量上传文件,带参数和进度
     * @param url
     * @param files
     * @throws IOException 
     */
    public static Response postFiles(final Context context, String url, List<FileInfoBean> files,  String userName)
	    throws IOException{
	OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
	
	MultipartBody.Builder bodyBuilder = new MultipartBody.Builder();
	bodyBuilder.setType(MultipartBody.FORM);
	//添加文件
	for(FileInfoBean file : files){
	    File object = new File(file.getAbsolutePath());
	    if(object.exists()){
		RequestBody body = RequestBody.create(MEDIA_TYPE_MP4, object);
		bodyBuilder.addFormDataPart(getCurrentTime(), file.getName(), 
			body);
	    }
	}
	
	//添加参数，用户名
	if(userName != null){
	    bodyBuilder.addFormDataPart("userName", userName);
	}
	
	Request request = new Request.Builder()
		.url(url)
		.post(bodyBuilder.build())
		.build();
	OkHttpClient client = builder.connectTimeout(30, TimeUnit.SECONDS)
		.writeTimeout(100, TimeUnit.SECONDS)
		.build();
	
	Call call = client.newCall(request);
	
	return call.execute();
	
	/*
	 //okhttp异步调用
	call.enqueue(new Callback() {
	    
	    @Override
	    public void onResponse(Call call, Response response) throws IOException {
		System.out.println("okhttp success");
		Looper.prepare();
		MyApplication.getInstance().showToast(context, "文件上传成功");
		Looper.loop();
		System.out.println(response.body().string());
	    }
	    
	    @Override
	    public void onFailure(Call arg0, IOException e) {
		e.printStackTrace();
		Looper.prepare();
		MyApplication.getInstance().showToast(context, "文件上传失败");
		Looper.loop();
	    }
	});*/
	
    }
    
    
}
