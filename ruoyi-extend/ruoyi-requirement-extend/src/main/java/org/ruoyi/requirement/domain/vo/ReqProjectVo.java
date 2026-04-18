package org.ruoyi.requirement.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.requirement.domain.ReqProject;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import java.io.Serial;
import java.io.Serializable;

@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = ReqProject.class)
public class ReqProjectVo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "主键")
    private Long id;

    @ExcelProperty(value = "项目编码")
    private String projectCode;

    @ExcelProperty(value = "项目名称")
    private String projectName;

    @ExcelProperty(value = "负责人")
    private Long ownerId;

    @ExcelProperty(value = "状态")
    private String status;

    @ExcelProperty(value = "描述")
    private String description;
}
