package com.wifi.lib;

import android.app.Activity;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SendDataModule {

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");


    public void postJson(final int sockttype, String val, String ip, String post, final MessageInform messageInform, Activity activity) {

        JSONObject user = new JSONObject();

        try {
            if (sockttype == 1) {
                user.put("sockttype", "1");
                user.put("transport", "0");
                user.put("link_ip", val);
                user.put("link_port", post);
            } else {
                user.put("sockttype", "0");
                user.put("transport", "0");
                user.put("link_ip", "192.168.4.1");
                user.put("link_port", "8080");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //申明给服务端传递一个json串
        //创建一个OkHttpClient对象
        OkHttpClient okHttpClient = new OkHttpClient();

        //创建一个RequestBody(参数1：数据类型 参数2传递的json串)
        String json = user.toString();
        RequestBody requestBody = RequestBody.create(JSON, json);

        //创建一个请求对象
        Request request = new Request.Builder().url("http://" + ip + ":80/config")
                .post(requestBody).build();

        //发送请求获取响应
        try {
            Response response = okHttpClient.newCall(request).execute();
            //判断请求是否成功
            if (response.isSuccessful()) {
                String s = response.body().string();
                Log.i("Success tag", s);//打印服务端返回结果
                try {
                    Thread.sleep(1000);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            messageInform.sendModuleCallback(true, sockttype);
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (final IOException e) {
            Log.e("TAG", "错误: " + e);
            //e.printStackTrace();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (e.toString().equals("java.net.SocketException: Connection reset") || e.toString().equals("java.net.SocketTimeoutException: timeout")) {
                        messageInform.sendModuleCallback(true, sockttype);
                    } else {
                        messageInform.sendModuleCallback(false, sockttype);
                    }
                }
            });
        }
    }

}
