package org.ruoyi.observability;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.domain.result.InterceptResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OrderMilkTeaStrategy extends AbstractToolInterceptStrategy {

    @Override
    public boolean matches(String toolName) {
        return "orderMilkTea".equals(toolName); // 匹配你的工具名
    }

    @Override
    public InterceptResult onBeforeExecute(String toolName, String argumentsJson, Long userId) {
        log.info("🔥 触发点奶茶交互拦截");

        // 1. 构造生成式 UI 需要的 JSON 数据（前端根据这个 JSON 渲染卡片） TODO 调用接口获取数据
        Map<String, Object> uiData = Map.of(
            "type", "milk_tea_card",
            "title", "为您推荐的奶茶",
            "items", List.of(
                Map.of("id", "1", "name", "珍珠奶茶", "price", "15元", "img", "url1"),
                Map.of("id", "2", "name", "芋泥波波", "price", "18元", "img", "url2")
            )
        );

        // 3. 推送 SSE 事件给前端，前端收到后渲染卡片
        pushMcpEvent("render_card", "pending", "pending", userId, uiData);

        // 4. 阻断原始 MCP 工具的执行，并告诉大模型当前状态
        String messageToLlm = "我已经为您展示了奶茶菜单卡片，请等待用户在界面上选择商品并点击确认。";
        return new InterceptResult(false, messageToLlm, uiData);
    }
}
