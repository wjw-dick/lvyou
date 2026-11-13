package net.jxdg.travel.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class JwtService {

    private final SecretKey key;
    private final long expireMinutes;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expire-minutes:720}") long expireMinutes) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            bytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expireMinutes = expireMinutes;
    }

    public String create(LoginUser user, Long viewTenantId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", user.getId());
        claims.put("username", user.getUsername());
        claims.put("realName", user.getRealName());
        claims.put("userType", user.getUserType());
        claims.put("tenantId", user.getTenantId());
        claims.put("tenantCode", user.getTenantCode());
        claims.put("perms", user.getPermissions() == null ? List.of() : List.copyOf(user.getPermissions()));
        if (viewTenantId != null) {
            claims.put("viewTenantId", viewTenantId);
        }
        Instant now = Instant.now();
        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(user.getId()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expireMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    @SuppressWarnings("unchecked")
    public LoginUser toLoginUser(Claims claims) {
        LoginUser u = new LoginUser();
        u.setId(((Number) claims.get("uid")).longValue());
        u.setUsername((String) claims.get("username"));
        u.setRealName((String) claims.get("realName"));
        u.setUserType((String) claims.get("userType"));
        Object tid = claims.get("tenantId");
        u.setTenantId(tid == null ? null : ((Number) tid).longValue());
        u.setTenantCode((String) claims.get("tenantCode"));
        Object perms = claims.get("perms");
        if (perms instanceof List<?> list) {
            u.setPermissions(Set.copyOf(list.stream().map(String::valueOf).toList()));
        }
        return u;
    }
}
