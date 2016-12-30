package com.unionpay.util;

/**
 * Okhttp request进度监听
 * @author lichen2
 */
public interface ProgressRequestListener {
    void onRequestProgress(long bytesWritten, long contentLength, boolean done);
}
