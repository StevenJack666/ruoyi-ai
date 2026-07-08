package org.ruoyi.service;

import org.ruoyi.common.chat.domain.dto.request.OpenApiChatRequest;
import org.ruoyi.common.chat.domain.response.OpenApiResponse;

public interface OpenApiService {
    /**
     * 调用内部对话
     * @param chatRequest
     * @return
     */
    OpenApiResponse openChat(OpenApiChatRequest chatRequest);
}
