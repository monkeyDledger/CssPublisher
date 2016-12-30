package com.unionpay.util;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

/**
 * 重写requestbody，读取上传进度
 * @author lichen2
 */
public class ProgressRequestUtil extends RequestBody{
    
    private RequestBody requestBody;
    
    private ProgressRequestListener progressListener;
    
    private BufferedSink bufferedSink;
    
    public ProgressRequestUtil(RequestBody requestBody, ProgressRequestListener progressListener) {
	this.requestBody = requestBody;
	this.progressListener = progressListener;
    }

    /**
     * 重写调用实际的响应体的contentType
     * @return MediaType
     */
    @Override
    public MediaType contentType() {
	// TODO Auto-generated method stub
	return requestBody.contentType();
    }
    
    /**
     * 重写调用实际的响应体的contentLength
     * @return contentLength
     * @throws IOException 异常
     */
    @Override
    public long contentLength() throws IOException {
        return requestBody.contentLength();
    }


    /**
     * 重写进行写入
     * @param sink sink
     * @throws IOException 异常
     */
    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        if (bufferedSink == null) {
//            //包装
            bufferedSink = Okio.buffer(sink(sink));

        }
        //写入
        requestBody.writeTo(bufferedSink);
        //必须调用flush，否则最后一部分数据可能不会被写入
        bufferedSink.flush();
    }

    /**
     * 写入，回调进度接口
     * @param sink Sink
     * @return Sink
     */
    private Sink sink(Sink sink) {
        return new ForwardingSink(sink) {
            //当前写入字节数
            long bytesWritten = 0L;
            //总字节长度，避免多次调用contentLength()方法
            long contentLength = 0L;

            @Override
            public void write(Buffer source, long byteCount) throws IOException {
                super.write(source, byteCount);
                if (contentLength == 0) {
                    //获得contentLength的值，后续不再调用
                    contentLength = contentLength();
                }
                //增加当前写入的字节数
                bytesWritten += byteCount;
                //回调
                if(progressListener != null){
                    progressListener.onRequestProgress(bytesWritten, contentLength, bytesWritten == contentLength);
                }
            }
        };
    }

}
