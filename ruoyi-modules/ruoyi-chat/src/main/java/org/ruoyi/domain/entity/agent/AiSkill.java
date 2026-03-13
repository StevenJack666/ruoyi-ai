package org.ruoyi.domain.entity.agent;

import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 技能实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_skill")
public class AiSkill extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String skillName;

    private String skillCode;

    private String description;
}
