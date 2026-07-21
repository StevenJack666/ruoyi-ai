package org.ruoyi.common.oss.service;

import jakarta.servlet.http.HttpServletResponse;
import org.ruoyi.common.oss.domain.vo.UploadVo;
import org.springframework.web.multipart.MultipartFile;

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
     * 获取服务提供商名称
     */
    String getProviderName();


    /**
     * 下载文件
     */
    void download(String path, HttpServletResponse response) throws IOException;
}
