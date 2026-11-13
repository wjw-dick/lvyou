package net.jxdg.travel.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.jxdg.travel.tenant.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * MyBatis-Plus 配置：
 * 1) 租户插件强制注入 tenant_id，白名单为平台级表；
 * 2) 分页插件；
 * 3) 审计字段自动填充。
 * 说明：system 超管未选择租户时（tenantId=null）不做租户过滤，可跨租户统计；
 * 下钻某租户时由 TenantContext 注入只读标记并在拦截器中阻止写操作。
 */
@Configuration
public class MybatisPlusConfig {

    /** 平台级白名单：不做租户隔离 */
    public static final Set<String> PLATFORM_TABLES = Set.of(
            "sys_tenant", "sys_platform_user", "sys_global_dict", "sys_platform_param",
            "sys_language", "sys_menu", "sys_sensitive_word", "sys_platform_notice"
    );

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        TenantLineInnerInterceptor tenant = new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                Long tenantId = TenantContext.getTenantId();
                return tenantId == null ? new NullValue() : new LongValue(tenantId);
            }

            @Override
            public boolean ignoreTable(String tableName) {
                Long tenantId = TenantContext.getTenantId();
                return tenantId == null || PLATFORM_TABLES.contains(tableName.toLowerCase());
            }
        });
        interceptor.addInnerInterceptor(tenant);
        PaginationInnerInterceptor page = new PaginationInnerInterceptor(DbType.MYSQL);
        page.setMaxLimit(500L);
        interceptor.addInnerInterceptor(page);
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(org.apache.ibatis.reflection.MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
            }

            @Override
            public void updateFill(org.apache.ibatis.reflection.MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
