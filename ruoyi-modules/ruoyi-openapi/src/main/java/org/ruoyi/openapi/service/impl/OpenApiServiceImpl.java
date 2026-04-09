package org.ruoyi.openapi.service.impl;

import org.ruoyi.openapi.service.IOpenApiService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class OpenApiServiceImpl implements IOpenApiService {

    @Override
    public Map<String, Object> getModuleInfo() {
        Map<String, Object> result = new HashMap<>(4);
        result.put("module", "ruoyi-openapi");
        result.put("status", "ok");
        result.put("description", "OpenAPI module initialized");
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }
}
