package org.ruoyi.workflow.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.workflow.common.ConditionalOnEnable;
import org.springframework.stereotype.Component;

/**
 * 剧本脚本路径解析
 * <p>
 * 脚本路径完全由 {@code t_playbook_script.script_path} 决定，这里只做规范化，
 * 不加目录白名单、不加后缀限制，避免把可用范围写死在配置里。
 * <p>
 * 注意：Java 服务与 Python 执行服务可能不在同一台机器上，脚本文件不一定可读，
 * 因此这里也不做存在性校验，存在性由执行侧负责。
 * 哈希计算采用"本机可读则计算，否则留空"，留空时由执行器回调补齐。
 *
 * @author ruoyi
 */
@ConditionalOnEnable
@Slf4j
@Component
public class ScriptPathResolver {

    /**
     * 返回规范化后的脚本路径
     *
     * @param rawPath 配置中的脚本路径
     * @return 规范化路径
     */
    public Path resolve(String rawPath) {
        if (StringUtils.isBlank(rawPath)) {
            throw new ServiceException("剧本脚本路径不能为空");
        }
        return Path.of(rawPath).toAbsolutePath().normalize();
    }

    /**
     * 计算脚本内容哈希，用于运行留痕
     * <p>
     * 文件在本机不可读时返回 null（分布式部署下 Java 侧可能未挂载脚本目录），
     * 此时由 Python 执行器在回调中回报实际哈希。
     *
     * @param path 已校验的脚本路径
     * @return sha256 十六进制串，或 null
     */
    public String trySha256(Path path) {
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            log.warn(
                "[剧本派发] 脚本文件在本机不可读，跳过哈希计算，将由执行器回报: {}",
                path
            );
            return null;
        }
        try (InputStream in = Files.newInputStream(path)) {
            return DigestUtil.sha256Hex(in);
        } catch (IOException e) {
            log.warn(
                "[剧本派发] 计算脚本哈希失败，将由执行器回报: {}",
                path,
                e
            );
            return null;
        }
    }
}
