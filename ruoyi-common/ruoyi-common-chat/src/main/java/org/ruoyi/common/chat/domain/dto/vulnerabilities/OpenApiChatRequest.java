package org.ruoyi.common.chat.domain.dto.vulnerabilities;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Map;

/**
 * 提供于外部的对话请求对象
 *
 * @author zengxb
 */
@Data
public class OpenApiChatRequest {

    // 【新增】业务场景标识：例如 "INTEL_CLASSIFY", "RISK_JUDGE"
    @NotEmpty(message = "场景不能为空")
    private String scene;

    // 输入的情报内容
    @NotEmpty(message = "内容不能为空")
    private String content;

    // 其他上下文参数（可选）
    private Map<String, Object> context;
}
