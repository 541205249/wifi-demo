package com.wifi.lib;

import java.io.UnsupportedEncodingException;

public class ToolClass {

    /**
     * 将16进制字符串转换为byte[]
     */
    public static byte[] hexString2ByteArray(String bs) {
        if (bs == null) {
            return null;
        }
        bs = bs.replace(" ", "");
        int bsLength = bs.length();
        if (bsLength % 2 != 0) {
            bs = "0" + bs;
            bsLength = bs.length();
        }
        byte[] cs = new byte[bsLength / 2];
        String st;
        for (int i = 0; i < bsLength; i = i + 2) {
            st = bs.substring(i, i + 2);
            cs[i / 2] = (byte) Integer.parseInt(st, 16);
        }
        return cs;
    }

    //byte数组转String
    public static String bytesToHexString(byte[] bArray) {
        StringBuffer sb = new StringBuffer(bArray.length);
        String sTemp;
        for (int i = 0; i < bArray.length; i++) {
            sTemp = Integer.toHexString(0xFF & bArray[i]);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase());
        }
        int length = sb.length();
        if (length == 1 || length == 0) {
            return sb.toString();
        }
        if (length % 2 == 1) {
            sb.insert(length - 1, " ");
            length = length - 1;
        }
        for (int i = length; i > 0; i = i - 2) {
            sb.insert(i, " ");
        }
        return sb.toString();
    }

    public static String hexStringToString(String s) {
        if (s == null || s.equals("")) {
            return null;
        }
        s = s.replace(" ", "");
        byte[] baKeyword = new byte[s.length() / 2];
        for (int i = 0; i < baKeyword.length; i++) {
            try {
                baKeyword[i] = (byte) (0xff & Integer.parseInt(
                        s.substring(i * 2, i * 2 + 2), 16));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            s = new String(baKeyword, "gbk");
            new String();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return s;
    }


    //String字符串的互转
    public static String changeHexString(boolean isChangeHex, String string) {
        if (string == null || string.isEmpty()) {
            return "";
        }
        if (isChangeHex) {
            try {
                return bytesToHexString(string.getBytes("GBK"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return "";
        } else {
            return hexStringToString(string);
        }
    }


}
