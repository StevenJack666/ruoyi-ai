package org.ruoyi.service.chat.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import org.ruoyi.common.chat.domain.bo.chat.ChatMessageBo;
import org.ruoyi.common.chat.domain.vo.MimeTypeUtils;
import org.ruoyi.common.chat.domain.vo.chat.ChatMessageVo;
import org.ruoyi.common.chat.domain.vo.file.FileInfoVo;
import org.ruoyi.common.chat.entity.chat.ChatMessage;
import org.ruoyi.common.chat.entity.rel.SessionMessageFileRel;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.domain.entity.knowledge.SessionUploadRecord;
import org.ruoyi.mapper.knowledge.SessionUploadRecordMapper;
import org.ruoyi.mapper.rel.SessionMessageFileMapper;
import org.ruoyi.service.chat.IChatMessageService;
import org.springframework.stereotype.Service;
import org.ruoyi.mapper.chat.ChatMessageMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 聊天消息Service业务层处理
 *
 * @author ageerle
 * @date 2025-12-14
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ChatMessageServiceImpl implements IChatMessageService {

    private final ChatMessageMapper baseMapper;

    private final SessionMessageFileMapper sessionMessageFileMapper;

    private final SessionUploadRecordMapper sessionUploadRecordMapper;

    /**
     * 查询聊天消息
     *
     * @param id 主键
     * @return 聊天消息
     */
    @Override
    public ChatMessageVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询聊天消息列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 聊天消息分页列表
     */
    @Override
    public TableDataInfo<ChatMessageVo> queryPageList(ChatMessageBo bo, PageQuery pageQuery) {
        // 1. 分页查询消息
        LambdaQueryWrapper<ChatMessage> lqw = buildLambdaQueryWrapper(bo);
        Page<ChatMessageVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);

        // 【卫语句】如果没有数据，直接返回，避免后续无意义的判断
        if (result == null || CollectionUtils.isEmpty(result.getRecords())) {
            return TableDataInfo.build(result);
        }

        List<ChatMessageVo> records = result.getRecords();
        List<Long> messageIds = records.stream().map(ChatMessageVo::getId).toList();

        // 2. 批量查询关联关系 (1次SQL)
        List<SessionMessageFileRel> allMessageFiles = sessionMessageFileMapper.selectList(
            new LambdaQueryWrapper<SessionMessageFileRel>()
                .in(SessionMessageFileRel::getMessageId, messageIds)
        );
        if (CollectionUtils.isEmpty(allMessageFiles)) {
            return TableDataInfo.build(result); // 无关联文件，直接返回
        }

        // 3. 批量查询文件详情 (1次SQL)
        List<Long> ossIds = allMessageFiles.stream()
            .map(SessionMessageFileRel::getOssFileId)
            .distinct() // 去重，防止同一个文件被多次查询
            .toList();

        List<SessionUploadRecord> uploadRecords = sessionUploadRecordMapper.selectList(
            new LambdaQueryWrapper<SessionUploadRecord>()
                .in(SessionUploadRecord::getOssId, ossIds)
        );

        // 4. 将上传记录转换为 Map，方便 O(1) 时间复杂度查找
        Map<Long, SessionUploadRecord> recordMap = uploadRecords.stream()
            .collect(Collectors.toMap(SessionUploadRecord::getOssId, Function.identity(), (a, b) -> a));

        // 5. 按 MessageId 分组关联关系，并填充到消息中
        Map<Long, List<SessionMessageFileRel>> filesGroupByMsgId = allMessageFiles.stream()
            .collect(Collectors.groupingBy(SessionMessageFileRel::getMessageId));

        for (ChatMessageVo record : records) {
            List<SessionMessageFileRel> msgFiles = filesGroupByMsgId.get(record.getId());
            if (CollectionUtils.isEmpty(msgFiles)) continue; // 该消息无文件，跳过

            List<FileInfoVo> fileInfoVoList = msgFiles.stream()
                .map(f -> recordMap.get(f.getOssFileId())) // 从 Map 中获取
                .filter(Objects::nonNull)                  // 过滤掉可能不存在的记录
                .map(uploadRecord -> {                     // 转换并组装 VO
                    FileInfoVo vo = new FileInfoVo();
                    vo.setName(uploadRecord.getFileName());
                    vo.setType(MimeTypeUtils.getMimeType(uploadRecord.getFileType()));
                    vo.setFileSize(String.valueOf(uploadRecord.getFileSize()));
                    vo.setUrl(uploadRecord.getOssUrl());
                    return vo;
                })
                .toList();

            record.setFileList(fileInfoVoList);
        }

        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的聊天消息列表
     *
     * @param bo 查询条件
     * @return 聊天消息列表
     */
    @Override
    public List<ChatMessageVo> queryList(ChatMessageBo bo) {
        LambdaQueryWrapper<ChatMessage> lqw = buildLambdaQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<ChatMessage> buildLambdaQueryWrapper(ChatMessageBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<ChatMessage> lqw = Wrappers.lambdaQuery();
        lqw.orderByAsc(ChatMessage::getId);
        lqw.eq(bo.getSessionId() != null, ChatMessage::getSessionId, bo.getSessionId());
        lqw.eq(bo.getUserId() != null, ChatMessage::getUserId, bo.getUserId());
        lqw.eq(StringUtils.isNotBlank(bo.getContent()), ChatMessage::getContent, bo.getContent());
        lqw.eq(StringUtils.isNotBlank(bo.getRole()), ChatMessage::getRole, bo.getRole());
        lqw.eq(bo.getTotalTokens() != null, ChatMessage::getTotalTokens, bo.getTotalTokens());
        lqw.like(StringUtils.isNotBlank(bo.getModelName()), ChatMessage::getModelName, bo.getModelName());
        return lqw;
    }

    private QueryWrapper<ChatMessage> buildQueryWrapperWithAlias(ChatMessageBo bo) {
        QueryWrapper<ChatMessage> qw = Wrappers.query();
        qw.eq(bo.getSessionId() != null, "cm.session_id", bo.getSessionId());
        qw.eq(bo.getUserId() != null, "cm.user_id", bo.getUserId());
        qw.eq(StringUtils.isNotBlank(bo.getContent()), "cm.content", bo.getContent());
        qw.eq(StringUtils.isNotBlank(bo.getRole()), "cm.role", bo.getRole());
        qw.eq(bo.getTotalTokens() != null, "cm.total_tokens", bo.getTotalTokens());
        qw.like(StringUtils.isNotBlank(bo.getModelName()), "cm.model_name", bo.getModelName());
        return qw;
    }

    /**
     * 新增聊天消息
     *
     * @param bo 聊天消息
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(ChatMessageBo bo) {
        ChatMessage add = MapstructUtils.convert(bo, ChatMessage.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改聊天消息
     *
     * @param bo 聊天消息
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(ChatMessageBo bo) {
        ChatMessage update = MapstructUtils.convert(bo, ChatMessage.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(ChatMessage entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除聊天消息信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    /**
     * 根据会话ID获取所有消息
     * 用于长期记忆功能
     *
     * @param sessionId 会话ID
     * @return 消息DTO列表
     */
    @Override
    public List<dev.langchain4j.data.message.ChatMessage> getMessagesBySessionId(Long sessionId) {
        if (sessionId == null) {
            return new java.util.ArrayList<>();
        }

        List<dev.langchain4j.data.message.ChatMessage> chatMessageList = new ArrayList<>();
        ChatMessageBo bo = new ChatMessageBo();
        bo.setSessionId(sessionId);
        List<ChatMessageVo> voList = queryList(bo);

        for (ChatMessageVo chatMessageVo : voList) {
            switch (chatMessageVo.getRole()) {
                case "user" -> chatMessageList.add(UserMessage.from(chatMessageVo.getContent()));
                case "assistant" -> chatMessageList.add(AiMessage.from(chatMessageVo.getContent()));
            }
        }
        return chatMessageList;
    }



    /**
     * 根据会话ID删除所有消息
     * 用于清理会话历史
     *
     * @param sessionId 会话ID
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteBySessionId(Long sessionId) {
        if (sessionId == null) {
            return false;
        }

        LambdaQueryWrapper<ChatMessage> lqw = Wrappers.lambdaQuery();
        lqw.eq(ChatMessage::getSessionId, sessionId);
        return baseMapper.delete(lqw) > 0;
    }

    /**
     * 保存聊天消息
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @param content   消息内容
     * @param role      角色类型
     * @param modelName 模型名称
     */
    @Override
    public Long saveChatMessage(Long userId, Long sessionId, String content, String role, String modelName) {
        try {
            if (userId == null) {
                log.warn("缺少用户ID，无法保存消息");
                return null;
            }

            ChatMessageBo messageBo = new ChatMessageBo();
            messageBo.setUserId(userId);
            messageBo.setSessionId(sessionId);
            messageBo.setContent(content);
            messageBo.setRole(role);
            messageBo.setModelName(modelName);

            insertByBo(messageBo);
            log.debug("保存聊天消息成功，角色: {}, 会话: {}", role, sessionId);
            return messageBo.getId();
        } catch (Exception e) {
            log.error("保存聊天消息时出错: {}", e.getMessage(), e);
            return null;
        }
    }
}
