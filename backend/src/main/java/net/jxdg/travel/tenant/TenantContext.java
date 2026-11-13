package net.jxdg.travel.tenant;

/**
 * 租户上下文：登录后由 JWT 解析写入，ThreadLocal 传递。
 * tenantId == null 且 platform == true 表示 system 超管全国视图。
 */
public final class TenantContext {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> PLATFORM = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> READ_ONLY = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<String> TENANT_CODE = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long tenantId, boolean platform, boolean readOnly, String tenantCode) {
        TENANT_ID.set(tenantId);
        PLATFORM.set(platform);
        READ_ONLY.set(readOnly);
        TENANT_CODE.set(tenantCode);
    }

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static String getTenantCode() {
        return TENANT_CODE.get();
    }

    public static boolean isPlatform() {
        return Boolean.TRUE.equals(PLATFORM.get());
    }

    public static boolean isReadOnly() {
        return Boolean.TRUE.equals(READ_ONLY.get());
    }

    public static void setTenantCode(String code) {
        TENANT_CODE.set(code);
    }

    public static void clear() {
        TENANT_ID.remove();
        PLATFORM.remove();
        READ_ONLY.remove();
        TENANT_CODE.remove();
    }
}
