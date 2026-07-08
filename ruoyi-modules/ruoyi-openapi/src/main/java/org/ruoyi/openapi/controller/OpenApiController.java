package org.ruoyi.openapi.controller;

import jakarta.validation.Valid;
import org.ruoyi.common.chat.domain.dto.request.OpenApiChatRequest;
import org.ruoyi.service.OpenApiService;
import org.ruoyi.common.core.domain.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * OpenApi 对外暴露信息Controller
 *
 * @author admin
 * @date 2024-10-08
 */
@RestController
@RequestMapping("/openapi")
public class OpenApiController {

    @Autowired
    private OpenApiService openApiService;

    /**
     * 调用内部对话接口 TODO 流控
     */
    @PostMapping("/chat")
    public R<?> chat(@RequestBody @Valid OpenApiChatRequest chatRequest) {
        return R.ok(openApiService.openChat(chatRequest));
    }
}
