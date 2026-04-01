package org.ruoyi.common.chat.domain.dto.request;

import lombok.Data;

import java.util.List;

/**
 * 上传文件信息体
 */
@Data
public class FileRunner {

    /**
     * 文件主键列表
     */
    private List<Long> ossIds;
}
