package org.ruoyi.openapi.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import org.ruoyi.common.core.domain.R;
import org.springframework.web.bind.annotation.*;


/**
 * 资源授权信息Controller
 *
 * @author admin
 * @date 2024-10-08
 */
@RestController
@RequestMapping("/openapi/test")
public class OpenApiTestController {

    /**
     * 查询资源授权信息列表
     */
    @SaIgnore
    @GetMapping("/list")
    public R<?> list() {
        return R.ok("测试成功");
    }

}
