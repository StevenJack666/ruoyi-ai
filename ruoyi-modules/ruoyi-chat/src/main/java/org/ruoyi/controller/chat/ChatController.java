package org.ruoyi.controller.chat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.service.chat.impl.ChatServiceFacade;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.ruoyi.common.core.domain.R;


/**
 * 聊天管理
 *
 * @author ageerle@163.com
 * @date 2023-03-01
 */
@Tag(name = "聊天管理", description = "聊天会话相关接口")
@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final ChatServiceFacade chatService;

    /**
     * 会话文档上传（仅本次对话有效，Redis缓存，30分钟过期）
     */
    @Operation(summary = "上传会话文档", description = "上传文档到指定会话，文档缓存30分钟后自动过期")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public R<Void> uploadSessionFile(
            @Parameter(description = "上传的文件", required = true) @RequestParam MultipartFile file,
            @Parameter(description = "会话ID", required = true) @RequestParam Long sessionId) {
        // TODO: 具体逻辑委托给 service 层
       chatService.attachSessionFile(file, sessionId);
        return R.ok("");
    }


    /**
     * 聊天接口
     */
    @PostMapping("/send")
    @ResponseBody
    public SseEmitter sseChat(@RequestBody @Valid ChatRequest chatRequest) {
        return chatService.sseChat(chatRequest);
    }

}
