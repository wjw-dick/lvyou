package net.jxdg.travel.modules.product.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import net.jxdg.travel.common.BizException;
import net.jxdg.travel.common.Result;
import net.jxdg.travel.modules.content.entity.BizAiConversation;
import net.jxdg.travel.modules.content.entity.BizCustomRequest;
import net.jxdg.travel.modules.content.entity.BizUgcPost;
import net.jxdg.travel.modules.content.mapper.BizAiConversationMapper;
import net.jxdg.travel.modules.content.mapper.BizCustomRequestMapper;
import net.jxdg.travel.modules.content.mapper.BizUgcPostMapper;
import net.jxdg.travel.modules.contract.entity.BizContract;
import net.jxdg.travel.modules.contract.mapper.BizContractMapper;
import net.jxdg.travel.modules.order.entity.BizOrder;
import net.jxdg.travel.modules.order.mapper.BizOrderMapper;
import net.jxdg.travel.modules.order.service.OrderService;
import net.jxdg.travel.modules.product.entity.BizDestination;
import net.jxdg.travel.modules.product.entity.BizProduct;
import net.jxdg.travel.modules.product.entity.BizProductItinerary;
import net.jxdg.travel.modules.product.entity.BizSchedule;
import net.jxdg.travel.modules.product.mapper.BizDestinationMapper;
import net.jxdg.travel.modules.product.mapper.BizProductItineraryMapper;
import net.jxdg.travel.modules.product.mapper.BizProductMapper;
import net.jxdg.travel.modules.product.mapper.BizScheduleMapper;
import net.jxdg.travel.modules.report.entity.BizCurrencyRate;
import net.jxdg.travel.modules.report.mapper.BizCurrencyRateMapper;
import net.jxdg.travel.modules.system.entity.*;
import net.jxdg.travel.modules.system.mapper.*;
import net.jxdg.travel.tenant.TenantContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 公开门户/小程序/公众号/H5/App 公共接口（无需登录）。
 * 租户由 PublicTenantInterceptor 通过 X-Tenant-Code 解析，查询自动租户隔离。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/public")
public class PublicController {

    private final BizProductMapper productMapper;
    private final BizProductItineraryMapper itineraryMapper;
    private final BizScheduleMapper scheduleMapper;
    private final BizDestinationMapper destinationMapper;
    private final SysTenantMapper tenantMapper;
    private final SysConfigMapper configMapper;
    private final SysNoticeMapper noticeMapper;
    private final SysNavMapper navMapper;
    private final SysContentI18nMapper contentI18nMapper;
    private final BizContractMapper contractMapper;
    private final BizCustomRequestMapper customRequestMapper;
    private final BizUgcPostMapper ugcPostMapper;
    private final BizAiConversationMapper aiConversationMapper;
    private final BizCurrencyRateMapper currencyRateMapper;
    private final BizOrderMapper orderMapper;
    private final OrderService orderService;
    private final StringRedisTemplate redis;

    @GetMapping("/tenant")
    public Result<Map<String, Object>> tenant(@RequestParam(required = false) String tenantCode) {
        Long tenantId = requireTenant();
        SysTenant t = tenantMapper.selectById(tenantId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("code", t.getCode());
        m.put("name", t.getName());
        m.put("province", t.getProvince());
        m.put("city", t.getCity());
        m.put("phone", t.getPhone());
        configMapper.selectList(Wrappers.<SysConfig>lambdaQuery()).forEach(c -> {
            if (!c.getConfigKey().toLowerCase().contains("secret")) {
                m.put(c.getConfigKey(), c.getConfigValue());
            }
        });
        return Result.ok(m);
    }

    @GetMapping("/products")
    public Result<Map<String, Object>> products(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "12") long size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String category,
                                                @RequestParam(required = false) String destination,
                                                @RequestParam(required = false) Integer days,
                                                @RequestParam(required = false) java.math.BigDecimal minPrice,
                                                @RequestParam(required = false) java.math.BigDecimal maxPrice,
                                                @RequestParam(required = false, defaultValue = "h5") String channel) {
        Page<BizProduct> p = productMapper.selectPage(new Page<>(page, size), Wrappers.<BizProduct>lambdaQuery()
                .eq(BizProduct::getStatus, 1)
                .like(channel != null && !channel.isBlank(), BizProduct::getChannels, channel)
                .and(keyword != null && !keyword.isBlank(), w -> w.like(BizProduct::getTitle, keyword)
                        .or().like(BizProduct::getDestination, keyword))
                .eq(category != null && !category.isBlank(), BizProduct::getCategory, category)
                .like(destination != null && !destination.isBlank(), BizProduct::getDestination, destination)
                .eq(days != null, BizProduct::getDays, days)
                .ge(minPrice != null, BizProduct::getAdultPrice, minPrice)
                .le(maxPrice != null, BizProduct::getAdultPrice, maxPrice)
                .orderByDesc(BizProduct::getSort).orderByDesc(BizProduct::getId));
        return Result.ok(Map.of("records", p.getRecords(), "total", p.getTotal(), "current", p.getCurrent(), "size", p.getSize()));
    }

