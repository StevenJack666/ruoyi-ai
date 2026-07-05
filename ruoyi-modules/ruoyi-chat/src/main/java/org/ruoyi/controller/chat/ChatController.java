package org.ruoyi.controller.chat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.domain.dto.ToolConfirmRequest;
import org.ruoyi.mcp.service.core.ToolConfirmationManager;
import org.ruoyi.service.chat.impl.ChatServiceFacade;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


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
    private final ToolConfirmationManager toolConfirmationManager;

    /**
     * 会话文档上传（仅本次对话有效，Redis缓存，30分钟过期）
     */
    @Operation(summary = "上传会话文档", description = "上传文档到指定会话，文档缓存30分钟后自动过期")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public R<Long> uploadSessionFile(
            @Parameter(description = "上传的文件", required = true) @RequestParam MultipartFile file,
            @Parameter(description = "会话ID", required = true) @RequestParam Long sessionId) {
        // TODO: 具体逻辑委托给 service 层
        return R.ok(chatService.attachSessionFile(file, sessionId));
    }


    /**
     * 聊天接口
     */
    @PostMapping("/send")
    @ResponseBody
    public SseEmitter sseChat(@RequestBody @Valid ChatRequest chatRequest) {
        return chatService.sseChat(chatRequest);
    }


    /**
     * 工具调用确认/拒绝
     */
    @Operation(summary = "工具调用确认", description = "用户同意或拒绝 AI 的工具调用请求")
    @PostMapping("/tool-confirm")
    @ResponseBody
    public R<Void> toolConfirm(@RequestBody ToolConfirmRequest request) {
        boolean success = toolConfirmationManager.respond(request.getConfirmId(), request.isApproved());
        log.info("用户{}工具调用: confirmId={}", request.isApproved() ? "同意" : "拒绝", request.getConfirmId());
        return success ? R.ok() : R.fail("确认请求不存在或已过期");
    }

}
