package org.ruoyi.support.domain;

import com.baomidou.mybatisplus.annotation.TableField;

import java.util.HashMap;
import java.util.Map;

public class CommonRequest {

    /** 请求参数 */
    @TableField(exist = false)
    protected Map<String, Object> params = new HashMap<>();

    public Map<String, Object> getParams()
    {
        if (params == null)
        {
            params = new HashMap<>();
        }
        return params;
    }

    public void setParams(Map<String, Object> params)
    {
        this.params = params;
    }
}
