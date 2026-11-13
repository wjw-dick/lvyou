package net.jxdg.travel.modules.ota.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import net.jxdg.travel.common.*;
import net.jxdg.travel.modules.ota.entity.BizOtaConfig;
import net.jxdg.travel.modules.ota.entity.BizOtaOrder;
import net.jxdg.travel.modules.ota.mapper.BizOtaConfigMapper;
import net.jxdg.travel.modules.ota.mapper.BizOtaOrderMapper;
import net.jxdg.travel.modules.order.entity.BizOrder;
import net.jxdg.travel.modules.order.mapper.BizOrderMapper;
import net.jxdg.travel.modules.product.entity.BizProduct;
import net.jxdg.travel.modules.product.mapper.BizProductMapper;
import net.jxdg.travel.tenant.TenantContext;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * OTA 对接（飞猪/携程/途牛/美团/去哪儿/马蜂窝）。
 * 真实环境通过各 OTA 开放平台拉单；此处提供统一同步入口与字段映射，便于联调。
 */
@RestController
@RequiredArgsConstructor
public class OtaController {

    private final BizOtaConfigMapper configMapper;
    private final BizOtaOrderMapper otaOrderMapper;
    private final BizOrderMapper orderMapper;
    private final BizProductMapper productMapper;
    private final NumberGenerator numberGenerator;
    private final CryptoUtil cryptoUtil;

