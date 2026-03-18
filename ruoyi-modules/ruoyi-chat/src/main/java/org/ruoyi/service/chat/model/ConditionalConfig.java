package org.ruoyi.service.chat.model;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * 条件工作流配置模型
 * 用于定义基于决策分支的复杂Agent执行流程
 */
@Data
public class ConditionalConfig {

    /**
     * 规划器Agent名称
     * 指定负责分析用户意图并做出路由决策的Agent。
     * 该Agent的输出将作为键（Key）用于匹配具体的执行分支。
     * 对应JSON中的 "planner" 字段。
     */
    private String planner;

    /**
     * 分支映射表
     * 定义了决策结果与具体执行Agent列表的映射关系。
     * Key: 决策关键词（如 "MOVIE", "FOOD", "BOTH"）。
     * Value: 匹配该关键词后需要执行的Agent名称列表。
     * 对应JSON中的 "branches" 对象。
     */
    private Map<String, List<String>> branches;

    /**
     * 后处理器Agent名称
     * 指定在分支执行完成后，负责对结果进行最终处理（如汇总、润色、检查）的Agent。
     * 对应JSON中的 "postProcessor" 字段。
     */
    private String postProcessor;
}
