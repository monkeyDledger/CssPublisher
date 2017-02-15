package com.unionpay.csspublisher;

import java.util.List;

import com.unionpay.adapter.AlbumsAdapter;
import com.unionpay.application.MyApplication;
import com.unionpay.model.PhotoUpImageBucket;
import com.unionpay.util.PhotoUpAlbumHelper;
import com.unionpay.util.PhotoUpAlbumHelper.GetAlbumList;
import com.unionpay.view.TopTitleBar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

/**
 * 本地相册列表
 * 
 * @author lichen2
 */
public class AlbumsActivity extends Activity {

    private final static String TAG = "AlbumsActivity";

    private String userName;
    private String url;

    private TopTitleBar topTitle;
    private GridView gridView;

    private AlbumsAdapter adapter;
    private PhotoUpAlbumHelper photoUpAlbumHelper;
    private List<PhotoUpImageBucket> list;

    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_albums);
	MyApplication.getInstance().addActivity(this);
	init();
	loadData();
	onItemClick();
    }

    private void init() {
	topTitle = (TopTitleBar) findViewById(R.id.albums_title);
	topTitle.setTitle("本地相册");
	topTitle.getLeftButton().setVisibility(View.VISIBLE);
	topTitle.getLeftButton().setOnClickListener(new OnClickListener() {
	    
	    @Override
	    public void onClick(View v) {
		finish();
	    }
	});;
	
	gridView = (GridView) findViewById(R.id.albums_gridview);
	adapter = new AlbumsAdapter(AlbumsActivity.this);
	gridView.setAdapter(adapter);
    }

    private void loadData() {
	photoUpAlbumHelper = PhotoUpAlbumHelper.getHelper();
	photoUpAlbumHelper.init(AlbumsActivity.this);
	photoUpAlbumHelper.setGetAlbumList(new GetAlbumList() {
	    @Override
	    public void getAlbumList(List<PhotoUpImageBucket> list) {
		adapter.setArrayList(list);
		adapter.notifyDataSetChanged();
		AlbumsActivity.this.list = list;
	    }
	});
	photoUpAlbumHelper.execute(false);
    }

    private void onItemClick() {
	gridView.setOnItemClickListener(new OnItemClickListener() {
	    @Override
	    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Intent intent = new Intent(AlbumsActivity.this, AlbumItemActivity.class);
		intent.putExtra("imagelist", list.get(position));
		startActivity(intent);
	    }
	});
    }

    @Override
    protected void onDestroy() {
	super.onDestroy();
    }

}
