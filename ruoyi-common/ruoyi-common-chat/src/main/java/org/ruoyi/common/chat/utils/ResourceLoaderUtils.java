package org.ruoyi.common.chat.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.ruoyi.common.chat.factory.ResourceLoaderFactory;
import org.ruoyi.common.chat.knowledge.ResourceLoader;
import org.ruoyi.common.core.utils.SpringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件提取工具类
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResourceLoaderUtils {

    // 获取配置文件中的上传路径
    private static String UPLOAD_PATH;

    // 静态初始化块加载配置
    static {
        try {
            // 获取 Spring 环境变量
            Environment env = SpringUtils.getBean(Environment.class);
            UPLOAD_PATH = env.getProperty("sys.upload.path");
            if (UPLOAD_PATH == null || UPLOAD_PATH.isEmpty()) {
                throw new RuntimeException("未配置 sys.upload.path 配置项");
            }
        } catch (Exception e) {
            throw new RuntimeException("加载文件上传路径失败：" + e.getMessage(), e);
        }
    }
    /**
     * 读取内容
     * @param filePath 文件地址
     * @return 文件内容
     */
    public static String load(String filePath) {
        try {
            // 根据文件地址找到文件后缀
            String suffix = filePath.substring(filePath.lastIndexOf(".") + 1);
            Path path = Paths.get(UPLOAD_PATH, filePath);
            // 获取资源读取解析器
            ResourceLoaderFactory resourceLoaderFactory = SpringUtils.getBean(ResourceLoaderFactory.class);
            // 根据文件类型获取对应读取器
            ResourceLoader resourceLoader = resourceLoaderFactory.getLoaderByFileType(suffix);
            // 解析文档内容
            InputStream inputStream = new FileInputStream(path.toFile());
            return resourceLoader.getContent(inputStream);
        } catch (Exception e){
            throw new SecurityException("文件读取异常：" + e.getMessage());
        }
    }
}
