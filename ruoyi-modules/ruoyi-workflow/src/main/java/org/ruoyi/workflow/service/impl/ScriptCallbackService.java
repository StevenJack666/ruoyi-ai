package org.ruoyi.workflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.json.utils.JsonUtils;
import org.ruoyi.workflow.common.ConditionalOnEnable;
import org.ruoyi.workflow.common.enums.ScriptOutboxStatus;
import org.ruoyi.workflow.domain.FlwScriptOutbox;
import org.ruoyi.workflow.domain.bo.ScriptCallbackBo;
import org.ruoyi.workflow.mapper.FlwScriptOutboxMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 剧本执行回调处理
 * <p>
 * 状态流转与流程推进在同一事务内完成：
 * 流程推进失败时整体回滚，outbox 状态回到"已派发"，允许 Python 侧重试。
 *
 * @author ruoyi
 */
@ConditionalOnEnable
@Slf4j
@RequiredArgsConstructor
@Service
public class ScriptCallbackService {

    private static final int ERROR_MSG_MAX_LEN = 480;

    private final FlwScriptOutboxMapper flwScriptOutboxMapper;
    private final ScriptTaskAdvanceService scriptTaskAdvanceService;

    /**
     * 处理执行成功/失败回调
     *
     * @param runId 执行标识
     * @param bo    回调内容
     * @return 是否处理成功（未知 runId 或重复回调也返回 true，避免调用方无限重试）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean handle(String runId, ScriptCallbackBo bo) {
        FlwScriptOutbox outbox = flwScriptOutboxMapper.selectByRunId(runId);
        if (outbox == null) {
            log.warn("[剧本回调] 未知 runId={}", runId);
            return false;
        }

        // 幂等：已回调直接返回
        if (ScriptOutboxStatus.CALLBACK.getCode().equals(outbox.getStatus())) {
            log.info("[剧本回调] 重复回调，忽略。runId={}", runId);
            return true;
        }

        // 并发抢占：只有第一个回调能推进流程
        int affected = flwScriptOutboxMapper.casStatus(
            outbox.getId(),
            ScriptOutboxStatus.DISPATCHED.getCode(),
            ScriptOutboxStatus.CALLBACK.getCode()
        );
        if (affected == 0) {
            log.info(
                "[剧本回调] 状态非已派发，忽略。runId={}, currentStatus={}",
                runId,
                outbox.getStatus()
            );
            return true;
        }

        if (Boolean.TRUE.equals(bo.getSuccess())) {
            Map<String, Object> variables = new HashMap<>(6);
            variables.put("scriptOutput", bo.getOutput());
            variables.put("scriptCostMs", bo.getCostMs());
            if (bo.getTrace() != null && !bo.getTrace().isEmpty()) {
                variables.put("scriptTrace", bo.getTrace());
            }
            log.info(
                "[剧本回调] 写入流程变量, runId={}, hasOutput={}, traceSize={}",
                runId,
                bo.getOutput() != null,
                bo.getTrace() == null ? 0 : bo.getTrace().size()
            );
            scriptTaskAdvanceService.pass(outbox.getTaskId(), variables);
            markFinished(outbox.getId(), null, bo);
        } else {
            String reason = truncate("剧本执行失败: " + bo.getError());
            scriptTaskAdvanceService.terminate(outbox.getTaskId(), reason);
            markFinished(outbox.getId(), reason);
        }

        // Java 侧不可读脚本文件时，用执行器回报的哈希补齐留痕
        if (
            StringUtils.isBlank(outbox.getScriptSha256()) &&
            StringUtils.isNotBlank(bo.getScriptSha256())
        ) {
            flwScriptOutboxMapper.update(
                null,
                Wrappers.<FlwScriptOutbox>lambdaUpdate()
                    .eq(FlwScriptOutbox::getId, outbox.getId())
                    .set(FlwScriptOutbox::getScriptSha256, bo.getScriptSha256())
            );
        }
        log.info(
            "[剧本回调] 处理完成。runId={}, success={}, costMs={}",
            runId,
            bo.getSuccess(),
            bo.getCostMs()
        );
        return true;
    }

    /**
     * 处理执行超时
     *
     * @param runId 执行标识
     * @return 是否处理成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean timeout(String runId) {
        FlwScriptOutbox outbox = flwScriptOutboxMapper.selectByRunId(runId);
        if (outbox == null) {
            return false;
        }
        int affected = flwScriptOutboxMapper.casStatus(
            outbox.getId(),
            ScriptOutboxStatus.DISPATCHED.getCode(),
            ScriptOutboxStatus.TIMEOUT.getCode()
        );
        if (affected == 0) {
            return true;
        }
        scriptTaskAdvanceService.terminate(outbox.getTaskId(), "剧本执行超时");
        markFinished(outbox.getId(), "剧本执行超时");
        log.warn(
            "[剧本回调] 执行超时，流程已终止。runId={}, taskId={}",
            runId,
            outbox.getTaskId()
        );
        return true;
    }

    /**
     * 派发重试耗尽后的收尾：标记失败并终止流程。
     * <p>
     * 只把状态改成失败而不终止流程的话，任务会永久停留在待办，
     * 既不会被超时扫描（只扫已派发）也不会有人处理。
     *
     * @param runId 执行标识
     * @param error 失败原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void markDispatchFailed(String runId, String error) {
        FlwScriptOutbox outbox = flwScriptOutboxMapper.selectByRunId(runId);
        if (outbox == null) {
            log.warn("[剧本派发] 标记失败时未找到记录, runId={}", runId);
            return;
        }
        int affected = flwScriptOutboxMapper.casStatus(
            outbox.getId(),
            ScriptOutboxStatus.DISPATCHED.getCode(),
            ScriptOutboxStatus.FAILED.getCode()
        );
        if (affected == 0) {
            log.info(
                "[剧本派发] 状态非已派发，跳过失败标记, runId={}, status={}",
                runId,
                outbox.getStatus()
            );
            return;
        }

        String reason = truncate(error);
        try {
            scriptTaskAdvanceService.terminate(outbox.getTaskId(), reason);
        } catch (Exception e) {
            // 终止失败也要把状态落下来，便于人工介入
            log.error(
                "[剧本派发] 派发失败后终止流程失败, runId={}, taskId={}",
                runId,
                outbox.getTaskId(),
                e
            );
        }
        markFinished(outbox.getId(), reason);
        log.error(
            "[剧本派发] 派发重试耗尽，已标记失败并终止流程, runId={}, taskId={}, reason={}",
            runId,
            outbox.getTaskId(),
            reason
        );
    }

    private void markFinished(Long id, String errorMsg) {
        markFinished(id, errorMsg, null);
    }

    /**
     * 收尾：写入完成时间、失败原因，以及本次执行的输出与轨迹
     * <p>
     * 输出按 runId 留痕，避免多个脚本节点共用 scriptOutput 变量时互相覆盖。
     *
     * @param id       主键
     * @param errorMsg 失败原因，成功时为 null
     * @param bo       回调内容，无输出时可为 null
     */
    private void markFinished(Long id, String errorMsg, ScriptCallbackBo bo) {
        String outputJson =
            bo != null && bo.getOutput() != null
                ? JsonUtils.toJsonString(bo.getOutput())
                : null;
        String traceJson =
            bo != null && bo.getTrace() != null && !bo.getTrace().isEmpty()
                ? JsonUtils.toJsonString(bo.getTrace())
                : null;
        flwScriptOutboxMapper.update(
            null,
            Wrappers.<FlwScriptOutbox>lambdaUpdate()
                .eq(FlwScriptOutbox::getId, id)
                .set(FlwScriptOutbox::getFinishedAt, new Date())
                .set(errorMsg != null, FlwScriptOutbox::getErrorMsg, errorMsg)
                .set(outputJson != null, FlwScriptOutbox::getOutput, outputJson)
                .set(traceJson != null, FlwScriptOutbox::getTrace, traceJson)
        );
    }

    private String truncate(String msg) {
        if (msg == null) {
            return null;
        }
        return msg.length() > ERROR_MSG_MAX_LEN
            ? msg.substring(0, ERROR_MSG_MAX_LEN)
            : msg;
    }
}
