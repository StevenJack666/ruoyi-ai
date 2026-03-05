package org.ruoyi.system.service.impl.provider;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.oss.constant.OssConstant;
import org.ruoyi.common.oss.UploadModeType;
import org.ruoyi.system.service.AbstractUploadService;
import org.ruoyi.system.utils.QwenFileUploadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

/**
 * qianWen 上传文件服务
 *
 * @author Zengxb
 * @date 2026/02/28
 */
@Slf4j
@Service
public class QianWenUploadServiceImpl extends AbstractUploadService {

    @Autowired
    private ConfigService configService;

    // 默认密钥
    private static String API_KEY;

    // 默认api路径地址
    private static String API_HOST;

    @Override
    public String uploadFile(File file) throws IOException {
        // 获取配置
        initConfig();
        // 使用工具类上传文件到阿里云
        String fileId = QwenFileUploadUtils.uploadFile(file, API_HOST, API_KEY);
        if (StringUtils.isEmpty(fileId)) {
            throw new ServiceException("文件上传失败，未获取到fileId");
        }
        return OssConstant.FILE_ID_PREFIX + fileId;
    }

    /**
     * 初始化配置并返回API密钥和主机
     */
    private void initConfig() {
        String apiKey = configService.getConfigValue(OssConstant.CONFIG_NAME_KEY);
        if (StringUtils.isEmpty(apiKey)) {
            throw new ServiceException("请先配置Qwen上传文件相关API_KEY");
        }
        API_KEY = apiKey;
        String apiHost = configService.getConfigValue(OssConstant.CONFIG_NAME_URL);
        if (StringUtils.isEmpty(apiHost)) {
            throw new ServiceException("请先配置Qwen上传文件相关API_HOST");
        }
        API_HOST = apiHost;
    }

    @Override
    public String getProviderName() {
        return UploadModeType.QIAN_WEN.getCode();
    }
}
