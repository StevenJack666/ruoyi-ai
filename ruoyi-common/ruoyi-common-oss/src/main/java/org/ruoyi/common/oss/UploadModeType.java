package org.ruoyi.common.oss;

import lombok.Getter;

/**
 * 模型分类
 *
 * @author ageerle@163.com
 * @date 2025-12-14
 */
@Getter
public enum UploadModeType {
    QIAN_WEN("qianwen", "通义千问"),
    DEFAULT("default","默认");

    private final String code;
    private final String description;

    UploadModeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

}
