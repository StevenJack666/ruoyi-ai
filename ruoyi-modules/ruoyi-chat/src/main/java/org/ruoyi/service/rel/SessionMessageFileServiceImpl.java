package org.ruoyi.service.rel;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.entity.rel.SessionMessageFile;
import org.ruoyi.common.chat.service.rel.ISessionMessageFileService;
import org.ruoyi.mapper.rel.SessionMessageFileMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class SessionMessageFileServiceImpl implements ISessionMessageFileService {

    private final SessionMessageFileMapper baseMapper;

    @Override
    public Long insert(SessionMessageFile sessionMessageFile) {
        baseMapper.insert(sessionMessageFile);
        return sessionMessageFile.getId();
    }

    @Override
    public List<SessionMessageFile> selectByMessageId(Long messageId) {
        LambdaQueryWrapper<SessionMessageFile> lqw = Wrappers.lambdaQuery();
        lqw.eq(SessionMessageFile::getMessageId, messageId);
        return baseMapper.selectList(lqw);
    }

    @Override
    public void batchInsert(List<SessionMessageFile> sessionMessageFileList) {
        if (sessionMessageFileList == null || sessionMessageFileList.isEmpty()) {
            return;
        }
        for (SessionMessageFile sessionMessageFile : sessionMessageFileList) {
            baseMapper.insert(sessionMessageFile);
        }
    }
}
