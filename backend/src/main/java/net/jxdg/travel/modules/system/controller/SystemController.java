package net.jxdg.travel.modules.system.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import net.jxdg.travel.common.*;
import net.jxdg.travel.modules.member.entity.BizMember;
import net.jxdg.travel.modules.member.mapper.BizMemberMapper;
import net.jxdg.travel.modules.order.entity.BizOrder;
import net.jxdg.travel.modules.order.mapper.BizOrderMapper;
import net.jxdg.travel.modules.product.entity.BizProduct;
import net.jxdg.travel.modules.product.entity.BizSchedule;
import net.jxdg.travel.modules.product.mapper.BizProductMapper;
import net.jxdg.travel.modules.product.mapper.BizScheduleMapper;
import net.jxdg.travel.modules.system.entity.*;
import net.jxdg.travel.modules.system.mapper.*;
import net.jxdg.travel.security.SecurityUtils;
import net.jxdg.travel.tenant.TenantContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/system")
public class SystemController {

    private final SysTenantUserMapper tenantUserMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;
    private final SysDictItemMapper dictItemMapper;
    private final SysGlobalDictMapper globalDictMapper;
    private final SysConfigMapper configMapper;
    private final SysNavMapper navMapper;
    private final SysNoticeMapper noticeMapper;
    private final SysAuditLogMapper auditLogMapper;
    private final SysTenantMapper tenantMapper;
    private final BizProductMapper productMapper;
    private final BizOrderMapper orderMapper;
    private final BizScheduleMapper scheduleMapper;
    private final BizMemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final CryptoUtil cryptoUtil;