    @GetMapping("/products/{id}")
    public Result<Map<String, Object>> product(@PathVariable Long id) {
        BizProduct p = productMapper.selectById(id);
        if (p == null || p.getStatus() != 1) throw BizException.notFound("线路不存在或已下架");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("product", p);
        out.put("itineraries", itineraryMapper.selectList(Wrappers.<BizProductItinerary>lambdaQuery()
                .eq(BizProductItinerary::getProductId, id).orderByAsc(BizProductItinerary::getDayNo)));
        out.put("schedules", scheduleMapper.selectList(Wrappers.<BizSchedule>lambdaQuery()
                .eq(BizSchedule::getProductId, id)
                .ge(BizSchedule::getDepartDate, LocalDate.now())
                .in(BizSchedule::getStatus, 1, 2, 3)
                .orderByAsc(BizSchedule::getDepartDate)));
        return Result.ok(out);
    }

    @GetMapping("/products/{id}/schedules")
    public Result<List<Map<String, Object>>> schedules(@PathVariable Long id, @RequestParam(required = false) String month) {
        LocalDate start = month == null || month.isBlank() ? LocalDate.now() : LocalDate.parse(month + "-01");
        LocalDate end = start.plusMonths(1).minusDays(1);
        List<Map<String, Object>> out = new ArrayList<>();
        for (BizSchedule s : scheduleMapper.selectList(Wrappers.<BizSchedule>lambdaQuery()
                .eq(BizSchedule::getProductId, id)
                .between(BizSchedule::getDepartDate, start, end)
                .in(BizSchedule::getStatus, 1, 2, 3)
                .orderByAsc(BizSchedule::getDepartDate))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("scheduleNo", s.getScheduleNo());
            m.put("departDate", s.getDepartDate());
            m.put("quota", s.getQuota());
            m.put("sold", s.getSold());
            m.put("left", Math.max(0, s.getQuota() - s.getSold()));
            m.put("status", s.getStatus());
            out.add(m);
        }
        return Result.ok(out);
    }

    @GetMapping("/destinations")
    public Result<List<BizDestination>> destinations() {
        return Result.ok(destinationMapper.selectList(Wrappers.<BizDestination>lambdaQuery()
                .eq(BizDestination::getStatus, 1).orderByAsc(BizDestination::getSort)));
    }

    @GetMapping("/notices")
    public Result<List<SysNotice>> notices() {
        return Result.ok(noticeMapper.selectList(Wrappers.<SysNotice>lambdaQuery()
                .eq(SysNotice::getStatus, 1).orderByDesc(SysNotice::getId)));
    }

    @GetMapping("/nav")
    public Result<String> nav(@RequestParam(defaultValue = "h5") String platform) {
        SysNav nav = navMapper.selectOne(Wrappers.<SysNav>lambdaQuery()
                .eq(SysNav::getPlatform, platform).last("limit 1"));
        return Result.ok(nav == null ? "[]" : nav.getContent());
    }

    @GetMapping("/i18n")
    public Result<List<SysContentI18n>> i18n(@RequestParam String bizType, @RequestParam Long bizId) {
        return Result.ok(contentI18nMapper.selectList(Wrappers.<SysContentI18n>lambdaQuery()
                .eq(SysContentI18n::getBizType, bizType).eq(SysContentI18n::getBizId, bizId)));
    }

    /** 游客下单（可匿名，实名制游客信息加密存储） */
    @PostMapping("/orders")
    public Result<BizOrder> createOrder(@RequestBody Map<String, Object> body) {
        requireTenant();
        return Result.ok(orderService.create(null, body));
    }

