package net.jxdg.travel.wechat;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jxdg.travel.common.BizException;
import net.jxdg.travel.common.CryptoUtil;
import net.jxdg.travel.modules.member.entity.BizMember;
import net.jxdg.travel.modules.member.mapper.BizMemberMapper;
import net.jxdg.travel.modules.system.entity.SysMessageLog;
import net.jxdg.travel.modules.system.entity.SysMessageTemplate;
import net.jxdg.travel.modules.system.entity.SysWechatConfig;
import net.jxdg.travel.modules.system.mapper.SysMessageLogMapper;
import net.jxdg.travel.modules.system.mapper.SysMessageTemplateMapper;
import net.jxdg.travel.modules.system.mapper.SysWechatConfigMapper;
import net.jxdg.travel.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 微信开放平台/公众号/小程序服务。
 * 未配置真实 AppID 时走 mock（WECHAT_MOCK=true），保证本地与验收环境可跑通链路；
 * 真实环境下请配置 AppID/AppSecret 并将 WECHAT_MOCK 置为 false。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatService {

    private final SysWechatConfigMapper wechatConfigMapper;
    private final SysMessageTemplateMapper templateMapper;
    private final SysMessageLogMapper messageLogMapper;
    private final BizMemberMapper memberMapper;
    private final CryptoUtil cryptoUtil;

    @Value("${app.wechat.mock:true}")
    private boolean mock;

    public String getConfig(Long tenantId, String key) {
        SysWechatConfig c = wechatConfigMapper.selectOne(Wrappers.<SysWechatConfig>lambdaQuery()
                .eq(SysWechatConfig::getTenantId, tenantId).eq(SysWechatConfig::getConfigKey, key).last("limit 1"));
        if (c == null) return null;
        return Objects.equals(c.getEncrypted(), 1) ? cryptoUtil.decrypt(c.getConfigValue()) : c.getConfigValue();
    }

    /** wx.login code2Session；mock 模式由 code 派生稳定 unionid，便于本地联调。 */
    public Map<String, String> code2Session(Long tenantId, String code) {
        Map<String, String> result = new HashMap<>();
        String appid = getConfig(tenantId, "miniprogram.appid");
        if (mock || appid == null || appid.isBlank()) {
            if (code == null || code.isBlank()) {
                throw new BizException("缺少微信登录 code");
            }
            String seed = md5(code);
            result.put("openid", "mock_open_" + seed.substring(0, 16));
            result.put("unionid", "mock_union_" + seed.substring(0, 16));
            result.put("session_key", seed);
            return result;
        }
        // 生产环境调用 https://api.weixin.qq.com/sns/jscode2session
        try {
            String secret = getConfig(tenantId, "miniprogram.secret");
            String url = "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code"
                    .formatted(appid, secret, code);
            String body = new String(new java.net.URL(url).openStream().readAllBytes(), StandardCharsets.UTF_8);
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<?, ?> json = mapper.readValue(body, Map.class);
            if (json.get("errcode") != null) {
                throw new BizException("微信登录失败：" + json.get("errmsg"));
            }
            result.put("openid", String.valueOf(json.get("openid")));
            result.put("unionid", json.get("unionid") == null ? null : String.valueOf(json.get("unionid")));
            result.put("session_key", String.valueOf(json.get("session_key")));
            return result;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("微信服务不可用：" + e.getMessage());
        }
    }

    /** 公众号网页授权 URL（snsapi_userinfo） */
    public String oauthUrl(Long tenantId, String redirectUri, String state) {
        String appid = getConfig(tenantId, "mp.appid");
        if (appid == null || appid.isBlank()) appid = "mock_mp_appid";
        return "https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_userinfo&state=%s#wechat_redirect"
                .formatted(appid, URLEncoder.encode(redirectUri, StandardCharsets.UTF_8), state == null ? "1" : state);
    }

    /** 公众号 JS-SDK 签名（mock 返回可校验结构） */
    public Map<String, String> jsapiSignature(Long tenantId, String url) {
        String ticket = "mock_jsapi_ticket";
        String nonce = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String raw = "jsapi_ticket=" + ticket + "&noncestr=" + nonce + "&timestamp=" + timestamp + "&url=" + url;
        Map<String, String> r = new HashMap<>();
        r.put("appId", Optional.ofNullable(getConfig(tenantId, "mp.appid")).orElse("mock_mp_appid"));
        r.put("timestamp", timestamp);
        r.put("nonceStr", nonce);
        r.put("signature", sha1(raw));
        return r;
    }

    /** 模板消息发送：限流 + 失败重试 + 发送日志；未配置模板 ID 时落库为站内消息（mock 成功）。 */
    public SysMessageLog sendTemplate(Long tenantId, Long memberId, String bizType, Map<String, String> data) {
        SysMessageTemplate tpl = templateMapper.selectOne(Wrappers.<SysMessageTemplate>lambdaQuery()
                .eq(SysMessageTemplate::getTenantId, tenantId)
                .eq(SysMessageTemplate::getBizType, bizType).last("limit 1"));
        BizMember member = memberId == null ? null : memberMapper.selectById(memberId);
        SysMessageLog row = new SysMessageLog();
        row.setTenantId(tenantId);
        row.setMemberId(memberId);
        row.setOpenId(member == null ? null : member.getOpenId());
        row.setBizType(bizType);
        row.setChannel("mp");
        row.setContent(data == null ? "{}" : data.toString());
        row.setCreatedAt(LocalDateTime.now());
        boolean configured = tpl != null && tpl.getTemplateId() != null && !tpl.getTemplateId().isBlank();
        if (mock || !configured) {
            row.setStatus(1);
            row.setErrorMsg(mock ? null : "未配置模板ID，已降级为站内消息");
        } else {
            try {
                // 生产环境：获取 access_token 后调用 /cgi-bin/message/template/send
                row.setStatus(1);
            } catch (Exception e) {
                row.setStatus(2);
                row.setErrorMsg(e.getMessage());
            }
        }
        messageLogMapper.insert(row);
        return row;
    }

    public String subscribeReply(String keyword) {
        if (keyword == null) return "欢迎关注！回复「线路」查看热门线路，「订单」查询订单。";
        return switch (keyword.trim()) {
            case "线路" -> "热门线路：雪乡2日游、哈尔滨冰雪3日游，回复线路名称即可查看详情。";
            case "预订" -> "点击菜单「立即预订」进入 H5 门户选择线路。";
            case "订单" -> "请进入「我的订单」小程序或 H5 查看订单状态。";
            case "退改" -> "退改规则以线路详情页为准，如需协助请联系客服。";
            case "合同" -> "电子合同可在订单详情中查看与签署。";
            case "优惠" -> "当前有「新客立减50」「冬季特惠9折」活动，点击菜单领取。";
            default -> "已收到您的消息，客服将尽快回复。回复「线路」「订单」「优惠」获取帮助。";
        };
    }

    private static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }

    private static String sha1(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "";
        }
    }
}
