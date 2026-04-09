package org.ruoyi.requirement.service;

import java.util.Collection;
import java.util.List;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.requirement.domain.bo.ReqNoteBo;
import org.ruoyi.requirement.domain.vo.ReqNoteAttachmentVo;
import org.ruoyi.requirement.domain.vo.ReqNoteVo;

public interface IReqNoteService {

    ReqNoteVo queryById(Long id);

    TableDataInfo<ReqNoteVo> queryPageList(ReqNoteBo bo, PageQuery pageQuery);

    List<ReqNoteVo> queryList(ReqNoteBo bo);

    Boolean insertByBo(ReqNoteBo bo);

    Boolean updateByBo(ReqNoteBo bo);

    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    ReqNoteAttachmentVo uploadAttachment(Long noteId, String fileUrl);

    List<ReqNoteAttachmentVo> listAttachments(Long noteId);

    Boolean removeAttachment(Long attachmentId);
}
