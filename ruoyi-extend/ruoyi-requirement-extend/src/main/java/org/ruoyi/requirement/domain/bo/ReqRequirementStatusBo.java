package org.ruoyi.requirement.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReqRequirementStatusBo {

    @NotNull(message = "需求ID不能为空")
    private Long id;

    @NotBlank(message = "状态不能为空")
    private String status;

    private String remark;
}
