package com.wifi.lib.command.ack;

public final class AckErrorCodes {
    public static final String INVALID_PARAM = "INVALID_PARAM";
    public static final String UNSUPPORTED = "UNSUPPORTED";
    public static final String BUSY = "BUSY";
    public static final String TIMEOUT = "TIMEOUT";
    public static final String NOT_READY = "NOT_READY";
    public static final String CRC_FAIL = "CRC_FAIL";
    public static final String SESSION_MISMATCH = "SESSION_MISMATCH";
    public static final String FRAME_DROPPED = "FRAME_DROPPED";
    public static final String INTERRUPTED = "INTERRUPTED";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private AckErrorCodes() {
    }
}
