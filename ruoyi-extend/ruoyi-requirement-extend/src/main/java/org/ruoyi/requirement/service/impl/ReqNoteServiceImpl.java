package org.ruoyi.requirement.service.impl;

import java.util.Collection;
import java.util.List;

import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.requirement.domain.ReqNote;
import org.ruoyi.requirement.domain.ReqNoteAttachment;
import org.ruoyi.requirement.domain.bo.ReqNoteBo;
import org.ruoyi.requirement.domain.vo.ReqNoteAttachmentVo;
import org.ruoyi.requirement.domain.vo.ReqNoteVo;
import org.ruoyi.requirement.mapper.ReqNoteAttachmentMapper;
import org.ruoyi.requirement.mapper.ReqNoteMapper;
import org.ruoyi.requirement.service.IReqNoteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ReqNoteServiceImpl implements IReqNoteService {

    private final ReqNoteMapper noteMapper;
    private final ReqNoteAttachmentMapper attachmentMapper;

    @Override
    public ReqNoteVo queryById(Long id) {
        return noteMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<ReqNoteVo> queryPageList(ReqNoteBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ReqNote> lqw = buildQueryWrapper(bo);
        Page<ReqNoteVo> result = noteMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<ReqNoteVo> queryList(ReqNoteBo bo) {
        return noteMapper.selectVoList(buildQueryWrapper(bo));
    }

    @Override
    public Boolean insertByBo(ReqNoteBo bo) {
        ReqNote add = MapstructUtils.convert(bo, ReqNote.class);
        if (add.getAttachmentCount() == null) {
            add.setAttachmentCount(0);
        }
        boolean ok = noteMapper.insert(add) > 0;
        if (ok) {
            bo.setId(add.getId());
        }
        return ok;
    }

    @Override
    public Boolean updateByBo(ReqNoteBo bo) {
        ReqNote current = noteMapper.selectById(bo.getId());
        if (current == null) {
            throw new ServiceException("记事不存在");
        }
        ReqNote update = MapstructUtils.convert(bo, ReqNote.class);
        return noteMapper.updateById(update) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        attachmentMapper.delete(Wrappers.<ReqNoteAttachment>lambdaQuery().in(ReqNoteAttachment::getNoteId, ids));
        return noteMapper.deleteByIds(ids) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReqNoteAttachmentVo uploadAttachment(Long noteId, String fileUrl) {
        ReqNote note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new ServiceException("记事不存在");
        }
        if (StringUtils.isBlank(fileUrl)) {
            throw new ServiceException("附件地址不能为空");
        }
        ReqNoteAttachment attachment = new ReqNoteAttachment();
        attachment.setNoteId(noteId);
        attachment.setFileUrl(fileUrl.trim());
        attachmentMapper.insert(attachment);
        refreshAttachmentCount(noteId);
        return attachmentMapper.selectVoById(attachment.getId());
    }

    @Override
    public List<ReqNoteAttachmentVo> listAttachments(Long noteId) {
        return attachmentMapper.selectVoList(Wrappers.<ReqNoteAttachment>lambdaQuery()
            .eq(ReqNoteAttachment::getNoteId, noteId)
            .orderByDesc(ReqNoteAttachment::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean removeAttachment(Long attachmentId) {
        ReqNoteAttachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new ServiceException("附件不存在");
        }
        boolean ok = attachmentMapper.deleteById(attachmentId) > 0;
        if (ok) {
            refreshAttachmentCount(attachment.getNoteId());
        }
        return ok;
    }

    private LambdaQueryWrapper<ReqNote> buildQueryWrapper(ReqNoteBo bo) {
        LambdaQueryWrapper<ReqNote> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getTitle()), ReqNote::getTitle, bo.getTitle());
        lqw.orderByDesc(ReqNote::getCreateTime);
        return lqw;
    }

    private void refreshAttachmentCount(Long noteId) {
        long count = attachmentMapper.selectCount(Wrappers.<ReqNoteAttachment>lambdaQuery()
            .eq(ReqNoteAttachment::getNoteId, noteId));
        ReqNote update = new ReqNote();
        update.setId(noteId);
        update.setAttachmentCount((int) count);
        noteMapper.updateById(update);
    }
}
