package org.ruoyi.service.chat.pipeline;

import lombok.Data;
import java.util.List;

@Data
public class ChatContext {
    private Long userId;
    private String tokenValue;
    private Object chatRequest;

    private List<String> retrievedDocs;
    private String toolResult;
    private boolean blocked;
    private String blockReason;
    private String responseContent;
    private Throwable error;
}
