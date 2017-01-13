package com.unionpay.csspublisher;

import com.unionpay.application.MyApplication;
import com.unionpay.model.ResultBean;
import com.unionpay.util.HttpUtil;
import com.unionpay.util.PreferenceUtil;
import com.unionpay.view.TopTitleBar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * 首页 登录管理
 * 
 * @author lichen2
 */
public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private TopTitleBar topTitle;
    private EditText userEdit;
    private EditText pwdEdit;
    private Button loginBtn, registerBtn;

    private String userName, password;
    private String savedUserName, savedPwd;

    private static String loginApiUrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	if((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0){  
	    finish();  
	    return;  
	 }  
	setContentView(R.layout.activity_main);

	MyApplication.getInstance().addActivity(this);

	topTitle = (TopTitleBar) findViewById(R.id.login_titlebar);
	topTitle.setTitle("用户登录");

	userEdit = (EditText) findViewById(R.id.input_phone);
	pwdEdit = (EditText) findViewById(R.id.input_password);
	loginBtn = (Button) findViewById(R.id.btn_login);
	registerBtn = (Button) findViewById(R.id.btn_fast_register);

	loginApiUrl = getString(R.string.http_server) + "login";

	autoLogin();

	loginBtn.setOnClickListener(new View.OnClickListener() {

	    @Override
	    public void onClick(View v) {
		login();
	    }
	});
	registerBtn.setOnClickListener(new View.OnClickListener() {

	    @Override
	    public void onClick(View v) {
		showRegisterToast();
	    }
	});
    }

    /**
     * 判断是否登录过，是则自动登录
     */
    private void autoLogin() {
	savedUserName = PreferenceUtil.getString("user_name", "");
	savedPwd = PreferenceUtil.getString("user_pwd", "");
	if (savedUserName.equals("") || savedPwd.equals("")) {
	    if (!savedUserName.equals("")) {
		userEdit.setText(savedUserName);
	    }
	    return;
	} else {
	    Intent intent = new Intent(MainActivity.this, RecordActivity.class);
	    startActivity(intent);
	}
    }

    /**
     * 登录
     */
    private void login() {
	userName = userEdit.getText().toString().trim();
	password = pwdEdit.getText().toString();

	if (userName.equals("") || password.equals("")) {
	    MyApplication.getInstance().showToast(this, "请输入正确的用户名和密码");
	} else {
	    new LoginTask(this, userName, password).execute();
	}
    }

    private void showRegisterToast() {
	MyApplication.getInstance().showToast(this, "当前只供内部测试，不提供注册功能");
    }

    /**
     * 验证登录信息
     * 
     * @author lichen2
     */
    class LoginTask extends AsyncTask<Object, Object, ResultBean> {

	private String userName;
	private String pwd;
	private ResultBean result;
	private Context context;

	public LoginTask(Context context, String userName, String password) {
	    this.context = context;
	    this.userName = userName;
	    this.pwd = password;
	}

	@Override
	protected ResultBean doInBackground(Object... params) {
	    try {
		result = HttpUtil.login(loginApiUrl, userName, pwd);
	    } catch (Exception e) {
		e.printStackTrace();
		MyApplication.getInstance().showToast(context, "发送请求失败 = =!");
	    }
	    return result;
	}

	protected void onPostExecute(ResultBean r) {
	    if (r == null) {
		MyApplication.getInstance().showToast(context, "登录验证请求失败");
		return;
	    }
	    if (r.getSuccess().equals("true")) {
		Intent intent = new Intent(context, RecordActivity.class);
		PreferenceUtil.setString("user_name", userName);
		PreferenceUtil.setString("user_pwd", password);
		startActivity(intent);
	    }
	    if (r.getSuccess().equals("false")) {
		MyApplication.getInstance().showToast(context, "用户名或密码错误");
		return;
	    }
	}
    }

    /**
     * 监听返回键，弹出退出应用提示框
     */
    @Override
    public void onBackPressed() {
	AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);

	boolean isCamera = PreferenceUtil.getBoolean("is_camera", false);
	boolean isRecord = PreferenceUtil.getBoolean("is_record", false);

	if (isCamera || isRecord) {
	    builder.setMessage("当前有正在直播的进程，是否继续退出应用");
	} else {
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
		MyApplication.getInstance().exit();
	    }
	});
	builder.show();
    }

}
