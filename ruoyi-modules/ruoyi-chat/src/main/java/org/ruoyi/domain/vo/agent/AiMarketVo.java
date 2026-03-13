package org.ruoyi.domain.vo.agent;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

import org.ruoyi.domain.entity.agent.AiMarket;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

/**
 * Agent 市场视图对象
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = AiMarket.class)
public class AiMarketVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "主键")
    private Long id;

    @ExcelProperty(value = "市场名称")
    private String marketName;

    @ExcelProperty(value = "描述")
    private String description;

    @ExcelProperty(value = "状态")
    private Integer status;

    @ExcelProperty(value = "编排配置")
    private String configJson;

    @ExcelProperty(value = "备注")
    private String remark;

    @ExcelProperty(value = "创建时间")
    private Date createTime;

    @ExcelProperty(value = "更新时间")
    private Date updateTime;
}
