package com.wifi.lib.utils;

import java.util.List;

public class ValidUtil {
    /**
     * 判断list是否为空
     */
    public static boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    /**
     * 判断list是否不为空
     */
    public static boolean isNotEmpty(List<?> list) {
        return !isEmpty(list);
    }

    public static boolean isEmpty(String text) {
        return text == null || text.isEmpty();
    }

    public static boolean isNotEmpty(String text) {
        return !isEmpty(text);
    }
}
