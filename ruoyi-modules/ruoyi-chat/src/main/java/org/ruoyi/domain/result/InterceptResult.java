package org.ruoyi.domain.result;

import lombok.AllArgsConstructor;
import lombok.Data;

// 定义拦截结果对象
@Data
@AllArgsConstructor
public class InterceptResult {
    private boolean shouldContinue; // 是否继续执行原始工具
    private String messageToLlm;    // 返回给大模型的提示语
    private Object uiDataJson;      // 推送给前端的 UI JSON 数据
}
