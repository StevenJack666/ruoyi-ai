package org.ruoyi.requirement.service;

import java.util.Collection;
import java.util.List;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.requirement.domain.bo.ReqProjectBugBo;
import org.ruoyi.requirement.domain.vo.ReqProjectBugVo;

public interface IReqProjectBugService {

    ReqProjectBugVo queryById(Long id);

    TableDataInfo<ReqProjectBugVo> queryPageList(ReqProjectBugBo bo, PageQuery pageQuery);

    List<ReqProjectBugVo> queryList(ReqProjectBugBo bo);

    Boolean insertByBo(ReqProjectBugBo bo);

    Boolean updateByBo(ReqProjectBugBo bo);

    Boolean changeStatus(Long id, String status, String remark);

    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
