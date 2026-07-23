package org.ruoyi.service.chat.impl;

import cn.dev33.satoken.stp.StpUtil;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.skills.FileSystemSkill;
import dev.langchain4j.skills.FileSystemSkillLoader;
import dev.langchain4j.skills.shell.ShellSkills;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


import org.ruoyi.agent.*;
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
import org.ruoyi.config.McpSseConfig;
import org.ruoyi.config.agent.SkillsPathResolver;
import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.domain.entity.knowledge.SessionUploadRecord;
import org.ruoyi.domain.vo.agent.AgentVo;
import org.ruoyi.domain.vo.knowledge.KnowledgeInfoVo;
import org.ruoyi.factory.ChatServiceFactory;
import org.ruoyi.factory.ResourceLoaderFactory;
import org.ruoyi.mapper.knowledge.SessionUploadRecordMapper;
import org.ruoyi.mcp.service.core.LangChain4jMcpToolProviderService;
import org.ruoyi.mcp.service.core.ToolProviderFactory;
import org.ruoyi.observability.*;
import org.ruoyi.service.agent.IAgentService;
import org.ruoyi.service.chat.AbstractChatService;
import org.ruoyi.service.chat.FileParseAsyncService;
import org.ruoyi.service.chat.IChatMessageService;
import org.ruoyi.service.chat.impl.memory.PersistentChatMemoryStore;
import org.ruoyi.service.knowledge.IKnowledgeInfoService;
import org.ruoyi.service.knowledge.TextSplitter;
import org.ruoyi.service.retrieval.KnowledgeRetrievalService;
import org.ruoyi.service.knowledge.retriever.CustomVectorRetriever;
import org.ruoyi.service.vector.VectorStoreService;
import org.ruoyi.system.service.ISysConfigService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import cn.hutool.core.collection.CollUtil;
import org.ruoyi.common.core.exception.ServiceException;

