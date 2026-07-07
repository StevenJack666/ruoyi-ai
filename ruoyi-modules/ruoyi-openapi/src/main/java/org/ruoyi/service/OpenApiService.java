package org.ruoyi.service;

import org.ruoyi.common.chat.domain.dto.vulnerabilities.OpenApiChatRequest;

public interface OpenApiService {
    /**
     * 调用内部对话
     * @param chatRequest
     * @return
     */
    Object openChat(OpenApiChatRequest chatRequest);
}
