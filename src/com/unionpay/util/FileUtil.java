package com.unionpay.util;

import java.io.File;
import java.lang.ref.WeakReference;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.unionpay.model.FileInfoBean;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * 文件操作工具类
 * @author lichen2
 */
public class FileUtil {
    
    private static String path;
    
    private static FileInfoBean fileInfo;
    private static List<FileInfoBean> fileList;
    
    public static boolean isDirectory(String path){
	File file = new File(path);
	if(file.isDirectory()){
	    return true;
	}else {
	    return false;
	}
    }
    
    /**
     * 根据目录返回目录下所有文件
     * 包括文件最后修改时间，文件名和文件绝对路径
     * @param dir
     * @return
     */
    public static List<FileInfoBean> getFileInfo(String path){
	if(isDirectory(path)){
	    File dir = new File(path);
	    fileList = new ArrayList<FileInfoBean>();
	    File[] files = dir.listFiles();
	    for(File file:files){
		String name = file.getName();
		if(name.endsWith(".mp4")){
		    fileInfo = new FileInfoBean();
		    String fileTime = formatFileDate(file.lastModified(), "yyyy-MM-dd HH:mm");
		    fileInfo.setName(name);
		    fileInfo.setTime(fileTime);
		    fileInfo.setAbsolutePath(file.getAbsolutePath());
		    fileList.add(fileInfo);
		}
	    }
	    return fileList;
	}else {
	    return null;
	}
    }
    
    /**
     * 重命名，复制到指定路径，并删除原文件
     * @param oldPath
     * @param newPath
     */
    public static void renameFileAndDelete(String oldPath, String newPath){
	File oldFile = new File(oldPath);
	File newFile = new File(newPath);
	if(oldFile.exists()){
	    oldFile.renameTo(newFile);
	}
	if(newFile.exists()){
	    oldFile.delete();
	}
    }
    
    /**
     * 将long型的时间格式转为指定格式String
     * @param time
     * @return
     */
    public static String formatFileDate(long time, String formatType){
	Date date = new Date(time);
	SimpleDateFormat format = new SimpleDateFormat(formatType);
	return format.format(date);
    }
    
    /**
     * 根据路径返回文件名
     * @param path
     * @return
     */
    public static String getNameByPath(String path){
	File file = new File(path);
	if(file.exists()){
	    return file.getName();
	}else {
	    return "";
	}
    }
    
    /**
     * 根据路径返回Bitmap
     * @param path
     * @param w
     * @param h
     * @return
     */
    public static Bitmap convertToBitmap(String path, int w, int h) {
	BitmapFactory.Options opts = new BitmapFactory.Options();
	// 设置为ture只获取图片大小
	opts.inJustDecodeBounds = true;
	opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
	// 返回为空
	BitmapFactory.decodeFile(path, opts);
	int width = opts.outWidth;
	int height = opts.outHeight;
	float scaleWidth = 0.f, scaleHeight = 0.f;
	if (width > w || height > h) {
		// 缩放
		scaleWidth = ((float) width) / w;
		scaleHeight = ((float) height) / h;
	}
	opts.inJustDecodeBounds = false;
	float scale = Math.max(scaleWidth, scaleHeight);
	opts.inSampleSize = (int)scale;
	WeakReference<Bitmap> weak = new WeakReference<Bitmap>(BitmapFactory.decodeFile(path, opts));
	return Bitmap.createScaledBitmap(weak.get(), w, h, true);
}

}
