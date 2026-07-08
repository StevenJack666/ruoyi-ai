package org.ruoyi.common.chat.domain.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * 返回相应对象
 */
@AllArgsConstructor
@RequiredArgsConstructor
@Data
public class OpenApiResponse {

    /**
     * 内容信息
     */
    Map<String, Object> result;
}