import java.io.IOException;
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
    private final SessionUploadRecordMapper recordMapper;
    private final McpSseConfig mcpSseConfig;
    // Redis上传文件的前缀Key
    private final String UPLOAD_REDIS_PREFIX_KEY = "session:docs:oss:";
    private final FileParseAsyncService fileParseAsyncService;
    private final IAgentService agentService;
    private final LangChain4jMcpToolProviderService langChain4jMcpToolProviderService;

    /**
     * 内存实例缓存，避免同一会话重复创建
     * Key: sessionId, Value: MessageWindowChatMemory实例
     */
    private static final Map<Object, MessageWindowChatMemory> memoryCache = new ConcurrentHashMap<>();

    // ChatServiceFacade.java
    // todo 文件上传，应该返回文件列表
    public List<Long> attachSessionFile(MultipartFile[] fileList, Long sessionId) {
        if (fileList == null || fileList.length == 0) {
            throw new ServiceException("上传的文件列表为空");
        }

        List<Long> fileOssIds = new ArrayList<>();
        Long userId = LoginHelper.getUserId();

        String actualCode = StringUtils.defaultIfEmpty(initUploadMode(), UploadModeType.DEFAULT.getCode());
        IUploadService uploadService = uploadServiceFactory.getOriginalService(actualCode);

        for (MultipartFile file : fileList) {
            Long ossId = processSingleFile(file, uploadService, sessionId, userId);
            fileOssIds.add(ossId);
        }
        return fileOssIds;
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

        // 0. 智能体解析：传入 agentId 时按智能体绑定的模型覆盖 model 字段
        //    （前端默认走智能体；enableThinking 不再作为对话模式开关，Supervisor 多 Agent 编排成为默认智能体路径）
        AgentVo agentVo = null;
        if (chatRequest.getAgentId() != null) {
            agentVo = agentService.queryById(chatRequest.getAgentId());
            if (agentVo != null && agentVo.getModelId() != null) {
                ChatModelVo agentModel = chatModelService.queryById(agentVo.getModelId());
                if (agentModel != null) {
                    chatRequest.setModel(agentModel.getModelName());
                }
            } else {
                log.warn("智能体不存在或未配置模型，回退到 model 字段: agentId={}", chatRequest.getAgentId());
            }
        }

        // 1. 根据模型名称查询完整配置
        ChatModelVo chatModelVo = chatModelService.selectModelByName(chatRequest.getModel());
        if (chatModelVo == null) {
            throw new IllegalArgumentException("模型不存在: " + chatRequest.getModel());
        }

        // 2. 构建上下文消息列表
        List<ChatMessage> contextMessages = buildContextMessages(chatRequest, agentVo);
        chatRequest.setEmitter(emitter);
        chatRequest.setUserId(userId);
        chatRequest.setTokenValue(tokenValue);
        chatRequest.setChatModelVo(chatModelVo);
        chatRequest.setContextMessages(contextMessages);

        // 处理聊天消息(包含多模态)
        processChatMessage(chatRequest, userId);

        // 3. 路由对话模式：工作流对话 / 智能体对话（两者均返回各自的 SseEmitter）
        return handleSpecialChatModes(chatRequest, agentVo);
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
    private SseEmitter handleSpecialChatModes(ChatRequest chatRequest, AgentVo agentVo) {
        // 处理工作流对话
        if (Boolean.TRUE.equals(chatRequest.getEnableWorkFlow())) {
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
        if (Boolean.TRUE.equals(chatRequest.getIsResume())) {
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

        // 模式2：智能体对话（默认走 Supervisor 多 Agent 编排）
        return handleAgentChat(chatRequest, agentVo);
    }

    /**
     * 智能体对话模式（默认）：构建 Supervisor 多 Agent 编排并异步执行，结果通过 SSE 推送。
     *
     * @param chatRequest 聊天请求
     * @param agentVo    智能体配置（可为 null，无智能体时用请求 model 兜底）
     */
    private SseEmitter handleAgentChat(ChatRequest chatRequest, AgentVo agentVo) {
        ChatModelVo chatModelVo = chatRequest.getChatModelVo();

        // 配置监督者模型：统一按 providerCode 走对应 AbstractChatService.buildChatModel，
        // 兼容 ZhiPu/QianWen/Ollama/Dify/Coze/CustomApi 等非 OpenAI 协议；默认实现为 OpenAI 兼容。
        AbstractChatService chatService = chatServiceFactory.getOriginalService(chatModelVo.getProviderCode());
        ChatModel plannerModel = chatService.buildChatModel(chatModelVo);

        Long userId = chatRequest.getUserId();

        // 工具装配：智能体有关联工具ID时按ID装配，否则回退到原有硬编码 MCP 客户端
        ToolProvider toolProvider;
        if (agentVo != null && agentVo.getMcpToolIds() != null && !agentVo.getMcpToolIds().isEmpty()) {
            toolProvider = langChain4jMcpToolProviderService.getToolProvider(agentVo.getMcpToolIds());
        } else {
            toolProvider = buildDefaultMcpToolProvider(userId);
        }

        // 结合工具获取拦截器
        ToolProvider interceptedToolProvider = (toolProviderRequest) -> {
            // 获取 MCP 提供的所有工具
            ToolProviderResult mcpResult = toolProvider.provideTools(toolProviderRequest);
            ToolProviderResult.Builder builder = ToolProviderResult.builder();
            if (mcpResult != null && mcpResult.tools() != null) {
                // 遍历所有 MCP 工具
                for (Map.Entry<ToolSpecification, ToolExecutor> entry : mcpResult.tools().entrySet()) {
                    ToolSpecification spec = entry.getKey();
                    ToolExecutor originalExecutor = entry.getValue();
                    ToolExecutor wrappedExecutor = new GobalToolExecutor(originalExecutor, userId);
                    builder.add(spec, wrappedExecutor);
                }
            }
            return builder.build();
        };

        // 1. 【核心】获取纯净的上下文消息列表（内部已包含：系统提示词 + 历史对话 + 原始提问）
        List<ChatMessage> contextMessages = chatRequest.getContextMessages();
        // 2. 检索会话文档（从原 buildContextMessages 中抽离出来的逻辑）
        String sessionDocContext = retrieveSessionDocContext(chatRequest);
        // 3. 知识库增强：智能体绑定了知识库时，对 supervisor 输入做一次 RAG 增强（全程唯一一次检索）
        String knowledgeContext = augmentAgentInput(chatRequest, agentVo);
        // 4. 文档和知识库的注入
        injectContextEnhancements(contextMessages, sessionDocContext, knowledgeContext, chatRequest.getContent());
        // 5. 获取历史上下文
        String prompt = formatHistoryMessages(contextMessages);

        // 动态获取子 Agent 列表（永远包含兜底 Agent + 可选的自定义 Agent）
        List<Object> subAgents = buildDynamicSubAgents(agentVo, plannerModel, interceptedToolProvider, sessionDocContext);

        // 构建监督者 Agent - 管理多个子 Agent
        SupervisorAgent supervisor = AgenticServices.supervisorBuilder()
            .chatModel(plannerModel)
            .subAgents(subAgents.toArray())
            .maxAgentsInvocations(3)
            .supervisorContext("你是一个极其严格的任务路由专家，必须按照以下【绝对优先级】和【全局规则】执行任务分配，绝不允许越权：\n" +
                "\n【全局规则：工具调用权限】" +
                "工具（Tools）是极其危险且消耗资源的操作。" +
                "1. 只有【动态自定义智能体】或【文档解析专家】在执行复杂任务时，才允许调用工具。" +
                "2. 【兜底闲聊助手】绝对禁止调用任何工具！" +
                "3. 你自己（Supervisor）只负责路由，绝对禁止自己直接调用工具！\n" +

                "\n【优先级 1：专属业务接管（最高统治权）】" +
                "如果当前存在【动态自定义智能体】，且该智能体配置了专属的系统提示词，" +
                "那么该智能体拥有最高统治权！无论用户的请求是什么，都必须无条件派发给它！" +
                "(注意：如果用户的请求需要调用工具、解析文档或执行复杂任务，该智能体将自行决定并调用工具，你无需干预)\n" +

                "\n【优先级 2：文档处理】" +
                "仅当【动态自定义智能体】不存在，且用户的请求涉及解析、提取、总结刚刚上传的文档内容时，" +
                "必须将其派发给【文档解析专家】！" +
                "(注意：你必须将完整的文档内容作为 documentContext 参数传递给该专家)\n" +

                "\n【优先级 3：兜底闲聊】" +
                "仅当以上两者都不适用，且用户的请求是极其简单的日常问候或闲聊时，才允许派发给【兜底闲聊助手】。" +
                "(警告：闲聊任务绝对不允许触发任何工具调用！)")
            .responseStrategy(SupervisorResponseStrategy.LAST).build();

        String tokenValue = chatRequest.getTokenValue();

        // 异步执行 supervisor，避免阻塞 HTTP 请求线程导致 SSE 事件被缓冲
        CompletableFuture.runAsync(() -> {
            try {
                String result = supervisor.invoke(prompt);
                SseMessageUtils.sendContent(userId, result);
                SseMessageUtils.sendDone(userId);
                // 保存助手回复到数据库（智能体对话为默认路径后，需在此落库以保留历史）
                if (StringUtils.isNotBlank(result)) {
                    chatMessageService.saveChatMessage(userId, chatRequest.getSessionId(),
                        result, RoleType.ASSISTANT.getName(), chatRequest.getModel());
                }
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
     * 兜底 MCP 工具装配（无智能体时使用，保留原有 3 个硬编码客户端逻辑）
     */
    private ToolProvider buildDefaultMcpToolProvider(Long userId) {
        String npxCommand = resolveNpxCommand();
        McpTransport playwrightTransport = new StdioMcpTransport.Builder()
            .command(List.of(npxCommand, "-y", "@playwright/mcp@latest"))
            .logEvents(true)
            .build();
        McpClient playwrightMcpClient = new DefaultMcpClient.Builder()
            .transport(playwrightTransport)
            .listener(new MyMcpClientListener(userId))
            .build();

        String userDir = System.getProperty("user.dir");
        McpTransport filesystemTransport = new StdioMcpTransport.Builder()
            .command(List.of(npxCommand, "-y",
                "@modelcontextprotocol/server-filesystem", userDir))
            .logEvents(true)
            .build();
        McpClient filesystemMcpClient = new DefaultMcpClient.Builder()
            .transport(filesystemTransport)
            .listener(new MyMcpClientListener(userId))
            .build();

        return McpToolProvider.builder()
            .mcpClients(List.of(playwrightMcpClient, filesystemMcpClient))
            .build();
    }

    private String resolveNpxCommand() {
        String configured = System.getProperty("mcp.npx.command");
        if (StringUtils.isNotBlank(configured)) return configured;
        String fromEnv = System.getenv("MCP_NPX_COMMAND");
        if (StringUtils.isNotBlank(fromEnv)) return fromEnv;
        return System.getProperty("os.name", "").toLowerCase().contains("win") ? "npx.cmd" : "npx";
    }

    /**
     * 装配磁盘 ShellSkills：智能体勾选了技能名时按名过滤，否则加载全部。
     * 无 skills 时返回 null（调用方据此跳过 SkillsAgent 的 toolProvider 注入）
     */
    private ShellSkills buildShellSkills(AgentVo agentVo) {
        java.nio.file.Path skillsPath = SkillsPathResolver.resolveSkillsPath();
        List<FileSystemSkill> skillsList = FileSystemSkillLoader.loadSkills(skillsPath);
        if (skillsList == null || skillsList.isEmpty()) {
            return null;
        }
        if (agentVo != null && agentVo.getSkillNames() != null && !agentVo.getSkillNames().isEmpty()) {
            skillsList = skillsList.stream()
                .filter(s -> agentVo.getSkillNames().contains(s.name()))
                .toList();
            if (skillsList.isEmpty()) {
                return null;
            }
        }
        return ShellSkills.from(skillsList);
    }

    /**
     * 智能体对话下的输入增强：智能体绑定知识库时，对原始 content 做多知识库 RAG 增强。
     * 无知识库时原样返回 content。
     */
    private String augmentAgentInput(ChatRequest chatRequest, AgentVo agentVo) {
        String content = chatRequest.getContent();
        List<Long> knowledgeIds = collectKnowledgeIds(chatRequest, agentVo);
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return content;
        }
        try {
            RetrievalAugmentor augmentor = buildMultiKnowledgeAugmentor(knowledgeIds);
            if (augmentor == null) {
                return content;
            }
            UserMessage userMessage = UserMessage.userMessage(content);
            Metadata metadata = Metadata.from(userMessage, chatRequest.getSessionId(), new ArrayList<>());
            AugmentationResult result = augmentor.augment(new AugmentationRequest(userMessage, metadata));
            ChatMessage augmented = result.chatMessage();
            return augmented instanceof UserMessage ? ((UserMessage) augmented).singleText() : content;
        } catch (Exception e) {
            log.warn("智能体对话 RAG 增强失败，回退原始输入: {}", e.getMessage());
            return content;
        }
    }

    /**
     * 汇总本次对话要检索的知识库ID列表：智能体绑定的 knowledgeIds 优先，回退到请求的 knowledgeId
     */
    private List<Long> collectKnowledgeIds(ChatRequest chatRequest, AgentVo agentVo) {
        if (agentVo != null && agentVo.getKnowledgeIds() != null && !agentVo.getKnowledgeIds().isEmpty()) {
            return agentVo.getKnowledgeIds();
        }
        if (StringUtils.isNotBlank(chatRequest.getKnowledgeId())) {
            try {
                return List.of(Long.valueOf(chatRequest.getKnowledgeId()));
            } catch (NumberFormatException ignored) {
            }
        }
        return List.of();
    }

    /**
     * 构建多知识库复合检索增强器。
     * 单知识库直接用 DefaultRetrievalAugmentor + CustomVectorRetriever；
     * 多知识库用一个复合 ContentRetriever 合并各库检索结果。
     */
    private RetrievalAugmentor buildMultiKnowledgeAugmentor(List<Long> knowledgeIds) {
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return null;
        }
        List<ContentRetriever> retrievers = new ArrayList<>();
        for (Long kid : knowledgeIds) {
            try {
                KnowledgeInfoVo kb = knowledgeInfoService.queryById(kid);
                if (kb == null) {
                    continue;
                }
                // 校验知识库是否公开
                Integer share = kb.getShare();
                boolean isPublic = share != null && share != 0L;
                if (!isPublic){
                    continue;
                }
                ChatModelVo embModel = chatModelService.selectModelByName(kb.getEmbeddingModel());
                if (embModel == null) {
                    log.warn("知识库向量模型未配置或不存在: kid={}, embeddingModel={}", kid, kb.getEmbeddingModel());
                    continue;
                }
                retrievers.add(new CustomVectorRetriever(knowledgeRetrievalService, kb, embModel));
            } catch (Exception e) {
                log.warn("构建知识库检索器失败: kid={}, err={}", kid, e.getMessage());
            }
        }
        if (retrievers.isEmpty()) {
            return null;
        }
        // 单库直接返回；多库用复合检索器
        ContentRetriever composite = retrievers.size() == 1
            ? retrievers.getFirst()
            : new CompositeContentRetriever(retrievers);
        return DefaultRetrievalAugmentor.builder()
            .contentRetriever(composite)
            .build();
    }

    /**
     * 复合内容检索器：对多个知识库检索器并发查询并合并结果
     */
    private static class CompositeContentRetriever implements ContentRetriever {
        private final List<ContentRetriever> delegates;

        CompositeContentRetriever(List<ContentRetriever> delegates) {
            this.delegates = delegates;
        }

        @Override
        public List<Content> retrieve(Query query) {
            List<CompletableFuture<List<Content>>> futures = delegates.stream()
                .map(r -> CompletableFuture.supplyAsync(() -> {
                    try {
                        List<Content> part = r.retrieve(query);
                        return part == null ? List.<Content>of() : part;
                    } catch (Exception e) {
                        log.warn("复合检索子检索器异常: {}", e.getMessage());
                        return List.<Content>of();
                    }
                })).toList();
            Map<String, Content> unique = new LinkedHashMap<>();
            for (CompletableFuture<List<Content>> future : futures) {
                for (Content content : future.join()) {
                    String key = content.textSegment().metadata().getString("kid") + "|"
                        + content.textSegment().metadata().getString("docId") + "|"
                        + content.textSegment().metadata().getString("fid");
                    if (key.endsWith("null|null|null")) key = content.textSegment().text();
                    unique.putIfAbsent(key, content);
                }
            }
            List<Content> bounded = new ArrayList<>();
            int chars = 0;
            for (Content content : unique.values()) {
                int next = content.textSegment().text().length();
                if (bounded.size() >= 20 || chars + next > 24000) break;
                bounded.add(content);
                chars += next;
            }
            return bounded;
        }
    }

    /**
     * 注入上下文增强内容（会话文档 + 知识库）
     * 核心逻辑：将文档作为 AiMessage 插入，将知识库与用户提问融合后作为 UserMessage 追加。
     * 确保大模型看到的最终结构绝对干净，不会出现重复提问。
     */
    private void injectContextEnhancements(List<ChatMessage> contextMessages,
                                           String sessionDocContext,
                                           String knowledgeContext,
                                           String originalContent) {

        // 1. 如果有文档，将列表末尾的原始提问移除，替换为文档内容
        if (StringUtils.isNotBlank(sessionDocContext)) {
            contextMessages.removeLast();
            contextMessages.addLast(AiMessage.from(sessionDocContext));
        }

        // 2. 注入知识库（统一处理最终提问）
        String finalQuestion;
        if (knowledgeContext.equals(originalContent)) {
            finalQuestion = originalContent; // 没检索到知识，用原问题
        } else {
            finalQuestion = knowledgeContext + "\n\n用户问题：" + originalContent; // 检索到了，拼接
        }

        // 3. 根据列表末尾的消息类型，决定是“替换”还是“追加”
        ChatMessage lastMsg = contextMessages.getLast();
        if (lastMsg instanceof UserMessage) {
            contextMessages.removeLast();
        }

        // 4. 统一加回最终提问
        contextMessages.addLast(UserMessage.userMessage(finalQuestion));
    }

    /**
     * 将上下文消息格式化为多轮对话文本（供只接受 String 输入的 Supervisor 使用）。
     * 跳过 SystemMessage（系统提示词单独前置）与最后一条当前用户消息（单独做 RAG 增强后拼接）。
     */
    private String formatHistoryMessages(List<ChatMessage> contextMessages) {
        if (contextMessages == null || contextMessages.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : contextMessages) {
            if (msg instanceof SystemMessage sysMsg) {
                sb.append(sysMsg.text()).append("\n\n");
            } else if (msg instanceof UserMessage userMsg) {
                sb.append("用户: ").append(userMsg.singleText()).append("\n");
            } else if (msg instanceof AiMessage aiMsg) {
                sb.append("助手: ").append(aiMsg.text()).append("\n");
            } else {
                // 兜底防御：遇到未知消息类型直接跳过，防止大模型看到 Java 对象内存地址
                log.debug("跳过非文本消息类型: {}", msg.getClass().getSimpleName());
            }
        }
        return sb.toString().trim();
    }

    /**
     * 处理思考模式
     *
     * @param chatRequest     聊天请求
     */
    private SseEmitter handleThinkingMode(ChatRequest chatRequest) {
        // 配置监督者模型
        QwenChatModel plannerModel = QwenChatModel.builder()
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

        List<McpClient> mcpClients = new ArrayList<>();
        mcpClients.add(playwrightMcpClient);
        mcpClients.add(filesystemMcpClient);

        boolean mcpSseConfigEnabled = mcpSseConfig.isEnabled();
        if (mcpSseConfigEnabled){
            // HTTP MCP远程客户端 TODO 远程连接可关闭
            McpTransport httpMcpTransport = StreamableHttpMcpTransport.builder()
                .url(mcpSseConfig.getUrl())
                .logRequests(true)
                .logResponses(true)
                .build();

            McpClient httpMcpClient = new DefaultMcpClient.Builder()
                .transport(httpMcpTransport)
                .listener(new MyMcpClientListener(userId))
                .build();

            mcpClients.add(httpMcpClient);
        }

        // 合并四个 MCP 客户端的工具
        ToolProvider toolProvider = McpToolProvider.builder()
            // bingMcpClient,
            .mcpClients(mcpClients)
            .build();

        // 结合工具获取拦截器
        ToolProvider interceptedToolProvider = (toolProviderRequest) -> {
            // 获取 MCP 提供的所有工具
            ToolProviderResult mcpResult = toolProvider.provideTools(toolProviderRequest);
            ToolProviderResult.Builder builder = ToolProviderResult.builder();
            if (mcpResult != null && mcpResult.tools() != null) {
                // 遍历所有 MCP 工具
                for (Map.Entry<ToolSpecification, ToolExecutor> entry : mcpResult.tools().entrySet()) {
                    ToolSpecification spec = entry.getKey();
                    ToolExecutor originalExecutor = entry.getValue();
                    ToolExecutor wrappedExecutor = new GobalToolExecutor(originalExecutor, userId);
                    builder.add(spec, wrappedExecutor);
                }
            }
            return builder.build();
        };

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

        // 构建子 Agent 6: HttpMcpAgent - 负责远程连接调用工具
        HttpMcpAgent httpMcpAgent = AgenticServices.agentBuilder(HttpMcpAgent.class)
            .chatModel(plannerModel)
            .toolProvider(interceptedToolProvider)
            .build();

        // 构建监督者 Agent - 管理多个子 Agent
        SupervisorAgent supervisor = AgenticServices.supervisorBuilder()
            .chatModel(plannerModel)
            //.listener(new SupervisorStreamListener(null))
            .subAgents(skillsAgent,searchAgent, sqlAgent, chartGenerationAgent, echartsAgent, httpMcpAgent)
            // 加入历史上下文 - 使用 ChatMemoryProvider 提供持久化的聊天内存
//            .chatMemoryProvider(memoryId -> createChatMemory(chatRequest.getSessionId()))
            .responseStrategy(SupervisorResponseStrategy.LAST)
            .build();

        String tokenValue = chatRequest.getTokenValue();

        // 异步执行 supervisor，避免阻塞 HTTP 请求线程导致 SSE 事件被缓冲
        CompletableFuture.runAsync(() -> {
            try {
                String result = supervisor.invoke(chatRequest.getContent());
                SseMessageUtils.sendContent(userId, result);
                SseMessageUtils.sendDone(userId);
                // 持久化DB
                if (StringUtils.isNotEmpty(result)){
                    chatMessageService.saveChatMessage(userId, chatRequest.getSessionId(),
                        result, RoleType.ASSISTANT.getName(), chatRequest.getModel());
                }
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
    private List<ChatMessage> buildContextMessages(ChatRequest chatRequest, AgentVo agentVo) {
        List<ChatMessage> messages = new ArrayList<>();
        // 0. 智能体自定义系统提示词（普通对话今天无 SystemMessage，这里新增注入点）
        if (agentVo != null && StringUtils.isNotBlank(agentVo.getSystemPrompt())) {
            messages.add(SystemMessage.from(agentVo.getSystemPrompt()));
        }
        // 1. 初始化当前用户消息
        UserMessage userMessage = UserMessage.userMessage(chatRequest.getContent());

        // 2. 从数据库查询历史对话消息（放在前面）
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

        // 3. 添加用户消息
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
    private List<String> searchSessionDocs(List<String> chunks, String query) {
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
     * 既然这部分内容会作为 AiMessage 注入，文案应采用“助手汇报”的口吻
     */
    private String buildSessionContext(List<String> chunks) {
        StringBuilder sb = new StringBuilder();
        // 1. 采用助手口吻，表明这是助手已经掌握的背景知识
        sb.append("【已读取并解析用户上传的参考文档，核心内容如下】\n");
        // 2. 遍历分块，使用清晰的分隔符，帮助大模型区分段落
        for (int i = 0; i < chunks.size(); i++) {
            sb.append("---\n").append(chunks.get(i)).append("\n");
        }
        // 3. 增加结尾标记，并给出明确的行动指令
        sb.append("---\n【文档读取完毕。我已掌握上述内容，请随时根据用户的最新提问进行解答】");
        return sb.toString();
    }

    /**
     * 上传模式
     */
    private String initUploadMode(){
        return sysConfigService.selectConfigByKey("upload.mode");
    }

    /**
     * 获取默认知识库ID
     */
    private String initKnowledgeId(){
        return sysConfigService.selectConfigByKey("knowledge.default.id");
    }

    /**
     * 处理单个文件的完整链路（上传、记录、异步解析）
     */
    private Long processSingleFile(MultipartFile file, IUploadService uploadService, Long sessionId, Long userId) {
        String fileName = file.getOriginalFilename();
        String ext = fileName.substring(fileName.lastIndexOf("."));
        long fileSize = file.getSize();
        log.info("上传文件: fileName={}, fileSize={}", fileName, fileSize);

        // 1. 获取文件字节数组（防止流被消费导致 NoSuchFileException）
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            log.error("会话文档读取失败: fileName={}", fileName, e);
            throw new ServiceException("文件读取失败，请重试");
        }

        // 2. 上传文件
        MultipartFile[] files = {file};
        UploadVo uploadVo = uploadService.upload(files);
        List<SysOssUploadVo> uploadVos = uploadVo.getUploadVos();
        if (CollectionUtils.isEmpty(uploadVos)) {
            throw new ServiceException("上传文件信息异常");
        }

        SysOssUploadVo sysOssUploadVo = uploadVos.getFirst();
        Long ossId = Long.parseLong(sysOssUploadVo.getOssId());

        // 3. 写入上传记录
        try {
            SessionUploadRecord record = new SessionUploadRecord();
            record.setUserId(userId);
            record.setSessionId(sessionId);
            record.setFileName(fileName);
            record.setFileType(ext);
            record.setFileSize(fileSize);;
            record.setOssId(ossId);
            record.setOssUrl(sysOssUploadVo.getUrl());
            recordMapper.insert(record);
        } catch (Exception e) {
            log.error("上传记录写入失败: fileName={}, ossId={}", fileName, ossId, e);
        }

        // 4. 异步解析与分块（保障前端上传体验）
        fileParseAsyncService.asyncParseAndCache(ossId, ext, fileBytes);
        return ossId;
    }

    /**
     * 解析上传文档内容
     */
    private String retrieveSessionDocContext(ChatRequest chatRequest){
        StringBuilder docContextBuilder = new StringBuilder();
        List<Long> ossIds = chatRequest.getOssIds();
        // 如果是追问（没有带新文件），则从当前会话的历史记录中获取已绑定的文件
        if (CollectionUtils.isEmpty(ossIds) && chatRequest.getSessionId() != null) {
            ossIds = sessionMessageFileService.selectOssIdsBySessionId(chatRequest.getSessionId());
        }
        // 上传文件操作
        if (CollUtil.isNotEmpty(ossIds)) {
            boolean isUpload = Boolean.TRUE.equals(chatRequest.getIsUploadFile());
            for (Long ossId : ossIds) {
                if (ossId == null) continue;
                String cacheKey = UPLOAD_REDIS_PREFIX_KEY + ossId;
                List<String> chunks = RedisUtils.getCacheList(cacheKey);
                if (CollUtil.isNotEmpty(chunks)) {
                    if (isUpload) {
                        // 上传消息：全量注入，确保模型能完整分析新文档
                        docContextBuilder.append(buildSessionContext(chunks));
                        log.info("会话文档全量注入: ossId={}, 分块数={}", ossId, chunks.size());
                    } else {
                        // 追问消息：基于关键词检索（RAG 模式），节省 Token
                        List<String> matchedChunks = searchSessionDocs(chunks, chatRequest.getContent());
                        if (!CollUtil.isEmpty(matchedChunks)) {
                            docContextBuilder.append(buildSessionContext(matchedChunks));
                        }
                    }
                }
            }
        }
        return docContextBuilder.isEmpty() ? null : docContextBuilder.toString();
    }

    /**
     * 根据 AgentVo 动态构建子 Agent 列表
     */
    private List<Object> buildDynamicSubAgents(AgentVo agentVo,
                                               ChatModel plannerModel,
                                               ToolProvider interceptedToolProvider,
                                               String sessionDocContext) {

        List<Object> subAgents = new ArrayList<>();

        // 1. 永远存在的兜底闲聊 Agent（防止 Supervisor 空转）
        ChitChatAgent chitChatAgent = AgenticServices.agentBuilder(ChitChatAgent.class)
            .chatModel(plannerModel)
            .build();
        subAgents.add(chitChatAgent);

        // 2. 智能配置Agent
        if (agentVo != null) {
            var customAgentBuilder = AgenticServices.agentBuilder(DynamicCustomAgent.class)
                .chatModel(plannerModel)
                .toolProvider(interceptedToolProvider);

            // 提示词存在注入
            String systemPrompt = agentVo.getSystemPrompt();
            if (StringUtils.isNotEmpty(systemPrompt)) {
                customAgentBuilder.systemMessage(systemPrompt);
            }

            DynamicCustomAgent customAgent = customAgentBuilder.build();
            subAgents.add(customAgent);
            log.info("成功动态生成自定义 Agent");
        }

        // 3. 【核心新增】如果当前会话有文档上下文，则动态生成文档解析 Agent
        if (StringUtils.isNotBlank(sessionDocContext)) {
            DocParseAgent docParseAgent = AgenticServices.agentBuilder(DocParseAgent.class)
                .chatModel(plannerModel)
                .build();
            subAgents.add(docParseAgent);
            log.info("检测到文档上下文，已动态激活【文档解析专家】");
        }

        // Skills 装配：智能体有勾选技能名时按名过滤磁盘 skills，否则加载全部
        ShellSkills skills = buildShellSkills(agentVo);
        // SkillsAgent：仅当有可用 skills 时才注入 systemMessage + toolProvider
        if (skills != null) {
            var skillsAgentBuilder = AgenticServices.agentBuilder(SkillsAgent.class)
                .chatModel(plannerModel);

            String skillsPrompt = "【可用技能与工具指南】\n" +
                skills.formatAvailableSkills() +
                "\n\n【执行规则】\n" +
                "1. 当用户的请求与上述任何技能相关时，请仔细分析意图，并**直接调用对应的工具**来完成任务。\n" +
                "2. 你无需进行任何前置的激活或确认步骤。\n" +
                "3. 如果用户的请求不明确属于哪个技能，请先向用户澄清需求，不要盲目调用工具。";

            skillsAgentBuilder
                .systemMessage(skillsPrompt)
                .toolProvider(skills.toolProvider());

            SkillsAgent skillsAgent = skillsAgentBuilder.build();
            subAgents.add(skillsAgent);
            log.info("成功动态生成 SkillsAgent");
        }
        return subAgents;
    }
}

