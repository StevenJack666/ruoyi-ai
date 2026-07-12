package org.ruoyi.service.chat.impl;

import cn.dev33.satoken.stp.StpUtil;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.skills.shell.ShellSkills;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.query.Metadata;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


import org.ruoyi.agent.ChartGenerationAgent;
import org.ruoyi.agent.EchartsAgent;
import org.ruoyi.agent.SkillsAgent;
import org.ruoyi.agent.SqlAgent;
import org.ruoyi.agent.WebSearchAgent;
import org.ruoyi.agent.tool.ExecuteSqlQueryTool;
import org.ruoyi.agent.tool.QueryAllTablesTool;
import org.ruoyi.agent.tool.QueryTableSchemaTool;
import org.ruoyi.common.chat.base.ThreadContext;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.dto.request.ReSumeRunner;
import org.ruoyi.common.chat.domain.dto.request.WorkFlowRunner;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.entity.rel.SessionMessageFileRel;
import org.ruoyi.common.chat.enums.RoleType;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.chat.service.chat.IChatService;
import org.ruoyi.common.chat.service.rel.ISessionMessageFileService;
import org.ruoyi.common.chat.service.workFlow.IWorkFlowStarterService;
import org.ruoyi.common.core.utils.ObjectUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.oss.domain.vo.SysOssUploadVo;
import org.ruoyi.common.oss.domain.vo.UploadVo;
import org.ruoyi.common.oss.enums.UploadModeType;
import org.ruoyi.common.oss.factory.UploadServiceFactory;
import org.ruoyi.common.oss.service.IUploadService;
import org.ruoyi.common.redis.utils.RedisUtils;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.sse.core.SseEmitterManager;
import org.ruoyi.common.sse.utils.SseMessageUtils;
import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.domain.entity.knowledge.SessionUploadRecord;
import org.ruoyi.domain.vo.knowledge.KnowledgeInfoVo;
import org.ruoyi.factory.ChatServiceFactory;
import org.ruoyi.factory.ResourceLoaderFactory;
import org.ruoyi.mapper.knowledge.SessionUploadRecordMapper;
import org.ruoyi.mcp.service.core.ToolProviderFactory;
import org.ruoyi.observability.*;
import org.ruoyi.service.chat.AbstractChatService;
import org.ruoyi.service.chat.IChatMessageService;
import org.ruoyi.service.chat.impl.memory.PersistentChatMemoryStore;
import org.ruoyi.service.knowledge.IKnowledgeInfoService;
import org.ruoyi.service.knowledge.ResourceLoader;
import org.ruoyi.service.knowledge.TextSplitter;
import org.ruoyi.service.retrieval.KnowledgeRetrievalService;
import org.ruoyi.service.knowledge.retriever.CustomVectorRetriever;
import org.ruoyi.service.vector.VectorStoreService;
import org.ruoyi.system.service.ISysConfigService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import cn.hutool.core.collection.CollUtil;
import org.ruoyi.common.core.exception.ServiceException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * 聊天服务门面层
 * <p>
 * 作为统一入口，负责：
 * 1. 构建对话上下文
 * 2. 路由到对应的处理器
 *
 * @author ageerle@163.com
 * @date 2025/12/13
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceFacade implements IChatService {

    private static final Integer DEFAULT_MAX_MESSAGES = 20;

    private final IChatModelService chatModelService;

    private final ISysConfigService sysConfigService;

    private final ISessionMessageFileService sessionMessageFileService;

    private final ChatServiceFactory chatServiceFactory;

    private final IKnowledgeInfoService knowledgeInfoService;

    private final VectorStoreService vectorStoreService;

    private final KnowledgeRetrievalService knowledgeRetrievalService;

    private final SseEmitterManager sseEmitterManager;

    private final IChatMessageService chatMessageService;

    private final IWorkFlowStarterService workFlowStarterService;

    private final ToolProviderFactory toolProviderFactory;
    private final ResourceLoaderFactory resourceLoaderFactory;    // 上传文件解析用
    private final TextSplitter textSplitter;                       // 分块用（CharacterTextSplitter）
    // ChatServiceFacade 加两个依赖
    private final UploadServiceFactory uploadServiceFactory;                 // OSS上传
    private final SessionUploadRecordMapper recordMapper;       // 记录写入

    /**
     * 内存实例缓存，避免同一会话重复创建
     * Key: sessionId, Value: MessageWindowChatMemory实例
     */
    private static final Map<Object, MessageWindowChatMemory> memoryCache = new ConcurrentHashMap<>();

    // ChatServiceFacade.java
    // todo 文件上传，应该返回文件列表
    public Long attachSessionFile(MultipartFile file, Long sessionId) {
        Long fileOssId = null;
        String fileName = file.getOriginalFilename();
        String ext = fileName.substring(fileName.lastIndexOf("."));
        long fileSize = file.getSize();
        Long userId = LoginHelper.getUserId();
        log.info("上传文件: fileName={}, fileSize={}", fileName, fileSize);

        // 1. 获取文件字节数组（防止流被消费 导致NoSuchFileException）
        byte[] fileBytes;
        try (InputStream is = file.getInputStream()) {
            fileBytes = is.readAllBytes();
        } catch (IOException e) {
            log.error("会话文档读取失败: fileName={}", fileName, e);
            throw new ServiceException("文件读取失败，请重试");
        }

        // 2. OSS持久化原始文件（用于溯源）
        String actualCode = StringUtils.defaultIfEmpty(initUploadMode(), UploadModeType.DEFAULT.getCode());
        IUploadService uploadService = uploadServiceFactory.getOriginalService(actualCode);
        // 将文件上传成数组
        MultipartFile[] files = {file};
        UploadVo uploadVo = uploadService.upload(files);

        List<SysOssUploadVo> uploadVos = uploadVo.getUploadVos();
        if (CollectionUtils.isEmpty(uploadVos)){
            throw new ServiceException("上传文件信息异常");
        }

        // 3. 写入上传记录（失败不影响主流程，仅记日志）
        for (SysOssUploadVo sysOssUploadVo : uploadVos) {
            try {
                SessionUploadRecord record = new SessionUploadRecord();
                record.setUserId(userId);
                record.setSessionId(sessionId);
                record.setFileName(fileName);
                record.setFileType(ext);
                record.setFileSize(fileSize);
                Long ossId = fileOssId = Long.parseLong(sysOssUploadVo.getOssId());
                record.setOssId(ossId);
                record.setOssUrl(sysOssUploadVo.getUrl());
                recordMapper.insert(record);
            } catch (Exception e) {
                log.error("上传记录写入失败: fileName={}", fileName, e);
                // 不抛异常，不影响后续解析
            }
        }

        // 4. 解析 todo，可以做成异步的，保障前端体验
        ResourceLoader loader = resourceLoaderFactory.getLoaderByFileType(ext);
        String text;
        try (InputStream is = new ByteArrayInputStream(fileBytes)) { // try-with-resources 自动关流
            text = loader.getContent(is);
        } catch (IOException e) {
            log.error("会话文档读取失败: fileName={}", fileName, e);
            throw new ServiceException("文件读取失败，请重试");
        }

        // 5. 分块
        List<String> chunks = loader.getChunkList(text, null);
        if (CollUtil.isEmpty(chunks)) {
            log.warn("会话文档分块为空: fileName={}", fileName);
            return fileOssId;
        }

        // 6. 存储缓存
        try {
            String cacheKey = "session:docs:" + sessionId;
            RedisUtils.deleteObject(cacheKey);
            RedisUtils.setCacheList(cacheKey, chunks);
            RedisUtils.expire(cacheKey, Duration.ofMinutes(30));
        } catch (Exception e) {
            log.error("会话文档缓存失败: sessionId={}", sessionId, e);
            throw new ServiceException("文档缓存失败，请重试");
        }
        return fileOssId;
    }


    /**
     * 统一聊天入口 - SSE流式响应
     *
     * @param chatRequest 聊天请求
     * @return SseEmitter
     */
    public SseEmitter sseChat(ChatRequest chatRequest) {
        // 4. 具体的服务实现
        Long userId = LoginHelper.getUserId();
        String tokenValue = StpUtil.getTokenValue();
        SseEmitter emitter = sseEmitterManager.connect(userId, tokenValue);

        // 1. 根据模型名称查询完整配置
        ChatModelVo chatModelVo = chatModelService.selectModelByName(chatRequest.getModel());
        if (chatModelVo == null) {
            throw new IllegalArgumentException("模型不存在: " + chatRequest.getModel());
        }
        // 处理聊天消息(包含多模态)
        processChatMessage(chatRequest, userId);
        // 2. 构建上下文消息列表
        List<ChatMessage> contextMessages = buildContextMessages(chatRequest);

        chatRequest.setEmitter(emitter);
        chatRequest.setUserId(userId);
        chatRequest.setTokenValue(tokenValue);
        chatRequest.setChatModelVo(chatModelVo);
        chatRequest.setContextMessages(contextMessages);

        // 3. 处理特殊聊天模式（工作流、人机交互恢复、思考模式）
        SseEmitter sseEmitter = handleSpecialChatModes(chatRequest);
        if (sseEmitter != null) {
            return sseEmitter;
        }

        // 4. 路由服务提供商
        String providerCode = chatModelVo.getProviderCode();
        log.info("路由到服务提供商: {}, 模型: {}", providerCode, chatRequest.getModel());
        AbstractChatService chatService = chatServiceFactory.getOriginalService(providerCode);

        StreamingChatResponseHandler handler = createResponseHandler(userId, tokenValue,chatRequest);

        // 5. 发起对话
        StreamingChatModel streamingChatModel = chatService.buildStreamingChatModel(chatModelVo, chatRequest);
        streamingChatModel.chat(contextMessages, handler);
        return emitter;
    }

    /**
     * 处理聊天消息
     */
    private void processChatMessage(ChatRequest chatRequest, Long userId) {
        // 获取是否上传文件
        boolean isUploadFile = chatRequest.getIsUploadFile() != null && chatRequest.getIsUploadFile();
        if (isUploadFile) {
            // 1. 非上传文件场景：保存用户输入的文本消息
            List<Long> ossIds = chatRequest.getOssIds();
            if (CollectionUtils.isEmpty(ossIds)){
                throw new ServiceException("上传的文件信息为空！");
            }
            // 2. 存储消息
            Long messageId = chatMessageService.saveChatMessage(userId, chatRequest.getSessionId(),
                chatRequest.getContent(), RoleType.USER.getName(), chatRequest.getModel());
            if(null == messageId){
                throw new ServiceException("存储消息异常：获取不到消息ID主键");
            }
            // 3. 保存关联对象
            List<SessionMessageFileRel> messageFileList = new ArrayList<>();
            for (Long ossId : ossIds) {
                SessionMessageFileRel sessionMessageFileRel = new SessionMessageFileRel();
                sessionMessageFileRel.setOssFileId(ossId);
                sessionMessageFileRel.setMessageId(messageId);
                sessionMessageFileRel.setSessionId(chatRequest.getSessionId());
                messageFileList.add(sessionMessageFileRel);
            }
            if (!messageFileList.isEmpty()){
                sessionMessageFileService.batchInsert(messageFileList);
            }
            return;
        }
        // 非文件：存储消息
        chatMessageService.saveChatMessage(userId, chatRequest.getSessionId(),
            chatRequest.getContent(), RoleType.USER.getName(), chatRequest.getModel());
    }

    /**
     * 处理特殊聊天模式（工作流、人机交互恢复、思考模式）
     *
     * @param chatRequest      聊天请求
     * @return 如果需要提前返回则返回SseEmitter，否则返回null
     */
    private SseEmitter handleSpecialChatModes(ChatRequest chatRequest) {
        // 处理工作流对话
        if (chatRequest.getEnableWorkFlow()) {
            log.info("处理工作流对话,会话: {}", chatRequest.getSessionId());

            WorkFlowRunner runner = chatRequest.getWorkFlowRunner();
            if (ObjectUtils.isEmpty(runner)) {
                log.warn("工作流参数为空");
            }
            return workFlowStarterService.streaming(
                ThreadContext.getCurrentUser(),
                runner.getUuid(),
                runner.getInputs(),
                chatRequest.getSessionId()
            );
        }

        // 处理人机交互恢复
        if (chatRequest.getIsResume()) {
            log.info("处理人机交互恢复");
            ReSumeRunner reSumeRunner = chatRequest.getReSumeRunner();
            if (ObjectUtils.isEmpty(reSumeRunner)) {
                log.warn("人机交互恢复参数为空");
            }
            workFlowStarterService.resumeFlow(
                reSumeRunner.getRuntimeUuid(),
                reSumeRunner.getFeedbackContent(),
                chatRequest.getEmitter()
            );

            return chatRequest.getEmitter();

        }
        // 处理思考模式
        if (chatRequest.getEnableThinking()) {
           return handleThinkingMode(chatRequest);
        }

        return null;
    }

    /**
     * 处理思考模式
     *
     * @param chatRequest     聊天请求

     */
    private SseEmitter handleThinkingMode(ChatRequest chatRequest) {
        // 配置监督者模型
        OpenAiChatModel plannerModel = OpenAiChatModel.builder()
            .baseUrl(chatRequest.getChatModelVo().getApiHost())
            .apiKey(chatRequest.getChatModelVo().getApiKey())
            .modelName(chatRequest.getChatModelVo().getModelName())
            .build();

        // Bing 搜索 MCP 客户端
        McpTransport bingTransport = new StdioMcpTransport.Builder()
            .command(List.of("C:\\Program Files\\nodejs\\npx.cmd", "-y", "bing-cn-mcp"))
            .logEvents(true)
            .build();

        Long userId = chatRequest.getUserId();
        McpClient bingMcpClient = new DefaultMcpClient.Builder()
            .transport(bingTransport)
            .listener(new MyMcpClientListener(userId))
            .build();

        // Playwright MCP 客户端 - 浏览器自动化工具
        McpTransport playwrightTransport = new StdioMcpTransport.Builder()
            .command(List.of("C:\\Program Files\\nodejs\\npx.cmd", "-y", "@playwright/mcp@latest"))
            .logEvents(true)
            .build();

        McpClient playwrightMcpClient = new DefaultMcpClient.Builder()
            .transport(playwrightTransport)
            .listener(new MyMcpClientListener(userId))
            .build();

        // Filesystem MCP 客户端 - 文件管理工具
        // 允许 AI 读取、写入、搜索文件（基于当前项目根目录）
        String userDir = System.getProperty("user.dir");
        McpTransport filesystemTransport = new StdioMcpTransport.Builder()
            .command(List.of("C:\\Program Files\\nodejs\\npx.cmd", "-y",
                "@modelcontextprotocol/server-filesystem", userDir))
            .logEvents(true)

            .build();

        McpClient filesystemMcpClient = new DefaultMcpClient.Builder()
            .transport(filesystemTransport)
            .listener(new MyMcpClientListener(userId))
            .build();

        // 合并三个 MCP 客户端的工具
        ToolProvider toolProvider = McpToolProvider.builder()
            // bingMcpClient,
            .mcpClients(List.of(playwrightMcpClient, filesystemMcpClient))
            .build();

        // ========== LangChain4j Skills 基本用法 ==========
        // 通过 SKILL.md 文件定义，LLM 按需通过 activate_skill 工具加载
        // 加载 Skills - 使用相对路径，基于项目根目录
        java.nio.file.Path skillsPath = java.nio.file.Path.of(userDir, "ruoyi-admin/src/main/resources/skills");
        List<dev.langchain4j.skills.FileSystemSkill> skillsList = dev.langchain4j.skills.FileSystemSkillLoader
            .loadSkills(skillsPath)
            ;

        ShellSkills skills = ShellSkills.from(skillsList);

        // 构建子 Agent
        WebSearchAgent searchAgent  = AgenticServices.agentBuilder(WebSearchAgent.class)
            .chatModel(plannerModel)
            .toolProvider(toolProvider)
            .listener(new MyAgentListener())
            .build();

        // 构建子 Agent 2: SkillsAgent - 负责文档处理技能（docx、pdf、xlsx）
        // 独立管理 Skills 工具
        SkillsAgent skillsAgent = AgenticServices.agentBuilder(SkillsAgent.class)
            .chatModel(plannerModel)
            .systemMessage("You have access to the following skills:\n" + skills.formatAvailableSkills()
                + "\nWhen the user's request relates to one of these skills, activate it first using the `activate_skill` tool before proceeding.")
            .toolProvider(skills.toolProvider())
            .build();

        // 构建子 Agent 3: SqlAgent - 负责数据库查询
        SqlAgent sqlAgent = AgenticServices.agentBuilder(SqlAgent.class)
            .chatModel(plannerModel)
            .tools(new QueryAllTablesTool(), new QueryTableSchemaTool(), new ExecuteSqlQueryTool())
            .listener(new MyAgentListener())
            .build();

        // 构建子 Agent 4: ChartGenerationAgent - 负责图表生成
        ChartGenerationAgent chartGenerationAgent = AgenticServices.agentBuilder(ChartGenerationAgent.class)
            .chatModel(plannerModel)
            .listener(new MyAgentListener())
            .build();

        // 构建子 Agent 5: EchartsAgent - 负责数据可视化（结合 SQL 查询生成 Echarts 图表）
        EchartsAgent echartsAgent = AgenticServices.agentBuilder(EchartsAgent.class)
            .chatModel(plannerModel)
            .tools(new QueryAllTablesTool(), new QueryTableSchemaTool(), new ExecuteSqlQueryTool())
            .listener(new MyAgentListener())
            .build();

        // 构建监督者 Agent - 管理多个子 Agent
        SupervisorAgent supervisor = AgenticServices.supervisorBuilder()
            .chatModel(plannerModel)
            //.listener(new SupervisorStreamListener(null))
            .subAgents(skillsAgent,searchAgent, sqlAgent, chartGenerationAgent, echartsAgent)
            // 加入历史上下文 - 使用 ChatMemoryProvider 提供持久化的聊天内存
            //.chatMemoryProvider(memoryId -> createChatMemory(chatRequest.getSessionId()))
            .responseStrategy(SupervisorResponseStrategy.LAST)
            .build();

        String tokenValue = chatRequest.getTokenValue();

        // 异步执行 supervisor，避免阻塞 HTTP 请求线程导致 SSE 事件被缓冲
        CompletableFuture.runAsync(() -> {
            try {
                String result = supervisor.invoke(chatRequest.getContent());
                SseMessageUtils.sendContent(userId, result);
                SseMessageUtils.sendDone(userId);
            } catch (Exception e) {
                log.error("Supervisor 执行失败", e);
                SseMessageUtils.sendError(userId, e.getMessage());
            } finally {
                SseMessageUtils.completeConnection(userId, tokenValue);
            }
        });
        return chatRequest.getEmitter();
    }

    /**
     * 支持外部 handler 的对话接口（跨模块调用）
     * 同时发送到 SSE 和外部 handler
     *
     * @param chatRequest     聊天请求
     * @param externalHandler 外部响应处理器（可为 null）
     */
    @Override
    public void chat(ChatRequest chatRequest, StreamingChatResponseHandler externalHandler) {
        // 1. 根据模型名称查询完整配置
        ChatModelVo chatModelVo = chatModelService.selectModelByName(chatRequest.getModel());
        if (chatModelVo == null) {
            throw new IllegalArgumentException("模型不存在: " + chatRequest.getModel());
        }

        // 3. 路由服务提供商
        String providerCode = chatModelVo.getProviderCode();
        log.info("跨模块调用 - 路由到服务提供商: {}, 模型: {}", providerCode, chatRequest.getModel());
        AbstractChatService chatService = chatServiceFactory.getOriginalService(providerCode);

        // 4. 获取用户信息
        Long userId = LoginHelper.getUserId();
        String tokenValue = StpUtil.getTokenValue();

        // 5. 建立 SSE 连接（用于前端监听）
        sseEmitterManager.connect(userId, tokenValue);

        // 保存用户消息
        chatMessageService.saveChatMessage(userId, chatRequest.getSessionId(), chatRequest.getContent(), RoleType.USER.getName(), chatRequest.getModel());

        // 6. 创建组合 handler：同时发送到 SSE 和外部 handler
        StreamingChatResponseHandler combinedHandler = createCombinedHandler(userId, tokenValue, externalHandler);

        // 7. 发起对话
        StreamingChatModel streamingChatModel = chatService.buildStreamingChatModel(chatModelVo, chatRequest);
        streamingChatModel.chat(chatRequest.getContent(), combinedHandler);
    }

    /**
     * 实现接口默认方法 - 不带 handler 的调用
     */
    @Override
    public SseEmitter chat(ChatRequest chatRequest) {
        return sseChat(chatRequest);
    }


    /**
     * 创建或获取聊天内存实例（缓存机制）
     * 同一个会话ID会返回同一个内存实例，避免重复创建和消息丢失
     *
     * @param memoryId 内存ID（会话ID）
     * @return MessageWindowChatMemory实例
     */
    private MessageWindowChatMemory createChatMemory(Object memoryId) {
        // 先从缓存中获取
        return memoryCache.computeIfAbsent(memoryId, key -> {
            try {
                PersistentChatMemoryStore store = new PersistentChatMemoryStore(chatMessageService);
                return MessageWindowChatMemory.builder()
                    .id(memoryId)
                    .maxMessages(DEFAULT_MAX_MESSAGES)
                    .chatMemoryStore(store)
                    .build();
            } catch (Exception e) {
                log.warn("创建聊天内存失败: {}", e.getMessage());
                return null;
            }
        });
    }


    /**
     * 构建上下文消息列表
     * 消息顺序：历史消息 → 当前用户消息（确保 AI 正确理解对话上下文）
     *
     * @param chatRequest 聊天请求
     * @return 上下文消息列表
     */
    private List<ChatMessage> buildContextMessages(ChatRequest chatRequest) {
        List<ChatMessage> messages = new ArrayList<>();
        // 1. 初始化当前用户消息
        UserMessage userMessage = UserMessage.userMessage(chatRequest.getContent());
        // 2. 会话文档检索（上传消息全量注入，追问走关键词检索）
        String sessionDocContext = null;
        if (chatRequest.getSessionId() != null) {
            String cacheKey = "session:docs:" + chatRequest.getSessionId();
            List<String> chunks = RedisUtils.getCacheList(cacheKey);
            if (!CollUtil.isEmpty(chunks)) {
                boolean isUpload = chatRequest.getIsUploadFile() != null && chatRequest.getIsUploadFile();
                if (isUpload) {
                    // 上传消息：全量注入，确保模型能完整分析文档
                    sessionDocContext = buildSessionContext(chunks);
                    log.info("会话文档全量注入: sessionId={}, 分块数={}", chatRequest.getSessionId(), chunks.size());
                } else {
                    // 追问消息：关键词检索
                    List<String> matchedChunks = searchSessionDocs(
                        chatRequest.getSessionId(), chatRequest.getContent());
                    if (!CollUtil.isEmpty(matchedChunks)) {
                        sessionDocContext = buildSessionContext(matchedChunks);
                    }
                }
            }
        }





        // 3. 知识库检索增强 (RAG)
        if (chatRequest.getKnowledgeId() != null) {
            KnowledgeInfoVo knowledgeInfoVo = knowledgeInfoService.queryById(Long.valueOf(chatRequest.getKnowledgeId()));
            if (knowledgeInfoVo != null) {

                // todo 校验当前用户是否有权限访问该知识库
                ChatModelVo chatModel = chatModelService.selectModelByName(knowledgeInfoVo.getEmbeddingModel());
                if (chatModel != null) {
                    log.info("执行高级 RAG 流程: kid={}", chatRequest.getKnowledgeId());

                    // 构建自定义检索器
                    CustomVectorRetriever retriever = new CustomVectorRetriever(
                            knowledgeRetrievalService, knowledgeInfoVo, chatModel);

                    // 构建增强流水线
                    RetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
                            .contentRetriever(retriever)
                            .build();

                    // 执行增强：编织上下文到 UserMessage
                    Metadata metadata = Metadata.from(userMessage, chatRequest.getSessionId(), new ArrayList<>());
                    AugmentationRequest augmentationRequest = new AugmentationRequest(userMessage, metadata);
                    AugmentationResult result = augmentor.augment(augmentationRequest);

                    ChatMessage augmented = result.chatMessage();
                    if (augmented instanceof UserMessage) {
                        userMessage = (UserMessage) augmented;
                        log.debug("RAG 增强完成，UserMessage 已注入背景知识");
                    }
                }
            }
        }

        // 4. 从数据库查询历史对话消息（放在前面）
        if (chatRequest.getSessionId() != null) {
            MessageWindowChatMemory memory = createChatMemory(chatRequest.getSessionId());
            if (memory != null) {
                List<ChatMessage> historicalMessages = memory.messages();
                if (historicalMessages != null && !historicalMessages.isEmpty()) {
                    messages.addAll(historicalMessages);
                    log.debug("已加载 {} 条历史消息用于会话 {}", historicalMessages.size(), chatRequest.getSessionId());
                }
            }
        }

        // 5. 末尾合并会话文档上下文
        if (sessionDocContext != null) {
            // 取出当前 userMessage 的文本（可能已被 RAG 增强过）
            String currentText = userMessage.singleText();
            // 判断是否已被 RAG 增强（包含原始问题之外的额外内容）
            boolean hasKnowledgeContext = !currentText.equals(chatRequest.getContent());

            if (hasKnowledgeContext) {
                // 知识库 + 会话文档 都存在 → 三合一
                String merged = sessionDocContext + "\n\n" + currentText;
                userMessage = UserMessage.userMessage(merged);
            } else {
                // 仅会话文档
                String merged = sessionDocContext + "\n\n用户问题：" + chatRequest.getContent();
                userMessage = UserMessage.userMessage(merged);
            }
        }


        // 6. 添加经过增强的用户消息（放在最后）
        messages.add(userMessage);

        return messages;
    }

    /**
     * 构建向量查询参数
     */
    private QueryVectorBo buildQueryVectorBo(ChatRequest chatRequest, KnowledgeInfoVo knowledgeInfoVo,
                                             ChatModelVo chatModel) {
        QueryVectorBo queryVectorBo = new QueryVectorBo();
        queryVectorBo.setQuery(chatRequest.getContent());
        queryVectorBo.setKid(chatRequest.getKnowledgeId());
        queryVectorBo.setApiKey(chatModel.getApiKey());
        queryVectorBo.setBaseUrl(chatModel.getApiHost());
        queryVectorBo.setVectorModelName(knowledgeInfoVo.getVectorModel());
        queryVectorBo.setEmbeddingModelName(knowledgeInfoVo.getEmbeddingModel());
        queryVectorBo.setMaxResults(knowledgeInfoVo.getRetrieveLimit());

        // 设置重排序参数
        queryVectorBo.setEnableRerank(knowledgeInfoVo.getEnableRerank() != null && knowledgeInfoVo.getEnableRerank() == 1);
        queryVectorBo.setRerankModelName(knowledgeInfoVo.getRerankModel());
        queryVectorBo.setRerankTopN(knowledgeInfoVo.getRerankTopN());
        queryVectorBo.setRerankScoreThreshold(knowledgeInfoVo.getRerankScoreThreshold());

        return queryVectorBo;
    }

    /**
     * 创建标准的响应处理器
     *
     * @param userId      用户ID
     * @param tokenValue  会话令牌
     * @return 标准的流式响应处理器
     */
    protected StreamingChatResponseHandler createResponseHandler(Long userId, String tokenValue,ChatRequest chatRequest) {
        return new StreamingChatResponseHandler() {

            private final StringBuilder messageBuffer = new StringBuilder();

            @SneakyThrows
            @Override
            public void onPartialResponse(String partialResponse) {
                // 将消息片段追加到缓冲区
                messageBuffer.append(partialResponse);

                // 实时发送内容事件到客户端
                SseMessageUtils.sendContent(userId, partialResponse);
                log.debug("收到消息片段: {}",  partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                try {
                    // 发送完成事件
                    SseMessageUtils.sendDone(userId);

                    // 消息流完成，保存消息到数据库和内存
                    String fullMessage = messageBuffer.toString();

                    if (fullMessage.isEmpty()) {
                          log.warn("接收到空消息");
                    } else {
                        // 保存助手回复消息
                        chatMessageService.saveChatMessage(userId, chatRequest.getSessionId(), fullMessage, RoleType.ASSISTANT.getName(), chatRequest.getModel());
                    }

                    // 关闭SSE连接
                    SseMessageUtils.completeConnection(userId, tokenValue);
                     log.info("消息结束，已保存到数据库");
                } catch (Exception e) {
                      log.error("完成响应时出错: {}", e.getMessage(), e);
                }
            }

            @Override
            public void onError(Throwable error) {
                // 发送错误事件
                SseMessageUtils.sendError(userId, error.getMessage());
                log.error("流式响应错误: {}", error.getMessage());
            }
        };
    }

    /**
     * 创建组合响应处理器 - 同时发送到 SSE 和外部 handler
     *
     * @param userId          用户ID
     * @param tokenValue      会话令牌
     * @param externalHandler 外部响应处理器（可为 null）
     * @return 组合的流式响应处理器
     */
    protected StreamingChatResponseHandler createCombinedHandler(Long userId, String tokenValue,
                                                                  StreamingChatResponseHandler externalHandler) {
        return new StreamingChatResponseHandler() {

            private final StringBuilder messageBuffer = new StringBuilder();

            @SneakyThrows
            @Override
            public void onPartialResponse(String partialResponse) {
                // 1. 追加到缓冲区
                messageBuffer.append(partialResponse);

                // 2. 发送内容事件到 SSE（前端可通过 SSE 监听）
                SseMessageUtils.sendContent(userId, partialResponse);

                // 3. 转发给外部 handler（Workflow 等模块可处理）
                if (externalHandler != null) {
                    externalHandler.onPartialResponse(partialResponse);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                try {
                    // 1. 发送完成事件
                    SseMessageUtils.sendDone(userId);

                    // 2. 关闭 SSE 连接
                    SseMessageUtils.completeConnection(userId, tokenValue);

                    // 3. 转发给外部 handler
                    if (externalHandler != null) {
                        externalHandler.onCompleteResponse(completeResponse);
                    }
                } catch (Exception e) {
                    log.error("完成响应时出错: {}", e.getMessage(), e);
                }
            }

            @Override
            public void onError(Throwable error) {
                // 发送错误事件
                SseMessageUtils.sendError(userId, error.getMessage());
                log.error("流式响应错误: {}", error.getMessage(), error);

                // 转发给外部 handler
                if (externalHandler != null) {
                    externalHandler.onError(error);
                }
            }
        };
    }

    /**
     * 搜索当前会话上传的文档，返回匹配的关键分块
     */
     /**
      * 搜索当前会话上传的文档，返回匹配的关键分块
      * 无关键词命中时降级为全量返回（最多20条）
      */
    private List<String> searchSessionDocs(Long sessionId, String query) {
         String cacheKey = "session:docs:" + sessionId;
         List<String> chunks = RedisUtils.getCacheList(cacheKey);
         if (chunks == null || chunks.isEmpty()) {
             return Collections.emptyList();
         }

         // 将用户问题分词
         Set<String> queryTokens = Arrays.stream(query.split("[，。、；：？！\\s]+"))
                 .filter(t -> t.length() >= 2)
                 .collect(Collectors.toSet());

         List<String> matched;
         if (queryTokens.isEmpty()) {
             matched = new ArrayList<>(chunks);
         } else {
             // 按命中关键词数排序
             matched = chunks.stream()
                     .map(chunk -> {
                         long hits = queryTokens.stream().filter(chunk::contains).count();
                         return Map.entry(chunk, hits);
                     })
                     .filter(e -> e.getValue() > 0)
                     .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                     .map(Map.Entry::getKey)
                     .collect(Collectors.toList());
         }

         // 无命中时降级返回全量，防超大文档加安全上限
         if (matched.isEmpty() || matched.size() < 5) {
             return chunks.size() > 20 ? chunks.subList(0, 20) : chunks;
         }

         return matched.stream().limit(5).collect(Collectors.toList());
    }


    /**
     * 将检索到的分块拼接为 LLM 可读的上下文
     */
    private String buildSessionContext(List<String> chunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户在本轮对话中上传了以下文档内容，如果用户问题与这些内容相关，请基于文档回答：\n");
        for (int i = 0; i < chunks.size(); i++) {
            sb.append("---\n").append(chunks.get(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * 上传模式
     * @return
     */
    private String initUploadMode(){
        return sysConfigService.selectConfigByKey("upload.mode");
    }
}

