package org.ruoyi.workflow.client;

import cn.hutool.core.lang.Dict;
import cn.hutool.crypto.digest.DigestUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.json.utils.JsonUtils;
import org.ruoyi.workflow.common.ConditionalOnEnable;
import org.ruoyi.workflow.domain.FlwScriptOutbox;
import org.ruoyi.workflow.domain.bo.ScriptCallbackBo;
import org.ruoyi.workflow.service.impl.ScriptCallbackService;
import org.springframework.stereotype.Component;

/**
 * 本地子进程执行器
 * <p>
 * 直接以 {@code python3 xxx.py} 方式执行脚本文件，不依赖任何 HTTP 服务。
 * 入参通过 stdin 传入流程变量 JSON，同时用环境变量传 runId / businessId；
 * stdout 输出一个 JSON，退出码 0 视为成功。
 * 执行完成后在同一个 JVM 内调用 {@link ScriptCallbackService} 推进流程。
 *
 * @author ruoyi
 */
@ConditionalOnEnable
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalPythonScriptExecutor {

    private static final int MAX_STREAM_CHARS = 200_000;

    private static final int MAX_ERROR_CHARS = 2000;

    /**
     * 提前于 outbox 截止时间终止进程，避免与超时扫描任务抢状态
     */
    private static final long DEADLINE_SAFE_MARGIN_MS = 15_000L;

    /**
     * 线程池参数，存放在 sys_config（参数管理），启动时读取一次
     */
    private static final String EXEC_CORE_SIZE_KEY =
        "soar.script.exec-core-size";

    private static final String EXEC_MAX_SIZE_KEY = "soar.script.exec-max-size";

    private static final String EXEC_QUEUE_SIZE_KEY =
        "soar.script.exec-queue-size";

    private static final int DEFAULT_CORE_SIZE = 4;

    private static final int DEFAULT_MAX_SIZE = 16;

    private static final int DEFAULT_QUEUE_SIZE = 200;

    private final ScriptCallbackService scriptCallbackService;

    private final ConfigService configService;

    private ThreadPoolExecutor pool;

    /**
     * 线程池已满：属于瞬时压力，调度器应回退状态而不是计为失败
     */
    public static class PoolBusyException extends RuntimeException {

        public PoolBusyException(String message) {
            super(message);
        }
    }

    @PostConstruct
    public void init() {
        int core = resolvePositive(EXEC_CORE_SIZE_KEY, DEFAULT_CORE_SIZE);
        int max = resolvePositive(EXEC_MAX_SIZE_KEY, DEFAULT_MAX_SIZE);
        if (max < core) {
            log.warn(
                "[剧本执行] {}={} 小于 {}={}，按 core 值修正",
                EXEC_MAX_SIZE_KEY,
                max,
                EXEC_CORE_SIZE_KEY,
                core
            );
            max = core;
        }
        int queue = resolvePositive(EXEC_QUEUE_SIZE_KEY, DEFAULT_QUEUE_SIZE);

        this.pool = new ThreadPoolExecutor(
            core,
            max,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(queue),
            runnable -> {
                Thread thread = new Thread(runnable, "soar-script-exec");
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.AbortPolicy()
        );
        log.info(
            "[剧本执行] 本地执行器已启动, pool={}/{}/{}",
            core,
            max,
            queue
        );
    }

    /**
     * 读取正整数配置：缺失、非数字或小于 1 时使用默认值
     */
    private int resolvePositive(String key, int defaultValue) {
        String value = configService.getConfigValue(key);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed < 1 ? defaultValue : parsed;
        } catch (NumberFormatException e) {
            log.warn(
                "[剧本执行] {} 不是合法整数: {}, 使用默认值 {}",
                key,
                value,
                defaultValue
            );
            return defaultValue;
        }
    }

    @PreDestroy
    public void destroy() {
        if (pool != null) {
            pool.shutdownNow();
        }
    }

    /**
     * 提交执行：只做线程池投递，真正的执行在池线程中进行
     *
     * @param outbox 已抢占的记录（status=1）
     */
    public void dispatch(FlwScriptOutbox outbox) {
        try {
            pool.execute(() -> execute(outbox));
        } catch (RejectedExecutionException e) {
            throw new PoolBusyException("剧本执行线程池已满，稍后重试");
        }
    }

    private void execute(FlwScriptOutbox outbox) {
        long started = System.currentTimeMillis();
        try {
            doExecute(outbox, started);
        } catch (Exception e) {
            log.error("[剧本执行] 执行异常, runId={}", outbox.getRunId(), e);
            fail(
                outbox.getRunId(),
                started,
                "执行异常: " + e.getMessage(),
                null
            );
        }
    }

    private void doExecute(FlwScriptOutbox outbox, long started) {
        String runId = outbox.getRunId();
        Path script = Path.of(outbox.getScriptPath());
        List<String> command = buildCommand(script, outbox);

        ProcessBuilder builder = new ProcessBuilder(command);
        Path parent = script.getParent();
        if (parent != null) {
            builder.directory(parent.toFile());
        }
        prepareEnvironment(builder.environment(), outbox);
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            log.error(
                "[剧本执行] 启动进程失败, runId={}, cmd={}",
                runId,
                command,
                e
            );
            fail(runId, started, "启动脚本进程失败: " + e.getMessage(), null);
            return;
        }

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        Thread outReader = drain(process.getInputStream(), stdout);
        Thread errReader = drain(process.getErrorStream(), stderr);
        writeStdin(process, outbox.getPayload());

        long waitMs = calcWaitMs(outbox);
        boolean finished;
        try {
            finished = process.waitFor(waitMs, TimeUnit.MILLISECONDS);
            outReader.join(5000L);
            errReader.join(5000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            fail(runId, started, "执行线程被中断", null);
            return;
        }

        if (!finished) {
            process.destroyForcibly();
            log.warn("[剧本执行] 执行超时, runId={}, waitMs={}", runId, waitMs);
            fail(
                runId,
                started,
                "脚本执行超时（约 " + waitMs / 1000 + "s）",
                null
            );
            return;
        }

        long costMs = System.currentTimeMillis() - started;
        String sha256 = trySha256(script);
        int exitCode = process.exitValue();

        if (exitCode != 0) {
            String detail = StringUtils.isBlank(stderr)
                ? stdout.toString()
                : stderr.toString();
            fail(
                runId,
                started,
                "脚本退出码 " + exitCode + ": " + truncate(detail),
                sha256
            );
            return;
        }

        Dict result = parseJson(stdout.toString());
        Object output = result;
        List<Map<String, Object>> trace = null;

        // 兼容两种输出：直接输出业务结果，或 {"output":..., "trace":[...]}
        if (
            result != null &&
            (result.containsKey("output") || result.containsKey("trace"))
        ) {
            output = result.get("output");
            Object traceObj = result.get("trace");
            if (traceObj instanceof List<?> list) {
                trace = new ArrayList<>(list.size());
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> casted = (Map<String, Object>) map;
                        trace.add(casted);
                    }
                }
            }
        }

        ScriptCallbackBo callback = new ScriptCallbackBo();
        callback.setSuccess(true);
        callback.setOutput(output);
        callback.setTrace(trace);
        callback.setCostMs(costMs);
        callback.setScriptSha256(sha256);
        scriptCallbackService.handle(runId, callback);
        log.info("[剧本执行] 执行完成, runId={}, costMs={}", runId, costMs);
    }

    /**
     * 组装启动命令：解释器 + 脚本路径
     * <p>
     * 解释器由 t_playbook_script.python_bin 指定，enqueue 阶段已校验非空。
     */
    private List<String> buildCommand(Path script, FlwScriptOutbox outbox) {
        return List.of(outbox.getPythonBin(), script.toString());
    }

    /**
     * 只透传必要环境变量，避免把应用进程的敏感配置暴露给脚本
     */
    private void prepareEnvironment(
        Map<String, String> env,
        FlwScriptOutbox outbox
    ) {
        env.clear();
        copyEnv(env, "PATH");
        copyEnv(env, "HOME");
        env.put("LANG", "C.UTF-8");
        env.put("PYTHONUNBUFFERED", "1");
        env.put("SOAR_RUN_ID", outbox.getRunId());
        env.put("SOAR_BUSINESS_ID", nullToEmpty(outbox.getBusinessId()));
        env.put("SOAR_FLOW_CODE", nullToEmpty(outbox.getFlowCode()));
        env.put("SOAR_NODE_CODE", nullToEmpty(outbox.getNodeCode()));
        env.put("SOAR_SCRIPT_PATH", nullToEmpty(outbox.getScriptPath()));
    }

    private void copyEnv(Map<String, String> env, String key) {
        String value = System.getenv(key);
        if (value != null) {
            env.put(key, value);
        }
    }

    private long calcWaitMs(FlwScriptOutbox outbox) {
        long timeoutSec =
            outbox.getTimeoutSec() == null ? 900L : outbox.getTimeoutSec();
        long now = System.currentTimeMillis();
        long deadline =
            outbox.getDeadlineAt() == null
                ? now + timeoutSec * 1000L
                : outbox.getDeadlineAt().getTime();
        long wait = deadline - now - DEADLINE_SAFE_MARGIN_MS;
        return Math.max(1000L, wait);
    }

    private Thread drain(InputStream in, StringBuilder target) {
        Thread thread = new Thread(() -> {
            try (
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)
                )
            ) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (target.length() < MAX_STREAM_CHARS) {
                        target.append(line).append('\n');
                    }
                }
            } catch (IOException e) {
                log.debug("[剧本执行] 读取输出流结束: {}", e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.setName("soar-script-io");
        thread.start();
        return thread;
    }

    private void writeStdin(Process process, String payload) {
        Thread thread = new Thread(() -> {
            try (OutputStream os = process.getOutputStream()) {
                String body = StringUtils.isBlank(payload) ? "{}" : payload;
                os.write(body.getBytes(StandardCharsets.UTF_8));
                os.flush();
            } catch (IOException e) {
                log.debug("[剧本执行] 写入 stdin 结束: {}", e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.setName("soar-script-stdin");
        thread.start();
    }

    private Dict parseJson(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        try {
            return JsonUtils.parseMap(text);
        } catch (Exception e) {
            log.warn("[剧本执行] stdout 不是合法 JSON，按原文返回");
            Dict fallback = new Dict();
            fallback.put("raw", truncate(text));
            return fallback;
        }
    }

    private String trySha256(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            return DigestUtil.sha256Hex(in);
        } catch (IOException e) {
            log.warn("[剧本执行] 计算脚本哈希失败: {}", path, e);
            return null;
        }
    }

    private void fail(String runId, long started, String error, String sha256) {
        ScriptCallbackBo callback = new ScriptCallbackBo();
        callback.setSuccess(false);
        callback.setError(error);
        callback.setCostMs(System.currentTimeMillis() - started);
        callback.setScriptSha256(sha256);
        try {
            scriptCallbackService.handle(runId, callback);
        } catch (Exception e) {
            log.error("[剧本执行] 回写失败状态时异常, runId={}", runId, e);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() > MAX_ERROR_CHARS
            ? text.substring(0, MAX_ERROR_CHARS)
            : text;
    }
}
