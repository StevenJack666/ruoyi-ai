package org.ruoyi.openapi.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.domain.dto.StartProcessReturnDTO;
import org.ruoyi.common.core.service.WorkflowTriggerService;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.config.AppIdContext;
import org.ruoyi.openapi.domain.AlertEvent;
import org.ruoyi.openapi.domain.AlertIngestResult;
import org.ruoyi.openapi.domain.AlertRule;
import org.ruoyi.openapi.mapper.AlertEventMapper;
import org.ruoyi.openapi.mapper.AlertRuleMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.expression.MapAccessor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Service;

/**
 * 告警接入与规则判定
 * <p>
 * 告警字段因来源而异，统一按 {@code Map} 透传，不做强类型建模：
 * 规则从 {@code source} / {@code type} / {@code severity} / {@code asset} 四个约定键取值，
 * 其余字段原样并入流程变量供剧本使用。
 * <p>
 * 处理顺序：匹配规则 → 去重 → 触发对应流程。
 * 不命中的告警同样落 {@code t_alert_event}，便于事后追溯"为什么没开单"。
 * <p>
 * 去重依赖 {@code t_alert_event} 的 (dedup_key, dedup_bucket) 唯一索引，
 * 靠数据库保证并发下只有一条告警能通过，无需额外的分布式锁。
 *
 * @author ruoyi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertIngestService {

    private static final ExpressionParser SPEL_PARSER =
        new SpelExpressionParser();

    /**
     * 告警唯一标识键，同时作为流程实例的 businessId
     */
    public static final String KEY_ALERT_ID = "alertId";

    private static final String KEY_SOURCE = "source";

    private static final String KEY_TYPE = "type";

    private static final String KEY_SEVERITY = "severity";

    private static final String KEY_ASSET = "asset";

    /**
     * 等级排序，用于 severity_min 比较
     */
    private static final Map<String, Integer> SEVERITY_ORDER = Map.of(
        "info",
        1,
        "low",
        2,
        "medium",
        3,
        "high",
        4,
        "critical",
        5
    );

    private static final int DEFAULT_DEDUP_WINDOW_SEC = 300;

    private static final int DEDUP_KEY_MAX_LEN = 200;

    private static final int ERROR_MSG_MAX_LEN = 480;

    private static final int RAW_MAX_LEN = 20000;

    private final AlertRuleMapper alertRuleMapper;
    private final AlertEventMapper alertEventMapper;

    /**
     * 工作流模块可能被 warm-flow.enabled=false 关闭，用 ObjectProvider 避免启动失败
     */
    private final ObjectProvider<WorkflowTriggerService> workflowTriggerServiceProvider;

    /**
     * @param alert 告警原始字段，必须包含 alertId
     */
    public AlertIngestResult ingest(Map<String, Object> alert) {
        Map<String, Object> payload =
            alert == null ? new LinkedHashMap<>(0) : new LinkedHashMap<>(alert);
        payload.values().removeIf(Objects::isNull);
        String alertId = stringValue(payload.get(KEY_ALERT_ID));

        AlertRule rule = match(payload);
        if (rule == null) {
            Long eventId = saveEvent(payload, alertId, null, "未命中任何规则");
            AlertIngestResult result = AlertIngestResult.ignored(
                "未命中任何规则"
            );
            result.setEventId(eventId);
            return result;
        }

        AlertEvent event = buildEvent(payload, alertId, rule);
        try {
            alertEventMapper.insert(event);
        } catch (DuplicateKeyException e) {
            // 唯一索引拦下：窗口内已有同类告警，直接抑制
            AlertIngestResult result = AlertIngestResult.ignored(
                "重复告警，已在 " + dedupWindow(rule) + "s 抑制窗口内"
            );
            result.setRuleId(rule.getId());
            result.setRuleName(rule.getRuleName());
            result.setFlowCode(rule.getFlowCode());
            log.info(
                "[告警接入] 重复告警已抑制, alertId={}, ruleId={}",
                alertId,
                rule.getId()
            );
            return result;
        }

        AlertIngestResult result = new AlertIngestResult();
        result.setMatched(true);
        result.setEventId(event.getId());
        result.setRuleId(rule.getId());
        result.setRuleName(rule.getRuleName());
        result.setFlowCode(rule.getFlowCode());
        result.setBusinessId(alertId);

        WorkflowTriggerService triggerService =
            workflowTriggerServiceProvider.getIfAvailable();
        if (triggerService == null) {
            finishEvent(event.getId(), null, "工作流模块未启用");
            result.setReason("工作流模块未启用");
            return result;
        }

        Map<String, Object> variables = new HashMap<>(payload);
        variables.put(KEY_ALERT_ID, alertId);
        variables.put("ruleId", rule.getId());
        variables.put("ruleName", rule.getRuleName());
        String appId = AppIdContext.getAppId();
        if (StringUtils.isNotBlank(appId)) {
            variables.put("triggerAppId", appId);
        }

        try {
            StartProcessReturnDTO dto = triggerService.startAsSystem(
                rule.getFlowCode(),
                alertId,
                variables
            );
            result.setTriggered(true);
            result.setProcessInstanceId(dto.getProcessInstanceId());
            result.setTaskId(dto.getTaskId());
            finishEvent(
                event.getId(),
                dto.getProcessInstanceId(),
                "已触发流程"
            );
            log.info(
                "[告警接入] 已触发流程, alertId={}, ruleId={}, flowCode={}, instanceId={}",
                alertId,
                rule.getId(),
                rule.getFlowCode(),
                dto.getProcessInstanceId()
            );
            return result;
        } catch (Exception e) {
            String reason = truncate(e.getMessage(), ERROR_MSG_MAX_LEN);
            finishEvent(event.getId(), null, reason);
            result.setReason(reason);
            log.error(
                "[告警接入] 触发流程失败, alertId={}, flowCode={}",
                alertId,
                rule.getFlowCode(),
                e
            );
            return result;
        }
    }

    private AlertRule match(Map<String, Object> alert) {
        List<AlertRule> rules = alertRuleMapper.selectList(
            Wrappers.<AlertRule>lambdaQuery()
                .eq(AlertRule::getEnabled, true)
                .orderByAsc(AlertRule::getPriority)
                .orderByAsc(AlertRule::getId)
        );
        for (AlertRule rule : rules) {
            if (matches(rule, alert)) {
                return rule;
            }
        }
        return null;
    }

    private boolean matches(AlertRule rule, Map<String, Object> alert) {
        return (
            matchesPattern(alert.get(KEY_SOURCE), rule.getSourcePattern()) &&
            matchesPattern(alert.get(KEY_TYPE), rule.getTypePattern()) &&
            matchesSeverity(alert.get(KEY_SEVERITY), rule.getSeverityMin()) &&
            matchesSpel(rule, alert)
        );
    }

    /**
     * 逗号分隔的精确值或 * 通配，任一命中即通过；规则为空表示不限
     */
    private boolean matchesPattern(Object value, String patterns) {
        if (StringUtils.isBlank(patterns)) {
            return true;
        }
        String text = stringValue(value);
        if (StringUtils.isBlank(text)) {
            return false;
        }
        for (String pattern : patterns.split(",")) {
            String trimmed = pattern.trim();
            if (!trimmed.isEmpty() && wildcardMatch(text, trimmed)) {
                return true;
            }
        }
        return false;
    }

    private boolean wildcardMatch(String text, String pattern) {
        StringBuilder regex = new StringBuilder("^");
        for (char c : pattern.toCharArray()) {
            if (c == '*') {
                regex.append(".*");
            } else if (c == '?') {
                regex.append('.');
            } else {
                regex.append(Pattern.quote(String.valueOf(c)));
            }
        }
        regex.append('$');
        return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE)
            .matcher(text)
            .matches();
    }

    private boolean matchesSeverity(Object severity, String severityMin) {
        if (StringUtils.isBlank(severityMin)) {
            return true;
        }
        Integer min = SEVERITY_ORDER.get(severityMin.trim().toLowerCase());
        if (min == null) {
            // 等级配置错误时不参与拦截，避免整条规则静默失效
            log.warn("[告警接入] severity_min 配置非法: {}", severityMin);
            return true;
        }
        Integer actual = SEVERITY_ORDER.get(
            stringValue(severity).trim().toLowerCase()
        );
        return actual != null && actual >= min;
    }

    /**
     * 只读 SpEL 上下文：不支持 T()、构造器与 Bean 方法调用，
     * 即使规则表被写入恶意表达式也无法执行任意代码。
     */
    private boolean matchesSpel(AlertRule rule, Map<String, Object> alert) {
        String expression = rule.getConditionSpel();
        if (StringUtils.isBlank(expression)) {
            return true;
        }
        try {
            SimpleEvaluationContext context =
                SimpleEvaluationContext.forPropertyAccessors(new MapAccessor())
                    .withRootObject(alert)
                    .build();
            Boolean hit = SPEL_PARSER.parseExpression(expression).getValue(
                context,
                Boolean.class
            );
            return Boolean.TRUE.equals(hit);
        } catch (Exception e) {
            log.warn(
                "[告警接入] SpEL 执行失败, ruleId={}, expr={}, error={}",
                rule.getId(),
                expression,
                e.getMessage()
            );
            return false;
        }
    }

    private Long saveEvent(
        Map<String, Object> alert,
        String alertId,
        AlertRule rule,
        String message
    ) {
        AlertEvent event = buildEvent(alert, alertId, rule);
        event.setResultMsg(message);
        alertEventMapper.insert(event);
        return event.getId();
    }

    private AlertEvent buildEvent(
        Map<String, Object> alert,
        String alertId,
        AlertRule rule
    ) {
        AlertEvent event = new AlertEvent();
        event.setAlertId(alertId);
        event.setSource(stringValue(alert.get(KEY_SOURCE)));
        event.setType(stringValue(alert.get(KEY_TYPE)));
        event.setSeverity(stringValue(alert.get(KEY_SEVERITY)));
        event.setAsset(stringValue(alert.get(KEY_ASSET)));
        event.setRaw(truncate(JSON.toJSONString(alert), RAW_MAX_LEN));
        if (rule == null) {
            event.setDecision(0);
            return event;
        }
        event.setDecision(1);
        event.setRuleId(rule.getId());
        event.setFlowCode(rule.getFlowCode());
        event.setBusinessId(alertId);
        event.setDedupKey(dedupKey(rule, alert));
        event.setDedupBucket(dedupBucket(rule));
        return event;
    }

    private void finishEvent(Long eventId, Long instanceId, String message) {
        AlertEvent update = new AlertEvent();
        update.setId(eventId);
        update.setInstanceId(instanceId);
        update.setResultMsg(truncate(message, ERROR_MSG_MAX_LEN));
        alertEventMapper.updateById(update);
    }

    private String dedupKey(AlertRule rule, Map<String, Object> alert) {
        String key =
            rule.getId() +
            "|" +
            stringValue(alert.get(KEY_SOURCE)) +
            "|" +
            stringValue(alert.get(KEY_TYPE)) +
            "|" +
            stringValue(alert.get(KEY_ASSET));
        return truncate(key, DEDUP_KEY_MAX_LEN);
    }

    /**
     * 固定时间桶，配合唯一索引实现窗口去重；返回 null 表示不去重
     */
    private Long dedupBucket(AlertRule rule) {
        int window = dedupWindow(rule);
        if (window <= 0) {
            return null;
        }
        return Instant.now().getEpochSecond() / window;
    }

    private int dedupWindow(AlertRule rule) {
        Integer window = rule.getDedupWindowSec();
        return window == null ? DEFAULT_DEDUP_WINDOW_SEC : window;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String truncate(String text, int maxLen) {
        if (text == null) {
            return null;
        }
        return text.length() > maxLen ? text.substring(0, maxLen) : text;
    }
}
