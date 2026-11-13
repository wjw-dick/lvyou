package net.jxdg.travel.modules.finance.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.jxdg.travel.common.*;
import net.jxdg.travel.modules.finance.dto.SettlementRow;
import net.jxdg.travel.modules.finance.entity.*;
import net.jxdg.travel.modules.finance.mapper.*;
import net.jxdg.travel.modules.order.entity.BizOrder;
import net.jxdg.travel.modules.order.entity.BizPaymentPlan;
import net.jxdg.travel.modules.order.mapper.BizOrderMapper;
import net.jxdg.travel.modules.order.mapper.BizPaymentPlanMapper;
import net.jxdg.travel.modules.insurance.entity.BizInsuranceOrder;
import net.jxdg.travel.modules.insurance.entity.BizInsuranceProduct;
import net.jxdg.travel.modules.insurance.mapper.BizInsuranceOrderMapper;
import net.jxdg.travel.modules.insurance.mapper.BizInsuranceProductMapper;
import net.jxdg.travel.modules.resource.entity.BizPurchaseOrder;
import net.jxdg.travel.modules.resource.mapper.BizPurchaseOrderMapper;
import net.jxdg.travel.security.SecurityUtils;
import net.jxdg.travel.tenant.TenantContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class FinanceController {

    private final BizSettlementMapper settlementMapper;
    private final BizSettlementItemMapper itemMapper;
    private final BizPaymentPlanMapper planMapper;
    private final BizInvoiceMapper invoiceMapper;
    private final BizApprovalMapper approvalMapper;
    private final BizCommissionMapper commissionMapper;
    private final BizSplitAccountMapper splitAccountMapper;
    private final BizSplitRecordMapper splitRecordMapper;
    private final BizInsuranceProductMapper insuranceProductMapper;
    private final BizInsuranceOrderMapper insuranceOrderMapper;
    private final BizGuaranteeFundMapper guaranteeFundMapper;
    private final BizOrderMapper orderMapper;
    private final BizPurchaseOrderMapper purchaseMapper;
    private final NumberGenerator numberGenerator;

    // ---------------- 结算 ----------------
    @GetMapping("/settlements")
    public Result<PageResult<BizSettlement>> settlements(@RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "20") long size,
                                                         @RequestParam(required = false) String type,
                                                         @RequestParam(required = false) Integer status,
                                                         @RequestParam(required = false) String keyword) {
        Page<BizSettlement> p = settlementMapper.selectPage(new Page<>(page, size),
                Wrappers.<BizSettlement>lambdaQuery()
                        .eq(type != null && !type.isBlank(), BizSettlement::getType, type)
                        .eq(status != null, BizSettlement::getStatus, status)
                        .like(keyword != null && !keyword.isBlank(), BizSettlement::getSettleNo, keyword)
                        .orderByDesc(BizSettlement::getId));
        return Result.ok(PageResult.of(p));
    }

    @GetMapping("/settlements/{id}")
    public Result<Map<String, Object>> settlement(@PathVariable Long id) {
        BizSettlement s = settlementMapper.selectById(id);
        if (s == null) throw BizException.notFound("结算单不存在");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("settlement", s);
        out.put("items", itemMapper.selectList(Wrappers.<BizSettlementItem>lambdaQuery()
                .eq(BizSettlementItem::getSettlementId, id)));
        out.put("plans", planMapper.selectList(Wrappers.<BizPaymentPlan>lambdaQuery()
                .eq(BizPaymentPlan::getSettlementId, id)));
        out.put("approvals", approvalMapper.selectList(Wrappers.<BizApproval>lambdaQuery()
                .eq(BizApproval::getBizType, "settlement").eq(BizApproval::getBizId, id)));
        return Result.ok(out);
    }

    @PostMapping("/settlements")
    @Transactional
    @Audit(module = "财务", action = "发起结算")
    public Result<BizSettlement> create(@RequestBody Map<String, Object> body) {
        String type = str(body.getOrDefault("type", "group"));
        BizSettlement s = new BizSettlement();
        s.setTenantId(TenantContext.getTenantId());
        s.setSettleNo(numberGenerator.next("ST"));
        s.setType(type);
        s.setPartnerType(str(body.get("partnerType")));
        s.setPartnerId(body.get("partnerId") == null ? null : Long.valueOf(String.valueOf(body.get("partnerId"))));
        s.setPartnerName(str(body.get("partnerName")));
        s.setScheduleId(body.get("scheduleId") == null ? null : Long.valueOf(String.valueOf(body.get("scheduleId"))));
        s.setOrderId(body.get("orderId") == null ? null : Long.valueOf(String.valueOf(body.get("orderId"))));
        s.setPeriodStart(body.get("periodStart") == null ? null : LocalDate.parse(String.valueOf(body.get("periodStart"))));
        s.setPeriodEnd(body.get("periodEnd") == null ? null : LocalDate.parse(String.valueOf(body.get("periodEnd"))));
        s.setCreatedBy(SecurityUtils.userId());
        s.setStatus(0);
        s.setApprovalStatus(0);

        BigDecimal amount = BigDecimal.ZERO;
        List<BizOrder> orders = List.of();
        if ("order".equals(type) && s.getOrderId() != null) {
            BizOrder o = orderMapper.selectById(s.getOrderId());
            if (o == null) throw BizException.notFound("订单不存在");
            orders = List.of(o);
        } else if ("group".equals(type) && s.getScheduleId() != null) {
            orders = orderMapper.selectList(Wrappers.<BizOrder>lambdaQuery()
                    .eq(BizOrder::getScheduleId, s.getScheduleId()).eq(BizOrder::getPayStatus, 1));
        }
        for (BizOrder o : orders) {
            amount = amount.add(o.getPaidAmount().subtract(o.getRefundAmount() == null ? BigDecimal.ZERO : o.getRefundAmount()));
        }
        if ("partner".equals(type) && "supplier".equals(s.getPartnerType()) && s.getPartnerId() != null) {
            List<BizPurchaseOrder> purchases = purchaseMapper.selectList(Wrappers.<BizPurchaseOrder>lambdaQuery()
                    .eq(BizPurchaseOrder::getSupplierId, s.getPartnerId())
                    .eq(BizPurchaseOrder::getStatus, 1));
            for (BizPurchaseOrder p : purchases) amount = amount.add(p.getAmount());
        }
        if (body.get("amount") != null) amount = new BigDecimal(String.valueOf(body.get("amount")));
        s.setAmount(amount);
        s.setPaidAmount(BigDecimal.ZERO);
        s.setRefundAmount(orders.stream().map(o -> o.getRefundAmount() == null ? BigDecimal.ZERO : o.getRefundAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        s.setCommission(orders.stream().map(o -> o.getCommissionAmount() == null ? BigDecimal.ZERO : o.getCommissionAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        s.setRealAmount(amount.subtract(s.getCommission()));
        settlementMapper.insert(s);

        for (BizOrder o : orders) {
            BizSettlementItem item = new BizSettlementItem();
            item.setTenantId(TenantContext.getTenantId());
            item.setSettlementId(s.getId());
            item.setBizType("order");
            item.setBizId(o.getId());
            item.setBizNo(o.getOrderNo());
            item.setAmount(o.getPaidAmount());
            item.setRemark(o.getProductTitle());
            itemMapper.insert(item);
        }
        return Result.ok(s);
    }

    @PostMapping("/settlements/{id}/confirm")
    @Audit(module = "财务", action = "确认结算单")
    public Result<Void> confirm(@PathVariable Long id) {
        BizSettlement s = settlementMapper.selectById(id);
        if (s == null) throw BizException.notFound("结算单不存在");
        s.setStatus(1);
        settlementMapper.updateById(s);
        return Result.ok();
    }

    @PostMapping("/settlements/{id}/approve")
    @Audit(module = "财务", action = "结算审批")
    public Result<Void> approve(@PathVariable Long id, @RequestParam boolean approve) {
        BizSettlement s = settlementMapper.selectById(id);
        if (s == null) throw BizException.notFound("结算单不存在");
        s.setApprovalStatus(approve ? 1 : 2);
        if (approve) s.setStatus(2);
        settlementMapper.updateById(s);
        BizApproval a = new BizApproval();
        a.setTenantId(TenantContext.getTenantId());
        a.setBizType("settlement");
        a.setBizId(id);
        a.setTitle(s.getSettleNo());
        a.setAmount(s.getAmount());
        a.setStatus(approve ? 1 : 2);
        a.setApplyBy(s.getCreatedBy() == null ? null : String.valueOf(s.getCreatedBy()));
        a.setApproveBy(SecurityUtils.username());
        a.setApproveAt(LocalDateTime.now());
        a.setCreatedAt(LocalDateTime.now());
        approvalMapper.insert(a);
        return Result.ok();
    }

    @PostMapping("/settlements/{id}/pay")
    @Audit(module = "财务", action = "结算打款")
    public Result<Void> pay(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        BizSettlement s = settlementMapper.selectById(id);
        if (s == null) throw BizException.notFound("结算单不存在");
        BigDecimal amount = body != null && body.get("amount") != null
                ? new BigDecimal(String.valueOf(body.get("amount"))) : s.getAmount();
        s.setPaidAmount(amount);
        s.setRealAmount(amount.subtract(s.getCommission() == null ? BigDecimal.ZERO : s.getCommission()));
        s.setStatus(3);
        settlementMapper.updateById(s);
        BizPaymentPlan plan = new BizPaymentPlan();
        plan.setTenantId(TenantContext.getTenantId());
        plan.setSettlementId(id);
        plan.setDirection("out");
        plan.setPlanDate(LocalDate.now());
        plan.setAmount(amount);
        plan.setStatus(1);
        plan.setRemark("结算打款");
        planMapper.insert(plan);
        return Result.ok();
    }

    @GetMapping("/settlements/export")
    public void export(HttpServletResponse response) throws Exception {
        List<BizSettlement> list = settlementMapper.selectList(Wrappers.<BizSettlement>lambdaQuery().orderByDesc(BizSettlement::getId));
        List<SettlementRow> rows = list.stream().map(s -> {
            SettlementRow r = new SettlementRow();
            r.setSettleNo(s.getSettleNo());
            r.setType(s.getType());
            r.setPartnerName(s.getPartnerName());
            r.setAmount(s.getAmount());
            r.setPaidAmount(s.getPaidAmount());
            r.setRefundAmount(s.getRefundAmount());
            r.setCommission(s.getCommission());
            r.setRealAmount(s.getRealAmount());
            r.setStatus(String.valueOf(s.getStatus()));
            return r;
        }).toList();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" +
                URLEncoder.encode("结算单.xlsx", StandardCharsets.UTF_8));
        EasyExcel.write(response.getOutputStream(), SettlementRow.class).sheet("结算").doWrite(rows);
    }

    // ---------------- 发票 ----------------
    @GetMapping("/invoices")
    public Result<PageResult<BizInvoice>> invoices(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "20") long size,
                                                   @RequestParam(required = false) Integer status) {
        return Result.ok(PageResult.of(invoiceMapper.selectPage(new Page<>(page, size),
                Wrappers.<BizInvoice>lambdaQuery().eq(status != null, BizInvoice::getStatus, status)
                        .orderByDesc(BizInvoice::getId))));
    }

    @PostMapping("/invoices")
    @Audit(module = "财务", action = "申请开票")
    public Result<Void> invoice(@RequestBody Map<String, Object> body) {
        BizInvoice inv = new BizInvoice();
        inv.setTenantId(TenantContext.getTenantId());
        inv.setTitle(String.valueOf(body.get("title")));
        inv.setTaxNo(str(body.get("taxNo")));
        inv.setAmount(new BigDecimal(String.valueOf(body.getOrDefault("amount", "0"))));
        inv.setType(str(body.getOrDefault("type", "normal")));
        inv.setOrderId(body.get("orderId") == null ? null : Long.valueOf(String.valueOf(body.get("orderId"))));
        inv.setSettlementId(body.get("settlementId") == null ? null : Long.valueOf(String.valueOf(body.get("settlementId"))));
        inv.setStatus(0);
        inv.setApplyBy(SecurityUtils.username());
        inv.setCreatedAt(LocalDateTime.now());
        invoiceMapper.insert(inv);
        return Result.ok();
    }

    @PostMapping("/invoices/{id}/issue")
    @Audit(module = "财务", action = "开具发票")
    public Result<Void> issueInvoice(@PathVariable Long id) {
        BizInvoice inv = invoiceMapper.selectById(id);
        if (inv == null) throw BizException.notFound("发票不存在");
        inv.setStatus(1);
        inv.setInvoiceNo("INV" + System.currentTimeMillis());
        inv.setIssuedAt(LocalDateTime.now());
        invoiceMapper.updateById(inv);
        return Result.ok();
    }

    // ---------------- 分账 ----------------
    @GetMapping("/split/accounts")
    public Result<List<BizSplitAccount>> splitAccounts() {
        return Result.ok(splitAccountMapper.selectList(Wrappers.<BizSplitAccount>lambdaQuery()));
    }

    @PostMapping("/split/accounts")
    @Audit(module = "财务", action = "保存分账账户")
    public Result<Void> saveSplitAccount(@RequestBody BizSplitAccount a) {
        if (a.getTenantId() == null) a.setTenantId(TenantContext.getTenantId());
        if (a.getId() == null) splitAccountMapper.insert(a);
        else splitAccountMapper.updateById(a);
        return Result.ok();
    }

    @GetMapping("/split/records")
    public Result<List<Map<String, Object>>> splitRecords(@RequestParam(required = false) Long orderId,
                                                          @RequestParam(required = false) Integer status) {
        List<BizSplitRecord> list = splitRecordMapper.selectList(Wrappers.<BizSplitRecord>lambdaQuery()
                .eq(orderId != null, BizSplitRecord::getOrderId, orderId)
                .eq(status != null, BizSplitRecord::getStatus, status)
                .orderByDesc(BizSplitRecord::getId));
        return Result.ok(list.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("record", r);
            BizSplitAccount a = splitAccountMapper.selectById(r.getAccountId());
            m.put("accountName", a == null ? "" : a.getName());
            return m;
        }).toList());
    }

    /** 按配置比例对已支付订单分账（失败记录可重试） */
    @PostMapping("/split/execute")
    @Transactional
    @Audit(module = "财务", action = "执行分账")
    public Result<Map<String, Object>> executeSplit(@RequestParam Long orderId) {
        BizOrder order = orderMapper.selectById(orderId);
        if (order == null) throw BizException.notFound("订单不存在");
        if (order.getPayStatus() != 1) throw new BizException("订单未支付，不能分账");
        List<BizSplitAccount> accounts = splitAccountMapper.selectList(Wrappers.<BizSplitAccount>lambdaQuery()
                .eq(BizSplitAccount::getStatus, 1));
        BigDecimal total = order.getPaidAmount();
        int created = 0;
        for (BizSplitAccount a : accounts) {
            BigDecimal amount = total.multiply(a.getSplitRate()).setScale(2, java.math.RoundingMode.HALF_UP);
            BizSplitRecord r = new BizSplitRecord();
            r.setTenantId(TenantContext.getTenantId());
            r.setOrderId(orderId);
            r.setAccountId(a.getId());
            r.setRate(a.getSplitRate());
            r.setAmount(amount);
            r.setStatus(1);
            r.setSplitNo(numberGenerator.next("SP"));
            r.setCreatedAt(LocalDateTime.now());
            splitRecordMapper.insert(r);
            created++;
        }
        return Result.ok(Map.of("created", created, "amount", total));
    }

    @PostMapping("/split/records/{id}/retry")
    @Audit(module = "财务", action = "分账重试")
    public Result<Void> retrySplit(@PathVariable Long id) {
        BizSplitRecord r = splitRecordMapper.selectById(id);
        if (r == null) throw BizException.notFound("分账记录不存在");
        r.setStatus(1);
        r.setRetryCount((r.getRetryCount() == null ? 0 : r.getRetryCount()) + 1);
        r.setErrorMsg(null);
        splitRecordMapper.updateById(r);
        return Result.ok();
    }

    // ---------------- 保险 ----------------
    @GetMapping("/insurance/products")
    public Result<List<BizInsuranceProduct>> insuranceProducts() {
        return Result.ok(insuranceProductMapper.selectList(Wrappers.<BizInsuranceProduct>lambdaQuery()));
    }

    @PostMapping("/insurance/products")
    @Audit(module = "保险", action = "保存保险产品")
    public Result<Void> saveInsuranceProduct(@RequestBody BizInsuranceProduct p) {
        if (p.getTenantId() == null) p.setTenantId(TenantContext.getTenantId());
        if (p.getId() == null) insuranceProductMapper.insert(p);
        else insuranceProductMapper.updateById(p);
        return Result.ok();
    }

    @GetMapping("/insurance/orders")
    public Result<List<Map<String, Object>>> insuranceOrders() {
        List<BizInsuranceOrder> list = insuranceOrderMapper.selectList(Wrappers.<BizInsuranceOrder>lambdaQuery()
                .orderByDesc(BizInsuranceOrder::getId));
        return Result.ok(list.stream().map(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("order", o);
            BizInsuranceProduct p = insuranceProductMapper.selectById(o.getInsuranceId());
            m.put("productName", p == null ? "" : p.getName());
            return m;
        }).toList());
    }

    @PostMapping("/insurance/orders")
    @Transactional
    @Audit(module = "保险", action = "在线投保")
    public Result<BizInsuranceOrder> insure(@RequestBody Map<String, Object> body) {
        BizInsuranceProduct p = insuranceProductMapper.selectById(Long.valueOf(String.valueOf(body.get("insuranceId"))));
        if (p == null) throw BizException.notFound("保险产品不存在");
        BizInsuranceOrder o = new BizInsuranceOrder();
        o.setTenantId(TenantContext.getTenantId());
        o.setInsuranceId(p.getId());
        o.setOrderId(body.get("orderId") == null ? null : Long.valueOf(String.valueOf(body.get("orderId"))));
        o.setInsuredName(str(body.get("insuredName")));
        o.setPremium(p.getPremium());
        o.setCoverage(new BigDecimal(String.valueOf(body.getOrDefault("coverage", "0"))));
        o.setStatus(0);
        o.setStartDate(body.get("startDate") == null ? null : LocalDate.parse(String.valueOf(body.get("startDate"))));
        o.setEndDate(body.get("endDate") == null ? null : LocalDate.parse(String.valueOf(body.get("endDate"))));
        o.setCreatedAt(LocalDateTime.now());
        insuranceOrderMapper.insert(o);
        return Result.ok(o);
    }

    @PostMapping("/insurance/orders/{id}/issue")
    @Audit(module = "保险", action = "保单出单")
    public Result<Void> issuePolicy(@PathVariable Long id) {
        BizInsuranceOrder o = insuranceOrderMapper.selectById(id);
        if (o == null) throw BizException.notFound("投保单不存在");
        o.setStatus(1);
        o.setPolicyNo("PL" + System.currentTimeMillis());
        insuranceOrderMapper.updateById(o);
        return Result.ok();
    }

    // ---------------- 质保金 ----------------
    @GetMapping("/funds")
    public Result<List<BizGuaranteeFund>> funds() {
        return Result.ok(guaranteeFundMapper.selectList(Wrappers.<BizGuaranteeFund>lambdaQuery()
                .orderByAsc(BizGuaranteeFund::getId)));
    }

    @PostMapping("/funds")
    @Audit(module = "合规", action = "保存质保金台账")
    public Result<Void> saveFund(@RequestBody BizGuaranteeFund f) {
        if (f.getTenantId() == null) f.setTenantId(TenantContext.getTenantId());
        if (f.getId() == null) guaranteeFundMapper.insert(f);
        else guaranteeFundMapper.updateById(f);
        return Result.ok();
    }

    @GetMapping("/funds/warnings")
    public Result<List<BizGuaranteeFund>> fundWarnings() {
        return Result.ok(guaranteeFundMapper.selectList(Wrappers.<BizGuaranteeFund>lambdaQuery()
                .isNotNull(BizGuaranteeFund::getExpireDate)
                .le(BizGuaranteeFund::getExpireDate, LocalDate.now().plusDays(90))));
    }

    // ---------------- 佣金 ----------------
    @GetMapping("/commissions")
    public Result<List<Map<String, Object>>> commissions(@RequestParam(required = false) Integer status) {
        List<BizCommission> list = commissionMapper.selectList(Wrappers.<BizCommission>lambdaQuery()
                .eq(status != null, BizCommission::getStatus, status).orderByDesc(BizCommission::getId));
        return Result.ok(list.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("commission", c);
            BizOrder o = orderMapper.selectById(c.getOrderId());
            m.put("orderNo", o == null ? "" : o.getOrderNo());
            m.put("productTitle", o == null ? "" : o.getProductTitle());
            return m;
        }).toList());
    }

    @PostMapping("/commissions/{id}/settle")
    @Audit(module = "财务", action = "佣金入账")
    public Result<Void> settleCommission(@PathVariable Long id) {
        BizCommission c = commissionMapper.selectById(id);
        if (c == null) throw BizException.notFound("佣金不存在");
        c.setStatus(1);
        c.setSettleAt(LocalDateTime.now());
        commissionMapper.updateById(c);
        return Result.ok();
    }

    @GetMapping("/finance/summary")
    public Result<Map<String, Object>> summary() {
        BigDecimal settleAmount = settlementMapper.selectMaps(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<BizSettlement>()
                        .select("IFNULL(SUM(amount),0) AS amount"))
                .stream().findFirst().map(m -> new BigDecimal(String.valueOf(m.get("amount")))).orElse(BigDecimal.ZERO);
        BigDecimal paid = settlementMapper.selectMaps(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<BizSettlement>()
                        .select("IFNULL(SUM(paid_amount),0) AS amount"))
                .stream().findFirst().map(m -> new BigDecimal(String.valueOf(m.get("amount")))).orElse(BigDecimal.ZERO);
        long pending = settlementMapper.selectCount(Wrappers.<BizSettlement>lambdaQuery().eq(BizSettlement::getStatus, 0));
        return Result.ok(Map.of("settleAmount", settleAmount, "paidAmount", paid, "pendingCount", pending));
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
