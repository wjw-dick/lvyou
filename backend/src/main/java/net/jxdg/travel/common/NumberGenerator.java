package net.jxdg.travel.common;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;

/**
 * 编号生成器：Redis 自增 + 分布式锁防重，编号含租户编码前缀。
 * 格式：{租户编码}-{类型}-{年}-{4位序号}，如 HLJ-TR-2026-0001。
 */
@Component
@RequiredArgsConstructor
public class NumberGenerator {

    private final StringRedisTemplate redis;

    public String next(String type) {
        Long tenantId = net.jxdg.travel.tenant.TenantContext.getTenantId();
        String code = net.jxdg.travel.tenant.TenantContext.getTenantCode();
        return next(tenantId, code, type);
    }

    public String next(Long tenantId, String tenantCode, String type) {
        if (tenantId == null) {
            throw new BizException(400, "缺少租户上下文，无法生成业务编号");
        }
        int year = LocalDate.now().getYear();
        String key = "num:%d:%s:%d".formatted(tenantId, type, year);
        Long seq;
        try {
            seq = redis.opsForValue().increment(key);
            if (seq != null && seq == 1L) {
                redis.expire(key, Duration.ofDays(800));
            }
        } catch (Exception e) {
            seq = System.currentTimeMillis() % 100000;
        }
        String prefix = (tenantCode == null || tenantCode.isBlank()) ? String.valueOf(tenantId) : tenantCode;
        return "%s-%s-%d-%04d".formatted(prefix, type, year, seq == null ? 1 : seq);
    }

    /** 下单/结算等接口防重：SETNX 5 秒 */
    public boolean tryLock(String key, Duration ttl) {
        Boolean ok = redis.opsForValue().setIfAbsent("lock:" + key, "1", ttl);
        return Boolean.TRUE.equals(ok);
    }

    public void release(String key) {
        redis.delete("lock:" + key);
    }
}
