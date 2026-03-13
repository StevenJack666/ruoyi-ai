package org.ruoyi.domain.vo.agent;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

import org.ruoyi.domain.entity.agent.AiSkill;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

/**
 * Agent 技能视图对象
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = AiSkill.class)
public class AiSkillVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "主键")
    private Long id;

    @ExcelProperty(value = "技能名称")
    private String skillName;

    @ExcelProperty(value = "技能编码")
    private String skillCode;

    @ExcelProperty(value = "描述")
    private String description;

    @ExcelProperty(value = "备注")
    private String remark;

    @ExcelProperty(value = "创建时间")
    private Date createTime;

    @ExcelProperty(value = "更新时间")
    private Date updateTime;
}
