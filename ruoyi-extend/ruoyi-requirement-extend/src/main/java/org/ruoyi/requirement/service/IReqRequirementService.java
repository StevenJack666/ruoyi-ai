package org.ruoyi.requirement.service;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.requirement.domain.bo.ReqRequirementBo;
import org.ruoyi.requirement.domain.bo.ReqRequirementCommentBo;
import org.ruoyi.requirement.domain.vo.ReqRequirementCommentVo;
import org.ruoyi.requirement.domain.vo.ReqRequirementHistoryVo;
import org.ruoyi.requirement.domain.vo.ReqRequirementVo;

import java.util.Collection;
import java.util.List;

public interface IReqRequirementService {

    ReqRequirementVo queryById(Long id);

    TableDataInfo<ReqRequirementVo> queryPageList(ReqRequirementBo bo, PageQuery pageQuery);

    List<ReqRequirementVo> queryList(ReqRequirementBo bo);

    Boolean insertByBo(ReqRequirementBo bo);

    Boolean updateByBo(ReqRequirementBo bo);

    Boolean changeStatus(Long id, String status, String remark);

    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    Boolean addComment(ReqRequirementCommentBo bo);

    List<ReqRequirementCommentVo> listComments(Long requirementId);

    List<ReqRequirementHistoryVo> listHistory(Long requirementId);
}
