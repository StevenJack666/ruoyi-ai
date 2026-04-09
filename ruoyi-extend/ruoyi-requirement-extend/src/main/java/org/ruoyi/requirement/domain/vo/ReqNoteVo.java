package org.ruoyi.requirement.domain.vo;

import java.io.Serial;
import java.io.Serializable;

import org.ruoyi.requirement.domain.ReqNote;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = ReqNote.class)
public class ReqNoteVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "主键")
    private Long id;

    @ExcelProperty(value = "标题")
    private String title;

    @ExcelProperty(value = "内容")
    private String content;

    @ExcelProperty(value = "附件数")
    private Integer attachmentCount;
}
