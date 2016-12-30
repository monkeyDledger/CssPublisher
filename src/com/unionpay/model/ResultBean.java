package com.unionpay.model;

/**
 * Server端返回数据类
 * @author lichen2
 */
public class ResultBean {
    
    public String success;
    
    public String data;
    
    public String error;

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
    
    

}
