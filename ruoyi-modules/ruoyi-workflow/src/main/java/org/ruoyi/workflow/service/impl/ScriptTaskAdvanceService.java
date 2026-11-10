package org.ruoyi.workflow.service.impl;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.enums.SkipType;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.service.TaskService;
import org.ruoyi.common.core.enums.BusinessStatusEnum;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.workflow.common.ConditionalOnEnable;
import org.ruoyi.workflow.common.enums.TaskStatusEnum;
import org.springframework.stereotype.Service;

/**
 * 系统推进流程任务
 * <p>
 * 与 {@code FlwTaskServiceImpl#completeTask} 的区别：本类不依赖登录态，
 * 供回调线程、超时线程等无用户上下文场景使用。
 * <p>
 * 不要在这里调用 {@code LoginHelper}：回调线程没有登录信息。
 *
 * @author ruoyi
 */
@ConditionalOnEnable
@Slf4j
@RequiredArgsConstructor
@Service
public class ScriptTaskAdvanceService {

    /**
     * 系统办理人 id，存放在 sys_config（参数管理），需与流程节点 permissionFlag 配置的账号一致
     */
    private static final String SYSTEM_USER_ID_KEY =
        "soar.script.system-user-id";

    private static final String DEFAULT_SYSTEM_USER_ID = "1";

    private final TaskService taskService;
    private final InsService insService;
    private final ConfigService configService;

    /**
     * 推进任务到下一节点
     *
     * @param taskId    任务 id
     * @param variables 写入流程的变量
     */
    public void pass(Long taskId, Map<String, Object> variables) {
        Task task = taskService.getById(taskId);
        if (task == null) {
            // 幂等：任务已被处理过，直接返回
            log.warn(
                "[剧本派发] 任务不存在或已办理，跳过推进, taskId={}",
                taskId
            );
            return;
        }

        // 显式合并到流程实例变量，保证下一个节点能读到 scriptOutput / scriptTrace
        Instance instance = insService.getById(task.getInstanceId());
        if (instance != null && variables != null && !variables.isEmpty()) {
            taskService.mergeVariable(instance, variables);
            insService.updateById(instance);
        }

        FlowParams params = FlowParams.build()
            .handler(systemUserId())
            // 系统调用绕过办理人权限校验
            .ignore(true)
            .variable(variables)
            .skipType(SkipType.PASS.getKey())
            .flowStatus(BusinessStatusEnum.WAITING.getStatus())
            .hisStatus(TaskStatusEnum.PASS.getStatus())
            .message("剧本执行完成");
        taskService.skip(taskId, params);
        log.info("[剧本派发] 任务已推进, taskId={}", taskId);
    }

    /**
     * 终止流程
     *
     * @param taskId 任务 id
     * @param reason 终止原因
     */
    public void terminate(Long taskId, String reason) {
        Task task = taskService.getById(taskId);
        if (task == null) {
            log.warn(
                "[剧本派发] 任务不存在或已办理，跳过终止, taskId={}",
                taskId
            );
            return;
        }
        Instance instance = insService.getById(task.getInstanceId());
        if (instance != null) {
            try {
                BusinessStatusEnum.checkInvalidStatus(instance.getFlowStatus());
            } catch (Exception e) {
                // 流程已处于终态，幂等返回
                log.warn(
                    "[剧本派发] 流程已结束，跳过终止, instanceId={}, status={}",
                    instance.getId(),
                    instance.getFlowStatus()
                );
                return;
            }
        }
        FlowParams params = FlowParams.build()
            .handler(systemUserId())
            .ignore(true)
            .message(reason)
            .flowStatus(BusinessStatusEnum.TERMINATION.getStatus())
            .hisStatus(TaskStatusEnum.TERMINATION.getStatus());
        taskService.termination(taskId, params);
        log.info("[剧本派发] 任务已终止, taskId={}, reason={}", taskId, reason);
    }

    /**
     * 系统办理人：读 sys_config，未配置时使用默认账号
     */
    private String systemUserId() {
        String value = configService.getConfigValue(SYSTEM_USER_ID_KEY);
        return StringUtils.isBlank(value) ? DEFAULT_SYSTEM_USER_ID : value;
    }
}
