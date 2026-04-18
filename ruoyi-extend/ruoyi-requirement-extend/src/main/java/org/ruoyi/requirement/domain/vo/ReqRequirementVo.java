package org.ruoyi.requirement.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.requirement.domain.ReqRequirement;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import java.io.Serial;
import java.util.Date;

@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = ReqRequirement.class)
public class ReqRequirementVo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "主键")
    private Long id;

    @ExcelProperty(value = "项目ID")
    private Long projectId;

    @ExcelProperty(value = "需求编码")
    private String reqCode;

    @ExcelProperty(value = "标题")
    private String title;

    @ExcelProperty(value = "类型")
    private String type;

    @ExcelProperty(value = "优先级")
    private String priority;

    @ExcelProperty(value = "状态")
    private String status;

    @ExcelProperty(value = "负责人")
    private Long ownerId;

    @ExcelProperty(value = "指派人")
    private Long assigneeId;

    @ExcelProperty(value = "来源")
    private String source;

    @ExcelProperty(value = "计划开始")
    private Date planStartTime;

    @ExcelProperty(value = "计划结束")
    private Date planEndTime;

    @ExcelProperty(value = "内容")
    private String content;

    private String flowCode;

    private Long processInstanceId;
}
