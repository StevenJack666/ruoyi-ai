package org.ruoyi.domain.dto.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ruoyi.domain.entity.mcp.McpTool;

import java.util.List;

/**
 * MCP 工具列表返回结果
 *
 * @author ruoyi team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolListResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 工具列表
     */
    private List<McpTool> data;

    /**
     * 消息状态码
     */
    private int code;

    /**
     * 总数
     */
    private int total;

    public static McpToolListResult of(List<McpTool> data) {
        return McpToolListResult.builder()
            .success(true)
            .code(200)
            .data(data)
            .total(data != null ? data.size() : 0)
            .build();
    }
}
