package net.jxdg.travel.modules.system.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import net.jxdg.travel.common.*;
import net.jxdg.travel.modules.member.entity.BizMember;
import net.jxdg.travel.modules.member.mapper.BizMemberMapper;
import net.jxdg.travel.modules.system.entity.SysMessageLog;
import net.jxdg.travel.modules.system.entity.SysMessageTemplate;
import net.jxdg.travel.modules.system.entity.SysNav;
import net.jxdg.travel.modules.system.entity.SysWechatConfig;
import net.jxdg.travel.modules.system.mapper.SysMessageLogMapper;
import net.jxdg.travel.modules.system.mapper.SysMessageTemplateMapper;
import net.jxdg.travel.modules.system.mapper.SysNavMapper;
import net.jxdg.travel.modules.system.mapper.SysWechatConfigMapper;
import net.jxdg.travel.tenant.TenantContext;
import net.jxdg.travel.wechat.WechatService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
public class WechatController {

    private final SysWechatConfigMapper configMapper;
    private final SysMessageTemplateMapper templateMapper;
    private final SysMessageLogMapper messageLogMapper;
    private final SysNavMapper navMapper;
    private final net.jxdg.travel.modules.system.mapper.SysTenantMapper tenantMapper;
    private final BizMemberMapper memberMapper;
    private final WechatService wechatService;
    private final CryptoUtil cryptoUtil;

    @GetMapping("/system/wechat/configs")
    public Result<List<Map<String, Object>>> configs() {
        List<SysWechatConfig> list = configMapper.selectList(Wrappers.<SysWechatConfig>lambdaQuery());
        List<Map<String, Object>> out = new ArrayList<>();
        for (SysWechatConfig c : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", c.getConfigKey());
            String value = c.getEncrypted() == 1 ? cryptoUtil.decrypt(c.getConfigValue()) : c.getConfigValue();
            m.put("value", c.getEncrypted() == 1 ? CryptoUtil.maskSecret(value) : value);
            m.put("encrypted", c.getEncrypted());
            out.add(m);
        }
        return Result.ok(out);
    }

