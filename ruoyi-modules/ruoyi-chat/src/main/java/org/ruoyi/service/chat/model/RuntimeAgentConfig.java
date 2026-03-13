package org.ruoyi.service.chat.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * 运行时子 Agent 配置
 */
@Data
@Builder
public class RuntimeAgentConfig {

    private String name;

    /**
     * 角色: primary(主agent) / child(子agent)
     */
    private String role;

    private String model;

    private String systemPrompt;

    /**
     * 绑定的技能编码或名称列表
     */
    private List<String> skills;

    private List<String> tools;
}
