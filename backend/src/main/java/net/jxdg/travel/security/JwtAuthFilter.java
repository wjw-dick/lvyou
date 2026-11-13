package net.jxdg.travel.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.jxdg.travel.common.Result;
import net.jxdg.travel.tenant.TenantContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JWT 认证过滤器：解析 token → 写入 SecurityContext 与 TenantContext。
 * 平台超管可通过 X-Tenant-Id 下钻租户，下钻后为只读。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring(7).trim();
        try {
            Claims claims = jwtService.parse(token);
            LoginUser user = jwtService.toLoginUser(claims);
            Set<String> perms = user.getPermissions() == null ? Set.of() : user.getPermissions();
            var authorities = perms.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getUserType()));

            boolean platform = "PLATFORM".equals(user.getUserType());
            Long tenantId = user.getTenantId();
            boolean readOnly = false;
            if (platform) {
                // 平台端未选租户 = 全国视图；选择租户 = 下钻只读
                String viewTenant = request.getHeader("X-Tenant-Id");
                if (StringUtils.hasText(viewTenant) && !"all".equalsIgnoreCase(viewTenant)) {
                    tenantId = Long.valueOf(viewTenant);
                    readOnly = true;
                    if (!HttpMethod.GET.matches(request.getMethod())) {
                        writeDenied(response, "平台超管下钻租户为只读视图，禁止修改租户数据");
                        return;
                    }
                } else {
                    tenantId = null;
                }
            }
            TenantContext.set(tenantId, platform, readOnly, user.getTenantCode());
            var auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(request, response);
        } catch (Exception e) {
            response.setStatus(401);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(objectMapper.writeValueAsString(Result.fail(401, "登录已失效，请重新登录")));
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private void writeDenied(HttpServletResponse response, String message) throws IOException {
        response.setStatus(403);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(403, message)));
    }
}
