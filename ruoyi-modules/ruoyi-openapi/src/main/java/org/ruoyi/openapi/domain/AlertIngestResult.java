package org.ruoyi.openapi.domain;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 告警接入结果
 * <p>
 * 无论命中与否都返回 code=200，用 {@code matched} / {@code triggered} 表达处理结果，
 * 避免告警源因为非 200 响应无限重投。
 *
 * @author ruoyi
 */
@Data
public class AlertIngestResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否命中规则
     */
    private boolean matched;

    /**
     * 是否成功创建（或复用）流程实例
     */
    private boolean triggered;

    /**
     * 未命中的原因 / 触发失败的原因
     */
    private String reason;

    /**
     * 告警事件留痕 id
     */
    private Long eventId;

    /**
     * 命中的规则
     */
    private Long ruleId;

    private String ruleName;

    /**
     * 规则指向的流程编码
     */
    private String flowCode;

    /**
     * 流程业务id
     */
    private String businessId;

    private Long processInstanceId;

    private Long taskId;

    public static AlertIngestResult ignored(String reason) {
        AlertIngestResult result = new AlertIngestResult();
        result.setMatched(false);
        result.setTriggered(false);
        result.setReason(reason);
        return result;
    }
}
