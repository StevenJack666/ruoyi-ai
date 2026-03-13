package org.ruoyi.domain.entity.agent;

import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 能力市场实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_market")
public class AiMarket extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String marketName;

    private String description;

    /**
     * 1=激活, 0=禁用
     */
    private Integer status;

    /**
     * Agent 编排配置(JSON)
     */


// {
//   "executionMode": "SUPERVISOR",
//   "systemPrompt": "市场级总提示词",
//   "supervisor": {
//     "name": "supervisor",
//     "role": "primary",
//     "model": "qwen-plus",
//     "systemPrompt": "你负责选择最合适的子agent",
//     "skills": ["route_decision"],
//     "tools": []
//   },
//   "agents": [
//     {
//       "name": "research-agent",
//       "role": "child",
//       "model": "qwen-plus",
//       "systemPrompt": "你是研究助手",
//       "skills": ["search", "summary"],
//       "tools": ["web-search"]
//     },
//     {
//       "name": "coding-agent",
//       "role": "child",
//       "model": "qwen-plus",
//       "systemPrompt": "你是Java工程师",
//       "skills": ["java_coding", "debugging"],
//       "tools": ["github-tool"]
//     }
//   ]
// }
    private String configJson;
}
