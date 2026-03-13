package org.ruoyi.domain.entity.agent;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 市场-工具关联实体
 */
@Data
@TableName("ai_market_tool")
public class AiMarketTool {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long marketId;

    private Long toolId;
}
