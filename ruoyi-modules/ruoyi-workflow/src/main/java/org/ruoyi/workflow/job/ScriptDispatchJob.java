package org.ruoyi.workflow.job;

import com.baomidou.lock.annotation.Lock4j;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.workflow.client.LocalPythonScriptExecutor;
import org.ruoyi.workflow.common.ConditionalOnEnable;
import org.ruoyi.workflow.common.enums.ScriptOutboxStatus;
import org.ruoyi.workflow.domain.FlwScriptOutbox;
import org.ruoyi.workflow.mapper.FlwScriptOutboxMapper;
import org.ruoyi.workflow.service.ScriptCallbackService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 剧本派发任务
 * <p>
 * 扫描 outbox 中的待派发记录，抢占后提交给 Python 执行服务。
 * 多实例部署下通过 Lock4j 保证同一时刻只有一个实例在扫描，
 * 通过 CAS（claimForDispatch）保证同一条记录只被提交一次。
 *
 * @author ruoyi
 */
@ConditionalOnEnable
@Slf4j
@Component
@RequiredArgsConstructor
public class ScriptDispatchJob {

    /**
     * 单次扫描批量上限，避免锁持有时间过长
     */
    private static final int BATCH_SIZE = 20;

    private static final int DEFAULT_TIMEOUT_SEC = 900;

    private static final int ERROR_MSG_MAX_LEN = 480;

    /**
     * 派发失败最大重试次数，存放在 sys_config（参数管理）
     */
    private static final String MAX_RETRY_KEY = "soar.script.max-retry";

    private static final int DEFAULT_MAX_RETRY = 3;

    private final FlwScriptOutboxMapper flwScriptOutboxMapper;
    private final LocalPythonScriptExecutor scriptExecutor;
    private final ScriptCallbackService scriptCallbackService;
    private final ConfigService configService;

    @Scheduled(fixedDelay = 2000)
    @Lock4j(
        keys = "'soar:script:dispatch'",
        acquireTimeout = 0,
        expire = 60_000
    )
    public void dispatch() {
        List<FlwScriptOutbox> rows = flwScriptOutboxMapper.selectDispatchable(
            BATCH_SIZE
        );
        if (rows.isEmpty()) {
            return;
        }
        for (FlwScriptOutbox row : rows) {
            long timeoutSec =
                row.getTimeoutSec() == null
                    ? DEFAULT_TIMEOUT_SEC
                    : row.getTimeoutSec();
            Date now = new Date();
            Date deadline = new Date(now.getTime() + timeoutSec * 1000L);

            // 抢占：状态 0 -> 1，同时写入派发时间与超时截止时间
            if (
                flwScriptOutboxMapper.claimForDispatch(
                    row.getId(),
                    now,
                    deadline
                ) == 0
            ) {
                continue;
            }

            try {
                scriptExecutor.dispatch(row);
                log.info(
                    "[剧本派发] 已提交执行, runId={}, script={}",
                    row.getRunId(),
                    row.getScriptPath()
                );
            } catch (LocalPythonScriptExecutor.PoolBusyException e) {
                // 线程池已满：瞬时压力，回退待派发且不计入重试次数
                flwScriptOutboxMapper.update(
                    null,
                    Wrappers.<FlwScriptOutbox>lambdaUpdate()
                        .eq(FlwScriptOutbox::getId, row.getId())
                        .eq(
                            FlwScriptOutbox::getStatus,
                            ScriptOutboxStatus.DISPATCHED.getCode()
                        )
                        .set(
                            FlwScriptOutbox::getStatus,
                            ScriptOutboxStatus.PENDING.getCode()
                        )
                );
                log.warn(
                    "[剧本派发] 线程池已满，回退待派发, runId={}",
                    row.getRunId()
                );
                continue;
            } catch (Exception e) {
                log.error("[剧本派发] 提交失败, runId={}", row.getRunId(), e);
                handleFailure(row, e);
            }
        }
    }

    /**
     * 提交失败：指数退避重试；超过上限时标记失败并终止流程，
     * 避免任务永久停留在待办（超时扫描只看"已派发"，扫不到失败记录）。
     */
    private void handleFailure(FlwScriptOutbox row, Exception e) {
        int retry = (row.getRetryCount() == null ? 0 : row.getRetryCount()) + 1;
        if (retry > maxRetry()) {
            scriptCallbackService.markDispatchFailed(
                row.getRunId(),
                "剧本派发失败: " + e.getMessage()
            );
            return;
        }

        Date nextRetryAt = new Date(
            System.currentTimeMillis() + (1L << Math.min(retry, 10)) * 1000L
        );
        flwScriptOutboxMapper.update(
            null,
            Wrappers.<FlwScriptOutbox>lambdaUpdate()
                .eq(FlwScriptOutbox::getId, row.getId())
                .eq(
                    FlwScriptOutbox::getStatus,
                    ScriptOutboxStatus.DISPATCHED.getCode()
                )
                .set(
                    FlwScriptOutbox::getStatus,
                    ScriptOutboxStatus.PENDING.getCode()
                )
                .set(FlwScriptOutbox::getRetryCount, retry)
                .set(FlwScriptOutbox::getNextRetryAt, nextRetryAt)
                .set(FlwScriptOutbox::getErrorMsg, truncate(e.getMessage()))
        );

        log.warn(
            "[剧本派发] 第 {} 次提交失败, runId={}, nextRetryAt={}",
            retry,
            row.getRunId(),
            nextRetryAt
        );
    }

    private int maxRetry() {
        String value = configService.getConfigValue(MAX_RETRY_KEY);
        if (StringUtils.isBlank(value)) {
            return DEFAULT_MAX_RETRY;
        }
        try {
            int retry = Integer.parseInt(value.trim());
            return retry < 0 ? DEFAULT_MAX_RETRY : retry;
        } catch (NumberFormatException e) {
            log.warn(
                "[剧本派发] {} 不是合法整数: {}, 使用默认值 {}",
                MAX_RETRY_KEY,
                value,
                DEFAULT_MAX_RETRY
            );
            return DEFAULT_MAX_RETRY;
        }
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
