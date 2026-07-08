package org.ruoyi.common.chat.domain.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
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

    // 输入的情报内容列表
    @NotEmpty(message = "内容列表不能为空")
    private List<String> contentList;

    // 其他上下文参数（可选）
    private Map<String, Object> context;
}
