package net.jxdg.travel.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import net.jxdg.travel.common.BizException;
import net.jxdg.travel.modules.distribution.entity.BizDistributor;
import net.jxdg.travel.modules.distribution.mapper.BizDistributorMapper;
import net.jxdg.travel.modules.guide.entity.BizGuide;
import net.jxdg.travel.modules.guide.mapper.BizGuideMapper;
import net.jxdg.travel.modules.member.entity.BizMember;
import net.jxdg.travel.modules.member.mapper.BizMemberMapper;
import net.jxdg.travel.modules.resource.entity.BizSupplier;
import net.jxdg.travel.modules.resource.mapper.BizSupplierMapper;
import net.jxdg.travel.modules.scenic.entity.BizScenicStaff;
import net.jxdg.travel.modules.scenic.mapper.BizScenicStaffMapper;
import net.jxdg.travel.modules.system.dto.LoginRequest;
import net.jxdg.travel.modules.system.dto.LoginResult;
import net.jxdg.travel.modules.system.dto.SocialLoginRequest;
import net.jxdg.travel.modules.system.entity.*;
import net.jxdg.travel.modules.system.mapper.*;
import net.jxdg.travel.security.JwtService;
import net.jxdg.travel.security.LoginUser;
import net.jxdg.travel.tenant.TenantContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysPlatformUserMapper platformUserMapper;
    private final SysTenantMapper tenantMapper;
    private final SysTenantUserMapper tenantUserMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;
    private final BizMemberMapper memberMapper;
    private final BizGuideMapper guideMapper;
    private final BizScenicStaffMapper scenicStaffMapper;
    private final BizSupplierMapper supplierMapper;
    private final BizDistributorMapper distributorMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final net.jxdg.travel.wechat.WechatService wechatService;

    /**
     * 平台端/租户端账号登录。
     */
    public LoginResult login(LoginRequest req) {
        if (req == null || req.getUsername() == null || req.getPassword() == null) {
            throw new BizException("请输入账号和密码");
        }
        if ("platform".equalsIgnoreCase(req.getType())) {
            return platformLogin(req);
        }
        return tenantLogin(req);
    }

    private LoginResult platformLogin(LoginRequest req) {
        SysPlatformUser user = platformUserMapper.selectOne(Wrappers.<SysPlatformUser>lambdaQuery()
                .eq(SysPlatformUser::getUsername, req.getUsername())
                .last("limit 1"));
        if (user == null || user.getStatus() == 0 || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BizException(401, "账号或密码错误");
        }
        LoginUser lu = new LoginUser();
        lu.setId(user.getId());
        lu.setUsername(user.getUsername());
        lu.setRealName(user.getRealName());
        lu.setUserType("PLATFORM");
        lu.setTenantId(null);
        Set<String> perms = new HashSet<>();
        perms.add("*");
        List<SysMenu> menus = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .eq(SysMenu::getScope, "platform").eq(SysMenu::getStatus, 1).orderByAsc(SysMenu::getSort));
        menus.forEach(m -> {
            if (m.getPerm() != null && !m.getPerm().isBlank()) perms.add(m.getPerm());
        });
        lu.setPermissions(perms);
        return build(lu, menus, null);
    }

    private LoginResult tenantLogin(LoginRequest req) {
        SysTenant tenant = requireActiveTenant(req.getTenantCode());
        TenantContext.set(tenant.getId(), false, false, tenant.getCode());
        try {
            SysTenantUser user = tenantUserMapper.selectOne(Wrappers.<SysTenantUser>lambdaQuery()
                    .eq(SysTenantUser::getUsername, req.getUsername())
                    .eq(SysTenantUser::getTenantId, tenant.getId())
                    .last("limit 1"));
            if (user == null || user.getStatus() == 0 || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
                throw new BizException(401, "账号或密码错误");
            }
            LoginUser lu = new LoginUser();
            lu.setId(user.getId());
            lu.setUsername(user.getUsername());
            lu.setRealName(user.getRealName());
            lu.setUserType("TENANT");
            lu.setTenantId(tenant.getId());
            lu.setTenantCode(tenant.getCode());

            List<SysUserRole> userRoles = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                    .eq(SysUserRole::getUserId, user.getId()).eq(SysUserRole::getUserType, "tenant"));
            List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).toList();
            List<SysRole> roles = roleIds.isEmpty() ? List.of() : roleMapper.selectBatchIds(roleIds);
            boolean isAdmin = Objects.equals(user.getIsAdmin(), 1) || roles.stream().anyMatch(r -> "tenant_admin".equals(r.getCode()));

            List<SysMenu> menus;
            if (isAdmin) {
                menus = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                        .eq(SysMenu::getScope, "tenant").eq(SysMenu::getStatus, 1).orderByAsc(SysMenu::getSort));
            } else {
                List<Long> menuIds = roleIds.isEmpty() ? List.of() : roleMenuMapper.selectList(
                                Wrappers.<SysRoleMenu>lambdaQuery().in(SysRoleMenu::getRoleId, roleIds))
                        .stream().map(SysRoleMenu::getMenuId).distinct().toList();
                menus = menuIds.isEmpty() ? List.of() : menuMapper.selectBatchIds(menuIds).stream()
                        .filter(m -> "tenant".equals(m.getScope()) && Objects.equals(m.getStatus(), 1))
                        .sorted(Comparator.comparing(SysMenu::getSort)).toList();
            }
            Set<String> perms = new HashSet<>();
            if (isAdmin) perms.add("*");
            menus.forEach(m -> {
                if (m.getPerm() != null && !m.getPerm().isBlank()) perms.add(m.getPerm());
            });
            lu.setPermissions(perms);
            return build(lu, menus, tenant);
        } finally {
            TenantContext.clear();
        }
    }

    public SysTenant requireActiveTenant(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BizException("缺少租户编码");
        }
        SysTenant tenant = tenantMapper.selectOne(Wrappers.<SysTenant>lambdaQuery()
                .eq(SysTenant::getCode, tenantCode).last("limit 1"));
        if (tenant == null) {
            throw new BizException(404, "租户不存在");
        }
        if (Objects.equals(tenant.getStatus(), 3)) {
            throw new BizException(403, "该租户已停用，请联系平台管理员");
        }
        if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDateTime.now())) {
            tenant.setStatus(4);
            tenantMapper.updateById(tenant);
            throw new BizException(403, "该租户已到期，请联系平台管理员续期");
        }
        if (!Objects.equals(tenant.getStatus(), 1) && !Objects.equals(tenant.getStatus(), 2)) {
            throw new BizException(403, "该租户状态异常，无法登录");
        }
        return tenant;
    }

    /** 游客端 UnionID 登录（小程序/公众号/H5/App 互通） */
    @Transactional
    public LoginResult memberLogin(SocialLoginRequest req) {
        SysTenant tenant = requireActiveTenant(req.getTenantCode());
        TenantContext.set(tenant.getId(), false, false, tenant.getCode());
        try {
            Map<String, String> session = wechatService.code2Session(tenant.getId(), req.getCode());
            String unionId = req.getUnionId() != null ? req.getUnionId() : session.get("unionid");
            String openId = req.getOpenId() != null ? req.getOpenId() : session.get("openid");
            if (unionId == null) {
                throw new BizException("微信登录失败：未获取到 UnionID");
            }
            BizMember member = memberMapper.selectOne(Wrappers.<BizMember>lambdaQuery()
                    .eq(BizMember::getUnionId, unionId).last("limit 1"));
            if (member == null) {
                member = new BizMember();
                member.setTenantId(tenant.getId());
                member.setUnionId(unionId);
                member.setOpenId(openId);
                member.setNickname(req.getNickname() == null ? "微信用户" : req.getNickname());
                member.setAvatar(req.getAvatar());
                member.setLevel(1);
                member.setPoints(0);
                member.setStatus(1);
                member.setLastActiveAt(LocalDateTime.now());
                memberMapper.insert(member);
            } else {
                if (openId != null) member.setOpenId(openId);
                if (req.getNickname() != null) member.setNickname(req.getNickname());
                member.setLastActiveAt(LocalDateTime.now());
                memberMapper.updateById(member);
            }
            LoginUser lu = new LoginUser();
            lu.setId(member.getId());
            lu.setUsername(member.getNickname());
            lu.setRealName(member.getNickname());
            lu.setUserType("MEMBER");
            lu.setTenantId(tenant.getId());
            lu.setTenantCode(tenant.getCode());
            lu.setPermissions(Set.of("member:self"));
            return build(lu, List.of(), tenant);
        } finally {
            TenantContext.clear();
        }
    }

    /** 导游端登录：UnionID 绑定导游档案 */
    public LoginResult guideLogin(SocialLoginRequest req) {
        SysTenant tenant = requireActiveTenant(req.getTenantCode());
        TenantContext.set(tenant.getId(), false, false, tenant.getCode());
        try {
            BizGuide guide = null;
            if (req.getUnionId() != null) {
                guide = guideMapper.selectOne(Wrappers.<BizGuide>lambdaQuery()
                        .eq(BizGuide::getUnionId, req.getUnionId()).last("limit 1"));
            }
            if (guide == null && req.getPhone() != null) {
                guide = guideMapper.selectOne(Wrappers.<BizGuide>lambdaQuery()
                        .eq(BizGuide::getPhone, req.getPhone()).last("limit 1"));
            }
            if (guide == null) {
                throw new BizException(401, "未找到绑定该微信/手机号的导游档案");
            }
            if (req.getUnionId() != null && guide.getUnionId() == null) {
                guide.setUnionId(req.getUnionId());
                guideMapper.updateById(guide);
            }
            LoginUser lu = new LoginUser();
            lu.setId(guide.getId());
            lu.setUsername(guide.getName());
            lu.setRealName(guide.getName());
            lu.setUserType("GUIDE");
            lu.setTenantId(tenant.getId());
            lu.setTenantCode(tenant.getCode());
            lu.setPermissions(Set.of("guide:self"));
            return build(lu, List.of(), tenant);
        } finally {
            TenantContext.clear();
        }
    }

    public LoginResult scenicLogin(SocialLoginRequest req) {
        SysTenant tenant = requireActiveTenant(req.getTenantCode());
        TenantContext.set(tenant.getId(), false, false, tenant.getCode());
        try {
            BizScenicStaff staff = scenicStaffMapper.selectOne(Wrappers.<BizScenicStaff>lambdaQuery()
                    .eq(BizScenicStaff::getUsername, req.getUsername())
                    .eq(BizScenicStaff::getTenantId, tenant.getId()).last("limit 1"));
            if (staff == null || !passwordEncoder.matches(req.getPassword(), staff.getPassword())) {
                throw new BizException(401, "账号或密码错误");
            }
            LoginUser lu = new LoginUser();
            lu.setId(staff.getId());
            lu.setUsername(staff.getUsername());
            lu.setRealName(staff.getName());
            lu.setUserType("SCENIC");
            lu.setTenantId(tenant.getId());
            lu.setTenantCode(tenant.getCode());
            lu.setPermissions(Set.of("scenic:self"));
            return build(lu, List.of(), tenant);
        } finally {
            TenantContext.clear();
        }
    }

    /** 供应商开放端登录（酒店/车队/餐厅/地接社） */
    public LoginResult supplierLogin(SocialLoginRequest req) {
        SysTenant tenant = requireActiveTenant(req.getTenantCode());
        TenantContext.set(tenant.getId(), false, false, tenant.getCode());
        try {
            BizSupplier s = supplierMapper.selectOne(Wrappers.<BizSupplier>lambdaQuery()
                    .eq(BizSupplier::getUsername, req.getUsername()).last("limit 1"));
            if (s == null || s.getPassword() == null || !passwordEncoder.matches(req.getPassword(), s.getPassword())) {
                throw new BizException(401, "账号或密码错误");
            }
            LoginUser lu = new LoginUser();
            lu.setId(s.getId());
            lu.setUsername(s.getUsername());
            lu.setRealName(s.getContact() == null ? s.getName() : s.getContact());
            lu.setUserType("SUPPLIER");
            lu.setTenantId(tenant.getId());
            lu.setTenantCode(tenant.getCode());
            lu.setPermissions(Set.of("supplier:self"));
            return build(lu, List.of(), tenant);
        } finally {
            TenantContext.clear();
        }
    }

    public LoginResult distributorLogin(SocialLoginRequest req) {
        SysTenant tenant = requireActiveTenant(req.getTenantCode());
        TenantContext.set(tenant.getId(), false, false, tenant.getCode());
        try {
            BizDistributor d = distributorMapper.selectOne(Wrappers.<BizDistributor>lambdaQuery()
                    .eq(BizDistributor::getUsername, req.getUsername()).last("limit 1"));
            if (d == null || !passwordEncoder.matches(req.getPassword(), d.getPassword())) {
                throw new BizException(401, "账号或密码错误");
            }
            LoginUser lu = new LoginUser();
            lu.setId(d.getId());
            lu.setUsername(d.getUsername());
            lu.setRealName(d.getName());
            lu.setUserType("DISTRIBUTOR");
            lu.setTenantId(tenant.getId());
            lu.setTenantCode(tenant.getCode());
            lu.setPermissions(Set.of("distributor:self"));
            return build(lu, List.of(), tenant);
        } finally {
            TenantContext.clear();
        }
    }

    private LoginResult build(LoginUser lu, List<SysMenu> menus, SysTenant tenant) {
        LoginResult r = new LoginResult();
        r.setToken(jwtService.create(lu, null));
        r.setUserId(lu.getId());
        r.setUsername(lu.getUsername());
        r.setRealName(lu.getRealName());
        r.setUserType(lu.getUserType());
        r.setTenantId(lu.getTenantId());
        r.setTenantCode(lu.getTenantCode());
        r.setTenantName(tenant == null ? "平台管理" : tenant.getName());
        r.setPermissions(lu.getPermissions());
        r.setMenus(menus.stream().map(m -> {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", m.getId());
            node.put("parentId", m.getParentId());
            node.put("title", m.getTitle());
            node.put("name", m.getName());
            node.put("path", m.getPath());
            node.put("component", m.getComponent());
            node.put("icon", m.getIcon());
            node.put("perm", m.getPerm());
            node.put("type", m.getType());
            node.put("sort", m.getSort());
            return node;
        }).toList());
        return r;
    }

    public LoginResult current() {
        LoginUser lu = net.jxdg.travel.security.SecurityUtils.current();
        SysTenant tenant = null;
        if (lu.getTenantId() != null) {
            tenant = tenantMapper.selectById(lu.getTenantId());
        }
        List<SysMenu> menus = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .eq(SysMenu::getScope, "PLATFORM".equals(lu.getUserType()) ? "platform" : "tenant")
                .eq(SysMenu::getStatus, 1).orderByAsc(SysMenu::getSort));
        if (!"PLATFORM".equals(lu.getUserType())) {
            Set<Long> allowed = new HashSet<>();
            if (lu.getPermissions() != null && lu.getPermissions().contains("*")) {
                menus.forEach(m -> allowed.add(m.getId()));
            } else {
                List<SysUserRole> urs = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getUserId, lu.getId()).eq(SysUserRole::getUserType, "tenant"));
                List<Long> roleIds = urs.stream().map(SysUserRole::getRoleId).toList();
                if (!roleIds.isEmpty()) {
                    roleMenuMapper.selectList(Wrappers.<SysRoleMenu>lambdaQuery().in(SysRoleMenu::getRoleId, roleIds))
                            .forEach(rm -> allowed.add(rm.getMenuId()));
                }
            }
            menus = menus.stream().filter(m -> allowed.contains(m.getId())).toList();
        }
        LoginResult r = new LoginResult();
        r.setUserId(lu.getId());
        r.setUsername(lu.getUsername());
        r.setRealName(lu.getRealName());
        r.setUserType(lu.getUserType());
        r.setTenantId(lu.getTenantId());
        r.setTenantCode(lu.getTenantCode());
        r.setTenantName(tenant == null ? "平台管理" : tenant.getName());
        r.setPermissions(lu.getPermissions());
        r.setMenus(menus.stream().map(m -> {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", m.getId());
            node.put("parentId", m.getParentId());
            node.put("title", m.getTitle());
            node.put("name", m.getName());
            node.put("path", m.getPath());
            node.put("component", m.getComponent());
            node.put("icon", m.getIcon());
            node.put("perm", m.getPerm());
            node.put("type", m.getType());
            node.put("sort", m.getSort());
            return node;
        }).toList());
        return r;
    }
}
