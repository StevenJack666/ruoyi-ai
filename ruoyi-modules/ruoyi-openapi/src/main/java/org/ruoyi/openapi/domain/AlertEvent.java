package org.ruoyi.openapi.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 告警接入事件 t_alert_event
 * <p>
 * 每条推送的告警都落一条记录，包含被规则忽略的告警，
 * 用于事后排查"这条告警为什么没开单"。
 *
 * @author ruoyi
 */
@Data
@TableName("t_alert_event")
public class AlertEvent {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 告警唯一标识
     */
    private String alertId;

    /**
     * 去重键：规则id + 来源 + 类型 + 资产
     */
    private String dedupKey;

    /**
     * 去重时间桶：当前秒数 / 去重窗口，配合唯一索引实现幂等
     */
    private Long dedupBucket;

    private String source;

    private String type;

    private String severity;

    private String asset;

    /**
     * 告警原始请求
     */
    private String raw;

    /**
     * 处理结果：0 忽略 1 建单
     */
    private Integer decision;

    private Long ruleId;

    private String flowCode;

    private String businessId;

    private Long instanceId;

    /**
     * 处理说明（未命中原因 / 触发失败原因）
     */
    private String resultMsg;
}
