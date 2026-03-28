package com.wifi.lib.command.profile;

import androidx.annotation.NonNull;

import com.wifi.lib.command.CommandCatalog;
import com.wifi.lib.command.CommandReservation;

/**
 * 验光业务的预留编码目录构建结果。
 * <p>
 * 业务编号常量定义在 {@link OptometryCommandCodes}，
 * 真实命令内容仍然由运行时加载的 CSV 编码表提供映射。
 */
public final class OptometryCommandCatalogs {
    private static final CommandCatalog CATALOG = new CommandCatalog.Builder()
            .addReservation(
                    OptometryCommandCodes.CODE_QUERY_MODULE_INFO,
                    "系统",
                    "握手",
                    "查询模块信息",
                    "s=发送，10=系统，01=握手，01=查询模块信息",
                    "App 主动查询模块基础信息"
            )
            .addReservation(
                    OptometryCommandCodes.CODE_REPORT_MODULE_INFO,
                    "系统",
                    "握手",
                    "模块信息上报",
                    "r=接收，10=系统，01=握手，02=模块信息上报",
                    "模块返回基础信息给 App"
            )
            .addReservation(
                    OptometryCommandCodes.CODE_SWITCH_AUTO_MODE,
                    "验光流程",
                    "模式控制",
                    "切换自动验光",
                    "s=发送，11=验光流程，02=模式控制，01=切换自动验光",
                    "App 请求模块切换到自动验光模式"
            )
            .addReservation(
                    OptometryCommandCodes.CODE_SWITCH_MANUAL_MODE,
                    "验光流程",
                    "模式控制",
                    "切换手动验光",
                    "s=发送，11=验光流程，02=模式控制，02=切换手动验光",
                    "App 请求模块切换到手动验光模式"
            )
            .addReservation(
                    OptometryCommandCodes.CODE_CONFIRM_AUTO_MODE,
                    "验光流程",
                    "模式控制",
                    "自动模式切换确认",
                    "r=接收，11=验光流程，02=模式控制，03=自动模式切换确认",
                    "模块向 App 确认已切换到自动验光模式"
            )
            .addReservation(
                    OptometryCommandCodes.CODE_CONFIRM_MANUAL_MODE,
                    "验光流程",
                    "模式控制",
                    "手动模式切换确认",
                    "r=接收，11=验光流程，02=模式控制，04=手动模式切换确认",
                    "模块向 App 确认已切换到手动验光模式"
            )
            .addReservation(
                    OptometryCommandCodes.CODE_START_OPTOMETRY,
                    "验光流程",
                    "流程控制",
                    "开始验光",
                    "s=发送，12=验光流程，01=流程控制，01=开始验光",
                    "App 请求模块开始一次验光"
            )
            .addReservation(
                    OptometryCommandCodes.CODE_STOP_OPTOMETRY,
                    "验光流程",
                    "流程控制",
                    "停止验光",
                    "s=发送，12=验光流程，01=流程控制，02=停止验光",
                    "App 请求模块停止当前验光"
            )
            .addReservation(
                    OptometryCommandCodes.CODE_CONFIRM_START_OPTOMETRY,
                    "验光流程",
                    "流程控制",
                    "开始验光确认",
                    "r=接收，12=验光流程，01=流程控制，03=开始验光确认",
                    "模块向 App 确认验光流程已启动"
            )
            .addReservation(
                    OptometryCommandCodes.CODE_CONFIRM_STOP_OPTOMETRY,
                    "验光流程",
                    "流程控制",
                    "停止验光确认",
                    "r=接收，12=验光流程，01=流程控制，04=停止验光确认",
                    "模块向 App 确认验光流程已停止"
            )
            .addReservation(
                    OptometryCommandCodes.CODE_REPORT_DEVICE_STATUS,
                    "验光流程",
                    "状态上报",
                    "设备状态上报",
                    "r=接收，12=验光流程，02=状态上报，01=设备状态上报",
                    "模块向 App 上报运行状态"
            )
            .addReservation(
                    OptometryCommandCodes.CODE_REPORT_OPTOMETRY_RESULT,
                    "验光流程",
                    "结果上报",
                    "验光结果上报",
                    "r=接收，12=验光流程，02=结果上报，02=验光结果上报",
                    "模块向 App 上报验光结果"
            )
            .build();

    private OptometryCommandCatalogs() {
    }

    @NonNull
    public static CommandCatalog getCatalog() {
        return CATALOG;
    }

    @NonNull
    public static CommandReservation requireReservation(@NonNull String code) {
        CommandReservation reservation = CATALOG.findByCode(code);
        if (reservation == null) {
            throw new IllegalArgumentException("未找到预留编码: " + code);
        }
        return reservation;
    }
}
