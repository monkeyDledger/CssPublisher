package com.unionpay.csspublisher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.unionpay.application.MyApplication;
import com.unionpay.model.FileInfoBean;
import com.unionpay.util.FileUtil;
import com.unionpay.util.HttpUtil;
import com.unionpay.util.PreferenceUtil;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import okhttp3.Response;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 我的页面 
 * 视频列表的展示、批量删除、上传文件
 * @author lichen2
 */
public class UserFragment extends Fragment {

    private static final String TAG = "UserFragment";

    private String userName;
    private String httpUrl = null;
    private static final int FILE_DELETE_CODE = 0;
    private static final int FILE_UPLOAD_CODE = 1;

    private ExpandableListView eListView;
    private RecordsListAdapter mAdapter;
    private TextView userTextView;
    private ImageView refreshView;
    private Button deleteBtn, uploadBtn;

    private String[] groupNames = { "本地录屏", "直播录屏", "摄像头录像" };
    private int[] icons = { R.drawable.local_record, R.drawable.rtmp_record, R.drawable.camera_record };
    private String[] paths = { "/sdcard/csspublisher/records/", "/sdcard/csspublisher/rtmps/",
	    "/sdcard/csspublisher/cameras/" };
    private List<FileInfoBean> fileList, allFiles;
    private List<List<FileInfoBean>> fileGroups;

    private AlertDialog.Builder builder = null;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	View view = inflater.inflate(R.layout.fragment_user, null);

	userTextView = (TextView) view.findViewById(R.id.user_name);
	refreshView = (ImageView) view.findViewById(R.id.user_refresh);
	deleteBtn = (Button) view.findViewById(R.id.user_delete_files);
	uploadBtn = (Button) view.findViewById(R.id.user_upload_files);
	eListView = (ExpandableListView) view.findViewById(R.id.user_expand_list);
	initData();

	mAdapter = new RecordsListAdapter(getActivity());

	userTextView.setOnClickListener(new View.OnClickListener() {

	    @Override
	    public void onClick(View v) {
		showLogOutDialog();
	    }
	});

