package org.ruoyi.domain.bo.agent;

import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.domain.entity.agent.AiMarket;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Agent 市场业务对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = AiMarket.class, reverseConvertGenerate = false)
public class AiMarketBo extends BaseEntity {

    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
    private Long id;

    @NotBlank(message = "市场名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String marketName;

    private String description;

    private Integer status;

    private String configJson;

    private String remark;

    private List<String> toolIds;

    private List<String> skillIds;
}
