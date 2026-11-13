package net.jxdg.travel.modules.system.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class LoginResult {
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private String userType;
    private Long tenantId;
    private String tenantCode;
    private String tenantName;
    private Set<String> permissions;
    private List<Map<String, Object>> menus;
}
