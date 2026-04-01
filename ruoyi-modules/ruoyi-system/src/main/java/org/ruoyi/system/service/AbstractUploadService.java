package org.ruoyi.system.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.oss.domain.vo.UploadVo;
import org.ruoyi.common.oss.service.IUploadService;
import org.ruoyi.common.oss.domain.SysOss;
import org.ruoyi.common.oss.domain.vo.SysOssUploadVo;
import org.ruoyi.system.mapper.SysOssMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
public abstract class AbstractUploadService implements IUploadService {

    // 上传文件服务器地址
    @Value("${sys.upload.path}")
    private String UPLOAD_PATH;

    @Resource
    private SysOssMapper baseMapper;

    public UploadVo upload(MultipartFile[] files) {
        UploadVo uploadVo = new UploadVo();
        for (MultipartFile file : files) {
            String originalName = file.getOriginalFilename();
            if (StringUtils.isEmpty(originalName)){
                throw new ServiceException("文件名不能为空");
            }
            int lastDotIndex = originalName != null ? originalName.lastIndexOf(".") : -1;
            String suffix = lastDotIndex > 0 ? originalName.substring(lastDotIndex + 1) : "";
            try {
                // 确保上传目录存在
                Path uploadDir = Paths.get(UPLOAD_PATH);
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }
                // 随机UUID
                UUID randomUUID = UUID.randomUUID();
                // 文件前缀
                String prefix = randomUUID.toString();
                // 随机文件名（避免文件名冲突）
                String uniqueFileName = randomUUID + "." + suffix;
                // 生成上传文件名
                Path targetPath = uploadDir.resolve(uniqueFileName);
                // 直接保存文件
                File pathFile = targetPath.toFile();
                file.transferTo(pathFile);
                // 调用子类实现方法
                String url = uploadFile(pathFile);
                // 获取文件相对路径地址
                String filePath = uploadDir.relativize(targetPath).toString();
                // 保存文件信息到数据库
                uploadVo.getUploadVos().add(buildEntity(filePath, url, suffix, prefix, originalName));
            } catch (IOException e) {
                throw new ServiceException("文件上传失败: " + e.getMessage());
            }
        }
        return uploadVo;
    }

    @NotNull
    private SysOssUploadVo buildEntity(String filePath, String url, String suffix, String prefix, String originalName) {
        SysOss oss = new SysOss();
        oss.setUrl(url);
        oss.setExt1(filePath);
        oss.setFileSuffix(suffix);
        oss.setFileName(prefix);
        oss.setOriginalName(originalName);
        oss.setService(getProviderName());
        baseMapper.insert(oss);
        SysOssUploadVo uploadVo = new SysOssUploadVo();
        uploadVo.setUrl(url);
        uploadVo.setFileName(prefix);
        uploadVo.setOssId(oss.getOssId().toString());
        uploadVo.setFilePath(filePath);
        return uploadVo;
    }
}
