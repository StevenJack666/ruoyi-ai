package org.ruoyi.observability;

import dev.langchain4j.internal.Json;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.domain.result.InterceptResult;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Slf4j
@Component
public class FileEditStrategy extends AbstractToolInterceptStrategy {

    @Override
    public boolean matches(String toolName) {
        return "edit_file".equals(toolName); // 匹配你的大模型工具名
    }

    @Override
    public InterceptResult onBeforeExecute(String toolName, String argumentsJson, Long userId) {
        // 1. 解析大模型传来的参数
        Map<String, Object> args = Json.fromJson(argumentsJson, Map.class);
        String filePath = (String) args.get("path");
        String afterContent = (String) args.get("afterContent");

        try{
            // 2. 获取原文件内容，并在内存中模拟替换，生成 Diff 数据
            String beforeContent = Files.readString(Path.of(filePath));

            // 3. 组装前端需要的 Diff 预览数据
            Map<String, Object> diffData = Map.of(
                "type", "file_diff_preview",
                "path", filePath,
                "oldContent", beforeContent,
                "newContent", afterContent
            );

            // 4. 推送 SSE 预览卡片给前端
            pushMcpEvent("render_card", "pending","pending", userId, diffData);

            // 5. 🔥 阻断真实执行，返回提示语给大模型
            return new InterceptResult(false, "已生成文件修改预览，等待用户在UI上确认。", null);
        }catch (Exception e){
            throw new ServiceException("获取文件内容异常");
        }
    }
}