    @GetMapping("/ota/configs")
    public Result<List<Map<String, Object>>> configs() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (BizOtaConfig c : configMapper.selectList(Wrappers.<BizOtaConfig>lambdaQuery())) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("ota", c.getOta());
            m.put("appKey", c.getAppKey());
            m.put("appSecret", c.getAppSecret() == null ? "" : CryptoUtil.maskSecret(c.getAppSecret()));
            m.put("enabled", c.getEnabled());
            m.put("commissionRate", c.getCommissionRate());
            out.add(m);
        }
        return Result.ok(out);
    }

    @PostMapping("/ota/configs")
    @Audit(module = "OTA", action = "保存 OTA 配置")
    public Result<Void> saveConfig(@RequestBody Map<String, Object> body) {
        String ota = String.valueOf(body.get("ota"));
        BizOtaConfig c = configMapper.selectOne(Wrappers.<BizOtaConfig>lambdaQuery()
                .eq(BizOtaConfig::getOta, ota).last("limit 1"));
        String secret = body.get("appSecret") == null ? null : String.valueOf(body.get("appSecret"));
        if (c == null) {
            c = new BizOtaConfig();
            c.setTenantId(TenantContext.getTenantId());
            c.setOta(ota);
            c.setAppKey(body.get("appKey") == null ? null : String.valueOf(body.get("appKey")));
            c.setAppSecret(secret == null || secret.contains("****") ? null : cryptoUtil.encrypt(secret));
            c.setEnabled(Boolean.parseBoolean(String.valueOf(body.getOrDefault("enabled", false))) ? 1 : 0);
            c.setCommissionRate(new BigDecimal(String.valueOf(body.getOrDefault("commissionRate", "0"))));
            configMapper.insert(c);
        } else {
            c.setAppKey(body.get("appKey") == null ? c.getAppKey() : String.valueOf(body.get("appKey")));
            if (secret != null && !secret.contains("****")) c.setAppSecret(cryptoUtil.encrypt(secret));
            c.setEnabled(Boolean.parseBoolean(String.valueOf(body.getOrDefault("enabled", c.getEnabled() == 1))) ? 1 : 0);
            if (body.get("commissionRate") != null)
                c.setCommissionRate(new BigDecimal(String.valueOf(body.get("commissionRate"))));
            configMapper.updateById(c);
        }
        return Result.ok();
    }

    @GetMapping("/ota/orders")
    public Result<List<BizOtaOrder>> orders(@RequestParam(required = false) String ota) {
        return Result.ok(otaOrderMapper.selectList(Wrappers.<BizOtaOrder>lambdaQuery()
                .eq(ota != null && !ota.isBlank(), BizOtaOrder::getOta, ota)
                .orderByDesc(BizOtaOrder::getId)));
    }

    /** 统一订单同步入口：自动落 OTA 订单 + 平台订单，同步佣金/手续费/退款/补款/真实到账 */
    @PostMapping("/ota/sync")
    @Audit(module = "OTA", action = "同步 OTA 订单")
    public Result<Map<String, Object>> sync(@RequestBody Map<String, Object> body) {
        String ota = String.valueOf(body.getOrDefault("ota", "fliggy"));
        List<?> orders = body.get("orders") instanceof List<?> l ? l : List.of();
        int synced = 0;
        for (Object item : orders) {
            if (!(item instanceof Map<?, ?> m)) continue;
            String otaNo = String.valueOf(m.get("otaOrderNo"));
            if (otaOrderMapper.selectCount(Wrappers.<BizOtaOrder>lambdaQuery()
                    .eq(BizOtaOrder::getOta, ota).eq(BizOtaOrder::getOtaOrderNo, otaNo)) > 0) continue;
            BigDecimal amount = decimal(m.get("amount"));
            BigDecimal commission = decimal(m.get("commission"));
            BigDecimal fee = decimal(m.get("fee"));
            BigDecimal refund = decimal(m.get("refundAmount"));
            BigDecimal supplement = decimal(m.get("supplementAmount"));
            BigDecimal settle = decimal(m.get("settleAmount"));
            if (settle.compareTo(BigDecimal.ZERO) == 0) {
                settle = amount.subtract(commission).subtract(fee).subtract(refund).add(supplement);
            }
            BizOtaOrder oo = new BizOtaOrder();
            oo.setTenantId(TenantContext.getTenantId());
            oo.setOta(ota);
            oo.setOtaOrderNo(otaNo);
            oo.setAmount(amount);
            oo.setCommission(commission);
            oo.setFee(fee);
            oo.setRefundAmount(refund);
            oo.setSupplementAmount(supplement);
            oo.setSettleAmount(settle);
            oo.setStatus(m.get("status") == null ? "PAID" : String.valueOf(m.get("status")));
            oo.setRawJson(m.toString());
            oo.setSyncedAt(LocalDateTime.now());

            if (m.get("productId") != null) {
                BizProduct p = productMapper.selectById(Long.valueOf(String.valueOf(m.get("productId"))));
                if (p != null) {
                    BizOrder order = new BizOrder();
                    order.setTenantId(TenantContext.getTenantId());
                    order.setOrderNo(numberGenerator.next("OD"));
                    order.setProductId(p.getId());
                    order.setProductTitle(p.getTitle());
                    order.setContactName(m.get("contactName") == null ? "OTA 游客" : String.valueOf(m.get("contactName")));
                    order.setContactPhone(m.get("contactPhone") == null ? "" : String.valueOf(m.get("contactPhone")));
                    order.setAdultCount(m.get("adultCount") == null ? 1 : Integer.parseInt(String.valueOf(m.get("adultCount"))));
                    order.setChildCount(m.get("childCount") == null ? 0 : Integer.parseInt(String.valueOf(m.get("childCount"))));
                    order.setTotalAmount(amount);
                    order.setPaidAmount(settle);
                    order.setRefundAmount(refund);
                    order.setCommissionAmount(commission);
                    order.setPayStatus(1);
                    order.setVerifyStatus(0);
                    order.setStatus(3);
                    order.setChannel("ota");
                    order.setSource("ota");
                    order.setOtaOrderNo(otaNo);
                    order.setDepartDate(m.get("departDate") == null ? LocalDate.now().plusDays(7)
                            : LocalDate.parse(String.valueOf(m.get("departDate"))));
                    order.setDeleted(0);
                    orderMapper.insert(order);
                    oo.setOrderId(order.getId());
                }
            }
            otaOrderMapper.insert(oo);
            synced++;
        }
        return Result.ok(Map.of("synced", synced, "ota", ota));
    }

    @GetMapping("/ota/stats")
    public Result<Map<String, Object>> stats() {
        List<BizOtaOrder> list = otaOrderMapper.selectList(Wrappers.<BizOtaOrder>lambdaQuery());
        BigDecimal amount = list.stream().map(BizOtaOrder::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal settle = list.stream().map(BizOtaOrder::getSettleAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commission = list.stream().map(BizOtaOrder::getCommission).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Long> byOta = new LinkedHashMap<>();
        list.forEach(o -> byOta.merge(o.getOta(), 1L, Long::sum));
        return Result.ok(Map.of("count", list.size(), "amount", amount, "settleAmount", settle,
                "commission", commission, "byOta", byOta));
    }

    private static BigDecimal decimal(Object o) {
        return o == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(o));
    }
}
