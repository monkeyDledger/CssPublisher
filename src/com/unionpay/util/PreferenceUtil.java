package com.unionpay.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

/**
 * SharedPreferences工具类
 * 存储数据
 * @author lichen2
 */
public class PreferenceUtil {
    
    private static SharedPreferences sharedPreferences = null;
    
    private static SharedPreferences.Editor editor;
    
    public PreferenceUtil(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }
    
    public static void init(String fileName, Context context) {
        if (null == sharedPreferences) {
            sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        }
    }
    
    /**
     * 
     * @return
     */
    public static SharedPreferences getSharedPreferences() {
        return sharedPreferences;
        
    }

    /**
     * 
     * @param key
     * @param def
     * @return
     */
    public static int getInt(String key, int def) {
        return sharedPreferences.getInt(key, def);
    }
    
    /**
     * 
     * @param key
     * @param def
     * @return
     */
    public static long getLong(String key, long def) {
        return sharedPreferences.getLong(key, def);
    }
    
    /**
     * 
     * @param key
     * @param context
     * @return
     */
    public static String getString(String key, String def) {
        return sharedPreferences.getString(key, def);
    }
    
    /**
     * 
     * @param key
     * @param context
     * @param def
     * @return
     */
    public static boolean getBoolean(String key, boolean def) {
        return sharedPreferences.getBoolean(key, def);
    }
    
    /**
     * 
     * @param key
     * @param list
     * @param context
     */
    public static void setIntList(String key, List<Integer> list, Context context) {
        List<String> listValue = new ArrayList();
        if (list != null) {
            Iterator localIterator = list.iterator();
            while (localIterator.hasNext())
                listValue.add(String.valueOf((Integer)localIterator.next()));
        }
        setList(key, listValue, context);
    }
    
    /**
     * 
     * @param key
     * @param list
     * @param context
     */
    public static void setList(String key, List<String> list, Context context) {
        String strResult = "";
        if (list != null && list.size() > 0) {
            Iterator iterator = list.iterator();
            while (iterator.hasNext()) {
                String strValue = (String)iterator.next();
                String strTemp = "";
                if ("".equals(strValue)) {
                    strTemp = (new StringBuilder()).append(strResult).append(" ").toString();
                }
                else {
                    strTemp = (new StringBuilder()).append(strResult).append(strValue).toString();
                }
                strResult = (new StringBuilder()).append(strTemp).append("#").toString();
            }
            Editor editor = sharedPreferences.edit();
            editor.putString(key, strResult);
            editor.commit();
        }
        
    }
    
    /**
     * 
     * @param key
     * @param value
     * @param context
     * @return
     */
    public static boolean setInt(String key, int value) {
        return sharedPreferences.edit().putInt(key, value).commit();
    }
    
    /**
     * 
     * @param key
     * @param value
     * @param context
     * @return
     */
    public static boolean setLong(String key, long value) {
        return sharedPreferences.edit().putLong(key, value).commit();
    }
    
    /**
     * 
     * @param key
     * @param value
     * @param context
     * @return
     */
    public static boolean setString(String key, String value) {
        return sharedPreferences.edit().putString(key, value).commit();
    }
    
    /**
     * 
     * @param key
     * @param value
     * @param context
     * @return
     */
    public static boolean setBoolean(String key, boolean value) {
        return sharedPreferences.edit().putBoolean(key, value).commit();
    }
    
    /**
     * 
     * @param key
     * @param context
     * @return
     */
    public static boolean remove(String key) {
        return sharedPreferences.edit().remove(key).commit();
    }
    
    /**
     * 
     * @param context
     * @return
     */
    public static boolean removeAll() {
        return sharedPreferences.edit().clear().commit();
    }
    
    
}
