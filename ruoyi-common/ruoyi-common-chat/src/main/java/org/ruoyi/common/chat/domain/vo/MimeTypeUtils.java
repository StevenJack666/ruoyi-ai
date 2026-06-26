package org.ruoyi.common.chat.domain.vo;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MimeTypeUtils {

    // 使用 ConcurrentHashMap 保证线程安全，且支持运行时动态扩展
    private static final Map<String, String> MIME_TYPE_MAP = new ConcurrentHashMap<>();

    static {
        // === 文档类 ===
        MIME_TYPE_MAP.put("pdf", "application/pdf");
        MIME_TYPE_MAP.put("doc", "application/msword");
        MIME_TYPE_MAP.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        MIME_TYPE_MAP.put("xls", "application/vnd.ms-excel");
        MIME_TYPE_MAP.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        MIME_TYPE_MAP.put("ppt", "application/vnd.ms-powerpoint");
        MIME_TYPE_MAP.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        MIME_TYPE_MAP.put("txt", "text/plain");
        MIME_TYPE_MAP.put("csv", "text/csv");

        // === 图片类 ===
        MIME_TYPE_MAP.put("jpg", "image/jpeg");
        MIME_TYPE_MAP.put("jpeg", "image/jpeg");
        MIME_TYPE_MAP.put("png", "image/png");
        MIME_TYPE_MAP.put("gif", "image/gif");
        MIME_TYPE_MAP.put("bmp", "image/bmp");
        MIME_TYPE_MAP.put("svg", "image/svg+xml");
        MIME_TYPE_MAP.put("webp", "image/webp");

        // === 音视频类 ===
        MIME_TYPE_MAP.put("mp3", "audio/mpeg");
        MIME_TYPE_MAP.put("wav", "audio/wav");
        MIME_TYPE_MAP.put("mp4", "video/mp4");
        MIME_TYPE_MAP.put("avi", "video/x-msvideo");
        MIME_TYPE_MAP.put("mov", "video/quicktime");

        // === 压缩包类 ===
        MIME_TYPE_MAP.put("zip", "application/zip");
        MIME_TYPE_MAP.put("rar", "application/x-rar-compressed");
        MIME_TYPE_MAP.put("7z", "application/x-7z-compressed");
        MIME_TYPE_MAP.put("tar", "application/x-tar");

        // === 证书与特殊格式（包含你提到的 .pfx） ===
        MIME_TYPE_MAP.put("pfx", "application/x-pkcs12");
        MIME_TYPE_MAP.put("p12", "application/x-pkcs12");
        MIME_TYPE_MAP.put("cer", "application/x-x509-ca-cert");
        MIME_TYPE_MAP.put("pem", "application/x-pem-file");
        MIME_TYPE_MAP.put("key", "application/octet-stream");
    }

    /**
     * 根据文件名后缀获取准确的 MIME 类型
     * @param fileName 文件名 (例如: "certificate.pfx")
     * @return 对应的 MIME 类型，如果未找到则返回默认的 application/octet-stream
     */
    public static String getMimeType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "application/octet-stream";
        }
        // 提取后缀并转小写
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        // 从 Map 中获取，如果没有则返回二进制流默认值
        return MIME_TYPE_MAP.getOrDefault(extension, "application/octet-stream");
    }
}
