package org.ruoyi.domain.entity.agent;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 市场-技能关联实体
 */
@Data
@TableName("ai_market_skill")
public class AiMarketSkill {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long marketId;

    private Long skillId;
}
