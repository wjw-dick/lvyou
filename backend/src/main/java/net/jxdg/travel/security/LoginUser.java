package net.jxdg.travel.security;

import lombok.Data;

import java.util.Set;

@Data
public class LoginUser {
    private Long id;
    private String username;
    private String realName;
    private String userType; // PLATFORM / TENANT / MEMBER / GUIDE / SCENIC / DISTRIBUTOR / SUPPLIER
    private Long tenantId;
    private String tenantCode;
    private boolean readOnly;
    private Set<String> permissions;
}
