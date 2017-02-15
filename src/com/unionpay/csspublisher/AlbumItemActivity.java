package com.unionpay.csspublisher;

import java.security.KeyStore.PrivateKeyEntry;
import java.util.ArrayList;
import java.util.List;

import com.unionpay.adapter.AlbumItemAdapter;
import com.unionpay.application.MyApplication;
import com.unionpay.model.PhotoUpImageBucket;
import com.unionpay.model.PhotoUpImageItem;
import com.unionpay.util.FileUtil;
import com.unionpay.util.HttpUtil;
import com.unionpay.util.PreferenceUtil;
import com.unionpay.view.TopTitleBar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import okhttp3.Response;

/**
 * 图片上传
 * 
 * @author lichen2
 */
public class AlbumItemActivity extends Activity {

    private static final String TAG = "AlbumItemActivity";

    private GridView gridView;
    private TopTitleBar topTitle;
    private AlertDialog.Builder imgDialog;
    private LayoutInflater inflater;
    private View imgView;
    private ImageView image;

    private PhotoUpImageBucket photoUpImageBucket;
    private ArrayList<PhotoUpImageItem> selectImages;
    private AlbumItemAdapter adapter;

    private Context context;
    private String userName;
    private String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	MyApplication.getInstance().addActivity(this);
	setContentView(R.layout.activity_album_item);
	context = this;
	initView();
	initData();
    }

    private void initView() {

	topTitle = (TopTitleBar) findViewById(R.id.ablum_item_title);
	topTitle.setTitle("图片上传");
	topTitle.getLeftButton().setVisibility(View.VISIBLE);
	topTitle.getLeftButton().setOnClickListener(new OnClickListener() {

	    @Override
	    public void onClick(View v) {
		finish();
	    }
	});

	// 图片上传
	topTitle.setRightButton(R.drawable.upload, new OnClickListener() {

	    @Override
	    public void onClick(View v) {
		if (selectImages.size() <= 0) {
		    MyApplication.getInstance().showToast(context, "请至少选择一张图片");
		    return;
		}
		Log.i(TAG, " "+selectImages.get(0).getImageName()+ ": "+ selectImages.get(0).getImagePath()+"  "
			+ selectImages.get(0).getImageName());
		new ImagesUploadTask(context, userName, url, selectImages).execute();
	    }
	});

	gridView = (GridView) findViewById(R.id.album_item_gridv);
	selectImages = new ArrayList<PhotoUpImageItem>();

	Intent intent = getIntent();
	photoUpImageBucket = (PhotoUpImageBucket) intent.getSerializableExtra("imagelist");
	adapter = new AlbumItemAdapter(photoUpImageBucket.getImageList(), AlbumItemActivity.this);
	gridView.setAdapter(adapter);
	
	//点击选中图片
	gridView.setOnItemClickListener(new OnItemClickListener() {
	    @Override
	    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

		CheckBox checkBox = (CheckBox) view.findViewById(R.id.check);
		boolean state = !checkBox.isChecked();
		checkBox.setChecked(state);
		photoUpImageBucket.getImageList().get(position).setSelected(state);
		// adapter.notifyDataSetChanged();

		if (photoUpImageBucket.getImageList().get(position).isSelected()) {
		    if (selectImages.contains(photoUpImageBucket.getImageList().get(position))) {

		    } else {
			selectImages.add(photoUpImageBucket.getImageList().get(position));
		    }
		} else {
		    if (selectImages.contains(photoUpImageBucket.getImageList().get(position))) {
			selectImages.remove(photoUpImageBucket.getImageList().get(position));
		    } else {

		    }
		}
	    }
	});
	
	//长按放大图片显示
	gridView.setOnItemLongClickListener(new OnItemLongClickListener() {

	    @Override
	    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		Log.i(TAG, "long click"+ position);
		inflater = LayoutInflater.from(context);
		imgDialog = new AlertDialog.Builder(context, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
		imgView = inflater.inflate(R.layout.dialog_image, null);
		image = (ImageView)imgView.findViewById(R.id.image_large);
		String imagePath = photoUpImageBucket.getImageList().get(position).getImagePath();
		image.setImageBitmap(FileUtil.convertToBitmap(imagePath, 720, 1280));
		imgDialog.setView(imgView);
		imgDialog.setCancelable(true);
		imgDialog.show();
		return true;
	    }
	    
	});
    }

    private void initData() {
	userName = PreferenceUtil.getString("user_name", "");
	url = getString(R.string.http_server) + "receiveImages";
    }

    /**
     * 上传进程
     * 
     * @author lichen2
     */
    class ImagesUploadTask extends AsyncTask<Object, Object, Response> {

	String userName;
	String url;
	List<PhotoUpImageItem> images;
	Context context;
	ProgressDialog dialog;

	public ImagesUploadTask(Context context, String userName, String url, List<PhotoUpImageItem> files) {
	    this.context = context;
	    this.userName = userName;
	    this.url = url;
	    this.images = files;
	}

	protected void onPreExecute() {
	    dialog = new ProgressDialog(context, ProgressDialog.THEME_DEVICE_DEFAULT_LIGHT);
	    dialog.setMessage("图片上传中...");
	    dialog.setCancelable(false);
	    dialog.show();
	}

	@Override
	protected Response doInBackground(Object... params) {
	    Response response = null;
	    try {
		response = HttpUtil.postImages(context, url, images, userName);
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	    return response;
	}
	
	@Override
	protected void onPostExecute(Response result) {
	    if(result != null && result.isSuccessful()){
		dialog.dismiss();
		MyApplication.getInstance().showToast(context, "上传成功");
	    }else {
		dialog.dismiss();
		MyApplication.getInstance().showToast(context, "上传失败");
	    }
	}

    }

}
