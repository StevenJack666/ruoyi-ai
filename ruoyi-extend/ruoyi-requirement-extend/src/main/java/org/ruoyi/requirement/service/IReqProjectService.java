package org.ruoyi.requirement.service;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.requirement.domain.bo.ReqProjectBo;
import org.ruoyi.requirement.domain.vo.ReqProjectVo;

import java.util.Collection;
import java.util.List;

public interface IReqProjectService {

    ReqProjectVo queryById(Long id);

    TableDataInfo<ReqProjectVo> queryPageList(ReqProjectBo bo, PageQuery pageQuery);

    List<ReqProjectVo> queryList(ReqProjectBo bo);

    Boolean insertByBo(ReqProjectBo bo);

    Boolean updateByBo(ReqProjectBo bo);

    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
