package org.ruoyi.requirement.domain.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

import org.ruoyi.requirement.domain.ReqProjectBug;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = ReqProjectBug.class)
public class ReqProjectBugVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "主键")
    private Long id;

    @ExcelProperty(value = "项目ID")
    private Long projectId;

    @ExcelProperty(value = "Bug编码")
    private String bugCode;

    @ExcelProperty(value = "标题")
    private String title;

    @ExcelProperty(value = "严重程度")
    private String severity;

    @ExcelProperty(value = "优先级")
    private String priority;

    @ExcelProperty(value = "状态")
    private String status;

    @ExcelProperty(value = "负责人")
    private Long ownerId;

    @ExcelProperty(value = "指派人")
    private Long assigneeId;

    @ExcelProperty(value = "发现版本")
    private String foundVersion;

    @ExcelProperty(value = "修复版本")
    private String fixedVersion;

    @ExcelProperty(value = "复现步骤")
    private String reproduceSteps;

    @ExcelProperty(value = "预期结果")
    private String expectedResult;

    @ExcelProperty(value = "实际结果")
    private String actualResult;

    @ExcelProperty(value = "解决时间")
    private Date resolvedTime;
}
