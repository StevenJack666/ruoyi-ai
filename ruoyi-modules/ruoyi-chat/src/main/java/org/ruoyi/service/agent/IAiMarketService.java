package org.ruoyi.service.agent;

import java.util.Collection;
import java.util.List;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.domain.bo.agent.AiMarketBo;
import org.ruoyi.domain.vo.agent.AiMarketVo;

/**
 * Agent 市场服务接口
 */
public interface IAiMarketService {

    AiMarketVo queryById(Long id);

    TableDataInfo<AiMarketVo> queryPageList(AiMarketBo bo, PageQuery pageQuery);

    List<AiMarketVo> queryList(AiMarketBo bo);

    Boolean insertByBo(AiMarketBo bo);

    Boolean updateByBo(AiMarketBo bo);

    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
