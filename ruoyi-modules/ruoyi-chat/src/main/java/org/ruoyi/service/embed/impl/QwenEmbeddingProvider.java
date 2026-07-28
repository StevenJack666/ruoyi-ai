package org.ruoyi.service.embed.impl;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.springframework.stereotype.Component;


/**
 * 千问多模态的向量模型
 */
@Slf4j
@Component("qianwen")
public class QwenEmbeddingProvider extends AliBaiLianMultiEmbeddingProvider {

    public QwenEmbeddingProvider() {
        super();
    }

    @Override
    public void configure(ChatModelVo config) {
        super.configure(config);
    }
}
