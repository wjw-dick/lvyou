package net.jxdg.travel.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import net.jxdg.travel.common.*;
import net.jxdg.travel.modules.member.entity.BizMember;
import net.jxdg.travel.modules.member.mapper.BizMemberMapper;
import net.jxdg.travel.modules.order.entity.BizOrder;
import net.jxdg.travel.modules.order.mapper.BizOrderMapper;
import net.jxdg.travel.modules.product.entity.BizProduct;
import net.jxdg.travel.modules.product.mapper.BizProductMapper;
import net.jxdg.travel.modules.system.entity.*;
import net.jxdg.travel.modules.system.mapper.*;
import net.jxdg.travel.security.SecurityUtils;
import net.jxdg.travel.tenant.TenantContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class PlatformController {

    private final SysTenantMapper tenantMapper;
    private final SysTenantUserMapper tenantUserMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;
    private final SysGlobalDictMapper globalDictMapper;
    private final SysPlatformParamMapper platformParamMapper;
    private final SysAuditLogMapper auditLogMapper;
    private final SysNoticeMapper noticeMapper;
    private final SysPlatformUserMapper platformUserMapper;
    private final BizOrderMapper orderMapper;
    private final BizProductMapper productMapper;
    private final BizMemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/platform/dashboard")
    public Result<Map<String, Object>> dashboard() {
        SecurityUtils.requirePlatform();
        List<SysTenant> tenants = tenantMapper.selectList(Wrappers.<SysTenant>lambdaQuery());
        long productCount = productMapper.selectCount(null);
        long orderCount = orderMapper.selectCount(null);
        long memberCount = memberMapper.selectCount(null);
        BigDecimal sales = sumOrderAmount();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysTenant t : tenants) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tenantId", t.getId());
            row.put("tenantName", t.getName());
            row.put("code", t.getCode());
            row.put("status", t.getStatus());
            row.put("productCount", productMapper.selectCount(Wrappers.<BizProduct>lambdaQuery().eq(BizProduct::getTenantId, t.getId())));
            row.put("orderCount", orderMapper.selectCount(Wrappers.<BizOrder>lambdaQuery().eq(BizOrder::getTenantId, t.getId())));
            row.put("memberCount", memberMapper.selectCount(Wrappers.<BizMember>lambdaQuery().eq(BizMember::getTenantId, t.getId())));
            BigDecimal amt = orderMapper.selectMaps(new QueryWrapper<BizOrder>()
                            .select("IFNULL(SUM(paid_amount),0) AS amount").eq("tenant_id", t.getId()))
                    .stream().findFirst().map(m -> new BigDecimal(String.valueOf(m.get("amount")))).orElse(BigDecimal.ZERO);
            row.put("salesAmount", amt);
            rows.add(row);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("tenantCount", tenants.size());
        out.put("productCount", productCount);
        out.put("orderCount", orderCount);
        out.put("memberCount", memberCount);
        out.put("salesAmount", sales);
        out.put("tenants", rows);
        return Result.ok(out);
    }

    private BigDecimal sumOrderAmount() {
        return orderMapper.selectMaps(new QueryWrapper<BizOrder>().select("IFNULL(SUM(paid_amount),0) AS amount"))
                .stream().findFirst().map(m -> new BigDecimal(String.valueOf(m.get("amount")))).orElse(BigDecimal.ZERO);
    }

    @GetMapping("/platform/tenants")
    public Result<PageResult<SysTenant>> tenants(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "20") long size,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Integer status) {
        SecurityUtils.requirePlatform();
        Page<SysTenant> p = tenantMapper.selectPage(new Page<>(page, size), Wrappers.<SysTenant>lambdaQuery()
                .and(keyword != null && !keyword.isBlank(), w -> w.like(SysTenant::getName, keyword).or().like(SysTenant::getCode, keyword))
                .eq(status != null, SysTenant::getStatus, status)
                .orderByDesc(SysTenant::getId));
        return Result.ok(PageResult.of(p));
    }

    @PostMapping("/platform/tenants")
    @Transactional
    @Audit(module = "平台-租户管理", action = "开通租户")
    public Result<SysTenant> createTenant(@RequestBody Map<String, Object> body) {
        SecurityUtils.requirePlatform();
        String code = String.valueOf(body.get("code"));
        if (tenantMapper.selectCount(Wrappers.<SysTenant>lambdaQuery().eq(SysTenant::getCode, code)) > 0) {
            throw new BizException("租户编码已存在");
        }
        SysTenant t = new SysTenant();
        t.setCode(code.toUpperCase());
        t.setName(String.valueOf(body.get("name")));
        t.setProvince(str(body.get("province")));
        t.setCity(str(body.get("city")));
        t.setContact(str(body.get("contact")));
        t.setPhone(str(body.get("phone")));
        t.setStatus(1);
        t.setOpenTime(LocalDateTime.now());
        t.setExpireTime(LocalDateTime.now().plusMonths(body.get("months") == null ? 12 : Integer.parseInt(String.valueOf(body.get("months")))));
        t.setQuotaUser(intVal(body.get("quotaUser"), 20));
        t.setQuotaProduct(intVal(body.get("quotaProduct"), 200));
        t.setQuotaOrder(intVal(body.get("quotaOrder"), 100000));
        t.setQuotaStorage(longVal(body.get("quotaStorage"), 10737418240L));
        tenantMapper.insert(t);

        Long oldTenant = TenantContext.getTenantId();
        try {
            TenantContext.set(t.getId(), true, false, t.getCode());
            SysTenantUser admin = new SysTenantUser();
            admin.setTenantId(t.getId());
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRealName(t.getName() + "管理员");
            admin.setPhone(t.getPhone());
            admin.setIsAdmin(1);
            admin.setStatus(1);
            tenantUserMapper.insert(admin);

            SysRole role = new SysRole();
            role.setTenantId(t.getId());
            role.setCode("tenant_admin");
            role.setName("租户管理员");
            role.setStatus(1);
            roleMapper.insert(role);
            List<SysMenu> menus = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery().eq(SysMenu::getScope, "tenant"));
            for (SysMenu m : menus) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setTenantId(t.getId());
                rm.setRoleId(role.getId());
                rm.setMenuId(m.getId());
                roleMenuMapper.insert(rm);
            }
            SysUserRole ur = new SysUserRole();
            ur.setTenantId(t.getId());
            ur.setUserId(admin.getId());
            ur.setRoleId(role.getId());
            ur.setUserType("tenant");
            userRoleMapper.insert(ur);
        } finally {
            if (oldTenant == null) TenantContext.clear();
            else TenantContext.set(oldTenant, true, false, null);
        }
        return Result.ok(t);
    }

    @PutMapping("/platform/tenants/{id}")
    @Audit(module = "平台-租户管理", action = "修改租户")
    public Result<Void> updateTenant(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        SecurityUtils.requirePlatform();
        SysTenant t = tenantMapper.selectById(id);
        if (t == null) throw BizException.notFound("租户不存在");
        if (body.get("name") != null) t.setName(String.valueOf(body.get("name")));
        if (body.get("contact") != null) t.setContact(str(body.get("contact")));
        if (body.get("phone") != null) t.setPhone(str(body.get("phone")));
        if (body.get("quotaUser") != null) t.setQuotaUser(intVal(body.get("quotaUser"), t.getQuotaUser()));
        if (body.get("quotaProduct") != null) t.setQuotaProduct(intVal(body.get("quotaProduct"), t.getQuotaProduct()));
        if (body.get("quotaOrder") != null) t.setQuotaOrder(intVal(body.get("quotaOrder"), t.getQuotaOrder()));
        if (body.get("quotaStorage") != null) t.setQuotaStorage(longVal(body.get("quotaStorage"), t.getQuotaStorage()));
        tenantMapper.updateById(t);
        return Result.ok();
    }

    @PostMapping("/platform/tenants/{id}/status")
    @Audit(module = "平台-租户管理", action = "变更租户状态")
    public Result<Void> status(@PathVariable Long id, @RequestParam Integer status) {
        SecurityUtils.requirePlatform();
        SysTenant t = tenantMapper.selectById(id);
        if (t == null) throw BizException.notFound("租户不存在");
        t.setStatus(status);
        tenantMapper.updateById(t);
        return Result.ok();
    }

    @PostMapping("/platform/tenants/{id}/renew")
    @Audit(module = "平台-租户管理", action = "租户续期")
    public Result<Void> renew(@PathVariable Long id, @RequestParam(defaultValue = "12") int months) {
        SecurityUtils.requirePlatform();
        SysTenant t = tenantMapper.selectById(id);
        if (t == null) throw BizException.notFound("租户不存在");
        LocalDateTime base = t.getExpireTime() != null && t.getExpireTime().isAfter(LocalDateTime.now())
                ? t.getExpireTime() : LocalDateTime.now();
        t.setExpireTime(base.plusMonths(months));
        if (Objects.equals(t.getStatus(), 4)) t.setStatus(2);
        tenantMapper.updateById(t);
        return Result.ok();
    }

    @GetMapping("/platform/dict")
    public Result<List<SysGlobalDict>> dict(@RequestParam(required = false) String type) {
        return Result.ok(globalDictMapper.selectList(Wrappers.<SysGlobalDict>lambdaQuery()
                .eq(type != null && !type.isBlank(), SysGlobalDict::getDictType, type)
                .orderByAsc(SysGlobalDict::getDictType).orderByAsc(SysGlobalDict::getSort)));
    }

    @PostMapping("/platform/dict")
    @Audit(module = "平台-全局字典", action = "保存字典")
    public Result<Void> saveDict(@RequestBody SysGlobalDict dict) {
        SecurityUtils.requirePlatform();
        if (dict.getId() == null) globalDictMapper.insert(dict);
        else globalDictMapper.updateById(dict);
        return Result.ok();
    }

    @DeleteMapping("/platform/dict/{id}")
    @Audit(module = "平台-全局字典", action = "删除字典")
    public Result<Void> deleteDict(@PathVariable Long id) {
        SecurityUtils.requirePlatform();
        globalDictMapper.deleteById(id);
        return Result.ok();
    }

    @GetMapping("/platform/params")
    public Result<List<SysPlatformParam>> params() {
        SecurityUtils.requirePlatform();
        return Result.ok(platformParamMapper.selectList(Wrappers.<SysPlatformParam>lambdaQuery().orderByAsc(SysPlatformParam::getId)));
    }

    @PutMapping("/platform/params")
    @Audit(module = "平台-参数", action = "修改平台参数")
    public Result<Void> saveParams(@RequestBody List<SysPlatformParam> params) {
        SecurityUtils.requirePlatform();
        for (SysPlatformParam p : params) {
            SysPlatformParam exist = platformParamMapper.selectOne(Wrappers.<SysPlatformParam>lambdaQuery()
                    .eq(SysPlatformParam::getParamKey, p.getParamKey()).last("limit 1"));
            if (exist == null) {
                platformParamMapper.insert(p);
            } else {
                exist.setParamValue(p.getParamValue());
                platformParamMapper.updateById(exist);
            }
        }
        return Result.ok();
    }

    @GetMapping("/platform/audit")
    public Result<PageResult<SysAuditLog>> audit(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "20") long size,
                                                 @RequestParam(required = false) String module,
                                                 @RequestParam(required = false) String keyword) {
        SecurityUtils.requirePlatform();
        Page<SysAuditLog> p = auditLogMapper.selectPage(new Page<>(page, size), Wrappers.<SysAuditLog>lambdaQuery()
                .eq(module != null && !module.isBlank(), SysAuditLog::getModule, module)
                .and(keyword != null && !keyword.isBlank(), w -> w.like(SysAuditLog::getOperatorName, keyword)
                        .or().like(SysAuditLog::getAction, keyword))
                .orderByDesc(SysAuditLog::getId));
        return Result.ok(PageResult.of(p));
    }

    @GetMapping("/platform/notices")
    public Result<List<SysNotice>> notices() {
        return Result.ok(noticeMapper.selectList(Wrappers.<SysNotice>lambdaQuery()
                .eq(SysNotice::getTenantId, 0).orderByDesc(SysNotice::getId)));
    }

    @PostMapping("/platform/notices")
    @Audit(module = "平台-公告", action = "发布平台公告")
    public Result<Void> saveNotice(@RequestBody SysNotice notice) {
        SecurityUtils.requirePlatform();
        notice.setTenantId(0L);
        if (notice.getId() == null) noticeMapper.insert(notice);
        else noticeMapper.updateById(notice);
        return Result.ok();
    }

    @DeleteMapping("/platform/notices/{id}")
    @Audit(module = "平台-公告", action = "删除平台公告")
    public Result<Void> deleteNotice(@PathVariable Long id) {
        SecurityUtils.requirePlatform();
        noticeMapper.deleteById(id);
        return Result.ok();
    }

    @GetMapping("/platform/users")
    public Result<List<SysPlatformUser>> users() {
        SecurityUtils.requirePlatform();
        return Result.ok(platformUserMapper.selectList(Wrappers.<SysPlatformUser>lambdaQuery().orderByAsc(SysPlatformUser::getId)));
    }

    @PostMapping("/platform/users")
    @Audit(module = "平台-用户", action = "保存平台用户")
    public Result<Void> saveUser(@RequestBody Map<String, Object> body) {
        SecurityUtils.requirePlatform();
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        SysPlatformUser u = id == null ? new SysPlatformUser() : platformUserMapper.selectById(id);
        if (u == null) throw BizException.notFound("用户不存在");
        u.setUsername(String.valueOf(body.get("username")));
        u.setRealName(str(body.get("realName")));
        u.setPhone(str(body.get("phone")));
        u.setRoleCode(str(body.getOrDefault("roleCode", "platform_admin")));
        u.setStatus(1);
        if (id == null) {
            u.setPassword(passwordEncoder.encode(str(body.getOrDefault("password", "admin123"))));
            platformUserMapper.insert(u);
        } else {
            if (body.get("password") != null && !String.valueOf(body.get("password")).isBlank()) {
                u.setPassword(passwordEncoder.encode(String.valueOf(body.get("password"))));
            }
            platformUserMapper.updateById(u);
        }
        return Result.ok();
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static int intVal(Object o, int def) {
        return o == null ? def : Integer.parseInt(String.valueOf(o));
    }

    private static long longVal(Object o, long def) {
        return o == null ? def : Long.parseLong(String.valueOf(o));
    }
}
