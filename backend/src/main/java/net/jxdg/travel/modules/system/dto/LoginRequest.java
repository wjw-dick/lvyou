package net.jxdg.travel.modules.system.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    /** platform / tenant */
    private String type = "tenant";
    private String tenantCode;
}

@Data
class WechatLoginRequest {
}
