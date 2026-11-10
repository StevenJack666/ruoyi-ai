package org.ruoyi.common.core.service;

import org.ruoyi.common.core.domain.dto.StartProcessReturnDTO;

import java.util.Map;

/**
 * 通用 流程触发服务
 * <p>
 * 供 openapi 等接入层使用：在无登录态的场景下触发流程。
 * 实现类由工作流模块提供；工作流未启用时该 Bean 不存在，调用方需做空判断。
 *
 * @author ruoyi
 */
public interface WorkflowTriggerService {

    /**
     * 系统触发流程（无登录态）
     *
     * @param flowCode   流程编码
     * @param businessId 业务id，用于幂等
     * @param variables  流程变量
     * @return 启动结果
     */
    StartProcessReturnDTO startAsSystem(String flowCode,
                                        String businessId,
                                        Map<String, Object> variables);

}
