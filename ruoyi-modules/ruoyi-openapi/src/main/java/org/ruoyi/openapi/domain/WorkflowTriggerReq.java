package org.ruoyi.openapi.domain;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 剧本触发请求
 *
 * @author ruoyi
 */
@Data
public class WorkflowTriggerReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程编码
     */
    @NotBlank(message = "流程编码不能为空")
    private String flowCode;

    /**
     * 业务id（告警id），用于幂等
     */
    @NotBlank(message = "业务ID不能为空")
    private String businessId;

    /**
     * 流程变量
     */
    private Map<String, Object> variables;

}