    @GetMapping("/users")
    public Result<PageResult<SysTenantUser>> users(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "20") long size,
                                                   @RequestParam(required = false) String keyword) {
        Page<SysTenantUser> p = tenantUserMapper.selectPage(new Page<>(page, size), Wrappers.<SysTenantUser>lambdaQuery()
                .and(keyword != null && !keyword.isBlank(), w -> w.like(SysTenantUser::getUsername, keyword)
                        .or().like(SysTenantUser::getRealName, keyword))
                .orderByAsc(SysTenantUser::getId));
        p.getRecords().forEach(u -> u.setPassword(null));
        return Result.ok(PageResult.of(p));
    }

    @PostMapping("/users")
    @Transactional
    @Audit(module = "系统-用户", action = "保存用户")
    public Result<Void> saveUser(@RequestBody Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        SysTenantUser u = id == null ? new SysTenantUser() : tenantUserMapper.selectById(id);
        if (u == null) throw BizException.notFound("用户不存在");
        u.setUsername(String.valueOf(body.get("username")));
        u.setRealName(str(body.get("realName")));
        u.setPhone(str(body.get("phone")));
        u.setEmail(str(body.get("email")));
        u.setIsAdmin(Boolean.parseBoolean(String.valueOf(body.getOrDefault("isAdmin", false))) ? 1 : 0);
        u.setStatus(1);
        if (id == null) {
            u.setTenantId(TenantContext.getTenantId());
            u.setPassword(passwordEncoder.encode(str(body.getOrDefault("password", "admin123"))));
            tenantUserMapper.insert(u);
        } else {
            if (body.get("password") != null && !String.valueOf(body.get("password")).isBlank()) {
                u.setPassword(passwordEncoder.encode(String.valueOf(body.get("password"))));
            }
            tenantUserMapper.updateById(u);
        }
        if (body.get("roleIds") instanceof List<?> roleIds) {
            userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery()
                    .eq(SysUserRole::getUserId, u.getId()).eq(SysUserRole::getUserType, "tenant"));
            for (Object rid : roleIds) {
                SysUserRole ur = new SysUserRole();
                ur.setTenantId(TenantContext.getTenantId());
                ur.setUserId(u.getId());
                ur.setRoleId(Long.valueOf(String.valueOf(rid)));
                ur.setUserType("tenant");
                userRoleMapper.insert(ur);
            }
        }
        return Result.ok();
    }

    @DeleteMapping("/users/{id}")
    @Audit(module = "系统-用户", action = "删除用户")
    public Result<Void> deleteUser(@PathVariable Long id) {
        if (id.equals(SecurityUtils.userId())) throw new BizException("不能删除当前登录用户");
        tenantUserMapper.deleteById(id);
        return Result.ok();
    }

    @GetMapping("/roles")
    public Result<List<Map<String, Object>>> roles() {
        List<SysRole> roles = roleMapper.selectList(Wrappers.<SysRole>lambdaQuery().orderByAsc(SysRole::getId));
        List<Map<String, Object>> out = new ArrayList<>();
        for (SysRole r : roles) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("code", r.getCode());
            m.put("name", r.getName());
            m.put("remark", r.getRemark());
            m.put("status", r.getStatus());
            m.put("menus", roleMenuMapper.selectList(Wrappers.<SysRoleMenu>lambdaQuery()
                    .eq(SysRoleMenu::getRoleId, r.getId())).stream().map(SysRoleMenu::getMenuId).toList());
            out.add(m);
        }
        return Result.ok(out);
    }

    @PostMapping("/roles")
    @Transactional
    @Audit(module = "系统-角色", action = "保存角色")
    public Result<Void> saveRole(@RequestBody Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        SysRole r = id == null ? new SysRole() : roleMapper.selectById(id);
        if (r == null) throw BizException.notFound("角色不存在");
        r.setCode(String.valueOf(body.get("code")));
        r.setName(String.valueOf(body.get("name")));
        r.setRemark(str(body.get("remark")));
        r.setStatus(1);
        if (id == null) {
            r.setTenantId(TenantContext.getTenantId());
            roleMapper.insert(r);
        } else {
            roleMapper.updateById(r);
        }
        if (body.get("menuIds") instanceof List<?> menuIds) {
            roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, r.getId()));
            for (Object mid : menuIds) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setTenantId(TenantContext.getTenantId());
                rm.setRoleId(r.getId());
                rm.setMenuId(Long.valueOf(String.valueOf(mid)));
                roleMenuMapper.insert(rm);
            }
        }
        return Result.ok();
    }

    @DeleteMapping("/roles/{id}")
    @Audit(module = "系统-角色", action = "删除角色")
    public Result<Void> deleteRole(@PathVariable Long id) {
        SysRole r = roleMapper.selectById(id);
        if (r != null && "tenant_admin".equals(r.getCode())) throw new BizException("租户管理员角色不可删除");
        roleMapper.deleteById(id);
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, id));
        return Result.ok();
    }

    @GetMapping("/menus")
    public Result<List<SysMenu>> menus(@RequestParam(defaultValue = "tenant") String scope) {
        return Result.ok(menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .eq(SysMenu::getScope, scope).eq(SysMenu::getStatus, 1).orderByAsc(SysMenu::getSort)));
    }

    @GetMapping("/dict")
    public Result<Map<String, Object>> dict(@RequestParam(required = false) String type) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("global", globalDictMapper.selectList(Wrappers.<SysGlobalDict>lambdaQuery()
                .eq(type != null && !type.isBlank(), SysGlobalDict::getDictType, type)
                .orderByAsc(SysGlobalDict::getDictType).orderByAsc(SysGlobalDict::getSort)));
        out.put("custom", dictItemMapper.selectList(Wrappers.<SysDictItem>lambdaQuery()
                .eq(type != null && !type.isBlank(), SysDictItem::getDictType, type)
                .orderByAsc(SysDictItem::getDictType).orderByAsc(SysDictItem::getSort)));
        return Result.ok(out);
    }

    @PostMapping("/dict")
    @Audit(module = "系统-字典", action = "保存自定义字典")
    public Result<Void> saveDict(@RequestBody SysDictItem item) {
        if (item.getTenantId() == null) item.setTenantId(TenantContext.getTenantId());
        if (item.getId() == null) dictItemMapper.insert(item);
        else dictItemMapper.updateById(item);
        return Result.ok();
    }

    @DeleteMapping("/dict/{id}")
    @Audit(module = "系统-字典", action = "删除自定义字典")
    public Result<Void> deleteDict(@PathVariable Long id) {
        dictItemMapper.deleteById(id);
        return Result.ok();
    }

    @GetMapping("/config")
    public Result<Map<String, String>> config() {
        Map<String, String> out = new LinkedHashMap<>();
        configMapper.selectList(Wrappers.<SysConfig>lambdaQuery()).forEach(c -> {
            String v = c.getEncrypted() == 1 ? cryptoUtil.decrypt(c.getConfigValue()) : c.getConfigValue();
            boolean secret = c.getConfigKey().toLowerCase().contains("secret")
                    || c.getConfigKey().toLowerCase().contains("key")
                    || c.getConfigKey().toLowerCase().contains("password");
            out.put(c.getConfigKey(), secret && v != null && v.length() > 8 ? CryptoUtil.maskSecret(v) : v);
        });
        return Result.ok(out);
    }

    @PutMapping("/config")
    @Audit(module = "系统-配置", action = "保存租户配置")
    public Result<Void> saveConfig(@RequestBody Map<String, String> body) {
        Long tenantId = TenantContext.getTenantId();
        body.forEach((k, v) -> {
            SysConfig c = configMapper.selectOne(Wrappers.<SysConfig>lambdaQuery()
                    .eq(SysConfig::getConfigKey, k).last("limit 1"));
            boolean secret = k.toLowerCase().contains("secret") || k.toLowerCase().contains("key")
                    || k.toLowerCase().contains("password");
            if (secret && (v == null || v.contains("****"))) return;
            if (c == null) {
                c = new SysConfig();
                c.setTenantId(tenantId);
                c.setConfigKey(k);
                c.setEncrypted(secret ? 1 : 0);
                c.setConfigValue(secret ? cryptoUtil.encrypt(v) : v);
                configMapper.insert(c);
            } else {
                c.setEncrypted(secret ? 1 : 0);
                c.setConfigValue(secret ? cryptoUtil.encrypt(v) : v);
                configMapper.updateById(c);
            }
        });
        return Result.ok();
    }

    @GetMapping("/nav")
    public Result<String> nav(@RequestParam(defaultValue = "admin") String platform) {
        SysNav nav = navMapper.selectOne(Wrappers.<SysNav>lambdaQuery()
                .eq(SysNav::getPlatform, platform).last("limit 1"));
        return Result.ok(nav == null ? "[]" : nav.getContent());
    }

    @PutMapping("/nav")
    @Audit(module = "系统-菜单可视化", action = "保存导航配置")
    public Result<Void> saveNav(@RequestParam(defaultValue = "admin") String platform, @RequestBody String content) {
        SysNav nav = navMapper.selectOne(Wrappers.<SysNav>lambdaQuery()
                .eq(SysNav::getPlatform, platform).last("limit 1"));
        if (nav == null) {
            nav = new SysNav();
            nav.setTenantId(TenantContext.getTenantId());
            nav.setPlatform(platform);
            nav.setContent(content);
            navMapper.insert(nav);
        } else {
            nav.setContent(content);
            navMapper.updateById(nav);
        }
        return Result.ok();
    }

    @GetMapping("/audit")
    public Result<PageResult<SysAuditLog>> audit(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "20") long size,
                                                 @RequestParam(required = false) String module,
                                                 @RequestParam(required = false) String keyword) {
        Page<SysAuditLog> p = auditLogMapper.selectPage(new Page<>(page, size), Wrappers.<SysAuditLog>lambdaQuery()
                .eq(module != null && !module.isBlank(), SysAuditLog::getModule, module)
                .and(keyword != null && !keyword.isBlank(), w -> w.like(SysAuditLog::getOperatorName, keyword)
                        .or().like(SysAuditLog::getAction, keyword))
                .orderByDesc(SysAuditLog::getId));
        return Result.ok(PageResult.of(p));
    }

    @GetMapping("/notices")
    public Result<List<SysNotice>> notices() {
        return Result.ok(noticeMapper.selectList(Wrappers.<SysNotice>lambdaQuery()
                .orderByDesc(SysNotice::getId)));
    }

    @PostMapping("/notices")
    @Audit(module = "系统-公告", action = "发布公告")
    public Result<Void> saveNotice(@RequestBody SysNotice notice) {
        notice.setTenantId(TenantContext.getTenantId());
        if (notice.getId() == null) noticeMapper.insert(notice);
        else noticeMapper.updateById(notice);
        return Result.ok();
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Long tenantId = TenantContext.getTenantId();
        SysTenant tenant = tenantMapper.selectById(tenantId);
        long productCount = productMapper.selectCount(Wrappers.<BizProduct>lambdaQuery());
        long orderCount = orderMapper.selectCount(Wrappers.<BizOrder>lambdaQuery());
        long memberCount = memberMapper.selectCount(Wrappers.<BizMember>lambdaQuery());
        long todayOrders = orderMapper.selectCount(Wrappers.<BizOrder>lambdaQuery()
                .ge(BizOrder::getCreatedAt, LocalDate.now().atStartOfDay()));
        java.math.BigDecimal sales = orderMapper.selectMaps(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<BizOrder>()
                        .select("IFNULL(SUM(paid_amount),0) AS amount"))
                .stream().findFirst().map(m -> new java.math.BigDecimal(String.valueOf(m.get("amount"))))
                .orElse(java.math.BigDecimal.ZERO);
        long upcoming = scheduleMapper.selectCount(Wrappers.<BizSchedule>lambdaQuery()
                .between(BizSchedule::getDepartDate, LocalDate.now(), LocalDate.now().plusDays(7))
                .in(BizSchedule::getStatus, 1, 2, 3));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("tenantName", tenant == null ? "" : tenant.getName());
        out.put("productCount", productCount);
        out.put("orderCount", orderCount);
        out.put("memberCount", memberCount);
        out.put("todayOrders", todayOrders);
        out.put("salesAmount", sales);
        out.put("upcomingSchedules", upcoming);
        out.put("quota", tenant == null ? Map.of() : Map.of(
                "user", tenant.getQuotaUser(), "product", tenant.getQuotaProduct(),
                "order", tenant.getQuotaOrder(), "storage", tenant.getQuotaStorage()));
        out.put("usage", Map.of(
                "product", productCount,
                "order", orderCount,
                "user", tenantUserMapper.selectCount(Wrappers.<SysTenantUser>lambdaQuery())));
        return Result.ok(out);
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
