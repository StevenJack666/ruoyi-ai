package org.ruoyi.common.chat.domain.vo.file;

import cn.idev.excel.annotation.ExcelIgnore;
import lombok.Data;

/**
 * 文件对象
 */
@Data
public class FileInfoVo {
    /**
     * 文件大小
     */
    @ExcelIgnore
    private String fileSize;

    /**
     * 文件名称
     */
    @ExcelIgnore
    private String fileName;

    /**
     * 文件类型
     */
    @ExcelIgnore
    private String fileType;

    /**
     * 文件地址
     */
    @ExcelIgnore
    private String ossUrl;
}
