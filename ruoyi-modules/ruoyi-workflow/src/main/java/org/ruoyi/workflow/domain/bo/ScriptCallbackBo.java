package org.ruoyi.workflow.domain.bo;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 剧本执行回调请求对象
 *
 * @author ruoyi
 */
@Data
public class ScriptCallbackBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否执行成功
     */
    private Boolean success;

    /**
     * 执行结果，供后续节点使用
     */
    private Object output;

    /**
     * 执行轨迹，结构由 Python 侧定义
     */
    private List<Map<String, Object>> trace;

    /**
     * 失败原因
     */
    private String error;

    /**
     * 执行耗时（毫秒）
     */
    private Long costMs;

    /**
     * 脚本内容哈希，由执行器回报；Java 侧不可读脚本文件时用于补齐留痕
     */
    private String scriptSha256;
}
