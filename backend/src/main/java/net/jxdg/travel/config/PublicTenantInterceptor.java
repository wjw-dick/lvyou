package net.jxdg.travel.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.jxdg.travel.modules.system.entity.SysTenant;
import net.jxdg.travel.modules.system.mapper.SysTenantMapper;
import net.jxdg.travel.tenant.TenantContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 公开端（H5/小程序/公众号）通过 X-Tenant-Code 或 tenantCode 参数解析租户上下文。
 * 解析后所有业务查询自动受租户插件过滤，实现租户数据隔离。
 */
@Component
@RequiredArgsConstructor
public class PublicTenantInterceptor implements HandlerInterceptor {

    private final SysTenantMapper tenantMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String code = request.getHeader("X-Tenant-Code");
        if (code == null || code.isBlank()) {
            code = request.getParameter("tenantCode");
        }
        if (code != null && !code.isBlank()) {
            SysTenant tenant = tenantMapper.selectOne(Wrappers.<SysTenant>lambdaQuery()
                    .eq(SysTenant::getCode, code).last("limit 1"));
            if (tenant != null) {
                TenantContext.set(tenant.getId(), false, false, tenant.getCode());
                request.setAttribute("RESOLVED_TENANT_ID", tenant.getId());
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}
