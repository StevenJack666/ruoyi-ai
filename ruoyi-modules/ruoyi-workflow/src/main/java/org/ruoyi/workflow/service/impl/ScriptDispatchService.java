package org.ruoyi.workflow.service.impl;

import cn.hutool.core.util.IdUtil;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.json.utils.JsonUtils;
import org.ruoyi.workflow.common.ConditionalOnEnable;
import org.ruoyi.workflow.common.enums.ScriptOutboxStatus;
import org.ruoyi.workflow.domain.FlwScriptOutbox;
import org.ruoyi.workflow.domain.PlaybookScript;
import org.ruoyi.workflow.mapper.FlwScriptOutboxMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 剧本脚本派发服务
 * <p>
 * 只负责把"待派发任务"写入 outbox，不在此处调用外部 HTTP。
 * 真正的调用由调度器异步完成，避免长耗时操作占用流程事务与数据库连接。
 *
 * @author ruoyi
 */
@ConditionalOnEnable
@Slf4j
@RequiredArgsConstructor
@Service
public class ScriptDispatchService {

    /**
     * 默认执行超时（秒）
     */
    private static final int DEFAULT_TIMEOUT_SEC = 900;

    private final FlwScriptOutboxMapper flwScriptOutboxMapper;
    private final ScriptPathResolver scriptPathResolver;

    /**
     * 写入待派发任务
     * <p>
     * 使用 {@link Propagation#MANDATORY}：必须在流程事务内调用，
     * 流程回滚时 outbox 记录一并回滚，避免"任务未创建但脚本被派发"。
     *
     * @param config    脚本配置
     * @param instance  流程实例
     * @param task      当前任务
     * @param variables 流程变量，作为派发入参
     * @return 本次执行的 runId
     */
    @Transactional(
        propagation = Propagation.MANDATORY,
        rollbackFor = Exception.class
    )
    public String enqueue(
        PlaybookScript config,
        Instance instance,
        Task task,
        Map<String, Object> variables
    ) {
        Path scriptPath = scriptPathResolver.resolve(config.getScriptPath());
        // 解释器只认数据库配置，未配置直接失败，避免派发后在子进程阶段才报错
        String pythonBin = config.getPythonBin();
        if (StringUtils.isBlank(pythonBin)) {
            throw new ServiceException(
                "剧本未配置 Python 解释器(t_playbook_script.python_bin), flowCode={}, nodeCode={}",
                config.getFlowCode(),
                config.getNodeCode()
            );
        }
        pythonBin = pythonBin.trim();
        String sha256 = scriptPathResolver.trySha256(scriptPath);

        FlwScriptOutbox outbox = new FlwScriptOutbox();
        outbox.setRunId(IdUtil.fastSimpleUUID());
        outbox.setInstanceId(instance.getId());
        outbox.setTaskId(task.getId());
        outbox.setBusinessId(instance.getBusinessId());
        outbox.setFlowCode(config.getFlowCode());
        outbox.setNodeCode(config.getNodeCode());
        outbox.setScriptPath(scriptPath.toString());
        outbox.setPythonBin(pythonBin);
        outbox.setScriptSha256(sha256);
        outbox.setPayload(
            JsonUtils.toJsonString(
                variables == null ? Collections.emptyMap() : variables
            )
        );
        outbox.setTimeoutSec(
            config.getTimeoutSec() == null
                ? DEFAULT_TIMEOUT_SEC
                : config.getTimeoutSec()
        );
        outbox.setStatus(ScriptOutboxStatus.PENDING.getCode());
        outbox.setRetryCount(0);
        flwScriptOutboxMapper.insert(outbox);

        log.info(
            "[剧本派发] 已写入 outbox, runId={}, flowCode={}, nodeCode={}, businessId={}, script={}, python={}",
            outbox.getRunId(),
            outbox.getFlowCode(),
            outbox.getNodeCode(),
            outbox.getBusinessId(),
            scriptPath,
            pythonBin
        );
        return outbox.getRunId();
    }
}
