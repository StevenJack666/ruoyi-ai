package org.ruoyi.workflow.common.enums;

import lombok.Getter;

/**
 * 脚本派发状态
 *
 * @author ruoyi
 */
@Getter
public enum ScriptOutboxStatus {

    /**
     * 待派发
     */
    PENDING(0, "待派发"),

    /**
     * 已派发，等待回调
     */
    DISPATCHED(1, "已派发"),

    /**
     * 已回调
     */
    CALLBACK(2, "已回调"),

    /**
     * 派发失败
     */
    FAILED(3, "失败"),

    /**
     * 执行超时
     */
    TIMEOUT(4, "超时"),

    /**
     * 已取消
     */
    CANCELED(5, "已取消");

    private final Integer code;

    private final String desc;

    ScriptOutboxStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
