package org.ruoyi.service.chat.support;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轻量 token 估算（不引入 tokenizer 依赖）。
 * <p>
 * 经验值：中文/全角字符 ≈ 1 token/字，英文数字等 ≈ 3~4 字符/token。
 * 估算结果故意偏大（安全系数 1.10），用于在调用前判断是否会超模型窗口。
 */
public final class TokenEstimator {

    /** 中文/全角字符权重 */
    private static final double CJK_WEIGHT = 1.0;
    /** 其他字符权重（约 4 字符 = 1 token） */
    private static final double OTHER_WEIGHT = 0.28;
    /** 安全系数 */
    private static final double SAFETY = 1.10;
    /** 单张图片的粗略 token 估算（qwen-vl 高分辨率量级，仅用于日志参考） */
    private static final int IMAGE_TOKEN_ESTIMATE = 1024;

    private TokenEstimator() {}

    /** 估算纯文本 token */
    public static int estimate(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        double tokens = 0;
        for (int i = 0; i < text.length(); i++) {
            tokens += isCjk(text.charAt(i)) ? CJK_WEIGHT : OTHER_WEIGHT;
        }
        return (int) Math.ceil(tokens * SAFETY);
    }

    /** 估算一组消息（含图文消息、工具调用与工具返回） */
    public static int estimateMessages(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (ChatMessage message : messages) {
            total += estimateMessage(message);
        }
        return total;
    }

    /** 估算工具定义（工具 schema 也占 input token，MCP 工具很容易吃掉几千 token） */
    public static int estimateToolSpecifications(List<ToolSpecification> specs) {
        if (specs == null || specs.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (ToolSpecification spec : specs) {
            total += estimate(spec.name());
            total += estimate(spec.description());
            if (spec.parameters() != null) {
                total += estimate(spec.parameters().toString());
            }
        }
        return total;
    }

    /** 统计图片数量 */
    public static int countImages(List<ChatMessage> messages) {
        if (messages == null) {
            return 0;
        }
        int count = 0;
        for (ChatMessage message : messages) {
            if (message instanceof UserMessage userMessage) {
                for (Content content : userMessage.contents()) {
                    if (content instanceof ImageContent) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /** 一次性估算一个 ChatRequest（文本 + 工具 + 图片） */
    public static RequestStat estimateRequest(ChatRequest request) {
        int textTokens = estimateMessages(request.messages());
        int toolTokens = estimateToolSpecifications(request.toolSpecifications());
        int images = countImages(request.messages());
        return new RequestStat(
            textTokens,
            toolTokens,
            images,
            textTokens + toolTokens + images * IMAGE_TOKEN_ESTIMATE
        );
    }

    private static int estimateMessage(ChatMessage message) {
        AtomicInteger tokens = new AtomicInteger();
        if (message instanceof SystemMessage systemMessage) {
            tokens.addAndGet(estimate(systemMessage.text()));
        } else if (message instanceof UserMessage userMessage) {
            for (Content content : userMessage.contents()) {
                if (content instanceof TextContent textContent) {
                    tokens.addAndGet(estimate(textContent.text()));
                } else if (content instanceof ImageContent) {
                    tokens.addAndGet(IMAGE_TOKEN_ESTIMATE);
                }
            }
        } else if (message instanceof AiMessage aiMessage) {
            tokens.addAndGet(estimate(aiMessage.text()));
            if (aiMessage.hasToolExecutionRequests()) {
                aiMessage.toolExecutionRequests().forEach(req ->
                    tokens.addAndGet(estimate(req.name()) + estimate(req.arguments()))
                );
            }
        } else if (message instanceof ToolExecutionResultMessage toolResult) {
            // 工具返回往往是隐形大户（文件内容、SQL 结果、网页正文）
            tokens.addAndGet(estimate(toolResult.text()));
        }
        return tokens.get();
    }

    private static boolean isCjk(char c) {
        return (c >= 0x4E00 && c <= 0x9FFF)
            || (c >= 0x3400 && c <= 0x4DBF)
            || (c >= 0x3000 && c <= 0x303F)
            || (c >= 0xFF00 && c <= 0xFFEF);
    }

    /** 请求估算结果 */
    public record RequestStat(int textTokens, int toolTokens, int images, int totalTokens) {}
}
