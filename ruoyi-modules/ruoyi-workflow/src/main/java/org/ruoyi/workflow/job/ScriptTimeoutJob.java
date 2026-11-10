package org.ruoyi.workflow.job;

import com.baomidou.lock.annotation.Lock4j;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.workflow.common.ConditionalOnEnable;
import org.ruoyi.workflow.domain.FlwScriptOutbox;
import org.ruoyi.workflow.mapper.FlwScriptOutboxMapper;
import org.ruoyi.workflow.service.impl.ScriptCallbackService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 剧本执行超时扫描任务
 * <p>
 * 扫描已派发但超过截止时间仍未回调的记录，按超时处理并终止流程。
 *
 * @author ruoyi
 */
@ConditionalOnEnable
@Slf4j
@Component
@RequiredArgsConstructor
public class ScriptTimeoutJob {

    private static final int BATCH_SIZE = 50;

    private final FlwScriptOutboxMapper flwScriptOutboxMapper;
    private final ScriptCallbackService scriptCallbackService;

    @Scheduled(fixedDelay = 30_000)
    @Lock4j(keys = "'soar:script:timeout'", acquireTimeout = 0, expire = 60_000)
    public void scan() {
        List<FlwScriptOutbox> rows = flwScriptOutboxMapper.selectOverdue(
            new Date(),
            BATCH_SIZE
        );
        if (rows.isEmpty()) {
            return;
        }
        for (FlwScriptOutbox row : rows) {
            try {
                scriptCallbackService.timeout(row.getRunId());
            } catch (Exception e) {
                log.error("[剧本超时] 处理失败, runId={}", row.getRunId(), e);
            }
        }
    }
}