    @PutMapping("/system/wechat/configs")
    @Audit(module = "微信配置", action = "保存微信配置")
    public Result<Void> save(@RequestBody Map<String, String> body) {
        Long tenantId = TenantContext.getTenantId();
        body.forEach((k, v) -> {
            if (v != null && v.contains("****")) return; // 回掩码未修改
            SysWechatConfig c = configMapper.selectOne(Wrappers.<SysWechatConfig>lambdaQuery()
                    .eq(SysWechatConfig::getConfigKey, k).last("limit 1"));
            boolean secret = k.toLowerCase().contains("secret") || k.toLowerCase().contains("aes");
            if (c == null) {
                c = new SysWechatConfig();
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

    @GetMapping("/system/message/templates")
    public Result<List<SysMessageTemplate>> templates() {
        return Result.ok(templateMapper.selectList(Wrappers.<SysMessageTemplate>lambdaQuery().orderByAsc(SysMessageTemplate::getId)));
    }

    @PostMapping("/system/message/templates")
    @Audit(module = "消息配置", action = "保存消息模板")
    public Result<Void> saveTemplate(@RequestBody SysMessageTemplate t) {
        if (t.getTenantId() == null) t.setTenantId(TenantContext.getTenantId());
        SysMessageTemplate exist = templateMapper.selectOne(Wrappers.<SysMessageTemplate>lambdaQuery()
                .eq(SysMessageTemplate::getTenantId, t.getTenantId())
                .eq(SysMessageTemplate::getBizType, t.getBizType()).last("limit 1"));
        if (exist != null) {
            exist.setTemplateId(t.getTemplateId());
            exist.setTitle(t.getTitle());
            exist.setFieldsJson(t.getFieldsJson());
            exist.setEnabled(t.getEnabled());
            templateMapper.updateById(exist);
        } else {
            templateMapper.insert(t);
        }
        return Result.ok();
    }

    @GetMapping("/system/message/logs")
    public Result<PageResult<SysMessageLog>> logs(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "20") long size) {
        return Result.ok(PageResult.of(messageLogMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size),
                Wrappers.<SysMessageLog>lambdaQuery().orderByDesc(SysMessageLog::getId))));
    }

    /** 公众号自定义菜单同步微信（未配置真实公众号时为模拟同步，本地保存一致） */
    @PostMapping("/system/wechat/menu/sync")
    @Audit(module = "微信配置", action = "同步公众号菜单")
    public Result<Map<String, Object>> syncMenu(@RequestBody String menuJson) {
        SysNav nav = navMapper.selectOne(Wrappers.<SysNav>lambdaQuery().eq(SysNav::getPlatform, "mp").last("limit 1"));
        if (nav == null) {
            nav = new SysNav();
            nav.setTenantId(TenantContext.getTenantId());
            nav.setPlatform("mp");
            nav.setContent(menuJson);
            navMapper.insert(nav);
        } else {
            nav.setContent(menuJson);
            navMapper.updateById(nav);
        }
        return Result.ok(Map.of("synced", true, "mock", true, "message", "菜单已保存并模拟同步微信"));
    }

    @GetMapping("/public/wechat/oauth-url")
    public Result<String> oauthUrl(@RequestParam String tenantCode, @RequestParam String redirectUri,
                                   @RequestParam(required = false) String state) {
        var tenant = tenantMapper.selectOne(Wrappers.<net.jxdg.travel.modules.system.entity.SysTenant>lambdaQuery()
                .eq(net.jxdg.travel.modules.system.entity.SysTenant::getCode, tenantCode).last("limit 1"));
        if (tenant == null) throw BizException.notFound("租户不存在");
        return Result.ok(wechatService.oauthUrl(tenant.getId(), redirectUri, state));
    }

    @GetMapping("/public/wechat/jsapi")
    public Result<Map<String, String>> jsapi(@RequestParam Long tenantId, @RequestParam String url) {
        return Result.ok(wechatService.jsapiSignature(tenantId, url));
    }

    @GetMapping("/public/wechat/subscribe-reply")
    public Result<String> reply(@RequestParam(required = false) String keyword) {
        return Result.ok(wechatService.subscribeReply(keyword));
    }

    /** 公众号服务器地址校验 */
    @GetMapping("/wechat/callback")
    public String verify(@RequestParam(value = "signature", required = false) String signature,
                         @RequestParam(value = "echostr", required = false) String echostr) {
        return echostr == null ? "ok" : echostr;
    }

    /** 事件回调：关注/取关/菜单点击/消息 */
    @PostMapping("/wechat/callback")
    public String callback(@RequestBody String body) {
        if (body != null && body.contains("unsubscribe")) return "success";
        if (body != null && body.contains("subscribe")) return wechatService.subscribeReply(null);
        return "success";
    }

    /** 内容运营：图文素材（占位，可对接微信永久素材 API） */
    @GetMapping("/system/wechat/materials")
    public Result<List<Map<String, Object>>> materials() {
        return Result.ok(List.of(
                Map.of("mediaId", "mock_media_1", "title", "雪乡冬季线路推荐", "url", "/products/1"),
                Map.of("mediaId", "mock_media_2", "title", "冰雪大世界开园公告", "url", "/products/2")));
    }

    @GetMapping("/system/wechat/tags")
    public Result<List<Map<String, Object>>> tags() {
        List<BizMember> members = memberMapper.selectList(Wrappers.<BizMember>lambdaQuery());
        long active = members.stream().filter(m -> m.getLastActiveAt() != null).count();
        return Result.ok(List.of(
                Map.of("id", 1, "name", "高活跃会员", "count", active),
                Map.of("id", 2, "name", "沉默会员", "count", members.size() - active)));
    }

    @GetMapping("/system/wechat/fans")
    public Result<PageResult<BizMember>> fans(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "20") long size) {
        return Result.ok(PageResult.of(memberMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size),
                Wrappers.<BizMember>lambdaQuery().isNotNull(BizMember::getOpenId).orderByDesc(BizMember::getId))));
    }

    @PostMapping("/system/wechat/mass")
    @Audit(module = "微信配置", action = "定向群发")
    public Result<Map<String, Object>> mass(@RequestBody Map<String, Object> body) {
        long total = memberMapper.selectCount(Wrappers.<BizMember>lambdaQuery().isNotNull(BizMember::getOpenId));
        return Result.ok(Map.of("sent", total, "mock", true, "message", "群发任务已提交（模拟），真实发送受微信频率限制"));
    }

    @GetMapping("/public/wechat/config")
    public Result<Map<String, Object>> publicConfig(@RequestParam String tenantCode) {
        var tenant = tenantMapper.selectOne(Wrappers.<net.jxdg.travel.modules.system.entity.SysTenant>lambdaQuery()
                .eq(net.jxdg.travel.modules.system.entity.SysTenant::getCode, tenantCode).last("limit 1"));
        if (tenant == null) throw BizException.notFound("租户不存在");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tenantId", tenant.getId());
        m.put("appId", wechatService.getConfig(tenant.getId(), "mp.appid"));
        m.put("mpAppId", wechatService.getConfig(tenant.getId(), "mp.appid"));
        m.put("mpAppSecretSet", wechatService.getConfig(tenant.getId(), "mp.secret") != null);
        return Result.ok(m);
    }

}
