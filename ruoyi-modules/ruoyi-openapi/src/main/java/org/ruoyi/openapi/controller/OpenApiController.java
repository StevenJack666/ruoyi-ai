package org.ruoyi.openapi.controller;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.openapi.service.IOpenApiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/openapi")
public class OpenApiController {

    private final IOpenApiService openApiService;

    @GetMapping("/ping")
    public R<Map<String, Object>> ping() {
        return R.ok(openApiService.getModuleInfo());
    }
}
