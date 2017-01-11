package com.unionpay.view;

import com.unionpay.csspublisher.R;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 自定义titlebar
 * @author lichen2
 */
public class TopTitleBar extends LinearLayout {

    private Context mContext;

    private ImageView left, right;

    private TextView title;

    public TopTitleBar(Context context) {
	super(context);
	init(context);
    }

    public TopTitleBar(Context context, AttributeSet attrs) {
	super(context, attrs);
	init(context);
    }

    public void init(Context context) {
	this.mContext = context;
	LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	inflater.inflate(R.layout.toptitle_layout, this);

	left = (ImageView) findViewById(R.id.left);
	right = (ImageView) findViewById(R.id.right);
	title = (TextView) findViewById(R.id.top_title);
	left.setOnClickListener(new OnClickListener() {
	    @Override
	    public void onClick(View v) {
		((Activity) mContext).finish();
	    }
	});
    }

    public void setLeftButton(int iconId, OnClickListener listener) {
	left.setImageResource(iconId);
	left.setVisibility(View.VISIBLE);
	left.setOnClickListener(listener);
    }

    public void setRightButton(int iconId, OnClickListener listener) {
	right.setImageResource(iconId);
	right.setVisibility(View.VISIBLE);
	right.setOnClickListener(listener);
    }

    public void setTitle(String titleString) {
	title.setText(titleString);
    }

    public void setTitle(int titleId) {
	title.setText(titleId);
    }

    public ImageView getLeftButton() {
	return left;
    }

    public ImageView getRightButton() {
	return right;
    }

    public TextView getTitle() {
	return title;
    }

}
