package com.wifi.lib.command.profile;

/**
 * 验光业务固定业务编号。
 * <p>
 * 这里只定义 App 侧稳定使用的编号常量，不定义真实协议字符串。
 */
public final class OptometryCommandCodes {
    public static final String CODE_QUERY_MODULE_INFO = "s100101";
    public static final String CODE_REPORT_MODULE_INFO = "r100102";
    public static final String CODE_SWITCH_AUTO_MODE = "s110201";
    public static final String CODE_SWITCH_MANUAL_MODE = "s110202";
    public static final String CODE_CONFIRM_AUTO_MODE = "r110203";
    public static final String CODE_CONFIRM_MANUAL_MODE = "r110204";
    public static final String CODE_START_OPTOMETRY = "s120101";
    public static final String CODE_STOP_OPTOMETRY = "s120102";
    public static final String CODE_CONFIRM_START_OPTOMETRY = "r120103";
    public static final String CODE_CONFIRM_STOP_OPTOMETRY = "r120104";
    public static final String CODE_REPORT_DEVICE_STATUS = "r120201";
    public static final String CODE_REPORT_OPTOMETRY_RESULT = "r120202";

    private OptometryCommandCodes() {
    }
}
