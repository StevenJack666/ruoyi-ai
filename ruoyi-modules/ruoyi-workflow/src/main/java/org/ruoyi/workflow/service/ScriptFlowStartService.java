package org.ruoyi.workflow.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.service.TaskService;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.mapper.FlowInstanceMapper;
import org.ruoyi.common.core.domain.dto.StartProcessReturnDTO;
import org.ruoyi.common.core.enums.BusinessStatusEnum;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.common.core.service.WorkflowTriggerService;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.json.utils.JsonUtils;
import org.ruoyi.workflow.common.ConditionalOnEnable;
import org.ruoyi.workflow.common.constant.FlowConstant;
import org.ruoyi.workflow.domain.FlowInstanceBizExt;
import org.ruoyi.workflow.mapper.FlwInstanceBizExtMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统触发流程（无登录态）实现
 * <p>
 * 与 FlwTaskServiceImpl#startWorkFlow 的区别：不读取 LoginHelper，
 * 发起人使用配置的系统账号，供告警 Webhook 等无用户上下文的场景使用。
 *
 * @author ruoyi
 */
@ConditionalOnEnable
@Slf4j
@RequiredArgsConstructor
@Service
public class ScriptFlowStartService implements WorkflowTriggerService {

    /**
     * 系统办理人 id，存放在 sys_config（参数管理），需与流程节点 permissionFlag 配置一致
     */
    private static final String SYSTEM_USER_ID_KEY =
        "soar.script.system-user-id";

    private static final String DEFAULT_SYSTEM_USER_ID = "1";

    private final InsService insService;
    private final TaskService taskService;
    private final FlowInstanceMapper flowInstanceMapper;
    private final FlwInstanceBizExtMapper flwInstanceBizExtMapper;
    private final ConfigService configService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StartProcessReturnDTO startAsSystem(
        String flowCode,
        String businessId,
        Map<String, Object> variables
    ) {
        if (StringUtils.isBlank(flowCode)) {
            throw new ServiceException("触发工作流时流程编码不能为空");
        }
        if (StringUtils.isBlank(businessId)) {
            throw new ServiceException("触发工作流时必须包含业务ID");
        }

        // 系统办理人来自 sys_config，需与流程节点 permissionFlag 配置一致
        String systemUserId = configService.getConfigValue(SYSTEM_USER_ID_KEY);
        if (StringUtils.isBlank(systemUserId)) {
            systemUserId = DEFAULT_SYSTEM_USER_ID;
        }
        Map<String, Object> vars =
            variables == null ? new HashMap<>(16) : new HashMap<>(variables);
        // 无登录态：发起人使用系统账号，部门留空
        vars.put(FlowConstant.INITIATOR, systemUserId);
        vars.put(FlowConstant.BUSINESS_ID, businessId);

        // 同一业务id只允许一个实例，重复触发直接返回当前任务
        FlowInstance exists = flowInstanceMapper.selectOne(
            new LambdaQueryWrapper<>(FlowInstance.class).eq(
                FlowInstance::getBusinessId,
                businessId
            )
        );
        if (ObjectUtil.isNotNull(exists)) {
            // SOAR 流程没有「申请人提交」阶段，实例一启动就是 waiting；
            // 复用已有实例时只拦截终态，运行中的实例直接复用
            if (
                !BusinessStatusEnum.WAITING.getStatus().equals(
                    exists.getFlowStatus()
                )
            ) {
                BusinessStatusEnum.checkStartStatus(exists.getFlowStatus());
            }
            List<Task> existTasks = taskService.list(
                new FlowTask().setInstanceId(exists.getId())
            );
            if (CollUtil.isEmpty(existTasks)) {
                throw new ServiceException(
                    "流程已结束，无法重复触发, businessId={}",
                    businessId
                );
            }
            taskService.mergeVariable(exists, vars);
            insService.updateById(exists);
            log.info(
                "[剧本触发] 复用已有流程实例, businessId={}, instanceId={}",
                businessId,
                exists.getId()
            );
            return buildResult(exists.getId(), existTasks.get(0).getId());
        }

        Definition definition = FlowEngine.defService().getPublishByFlowCode(
            flowCode
        );
        if (definition == null) {
            throw new ServiceException("未找到已发布的流程定义: {}", flowCode);
        }
        Dict dict = JsonUtils.parseMap(definition.getExt());
        boolean autoPass =
            !ObjectUtil.isNull(dict) && dict.getBool(FlowConstant.AUTO_PASS);
        vars.put(FlowConstant.AUTO_PASS, autoPass);

        FlowInstanceBizExt bizExt = new FlowInstanceBizExt();
        bizExt.setBusinessId(businessId);
        vars.put(FlowConstant.BUSINESS_CODE, generateBusinessCode(bizExt));

        FlowParams flowParams = FlowParams.build()
            .handler(systemUserId)
            .flowCode(flowCode)
            .variable(vars)
            // SOAR 全自动流程没有「提交」动作，直接进入审批中，
            // 否则实例停在 draft，所有按 waiting 过滤的待办视图（我的任务等）都查不到
            .flowStatus(BusinessStatusEnum.WAITING.getStatus());

        Instance instance;
        try {
            instance = insService.start(businessId, flowParams);
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }

        bizExt.setInstanceId(instance.getId());
        flwInstanceBizExtMapper.saveOrUpdateByInstanceId(bizExt);

        // 申请人执行流程：第一个中间节点必须存在且只有一个
        List<Task> taskList = taskService.list(
            new FlowTask().setInstanceId(instance.getId())
        );
        if (CollUtil.isEmpty(taskList)) {
            throw new ServiceException(
                "流程启动后未生成任务，请检查流程定义: {}",
                flowCode
            );
        }
        if (taskList.size() > 1) {
            throw new ServiceException(
                "流程启动时产生了 {} 个任务，请确认「开始」节点只连接一个中间节点",
                taskList.size()
            );
        }

        Long taskId = taskList.get(0).getId();
        log.info(
            "[剧本触发] 流程已启动, flowCode={}, businessId={}, instanceId={}, taskId={}",
            flowCode,
            businessId,
            instance.getId(),
            taskId
        );
        return buildResult(instance.getId(), taskId);
    }

    private StartProcessReturnDTO buildResult(Long instanceId, Long taskId) {
        StartProcessReturnDTO dto = new StartProcessReturnDTO();
        dto.setProcessInstanceId(instanceId);
        dto.setTaskId(taskId);
        return dto;
    }

    private String generateBusinessCode(FlowInstanceBizExt bizExt) {
        if (StringUtils.isBlank(bizExt.getBusinessCode())) {
            String businessCode = String.valueOf(System.currentTimeMillis());
            bizExt.setBusinessCode(businessCode);
            return businessCode;
        }
        return bizExt.getBusinessCode();
    }
}
