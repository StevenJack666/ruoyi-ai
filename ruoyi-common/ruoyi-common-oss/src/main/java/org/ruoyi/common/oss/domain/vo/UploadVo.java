package org.ruoyi.common.oss.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 上传对象信息
 */
@Data
public class UploadVo {
    /**
     * 上传对象列表
     */
    private List<SysOssUploadVo> uploadVos = new ArrayList<>();
}
