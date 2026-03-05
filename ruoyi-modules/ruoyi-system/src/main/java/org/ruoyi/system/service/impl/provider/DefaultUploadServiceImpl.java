package org.ruoyi.system.service.impl.provider;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.oss.UploadModeType;
import org.ruoyi.system.service.AbstractUploadService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

/**
 * 默认上传文件服务
 *
 * @author Zengxb
 * @date 2026/02/28
 */
@Slf4j
@Service
public class DefaultUploadServiceImpl extends AbstractUploadService {

    @Override
    public String uploadFile(File file) throws IOException {
        return file.getAbsolutePath();
    }

    @Override
    public String getProviderName() {
        return UploadModeType.DEFAULT.getCode();
    }
}
