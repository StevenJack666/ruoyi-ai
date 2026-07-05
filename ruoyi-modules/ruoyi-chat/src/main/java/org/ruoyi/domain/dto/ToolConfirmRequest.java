package org.ruoyi.domain.dto;

import lombok.Data;

/**
 * 工具调用确认请求 DTO
 *
 * @author ruoyi team
 */
@Data
public class ToolConfirmRequest {
    /** 确认ID */
    private String confirmId;
    /** true=同意调用, false=拒绝调用 */
    private boolean approved;
}
