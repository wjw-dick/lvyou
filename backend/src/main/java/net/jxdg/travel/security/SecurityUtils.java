package net.jxdg.travel.security;

import net.jxdg.travel.common.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser user)) {
            throw new BizException(401, "未登录");
        }
        return user;
    }

    public static LoginUser currentOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof LoginUser user ? user : null;
    }

    public static Long userId() {
        return current().getId();
    }

    public static String username() {
        return current().getUsername();
    }

    public static Long tenantId() {
        return current().getTenantId();
    }

    public static boolean isPlatform() {
        LoginUser u = currentOrNull();
        return u != null && "PLATFORM".equals(u.getUserType());
    }

    public static void requirePlatform() {
        if (!isPlatform()) {
            throw BizException.forbidden("仅平台超级管理员可操作");
        }
    }

    public static void requireTenantAdmin() {
        LoginUser u = current();
        if (!"TENANT".equals(u.getUserType()) || u.getTenantId() == null) {
            throw BizException.forbidden("仅租户管理员可操作");
        }
    }
}
