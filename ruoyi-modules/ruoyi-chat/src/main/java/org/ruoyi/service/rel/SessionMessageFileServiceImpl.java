package org.ruoyi.service.rel;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.entity.rel.SessionMessageFileRel;
import org.ruoyi.common.chat.service.rel.ISessionMessageFileService;
import org.ruoyi.domain.entity.knowledge.SessionUploadRecord;
import org.ruoyi.mapper.rel.SessionMessageFileMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class SessionMessageFileServiceImpl implements ISessionMessageFileService {

    private final SessionMessageFileMapper baseMapper;

    @Override
    public Long insert(SessionMessageFileRel sessionMessageFileRel) {
        baseMapper.insert(sessionMessageFileRel);
        return sessionMessageFileRel.getId();
    }

    @Override
    public List<SessionMessageFileRel> selectByMessageId(Long messageId) {
        LambdaQueryWrapper<SessionMessageFileRel> lqw = Wrappers.lambdaQuery();
        lqw.eq(SessionMessageFileRel::getMessageId, messageId);
        return baseMapper.selectList(lqw);
    }

    @Override
    public void batchInsert(List<SessionMessageFileRel> sessionMessageFileRelList) {
        if (sessionMessageFileRelList == null || sessionMessageFileRelList.isEmpty()) {
            return;
        }
        for (SessionMessageFileRel sessionMessageFileRel : sessionMessageFileRelList) {
            baseMapper.insert(sessionMessageFileRel);
        }
    }

    @Override
    public List<Long> selectOssIdsBySessionId(Long sessionId) {
        LambdaQueryWrapper<SessionMessageFileRel> lqw = Wrappers.lambdaQuery();
        lqw.eq(SessionMessageFileRel::getSessionId, sessionId);
        List<SessionMessageFileRel> sessionMessageFileRelList = baseMapper.selectList(lqw);
        if (CollUtil.isEmpty(sessionMessageFileRelList)){
            return List.of();
        }
        return sessionMessageFileRelList.stream().map(SessionMessageFileRel::getOssFileId).toList();
    }
}