    /** 订单查询：订单号 + 手机尾号，限流防撞库；仅返回摘要 */
    @PostMapping("/order-query")
    public Result<Map<String, Object>> orderQuery(@RequestBody Map<String, Object> body,
                                                   jakarta.servlet.http.HttpServletRequest request) {
        rateLimit("order-query:" + clientIp(request), 10);
        String orderNo = str(body.get("orderNo"));
        String phoneTail = str(body.get("phoneTail"));
        if (orderNo == null || phoneTail == null) throw new BizException("请输入订单号和手机尾号");
        BizOrder order = orderMapper.selectOne(Wrappers.<BizOrder>lambdaQuery()
                .eq(BizOrder::getOrderNo, orderNo).last("limit 1"));
        if (order == null || order.getContactPhone() == null || !order.getContactPhone().endsWith(phoneTail)) {
            throw new BizException("订单号或手机尾号不正确");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderNo", order.getOrderNo());
        m.put("productTitle", order.getProductTitle());
        m.put("departDate", order.getDepartDate());
        m.put("status", order.getStatus());
        m.put("payStatus", order.getPayStatus());
        m.put("verifyStatus", order.getVerifyStatus());
        m.put("contactMask", order.getContactName() == null ? "" : order.getContactName().charAt(0) + "**");
        return Result.ok(m);
    }

    /** 合同验真：只返回摘要 */
    @GetMapping("/contracts/verify")
    public Result<Map<String, Object>> verifyContract(@RequestParam String contractNo,
                                                       jakarta.servlet.http.HttpServletRequest request) {
        rateLimit("contract-verify:" + clientIp(request), 20);
        BizContract c = contractMapper.selectOne(Wrappers.<BizContract>lambdaQuery()
                .eq(BizContract::getContractNo, contractNo).last("limit 1"));
        if (c == null) throw BizException.notFound("合同不存在");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("contractNo", c.getContractNo());
        m.put("title", c.getTitle());
        m.put("signStatus", c.getSignStatus());
        m.put("signTime", c.getSignTime());
        m.put("valid", true);
        return Result.ok(m);
    }

    @PostMapping("/custom-request")
    public Result<BizCustomRequest> customRequest(@RequestBody BizCustomRequest req) {
        requireTenant();
        req.setTenantId(TenantContext.getTenantId());
        req.setStatus(0);
        customRequestMapper.insert(req);
        return Result.ok(req);
    }

    @GetMapping("/ugc")
    public Result<List<BizUgcPost>> ugc(@RequestParam(required = false) Long productId) {
        return Result.ok(ugcPostMapper.selectList(Wrappers.<BizUgcPost>lambdaQuery()
                .eq(BizUgcPost::getStatus, 1)
                .eq(productId != null, BizUgcPost::getProductId, productId)
                .orderByDesc(BizUgcPost::getId).last("limit 50")));
    }

    /** AI 客服：规则应答 + 关键结论转人工（T93） */
    @PostMapping("/ai/chat")
    public Result<Map<String, Object>> aiChat(@RequestBody Map<String, Object> body) {
        requireTenant();
        String q = str(body.get("question"));
        if (q == null || q.isBlank()) throw new BizException("请输入问题");
        String answer;
        boolean transfer = false;
        String intent = "general";
        if (q.contains("退") || q.contains("退款") || q.contains("改期")) {
            answer = "退改需按线路退改规则执行，涉及退款金额与到账时间，已为您转接人工客服确认。";
            transfer = true;
            intent = "refund";
        } else if (q.contains("订单")) {
            answer = "请提供订单号，或登录后在「我的订单」查看；涉及订单变更已转人工。";
            transfer = true;
            intent = "order";
        } else if (q.contains("合同")) {
            answer = "电子合同可在订单详情中查看与签署，签署后可在 H5 门户验真。";
            intent = "contract";
        } else if (q.contains("线路") || q.contains("推荐") || q.contains("去哪")) {
            List<BizProduct> list = productMapper.selectList(Wrappers.<BizProduct>lambdaQuery()
                    .eq(BizProduct::getStatus, 1).orderByDesc(BizProduct::getSold).last("limit 3"));
            answer = "为您推荐：" + list.stream().map(BizProduct::getTitle).reduce((a, b) -> a + "、" + b).orElse("暂无线路")
                    + "。以上推荐仅供参考，出团与余位请以详情页为准。";
            intent = "recommend";
        } else {
            answer = "您好，我是旅游智能助手（回答仅供参考）。可咨询线路、订单、退改、合同，涉及退改/退款/出团的关键结论会转人工确认。";
        }
        BizAiConversation c = new BizAiConversation();
        c.setTenantId(TenantContext.getTenantId());
        c.setMemberId(body.get("memberId") == null ? null : Long.valueOf(String.valueOf(body.get("memberId"))));
        c.setQuestion(q);
        c.setAnswer(answer);
        c.setIntent(intent);
        c.setTransferred(transfer ? 1 : 0);
        aiConversationMapper.insert(c);
        return Result.ok(Map.of("answer", answer, "transferred", transfer, "intent", intent,
                "disclaimer", "回答由 AI 生成，仅供参考"));
    }

    @GetMapping("/currencies")
    public Result<List<BizCurrencyRate>> currencies() {
        return Result.ok(currencyRateMapper.selectList(Wrappers.<BizCurrencyRate>lambdaQuery()));
    }

    private Long requireTenant() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new BizException("缺少租户标识（X-Tenant-Code）");
        return tenantId;
    }

    private void rateLimit(String key, int perMinute) {
        Long count = redis.opsForValue().increment("rate:" + key);
        if (count != null && count == 1L) redis.expire("rate:" + key, Duration.ofMinutes(1));
        if (count != null && count > perMinute) throw new BizException(429, "请求过于频繁，请稍后再试");
    }

    private String clientIp(jakarta.servlet.http.HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return ip == null ? request.getRemoteAddr() : ip.split(",")[0];
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