	eListView.setOnGroupClickListener(new OnGroupClickListener() {

	    @Override
	    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
		// TODO Auto-generated method stub
		return false;
	    }
	});
	eListView.setOnChildClickListener(new OnChildClickListener() {

	    @Override
	    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition,
		    long id) {
		FileInfoBean selectedItem = fileGroups.get(groupPosition).get(childPosition);
		showDialog(selectedItem);
		return false;
	    }
	});

	eListView.setAdapter(mAdapter);

	// 重新获取本地所有视频文件，并更新
	refreshView.setOnClickListener(new View.OnClickListener() {

	    @Override
	    public void onClick(View v) {
		refreshList();
		MyApplication.getInstance().showToast(getActivity(), "视频列表已更新");
	    }
	});

	deleteBtn.setOnClickListener(new View.OnClickListener() {

	    @Override
	    public void onClick(View v) {
		getAllFiles();
		if (allFiles == null || allFiles.size() == 0) {
		    MyApplication.getInstance().showToast(getActivity(), "未发现视频文件");
		} else {
		    showFileChooseDialog("删除", FILE_DELETE_CODE);
		}
	    }
	});

	uploadBtn.setOnClickListener(new View.OnClickListener() {

	    @Override
	    public void onClick(View v) {
		getAllFiles();
		if (allFiles == null || allFiles.size() == 0) {
		    MyApplication.getInstance().showToast(getActivity(), "未发现视频文件");
		} else {
		    showFileChooseDialog("上传", FILE_UPLOAD_CODE);
		}
	    }
	});

	return view;
    }

    @SuppressWarnings("deprecation")
    private void initData() {

	userName = PreferenceUtil.getString("user_name", "你");
	userTextView.setText(userName);
	
	httpUrl = getString(R.string.node_server);

	fileList = new ArrayList<FileInfoBean>();
	allFiles = new ArrayList<FileInfoBean>();
	fileGroups = new ArrayList<List<FileInfoBean>>();
	getFileList();
    }

    /**
     * 获取视频文件列表
     */
    private void getFileList() {
	fileList.clear();
	fileGroups.clear();
	for (int i = 0; i < paths.length; i++) {
	    fileList = FileUtil.getFileInfo(paths[i]);
	    fileGroups.add(fileList);
	}
    }

    private void refreshList() {
	getFileList();
	Log.i(TAG, "" + fileList.size());
	mAdapter.notifyDataSetChanged();
    }

    /**
     * 获取当前所有视频文件
     * 
     * @return List<FileInfoBean>
     */
    private List<FileInfoBean> getAllFiles() {
	if (allFiles != null) {
	    allFiles.clear();
	    for (int i = 0; i < paths.length; i++) {
		fileList = FileUtil.getFileInfo(paths[i]);
		for (int j = 0; j < fileList.size(); j++) {
		    allFiles.add(fileList.get(j));
		}
	    }
	}
	if (allFiles.size() == 0) {
	    return null;
	} else {
	    return allFiles;
	}
    }

    /**
     * 退出登录确认框
     */
    private void showLogOutDialog() {
	builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
	builder.setMessage("退出登录");
	builder.setNegativeButton("取消", new OnClickListener() {

	    @Override
	    public void onClick(DialogInterface dialog, int which) {
		dialog.dismiss();
	    }
	});
	builder.setPositiveButton("确认", new OnClickListener() {

	    @Override
	    public void onClick(DialogInterface dialog, int which) {
		Intent intent = new Intent(getActivity(), MainActivity.class);
		PreferenceUtil.setString("user_pwd", "");
		startActivity(intent);
	    }
	});
	builder.show();
    }

    /**
     * 视频播放确认框
     */
    private void showDialog(final FileInfoBean file) {
	builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
	builder.setMessage("播放该视频");
	final String path = file.getAbsolutePath();

	builder.setNegativeButton("取消", new OnClickListener() {

	    @Override
	    public void onClick(DialogInterface dialog, int which) {
		dialog.dismiss();
	    }
	});
	builder.setPositiveButton("确认", new OnClickListener() {

	    @Override
	    public void onClick(DialogInterface dialog, int which) {
		if (file.getAbsolutePath() != null) {
		    Intent intent = new Intent(getActivity(), VideoPlayActivity.class);
		    intent.putExtra("video_path", file.getAbsolutePath());
		    startActivity(intent);
		} else {
		    MyApplication.getInstance().showToast(getActivity(), "文件路径错误，无法播放");
		}
	    }
	});
	builder.show();
    }

    /**
     * 文件删除多选框
     */
    private void showFileChooseDialog(String positiveText, final int code) {
	final List<FileInfoBean> choosedFiles = new ArrayList<>();
	final int length = allFiles.size();
	String[] fileInfo = new String[length];
	final boolean[] isChecked = new boolean[length];
	for (int i = 0; i < length; i++) {
	    fileInfo[i] = allFiles.get(i).getName();
	    isChecked[i] = false;
	}

	builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
	builder.setTitle("文件操作");
	builder.setMultiChoiceItems(fileInfo, isChecked, new OnMultiChoiceClickListener() {

	    @Override
	    public void onClick(DialogInterface dialog, int which, boolean checked) {
	    }
	});
	builder.setNegativeButton("取消", new OnClickListener() {

	    @Override
	    public void onClick(DialogInterface dialog, int which) {
		// TODO Auto-generated method stub
		dialog.dismiss();
	    }
	});
	builder.setPositiveButton(positiveText, new OnClickListener() {

	    @Override
	    public void onClick(DialogInterface dialog, int which) {

		for (int i = 0; i < length; i++) {
		    if (isChecked[i]) {
			choosedFiles.add(allFiles.get(i));
		    }
		}
		Log.e(TAG, "choosed files" + choosedFiles.size());
		if(choosedFiles.size() == 0){
		    MyApplication.getInstance().showToast(getActivity(), "至少选择一个文件");
		    return ;
		}

		if (code == FILE_DELETE_CODE) {
		    if (deleteFiles(choosedFiles)) {
			refreshList();
			MyApplication.getInstance().showToast(getActivity(), "文件删除成功");
		    } else {
			MyApplication.getInstance().showToast(getActivity(), "文件删除失败");
		    }
		} else {
		    new uploadTask(getActivity(), httpUrl+"receiveFiles", userName, choosedFiles).execute();
		}
	    }
	});
	builder.show();
    }

    /**
     * 文件删除操作
     * 
     * @param files
     */
    private boolean deleteFiles(List<FileInfoBean> files) {
	for (FileInfoBean file : files) {
	    try {
		File f = new File(file.getAbsolutePath());
		if (f.exists()) {
		    f.delete();
		} else {
		    return false;
		}
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
	return true;
    }
    
    class uploadTask extends AsyncTask<Object, Object, Response>{
	
	String userName;
	Context context;
	List<FileInfoBean> files;
	String url;
	ProgressDialog progress;
	Response response;
	
	public uploadTask(Context context, String url, String userName, List<FileInfoBean> list) {
	    this.context = context;
	    this.url = url;
	    this.userName = userName;
	    this.files = list;
	}
	
	protected void onPreExecute() {
	    progress = new ProgressDialog(context, ProgressDialog.THEME_DEVICE_DEFAULT_LIGHT);
	    progress.setMessage("上传ing...");
	    progress.show();
	}

	@Override
	protected Response doInBackground(Object... params) {
	    try {
		response = HttpUtil.postFiles(context, url, files, userName);
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	    return response;
	}
	
	protected void onPostExecute(Response r) {
	    if(r != null && r.isSuccessful()){
		progress.dismiss();
		MyApplication.getInstance().showToast(context, "上传成功");
	    }else {
		MyApplication.getInstance().showToast(context, "上传成功");
	    }
	}
    }


    /**
     * expandablelist 适配器
     * 
     * @author lichen2
     */
    class RecordsListAdapter extends BaseExpandableListAdapter {

	private Context context;
	private LayoutInflater inflater;

	public RecordsListAdapter(Context context) {
	    this.context = context;
	    this.inflater = LayoutInflater.from(context);
	}

	@Override
	public int getGroupCount() {
	    // TODO Auto-generated method stub
	    return groupNames.length;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
	    // TODO Auto-generated method stub
	    // return fileArrayLists[groupPosition].size();
	    return fileGroups.get(groupPosition).size();
	}

	@Override
	public Object getGroup(int groupPosition) {
	    // TODO Auto-generated method stub
	    // return fileArrayLists[groupPosition];
	    return fileGroups.get(groupPosition);
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
	    // TODO Auto-generated method stub
	    // return fileArrayLists[groupPosition].get(childPosition);
	    return fileGroups.get(groupPosition).get(childPosition);
	}

	@Override
	public long getGroupId(int groupPosition) {
	    // TODO Auto-generated method stub
	    return 0;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
	    // TODO Auto-generated method stub
	    return 0;
	}

	@Override
	public boolean hasStableIds() {
	    // TODO Auto-generated method stub
	    return false;
	}

	/**
	 * 获取显示指定组的视图
	 */
	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
	    GroupHolder groupHolder = null;

	    // 初始化控件
	    if (convertView == null) {
		convertView = inflater.inflate(R.layout.user_list_group, null);
		groupHolder = new GroupHolder();
		groupHolder.iconImage = (ImageView) convertView.findViewById(R.id.user_group_image);
		groupHolder.arrowImage = (ImageView) convertView.findViewById(R.id.user_group_arrow);
		groupHolder.groupText = (TextView) convertView.findViewById(R.id.user_group_name);

		convertView.setTag(groupHolder);
	    } else {
		groupHolder = (GroupHolder) convertView.getTag();
	    }

	    groupHolder.iconImage.setImageResource(icons[groupPosition]);
	    groupHolder.groupText.setText(groupNames[groupPosition]);

	    if (isExpanded) {
		groupHolder.arrowImage.setImageResource(R.drawable.arrow_down);
	    } else {
		groupHolder.arrowImage.setImageResource(R.drawable.arrow_right);
	    }

	    return convertView;
	}

	/**
	 * 获取一个组的子视图
	 * 
	 * @param groupPosition
	 * @param childPosition
	 * @param isLastChild
	 * @param convertView
	 * @param parent
	 * @return
	 */
	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
		ViewGroup parent) {
	    ItemHolder itemHolder = null;
	    if (convertView == null) {
		itemHolder = new ItemHolder();
		convertView = inflater.inflate(R.layout.user_list_item, null);
		itemHolder.itemLayout = (LinearLayout) convertView.findViewById(R.id.user_with_records);
		itemHolder.noRecordsText = (TextView) convertView.findViewById(R.id.user_no_records);
		itemHolder.timeText = (TextView) convertView.findViewById(R.id.user_record_time);
		itemHolder.nameText = (TextView) convertView.findViewById(R.id.user_record_name);
		convertView.setTag(itemHolder);
	    } else {
		itemHolder = (ItemHolder) convertView.getTag();
	    }

	    List<FileInfoBean> itemList = fileGroups.get(groupPosition);
	    if (itemList == null || itemList.size() == 0) {
		// 该group下没有视频文件
		itemHolder.itemLayout.setVisibility(View.GONE);
		itemHolder.noRecordsText.setVisibility(View.VISIBLE);
	    } else {
		itemHolder.timeText.setText(itemList.get(childPosition).getTime());
		itemHolder.nameText.setText(itemList.get(childPosition).getName());
	    }

	    return convertView;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
	    // TODO Auto-generated method stub
	    return true;
	}

	class GroupHolder {
	    public ImageView iconImage, arrowImage;

	    public TextView groupText;
	}

	class ItemHolder {
	    public LinearLayout itemLayout;

	    public TextView noRecordsText, timeText, nameText;
	}

    }

}
