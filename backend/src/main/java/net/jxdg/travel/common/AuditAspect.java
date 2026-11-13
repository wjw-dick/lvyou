package net.jxdg.travel.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jxdg.travel.modules.system.entity.SysAuditLog;
import net.jxdg.travel.modules.system.mapper.SysAuditLogMapper;
import net.jxdg.travel.security.LoginUser;
import net.jxdg.travel.security.SecurityUtils;
import net.jxdg.travel.tenant.TenantContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final SysAuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint pjp, Audit audit) throws Throwable {
        Object result = null;
        String error = null;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable e) {
            error = e.getMessage();
            throw e;
        } finally {
            try {
                save(pjp, audit, error);
            } catch (Exception e) {
                log.warn("audit log failed", e);
            }
        }
    }

    private void save(ProceedingJoinPoint pjp, Audit audit, String error) {
        SysAuditLog logRow = new SysAuditLog();
        logRow.setTenantId(TenantContext.getTenantId() == null ? 0L : TenantContext.getTenantId());
        LoginUser user = SecurityUtils.currentOrNull();
        if (user != null) {
            logRow.setOperatorId(user.getId());
            logRow.setOperatorName(user.getRealName() == null ? user.getUsername() : user.getRealName());
            logRow.setUserType(user.getUserType());
        }
        logRow.setModule(audit.module());
        logRow.setAction(audit.action());
        logRow.setTarget(pjp.getTarget().getClass().getSimpleName() + "." + pjp.getSignature().getName());
        String detail;
        try {
            detail = objectMapper.writeValueAsString(Arrays.stream(pjp.getArgs())
                    .filter(a -> a == null || !(a instanceof jakarta.servlet.ServletRequest)
                            && !(a instanceof jakarta.servlet.ServletResponse))
                    .limit(3).toList());
        } catch (Exception e) {
            detail = "";
        }
        logRow.setDetail(detail.length() > 2000 ? detail.substring(0, 2000) : detail);
        HttpServletRequest request = currentRequest();
        logRow.setIp(request == null ? null : clientIp(request));
        logRow.setResult(error == null ? "success" : "fail");
        logRow.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(logRow);
    }

    private HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        return attrs instanceof ServletRequestAttributes sra ? sra.getRequest() : null;
    }

    public static String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }
}
