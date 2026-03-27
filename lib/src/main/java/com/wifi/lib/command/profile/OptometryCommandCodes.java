package com.wifi.lib.command.profile;

/**
 * 验光业务固定业务编号。
 * <p>
 * 这里只定义 App 侧稳定使用的编号常量，不定义真实协议字符串。
 */
public final class OptometryCommandCodes {
    public static final String CODE_QUERY_MODULE_INFO = "100101";
    public static final String CODE_REPORT_MODULE_INFO = "100102";
    public static final String CODE_SWITCH_AUTO_MODE = "110201";
    public static final String CODE_SWITCH_MANUAL_MODE = "110202";
    public static final String CODE_CONFIRM_AUTO_MODE = "110203";
    public static final String CODE_CONFIRM_MANUAL_MODE = "110204";
    public static final String CODE_START_OPTOMETRY = "120101";
    public static final String CODE_STOP_OPTOMETRY = "120102";
    public static final String CODE_CONFIRM_START_OPTOMETRY = "120103";
    public static final String CODE_CONFIRM_STOP_OPTOMETRY = "120104";
    public static final String CODE_REPORT_DEVICE_STATUS = "120201";
    public static final String CODE_REPORT_OPTOMETRY_RESULT = "120202";

    private OptometryCommandCodes() {
    }
}
