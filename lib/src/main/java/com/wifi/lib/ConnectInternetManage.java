package com.wifi.lib;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

public class ConnectInternetManage implements MessageInform {

    private Context mContext;
    private String mIp;
    private Handler mHandler;
    private JsonsRootBean mJsonRootBean;
    private ConnectService mConnectService;
    private Activity mActivity;
    private boolean mIsReconnect = true;

    private TextView mReceiveTv;
    private int mReceiveNumber = 0;
    public static boolean mSendHex = false;
    public static boolean mAccept = false;

    public ConnectInternetManage(Context context, String ip, Handler handler) {
        this.mContext = context;
        this.mActivity = (Activity) context;
        this.mIp = ip;
        this.mHandler = handler;
    }

    public ConnectInternetManage(Context context, String ip, String post, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
        mJsonRootBean = new JsonsRootBean();
        mJsonRootBean.setAddress(ip);
        mJsonRootBean.setErrCode(0);
        mJsonRootBean.setPort(Integer.parseInt(post));
        mConnectService = new ConnectService(mHandler, mContext, mJsonRootBean.getAddress(), mJsonRootBean.getIntPort(), mManageHandler);
    }

    private Handler mManageHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull final Message msg) {
            if (msg.what == 0x01) {
                log("延时起效..");
                reconnect();
            }

            if (msg.what == 0x02) {
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mReceiveTv != null) {
                            try {
                                mReceiveNumber += Integer.parseInt(msg.obj.toString());
                                mReceiveTv.setText("R：" + mReceiveNumber);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
            return false;
        }
    });

    private synchronized void reconnect() {
        if (!mIsReconnect) {
            log("错误进入...");
            return;
        }
        mIsReconnect = false;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    do {
                        Thread.sleep(2000);
                        log("等待网络中...");
                    } while (!networkAvailable(mContext));

                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            log("有网络了,尝试重连..");
                            if (mJsonRootBean != null) {
                                mIsReconnect = true;
                                mConnectService = new ConnectService(mHandler, mContext, mJsonRootBean.getAddress(), mJsonRootBean.getIntPort(), mManageHandler);
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    //与模块通信  -->  发送信息给模块
    public void sendData(String data) {
        mConnectService.send(data);
    }

    public void getServices() {
        getService();//请求服务器
    }

    //断开服务器
    public void disConnectService() {
        try {
            if (mConnectService != null)
                mConnectService.showdown();
            if (isWifiConnected(mContext)) {//判断断开服务时，是否还连接着WiFi
                mJsonRootBean = null;
                sendDataModule(0);//配置模块为服务端，且改回原有的端口与ip
            } else {
                callbackMessage("4");
            }
            Message message = mHandler.obtainMessage();
            message.what = 0x04;
            message.obj = "dis";
            mHandler.sendMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unConnect() {
        try {
            if (mConnectService != null)
                mConnectService.showdown();
            mJsonRootBean = null;
            Message message = mHandler.obtainMessage();
            message.what = 0x04;
            message.obj = "dis";
            mHandler.sendMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JsonsRootBean getJsonRootBean() {
        return mJsonRootBean;
    }

    public void setReceiveTv(TextView receiveTv) {
        this.mReceiveTv = receiveTv;
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mConnectService != null && mConnectService.getState())
                    mReceiveTv.setText("R：" + mReceiveNumber);
            }
        });
    }

    public void clearReceiveNumber() {
        if (mReceiveTv == null)
            return;
        mReceiveNumber = 0;
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    mReceiveTv.setText("R：" + mReceiveNumber);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void setSendHex(boolean sendHex) {
        mSendHex = sendHex;
    }

    public void setAccept(boolean accept) {
        mAccept = accept;
    }

    public void getServiceMessage() {
        getService();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (mJsonRootBean == null) ;
                sendJsonRootBean(mJsonRootBean);

            }
        }).start();
    }

    @Override
    public void serviceCallback(boolean result, String data) {
        if (result) {
            //请求成功，解析数据
            Gson gson = new Gson();
            mJsonRootBean = gson.fromJson(data, JsonsRootBean.class);
            if (mJsonRootBean.getErrCode() == 0) {
                //服务器请求成功
                log("服务器请求成功..");
                callbackMessage("1");
                sendDataModule(1);
                sendJsonRootBean(mJsonRootBean);//把JsonRootBean提交给fragment
                //Toast.makeText(mContext, "服务器请求成功，正在配置模块，请稍后...", Toast.LENGTH_SHORT).show();
            } else if (mJsonRootBean.getErrCode() == 3) {
                callbackServiceError("错误：服务器请求延时，稍后再试（服务器繁忙）");
            } else if (mJsonRootBean.getErrCode() == 4) {
                callbackServiceError("错误：服务器资源已被占满，请稍后重试");
            } else {
                callbackServiceError("错误：服务器出现未知错误，错误代码:" + mJsonRootBean.getErrCode());
            }
        } else {
            //请求失败，特殊处理
            callbackServiceError("错误：出现未知错误..");
        }
    }

    @Override
    public void sendModuleCallback(boolean result, int pattern) {
        if (result) {
            //模块成功收到，断开连接
            if (pattern == 1) {
                //模块成功配置到服务器 -->  phone connect service
                log("模块成功配置到服务器");
                callbackMessage("2");
                mConnectService = new ConnectService(mHandler, mContext, mJsonRootBean.getAddress(), mJsonRootBean.getIntPort(), mManageHandler);
            } else {
                //模块成功配置初始值
                log("模块成功配置初始值");
                callbackMessage("3");
                Message message = mHandler.obtainMessage();
                message.what = 0x07;
                mHandler.sendMessage(message);
                Toast.makeText(mContext, "等待模块连上路由器", Toast.LENGTH_SHORT).show();
            }
        } else {
            //模块没收到，再次处理?
            if (pattern == 1) {
                callbackModuleError("错误：配置模块连接服务器失败，请等待app重连回模块");
            } else {
                callbackModuleError("错误：断开服务器失败，请到网页端配置Socket类型为 Service");
            }
        }
    }

    private void getService() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                new GetService().postJson(ConnectInternetManage.this, mActivity);//请求服务器
            }
        }).start();
    }

    private void sendDataModule(final int pattern) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mJsonRootBean != null)
                    new SendDataModule().postJson(pattern, mJsonRootBean.getAddress(), mIp, mJsonRootBean.getPort(), ConnectInternetManage.this, mActivity);
                else
                    new SendDataModule().postJson(pattern, null, mIp, null, ConnectInternetManage.this, mActivity);
            }
        }).start();
    }

    private static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifiNetworkInfo.isConnected();
    }

    /**
     * 判断当前网络是否可用
     */
    private static boolean networkAvailable(Context context) {
        // 得到连接管理器对象
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager
                    .getActiveNetworkInfo();
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void log(String str) {
        Log.d("AppRunConnectService", str);
    }

    private void callbackServiceError(String string) {
        Message message = mHandler.obtainMessage();
        message.what = 0x08;
        message.obj = string;
        mHandler.sendMessage(message);
    }

    private void callbackModuleError(String string) {
        Message message = mHandler.obtainMessage();
        message.what = 0x09;
        message.obj = string;
        mHandler.sendMessage(message);
    }

    private void callbackMessage(String string) {
        Message message = mHandler.obtainMessage();
        message.what = 0x10;
        message.obj = string;
        mHandler.sendMessage(message);
    }

    private void sendJsonRootBean(JsonsRootBean jsonsRootBean) {
        Message message = mHandler.obtainMessage();
        message.what = 0x01;
        message.obj = jsonsRootBean;
        mHandler.sendMessage(message);
    }
}
