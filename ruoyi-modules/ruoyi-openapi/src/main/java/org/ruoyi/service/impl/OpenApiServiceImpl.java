package org.ruoyi.service.impl;

import org.ruoyi.common.chat.domain.dto.vulnerabilities.OpenApiChatRequest;
import org.ruoyi.common.chat.service.chat.IChatService;
import org.ruoyi.service.OpenApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OpenApiServiceImpl implements OpenApiService {

    @Autowired
    private IChatService chatService;

    /**
     * 调用内部对话接口
     * @param chatRequest 对话请求对象
     * @return
     */
    @Override
    public Object openChat(OpenApiChatRequest chatRequest) {
        return chatService.openChat(chatRequest);
    }
}
