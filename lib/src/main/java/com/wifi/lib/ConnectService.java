package com.wifi.lib;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

import static com.wifi.lib.ToolClass.bytesToHexString;
import static com.wifi.lib.ToolClass.hexString2ByteArray;


public class ConnectService {

    private static final int TMO_CONNECTTION = 2000;

    private String ip;
    private int port;
    private BufferedReader in;
    private OutputStream outputStream;//发送流
    private Socket socket;

    private Thread sendSocketThread;
    private Thread listenerSocketThread;

    private String AS = "AppRunPhoneConnect";

    private ArrayList<String> list = new ArrayList<>();

    private boolean sign = true;

    private Handler mHandler;

    private Context context;

    private Handler mManageHandler;

    private DormancyManager dormancyManager;//保持CPU唤醒30分钟


    ConnectService(Handler handler, Context context, String ip, int port, Handler mManageHandler) {
        this.mHandler = handler;
        this.context = context;
        this.ip = ip;
        this.port = port;
        this.mManageHandler = mManageHandler;
        dormancyManager = new DormancyManager();
        dormancyManager.wakeAndUnlock(context);
        open();
    }

    private void open() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = buildConnection(ip, port);
                    if (socket == null) {
                        Log.d(AS, "socket 创建失败");
                        Message message = mHandler.obtainMessage();
                        message.what = 0x04;
                        message.obj = "delConnect";
                        mHandler.sendMessage(message);
                        /*Looper.prepare();
                        Toast.makeText(context, "连接失败", Toast.LENGTH_SHORT).show();
                        Looper.loop();*/
                        return;
                    }
                    outputStream = socket.getOutputStream();
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "ISO-8859-15"));
                    Log.d(AS, "socket: " + socket);
                    if (socket != null) {
                        setSendSocketThread();
                        setListenerSocketThread();
                        Log.d(AS, "创建socket成功");
                        Message message = mHandler.obtainMessage();
                        message.what = 0x04;
                        message.obj = "connected";
                        mHandler.sendMessage(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    close();
                    Looper.prepare();
                    Toast.makeText(context, "创建流失败 " + e.toString(), Toast.LENGTH_SHORT).show();
                    Looper.loop();
                }
            }
        }).start();
    }

    private static Socket buildConnection(String ip, int port) {
        if (ip == null || port <= 0) {
            return null;
        }
        Socket sock = new Socket();
        try {
            sock.connect(new InetSocketAddress(ip, port), TMO_CONNECTTION);
            if (!sock.isConnected()) {
                sock.close();
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            try {
                sock.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return null;
        }

        return sock;
    }

    private void close() {
        try {
            if (dormancyManager != null) {
                dormancyManager.unLock();
            }
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
            if (in != null) {
                in.close();
                in = null;
            }
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void setListenerSocketThread() {
        Log.d(AS, "设置线程工作");
        listenerSocketThread = null;
        listenerSocketThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int len;
                char[] chars = new char[4096];
                String str;
                while (sign) {
                    try {
                        if (socket != null && !socket.isClosed() && socket.isConnected() && !socket.isInputShutdown())
                            while (((len = in.read(chars)) != -1)) {

                                //str = showResult16Str(getInfoBuff(chars,len).getBytes());

                                if (ConnectInternetManage.mAccept) {
                                    str = bytesToHexString(getInfoBuff(chars, len).getBytes("ISO-8859-15"));
                                } else {
                                    str = new String(getInfoBuff(chars, len).getBytes("ISO-8859-15"),
                                            "GB18030");
                                }
                                Log.d(AS, "收到数据" + str);
                                Message message = mHandler.obtainMessage();
                                message.what = 0x00;
                                message.obj = str;
                                mHandler.sendMessage(message);
                            }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (e.toString().equals("java.net.SocketException: Connection reset")) {
                            Log.e(AS, "连接断开");

                        }

                        if (e.toString().equals("java.net.SocketException: Software caused connection abort")) {
                            Log.e(AS, "可能是切换网络");
                            Message message = mHandler.obtainMessage();
                            message.what = 0x04;
                            message.obj = "dis";
                            mHandler.sendMessage(message);
                            mManageHandler.sendEmptyMessageDelayed(0x01, 2000);
                        }
                        close();
                        /*Looper.prepare();
                        Toast.makeText(context, "接收线程出错 "+e.toString(), Toast.LENGTH_LONG).show();
                        Looper.loop();*/
                    }
                }
            }
        });
        listenerSocketThread.start();
    }

    private void setSendSocketThread() {
        Log.d(AS, "设置线程工作");
        sendSocketThread = null;
        sendSocketThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (sign) {

                    try {
                        if (socket != null && !socket.isClosed() &&
                                socket.isConnected() && !socket.isInputShutdown()) {
                            // 如果消息集合有东西，并且发送线程在工作。
                            if (list.size() > 0 && !socket.isOutputShutdown()) {
                                //out.println(replace(list.get(0)));
                                /*if (ConnectWifi.SendHex) {
                                    String str = str2HexStr(list.get(0));
                                    outputStream.write(str.getBytes("GBK"));
                                    list.remove(0);
                                } else {*/
                                String str;
                                if (ConnectInternetManage.mSendHex) {
                                    outputStream.write(hexString2ByteArray(list.get(0)));
                                } else {
                                    str = list.get(0);
                                    outputStream.write(str.getBytes("GBK"));//这个让25模块可以接收中文
                                }
                                outputStream.flush();
                                list.remove(0);
                                //}
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        //发送线程出错
                        sign = false;
                        Message message = mHandler.obtainMessage();
                        message.what = 0x04;
                        message.obj = "dis";
                        mHandler.sendMessage(message);
                        mManageHandler.sendEmptyMessageDelayed(0x01, 2000);
                        /*Looper.prepare();
                        Toast.makeText(context, "发送线程出错 "+e.toString(), Toast.LENGTH_LONG).show();
                        Looper.loop();*/
                    }

                }
            }
        });
        sendSocketThread.start();
    }

    //发送数据
    void send(String s) {
        list.add(s);
    }

    void showdown() {
        Log.d(AS, "断开服务器");
        sign = false;
        close();
        sendSocketThread.interrupt();
        listenerSocketThread.interrupt();
    }

    public boolean getState() {
        if (socket == null)
            return false;
        if (socket.isClosed())
            return false;
        return sign;
    }

    //解码
    private String getInfoBuff(char[] paramArrayOfChar, int paramInt) {
        synchronized (this) {
            char[] arrayOfChar = new char[paramInt];
            int b = 0;
            try {
                for (b = 0; ; b++) {
                    if (b >= paramInt) {
                        Log.d("TAG", "数组长度: " + new String(arrayOfChar).length());
                        //getDatas += new String(arrayOfChar).length();
                        Message message = mManageHandler.obtainMessage();
                        message.what = 0x02;

                        message.obj = new String(arrayOfChar).length();
                        mManageHandler.sendMessage(message);
                        return new String(arrayOfChar);
                    }
                    arrayOfChar[b] = paramArrayOfChar[b];
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("TAG", "数组长度: " + new String(arrayOfChar).length());
                Log.e("TAG", "b= " + b + "  paramInt= " + paramInt + "   paramArrayOfChar= " + paramArrayOfChar.length);
                return new String(arrayOfChar);
            }
        }
    }


}
