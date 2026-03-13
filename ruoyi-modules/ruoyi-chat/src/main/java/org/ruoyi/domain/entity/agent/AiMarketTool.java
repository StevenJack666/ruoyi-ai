package org.ruoyi.domain.entity.agent;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import org.ruoyi.common.mybatis.core.domain.BaseEntity;
/**
 * 市场-工具关联实体
 */
@Data
@TableName("ai_market_tool")
public class AiMarketTool extends BaseEntity { 

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long marketId;

    private Long toolId;
}
