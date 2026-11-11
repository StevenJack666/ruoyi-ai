package org.ruoyi.openapi.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 告警规则 t_alert_rule
 * <p>
 * 告警接入时按 priority 升序逐条匹配，第一条命中的规则决定走哪个流程。
 *
 * @author ruoyi
 */
@Data
@TableName("t_alert_rule")
public class AlertRule {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 匹配的告警来源，逗号分隔，支持 * 通配；为空表示不限
     */
    private String sourcePattern;

    /**
     * 匹配的告警类型，逗号分隔，支持 * 通配；为空表示不限
     */
    private String typePattern;

    /**
     * 最低告警等级（info/low/medium/high/critical），为空表示不限
     */
    private String severityMin;

    /**
     * 附加 SpEL 条件，只读上下文执行，为空表示不校验
     */
    private String conditionSpel;

    /**
     * 命中后触发的流程编码
     */
    private String flowCode;

    /**
     * 去重窗口（秒），0 表示不去重
     */
    private Integer dedupWindowSec;

    /**
     * 匹配优先级，值越小越先匹配
     */
    private Integer priority;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 备注
     */
    private String remark;
}
