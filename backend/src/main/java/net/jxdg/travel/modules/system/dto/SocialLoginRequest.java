package net.jxdg.travel.modules.system.dto;

import lombok.Data;

@Data
public class SocialLoginRequest {
    private String tenantCode;
    private String code;
    private String unionId;
    private String openId;
    private String nickname;
    private String avatar;
    private String phone;
    private String username;
    private String password;
}
