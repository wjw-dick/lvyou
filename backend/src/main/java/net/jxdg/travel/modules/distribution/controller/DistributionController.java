package net.jxdg.travel.modules.distribution.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import net.jxdg.travel.common.*;
import net.jxdg.travel.modules.distribution.entity.BizDistributor;
import net.jxdg.travel.modules.distribution.entity.BizDistributorPrice;
import net.jxdg.travel.modules.distribution.entity.BizPromotionLink;
import net.jxdg.travel.modules.distribution.mapper.BizDistributorMapper;
import net.jxdg.travel.modules.distribution.mapper.BizDistributorPriceMapper;
import net.jxdg.travel.modules.distribution.mapper.BizPromotionLinkMapper;
import net.jxdg.travel.modules.finance.entity.BizCommission;
import net.jxdg.travel.modules.finance.mapper.BizCommissionMapper;
import net.jxdg.travel.modules.order.entity.BizOrder;
import net.jxdg.travel.modules.order.mapper.BizOrderMapper;
import net.jxdg.travel.modules.product.entity.BizProduct;
import net.jxdg.travel.modules.product.entity.BizSchedule;
import net.jxdg.travel.modules.product.mapper.BizProductMapper;
import net.jxdg.travel.modules.product.mapper.BizScheduleMapper;
import net.jxdg.travel.security.LoginUser;
import net.jxdg.travel.security.SecurityUtils;
import net.jxdg.travel.tenant.TenantContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class DistributionController {

    private final BizDistributorMapper distributorMapper;
    private final BizDistributorPriceMapper distributorPriceMapper;
    private final BizPromotionLinkMapper promotionLinkMapper;
    private final BizCommissionMapper commissionMapper;
    private final BizOrderMapper orderMapper;
    private final BizProductMapper productMapper;
    private final BizScheduleMapper scheduleMapper;
    private final NumberGenerator numberGenerator;
    private final PasswordEncoder passwordEncoder;

    // ---------------- 分销商管理 ----------------
    @GetMapping("/distributors")
    public Result<PageResult<BizDistributor>> page(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "20") long size,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Integer status) {
        Page<BizDistributor> p = distributorMapper.selectPage(new Page<>(page, size),
                Wrappers.<BizDistributor>lambdaQuery()
                        .like(keyword != null && !keyword.isBlank(), BizDistributor::getName, keyword)
                        .eq(status != null, BizDistributor::getStatus, status)
                        .orderByDesc(BizDistributor::getId));
        p.getRecords().forEach(d -> d.setPassword(null));
        return Result.ok(PageResult.of(p));
    }

    @PostMapping("/distributors")
    @Audit(module = "分销", action = "保存分销商")
    public Result<Void> save(@RequestBody Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        BizDistributor d = id == null ? new BizDistributor() : distributorMapper.selectById(id);
        if (d == null) throw BizException.notFound("分销商不存在");
        d.setName(String.valueOf(body.get("name")));
        d.setType(str(body.getOrDefault("type", "shop")));
        d.setContact(str(body.get("contact")));
        d.setPhone(str(body.get("phone")));
        d.setUsername(str(body.get("username")));
        d.setShopName(str(body.get("shopName")));
        if (body.get("commissionRate") != null)
            d.setCommissionRate(new BigDecimal(String.valueOf(body.get("commissionRate"))));
        d.setParentId(body.get("parentId") == null ? null : Long.valueOf(String.valueOf(body.get("parentId"))));
        d.setStatus(body.get("status") == null ? 1 : Integer.parseInt(String.valueOf(body.get("status"))));
        if (id == null) {
            d.setTenantId(TenantContext.getTenantId());
            d.setPassword(passwordEncoder.encode(str(body.getOrDefault("password", "admin123"))));
            distributorMapper.insert(d);
        } else {
            if (body.get("password") != null && !String.valueOf(body.get("password")).isBlank()) {
                d.setPassword(passwordEncoder.encode(String.valueOf(body.get("password"))));
            }
            distributorMapper.updateById(d);
        }
        return Result.ok();
    }

    @DeleteMapping("/distributors/{id}")
    @Audit(module = "分销", action = "删除分销商")
    public Result<Void> delete(@PathVariable Long id) {
        distributorMapper.deleteById(id);
        return Result.ok();
    }

    // ---------------- 分销报价 ----------------
    @GetMapping("/distributors/{id}/prices")
    public Result<List<Map<String, Object>>> prices(@PathVariable Long id) {
        List<BizDistributorPrice> list = distributorPriceMapper.selectList(Wrappers.<BizDistributorPrice>lambdaQuery()
                .eq(BizDistributorPrice::getDistributorId, id));
        return Result.ok(list.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("price", p);
            BizProduct prod = productMapper.selectById(p.getProductId());
            m.put("productTitle", prod == null ? "" : prod.getTitle());
            m.put("adultPrice", prod == null ? null : prod.getAdultPrice());
            return m;
        }).toList());
    }

    @PostMapping("/distributors/{id}/prices")
    @Audit(module = "分销", action = "保存分销报价")
    public Result<Void> savePrice(@PathVariable Long id, @RequestBody List<Map<String, Object>> prices) {
        for (Map<String, Object> m : prices) {
            Long productId = Long.valueOf(String.valueOf(m.get("productId")));
            BizDistributorPrice p = distributorPriceMapper.selectOne(Wrappers.<BizDistributorPrice>lambdaQuery()
                    .eq(BizDistributorPrice::getDistributorId, id)
                    .eq(BizDistributorPrice::getProductId, productId).last("limit 1"));
            if (p == null) {
                p = new BizDistributorPrice();
                p.setTenantId(TenantContext.getTenantId());
                p.setDistributorId(id);
                p.setProductId(productId);
                p.setAdultPrice(new BigDecimal(String.valueOf(m.get("adultPrice"))));
                p.setChildPrice(new BigDecimal(String.valueOf(m.getOrDefault("childPrice", "0"))));
                distributorPriceMapper.insert(p);
            } else {
                p.setAdultPrice(new BigDecimal(String.valueOf(m.get("adultPrice"))));
                p.setChildPrice(new BigDecimal(String.valueOf(m.getOrDefault("childPrice", "0"))));
                distributorPriceMapper.updateById(p);
            }
        }
        return Result.ok();
    }

    // ---------------- 分销商端 ----------------
    @GetMapping("/distributor/products")
    public Result<List<Map<String, Object>>> distributorProducts() {
        LoginUser u = requireDistributor();
        List<Map<String, Object>> out = new ArrayList<>();
        for (BizProduct p : productMapper.selectList(Wrappers.<BizProduct>lambdaQuery().eq(BizProduct::getStatus, 1))) {
            BizDistributorPrice dp = distributorPriceMapper.selectOne(Wrappers.<BizDistributorPrice>lambdaQuery()
                    .eq(BizDistributorPrice::getDistributorId, u.getId())
                    .eq(BizDistributorPrice::getProductId, p.getId()).last("limit 1"));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("title", p.getTitle());
            m.put("days", p.getDays());
            m.put("destination", p.getDestination());
            m.put("cover", p.getCover());
            m.put("adultPrice", dp == null ? p.getAdultPrice() : dp.getAdultPrice());
            m.put("childPrice", dp == null ? p.getChildPrice() : dp.getChildPrice());
            m.put("stock", Math.max(0, p.getStock() - p.getSold()));
            out.add(m);
        }
        return Result.ok(out);
    }

    @GetMapping("/distributor/orders")
    public Result<List<BizOrder>> distributorOrders() {
        LoginUser u = requireDistributor();
        return Result.ok(orderMapper.selectList(Wrappers.<BizOrder>lambdaQuery()
                .eq(BizOrder::getDistributorId, u.getId()).orderByDesc(BizOrder::getId)));
    }

    @GetMapping("/distributor/commission")
    public Result<Map<String, Object>> distributorCommission() {
        LoginUser u = requireDistributor();
        List<BizCommission> list = commissionMapper.selectList(Wrappers.<BizCommission>lambdaQuery()
                .eq(BizCommission::getDistributorId, u.getId()));
        BigDecimal total = list.stream().map(BizCommission::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal settled = list.stream().filter(c -> c.getStatus() == 1)
                .map(BizCommission::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return Result.ok(Map.of("total", total, "settled", settled, "pending", total.subtract(settled),
                "records", list));
    }

    /** 换线操作：分销订单更换线路并重算金额/佣金 */
    @PostMapping("/distribution/orders/{orderId}/change-product")
    @Transactional
    @Audit(module = "分销", action = "换线操作")
    public Result<Void> changeProduct(@PathVariable Long orderId, @RequestParam Long productId) {
        BizOrder o = orderMapper.selectById(orderId);
        BizProduct p = productMapper.selectById(productId);
        if (o == null || p == null) throw BizException.notFound("订单或线路不存在");
        o.setProductId(productId);
        o.setProductTitle(p.getTitle());
        BizDistributorPrice dp = o.getDistributorId() == null ? null
                : distributorPriceMapper.selectOne(Wrappers.<BizDistributorPrice>lambdaQuery()
                .eq(BizDistributorPrice::getDistributorId, o.getDistributorId())
                .eq(BizDistributorPrice::getProductId, productId).last("limit 1"));
        BigDecimal adultPrice = dp == null ? p.getAdultPrice() : dp.getAdultPrice();
        BigDecimal childPrice = dp == null ? p.getChildPrice() : dp.getChildPrice();
        BigDecimal total = adultPrice.multiply(BigDecimal.valueOf(o.getAdultCount()))
                .add(childPrice.multiply(BigDecimal.valueOf(o.getChildCount())));
        o.setTotalAmount(total);
        orderMapper.updateById(o);
        return Result.ok();
    }

    /** 按报名人数排序的排期（分销选线） */
    @GetMapping("/distribution/schedules")
    public Result<List<Map<String, Object>>> schedules(@RequestParam(defaultValue = "signups") String sort) {
        List<BizSchedule> list = scheduleMapper.selectList(Wrappers.<BizSchedule>lambdaQuery()
                .in(BizSchedule::getStatus, 1, 2, 3)
                .orderByDesc("signups".equals(sort), BizSchedule::getSold)
                .orderByAsc(BizSchedule::getDepartDate));
        return Result.ok(list.stream().map(s -> {
            BizProduct p = productMapper.selectById(s.getProductId());
            return Map.<String, Object>of("scheduleId", s.getId(), "scheduleNo", s.getScheduleNo(),
                    "productId", s.getProductId(), "productTitle", p == null ? "" : p.getTitle(),
                    "departDate", s.getDepartDate(), "sold", s.getSold(),
                    "left", Math.max(0, s.getQuota() - s.getSold()));
        }).toList());
    }

    // ---------------- 推广裂变 ----------------
    @GetMapping("/promotion/links")
    public Result<List<BizPromotionLink>> links(@RequestParam(required = false) Long memberId) {
        return Result.ok(promotionLinkMapper.selectList(Wrappers.<BizPromotionLink>lambdaQuery()
                .eq(memberId != null, BizPromotionLink::getMemberId, memberId)
                .orderByDesc(BizPromotionLink::getId)));
    }

    @PostMapping("/promotion/links")
    @Audit(module = "分销", action = "生成推广链接")
    public Result<BizPromotionLink> createLink(@RequestBody Map<String, Object> body) {
        LoginUser u = SecurityUtils.current();
        BizPromotionLink l = new BizPromotionLink();
        l.setTenantId(TenantContext.getTenantId());
        l.setMemberId("MEMBER".equals(u.getUserType()) ? u.getId() : null);
        l.setDistributorId("DISTRIBUTOR".equals(u.getUserType()) ? u.getId() : null);
        l.setProductId(body.get("productId") == null ? null : Long.valueOf(String.valueOf(body.get("productId"))));
        l.setCode(numberGenerator.next("PR"));
        l.setType(str(body.getOrDefault("type", "cps")));
        l.setClicks(0);
        l.setOrders(0);
        l.setCommission(BigDecimal.ZERO);
        l.setCreatedAt(LocalDateTime.now());
        promotionLinkMapper.insert(l);
        return Result.ok(l);
    }

    @GetMapping("/public/promotion/{code}")
    public Result<Map<String, Object>> track(@PathVariable String code) {
        BizPromotionLink l = promotionLinkMapper.selectOne(Wrappers.<BizPromotionLink>lambdaQuery()
                .eq(BizPromotionLink::getCode, code).last("limit 1"));
        if (l == null) throw BizException.notFound("推广链接无效");
        l.setClicks((l.getClicks() == null ? 0 : l.getClicks()) + 1);
        promotionLinkMapper.updateById(l);
        return Result.ok(Map.of("code", l.getCode(), "productId", l.getProductId() == null ? 0 : l.getProductId(),
                "memberId", l.getMemberId() == null ? 0 : l.getMemberId(),
                "distributorId", l.getDistributorId() == null ? 0 : l.getDistributorId()));
    }

    @GetMapping("/promotion/stats")
    public Result<Map<String, Object>> promotionStats() {
        List<BizPromotionLink> links = promotionLinkMapper.selectList(Wrappers.<BizPromotionLink>lambdaQuery());
        long clicks = links.stream().mapToLong(l -> l.getClicks() == null ? 0 : l.getClicks()).sum();
        long orders = links.stream().mapToLong(l -> l.getOrders() == null ? 0 : l.getOrders()).sum();
        BigDecimal commission = links.stream().map(BizPromotionLink::getCommission)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Result.ok(Map.of("links", links.size(), "clicks", clicks, "orders", orders, "commission", commission));
    }

    /** 订单完成时写入佣金（供订单模块/定时任务调用） */
    public void accrueCommission(BizOrder order) {
        if (order.getDistributorId() == null) return;
        BizDistributor d = distributorMapper.selectById(order.getDistributorId());
        if (d == null) return;
        BigDecimal rate = d.getCommissionRate() == null ? BigDecimal.ZERO : d.getCommissionRate();
        BigDecimal amount = order.getPaidAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP);
        BizCommission c = new BizCommission();
        c.setTenantId(TenantContext.getTenantId());
        c.setOrderId(order.getId());
        c.setDistributorId(d.getId());
        c.setType("distributor");
        c.setRate(rate);
        c.setAmount(amount);
        c.setStatus(0);
        c.setCreatedAt(LocalDateTime.now());
        commissionMapper.insert(c);
        order.setCommissionAmount(amount);
        orderMapper.updateById(order);
    }

    private LoginUser requireDistributor() {
        LoginUser u = SecurityUtils.current();
        if (!"DISTRIBUTOR".equals(u.getUserType())) throw BizException.forbidden("仅分销商账号可访问");
        return u;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
