package com.wifi.lib;

import android.app.Activity;
import android.util.Log;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GetService {

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");


    public void postJson(final MessageInform messageInform, Activity activity) {


        //申明给服务端传递一个json串
        //创建一个OkHttpClient对象
        OkHttpClient okHttpClient = new OkHttpClient();


        //创建一个请求对象
        Request request = new Request.Builder().url("http://www.hc-01.com/api/socket")
                .build();

        //发送请求获取响应
        try {
            Response response = okHttpClient.newCall(request).execute();
            //判断请求是否成功
            if (response.isSuccessful()) {
                final String s = response.body().string();
                Log.i("TAG", s);//打印服务端返回结果
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        messageInform.serviceCallback(true, s);
                    }
                });
            }
        } catch (final IOException e) {
            Log.e("TAG", "错误: " + e);
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    messageInform.serviceCallback(false, e.toString());
                }
            });
            e.printStackTrace();
        }
    }


}
