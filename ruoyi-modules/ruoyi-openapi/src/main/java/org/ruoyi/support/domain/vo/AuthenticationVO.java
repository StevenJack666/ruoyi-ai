package org.ruoyi.support.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description:
 * @Author: xyn
 * @date: 2020/12/21 9:58
 */
@Data
public class AuthenticationVO {
    private String appId;
    private String securityKey;
    private String securityType;
    private String organization;
    private List<Long> ip = new ArrayList<>();
    private List<String> url = new ArrayList<>();
}
