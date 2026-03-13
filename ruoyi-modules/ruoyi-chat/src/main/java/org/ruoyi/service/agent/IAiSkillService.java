package org.ruoyi.service.agent;

import java.util.Collection;
import java.util.List;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.domain.bo.agent.AiSkillBo;
import org.ruoyi.domain.vo.agent.AiSkillVo;

/**
 * Agent 技能服务接口
 */
public interface IAiSkillService {

    AiSkillVo queryById(Long id);

    TableDataInfo<AiSkillVo> queryPageList(AiSkillBo bo, PageQuery pageQuery);

    List<AiSkillVo> queryList(AiSkillBo bo);

    Boolean insertByBo(AiSkillBo bo);

    Boolean updateByBo(AiSkillBo bo);

    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
