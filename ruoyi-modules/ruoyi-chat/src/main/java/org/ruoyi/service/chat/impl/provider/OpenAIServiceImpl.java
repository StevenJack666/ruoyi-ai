package org.ruoyi.service.chat.impl.provider;


import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;

import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.observability.MyChatModelListener;
import org.ruoyi.service.chat.AbstractChatService;
import org.springframework.stereotype.Service;


/**
 * OPENAI服务调用
 *
 * @author ageerle@163.com
 * @date 2025/12/13
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OpenAIServiceImpl implements AbstractChatService {

    @Override
    public StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo,ChatRequest chatRequest) {
        String baseUrl = chatModelVo.getApiHost();
        String apiKey = chatModelVo.getApiKey();
        String modelName = chatModelVo.getModelName();
        
        log.info("构建 OpenAI 兼容模型");
        log.info("  BaseUrl: {}", baseUrl);
        log.info("  ModelName: {}", modelName);
        log.info("  ApiKey: {}", apiKey != null ? "***" + apiKey.substring(Math.max(0, apiKey.length() - 4)) : "NULL");
        log.info("  EnableThinking: {}", chatRequest.getEnableThinking());
        
        try {
     
            StreamingChatModel model = OpenAiStreamingChatModel.builder()
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .timeout(Duration.ofSeconds(120))
                    .listeners(List.of(new MyChatModelListener()))
                    .returnThinking(chatRequest.getEnableThinking())
                    .httpClientBuilder(JdkHttpClient.builder().httpClientBuilder(buildInsecureHttpClientBuilder()))
                    .build();
            log.info("OpenAI 兼容模型构建成功");
            return model;
        } catch (Exception e) {
            log.error("构建 OpenAI 兼容模型失败", e);
            throw new RuntimeException("无法构建模型: " + e.getMessage(), e);
        }
    }

    @Override
    public ChatModel buildChatModel(ChatModelVo chatModelVo) {
        return OpenAiChatModel.builder()
            .baseUrl(chatModelVo.getApiHost())
            .apiKey(chatModelVo.getApiKey())
            .modelName(chatModelVo.getModelName())
            .timeout(Duration.ofSeconds(120))
            .httpClientBuilder(JdkHttpClient.builder().httpClientBuilder(buildInsecureHttpClientBuilder()))
            .build();
    }

    @Override
    public String getProviderName() {
        return ChatModeType.OPEN_AI.getCode();
    }

    private HttpClient.Builder buildInsecureHttpClientBuilder() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            SSLParameters sslParameters = new SSLParameters();
            sslParameters.setEndpointIdentificationAlgorithm(null);

            return HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .sslParameters(sslParameters);
        } catch (Exception e) {
            log.error("创建不安全 HttpClientBuilder 失败", e);
            throw new RuntimeException("无法创建不安全 HttpClientBuilder", e);
        }
    }
}
