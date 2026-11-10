package org.ruoyi.enums;

import lombok.Getter;

/**
 * 会话上传文档解析进度
 *
 * @author ageerle@163.com
 */
@Getter
public enum ParseStatus {

    /** 解析中 */
    PARSING(0, "解析中"),
    /** 已完成 */
    COMPLETED(1, "已完成"),
    /** 解析失败 */
    FAILED(2, "解析失败");

    private final int code;
    private final String desc;

    ParseStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ParseStatus ofCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ParseStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
