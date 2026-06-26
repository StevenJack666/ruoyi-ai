package org.ruoyi.common.chat.service.rel;

import org.ruoyi.common.chat.entity.rel.SessionMessageFile;

import java.util.List;

/**
 * 会话消息文件Service接口
 *
 * @author ageerle
 * @date 2025-12-14
 */
public interface ISessionMessageFileService {

    /**
     * 存储
     * @param sessionMessageFile
     * @return
     */
    Long insert(SessionMessageFile sessionMessageFile);

    /**
     * 根据消息ID查询关联数据
     * @param messageId
     * @return
     */
    List<SessionMessageFile> selectByMessageId(Long messageId);

    /**
     * 批量存储数据
     * @param sessionMessageFileList
     */
    void batchInsert(List<SessionMessageFile> sessionMessageFileList);
}
