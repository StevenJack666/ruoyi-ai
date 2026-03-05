package org.ruoyi.common.oss.service;

import org.ruoyi.common.oss.domain.vo.UploadVo;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * 公共上传文件接口
 */
public interface IUploadService {

    /**
     * 模板方法（对外暴露）
     */
    UploadVo upload(MultipartFile[] file);

    /**
     * 上传文件
     */
    String uploadFile(File file) throws IOException;

    /**
     * 获取服务提供商名称
     */
    String getProviderName();
}
