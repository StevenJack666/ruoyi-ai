package org.ruoyi.openapi.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.domain.dto.StartProcessReturnDTO;
import org.ruoyi.common.core.service.WorkflowTriggerService;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.config.AppIdContext;
import org.ruoyi.openapi.domain.WorkflowTriggerReq;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 剧本触发 OpenAPI
 * <p>
 * 供告警源（SIEM / 邮件网关 / SOAR 前置）调用。
 * 鉴权由 OpenApiInterceptor 统一处理（AppId + 签名 + IP + URI 授权），
 * 本控制器只依赖 common-core 的接口，不感知工作流实现。
 *
 * @author ruoyi
 */
@SaIgnore
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/openapi/workflow")
public class OpenApiWorkflowController {

    /**
     * 工作流模块可能被 warm-flow.enabled=false 关闭，用 ObjectProvider 避免启动失败
     */
    private final ObjectProvider<WorkflowTriggerService> workflowTriggerServiceProvider;

    /**
     * 触发剧本
     *
     * @param req 触发参数
     */
    @PostMapping("/trigger")
    public R<StartProcessReturnDTO> trigger(@Validated @RequestBody WorkflowTriggerReq req) {
        WorkflowTriggerService triggerService = workflowTriggerServiceProvider.getIfAvailable();
        if (triggerService == null) {
            log.warn("[剧本触发] 工作流模块未启用，忽略触发请求, flowCode={}", req.getFlowCode());
            return R.fail("工作流模块未启用");
        }

        Map<String, Object> variables = req.getVariables() == null
            ? new HashMap<>(16)
            : new HashMap<>(req.getVariables());

        // 留痕：记录调用方 appId（开启验签时由拦截器写入）
        String appId = AppIdContext.getAppId();
        if (StringUtils.isNotBlank(appId)) {
            variables.put("triggerAppId", appId);
        }

        StartProcessReturnDTO result = triggerService.startAsSystem(
            req.getFlowCode(), req.getBusinessId(), variables);
        log.info("[剧本触发] appId={}, flowCode={}, businessId={}, instanceId={}",
            appId, req.getFlowCode(), req.getBusinessId(), result.getProcessInstanceId());
        return R.ok("触发成功", result);
    }

}
