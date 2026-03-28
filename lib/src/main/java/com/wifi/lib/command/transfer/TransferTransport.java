package com.wifi.lib.command.transfer;

import androidx.annotation.NonNull;

/**
 * 文件传输层的最小发送抽象。
 * <p>
 * 当前默认以文本帧形式发送，便于直接复用现有 TCP 文本链路。
 */
public interface TransferTransport {
    void send(@NonNull String frame);
}
