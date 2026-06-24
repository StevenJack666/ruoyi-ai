package org.ruoyi.common.oss.enums;

import lombok.Getter;

/**
 * 上传模式分类
 *
 * @author ageerle@163.com
 * @date 2025-12-14
 */
@Getter
public enum UploadModeType {

    DEFAULT("default","默认");

    private final String code;
    private final String description;

    UploadModeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

}
