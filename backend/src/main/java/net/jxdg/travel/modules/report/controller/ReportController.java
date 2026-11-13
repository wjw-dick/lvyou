package net.jxdg.travel.modules.report.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.jxdg.travel.common.PageResult;
import net.jxdg.travel.common.Result;
import net.jxdg.travel.modules.finance.entity.BizCommission;
import net.jxdg.travel.modules.finance.entity.BizSettlement;
import net.jxdg.travel.modules.finance.mapper.BizCommissionMapper;
import net.jxdg.travel.modules.finance.mapper.BizSettlementMapper;
import net.jxdg.travel.modules.guide.entity.BizGuideExpense;
import net.jxdg.travel.modules.guide.mapper.BizGuideExpenseMapper;
import net.jxdg.travel.modules.member.entity.BizMember;
import net.jxdg.travel.modules.member.mapper.BizMemberMapper;
import net.jxdg.travel.modules.order.entity.BizOrder;
import net.jxdg.travel.modules.order.entity.BizVerifyRecord;
import net.jxdg.travel.modules.order.mapper.BizOrderMapper;
import net.jxdg.travel.modules.order.mapper.BizVerifyRecordMapper;
import net.jxdg.travel.modules.product.entity.BizProduct;
import net.jxdg.travel.modules.product.entity.BizSchedule;
import net.jxdg.travel.modules.product.mapper.BizProductMapper;
import net.jxdg.travel.modules.product.mapper.BizScheduleMapper;
import net.jxdg.travel.modules.report.dto.OrderReportRow;
import net.jxdg.travel.modules.report.entity.BizReportDaily;
import net.jxdg.travel.modules.report.mapper.BizReportDailyMapper;
import net.jxdg.travel.modules.resource.entity.BizPurchaseOrder;
import net.jxdg.travel.modules.resource.mapper.BizPurchaseOrderMapper;
import net.jxdg.travel.modules.system.entity.SysTenant;
import net.jxdg.travel.modules.system.mapper.SysTenantMapper;
import net.jxdg.travel.security.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class ReportController {

    private final BizOrderMapper orderMapper;
    private final BizScheduleMapper scheduleMapper;
    private final BizProductMapper productMapper;
    private final BizGuideExpenseMapper expenseMapper;
    private final BizSettlementMapper settlementMapper;
    private final BizCommissionMapper commissionMapper;
    private final BizMemberMapper memberMapper;
    private final BizPurchaseOrderMapper purchaseMapper;
    private final BizVerifyRecordMapper verifyRecordMapper;
    private final BizReportDailyMapper reportDailyMapper;
    private final SysTenantMapper tenantMapper;

    @GetMapping("/report/business")
    public Result<Map<String, Object>> business(@RequestParam(required = false) String startDate,
                                                @RequestParam(required = false) String endDate) {
        LocalDate start = startDate == null ? LocalDate.now().minusDays(30) : LocalDate.parse(startDate);
        LocalDate end = endDate == null ? LocalDate.now() : LocalDate.parse(endDate);
        List<BizOrder> orders = orderMapper.selectList(Wrappers.<BizOrder>lambdaQuery()
                .between(BizOrder::getCreatedAt, start.atStartOfDay(), end.plusDays(1).atStartOfDay()));
        BigDecimal sales = orders.stream().filter(o -> o.getPayStatus() != null && o.getPayStatus() > 0)
                .map(BizOrder::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refunds = orders.stream().map(o -> o.getRefundAmount() == null ? BigDecimal.ZERO : o.getRefundAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<BizSchedule> schedules = scheduleMapper.selectList(Wrappers.<BizSchedule>lambdaQuery()
                .between(BizSchedule::getDepartDate, start, end));
        long grouped = schedules.stream().filter(s -> s.getStatus() != null && s.getStatus() >= 2).count();
        BigDecimal groupRate = schedules.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(grouped * 100.0 / schedules.size()).setScale(2, RoundingMode.HALF_UP);

        Map<String, BigDecimal> byProduct = new LinkedHashMap<>();
        Map<String, Long> orderCountByProduct = new LinkedHashMap<>();
        BigDecimal cost = BigDecimal.ZERO;
        for (BizOrder o : orders) {
            byProduct.merge(o.getProductTitle() == null ? "未命名" : o.getProductTitle(),
                    o.getPaidAmount() == null ? BigDecimal.ZERO : o.getPaidAmount(), BigDecimal::add);
            orderCountByProduct.merge(o.getProductTitle() == null ? "未命名" : o.getProductTitle(), 1L, Long::sum);
            BizProduct p = productMapper.selectById(o.getProductId());
            if (p != null && p.getCostPrice() != null) {
                cost = cost.add(p.getCostPrice().multiply(BigDecimal.valueOf(
                        (o.getAdultCount() == null ? 0 : o.getAdultCount())
                                + (o.getChildCount() == null ? 0 : o.getChildCount()))));
            }
        }
        Map<String, BigDecimal> byChannel = new LinkedHashMap<>();
        orders.forEach(o -> byChannel.merge(o.getChannel() == null ? "unknown" : o.getChannel(),
                o.getPaidAmount() == null ? BigDecimal.ZERO : o.getPaidAmount(), BigDecimal::add));
        BigDecimal guideExpenses = expenseMapper.selectList(Wrappers.<BizGuideExpense>lambdaQuery()
                        .between(BizGuideExpense::getCreatedAt, start.atStartOfDay(), end.plusDays(1).atStartOfDay()))
                .stream().map(BizGuideExpense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal settleAmount = settlementMapper.selectList(Wrappers.<BizSettlement>lambdaQuery())
                .stream().map(BizSettlement::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commissions = commissionMapper.selectList(Wrappers.<BizCommission>lambdaQuery())
                .stream().map(BizCommission::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long members = memberMapper.selectCount(Wrappers.<BizMember>lambdaQuery());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("startDate", start.toString());
        out.put("endDate", end.toString());
        out.put("orderCount", orders.size());
        out.put("salesAmount", sales);
        out.put("refundAmount", refunds);
        out.put("groupRate", groupRate);
        out.put("grossProfit", sales.subtract(cost).subtract(refunds));
        out.put("guideExpense", guideExpenses);
        out.put("settlementAmount", settleAmount);
        out.put("commission", commissions);
        out.put("memberCount", members);
        out.put("byProduct", byProduct);
        out.put("orderCountByProduct", orderCountByProduct);
        out.put("byChannel", byChannel);
        return Result.ok(out);
    }

    @GetMapping("/report/cockpit")
    public Result<Map<String, Object>> cockpit() {
        LocalDate today = LocalDate.now();
        List<BizOrder> todayOrders = orderMapper.selectList(Wrappers.<BizOrder>lambdaQuery()
                .ge(BizOrder::getCreatedAt, today.atStartOfDay()));
        BigDecimal todaySales = todayOrders.stream().map(o -> o.getPaidAmount() == null ? BigDecimal.ZERO : o.getPaidAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<BizSchedule> upcoming = scheduleMapper.selectList(Wrappers.<BizSchedule>lambdaQuery()
                .between(BizSchedule::getDepartDate, today, today.plusDays(7)));
        long warning = upcoming.stream().filter(s -> s.getSold() < s.getMinGroup()).count();
        List<BizVerifyRecord> verifies = verifyRecordMapper.selectList(Wrappers.<BizVerifyRecord>lambdaQuery()
                .ge(BizVerifyRecord::getVerifyAt, today.atStartOfDay()));
        int traffic = verifies.stream().mapToInt(v -> 1).sum();
        Map<String, BigDecimal> source = new LinkedHashMap<>();
        orderMapper.selectList(Wrappers.<BizOrder>lambdaQuery().ge(BizOrder::getCreatedAt, today.minusDays(30).atStartOfDay()))
                .forEach(o -> source.merge(o.getSource() == null ? "self" : o.getSource(),
                        o.getPaidAmount() == null ? BigDecimal.ZERO : o.getPaidAmount(), BigDecimal::add));
        return Result.ok(Map.of(
                "todayOrders", todayOrders.size(),
                "todaySales", todaySales,
                "upcomingSchedules", upcoming.size(),
                "warningSchedules", warning,
                "todayTraffic", traffic,
                "sourceAmount", source,
                "customerStructure", Map.of(
                        "newMembers", memberMapper.selectCount(Wrappers.<BizMember>lambdaQuery()
                                .ge(BizMember::getCreatedAt, today.minusDays(30).atStartOfDay())),
                        "totalMembers", memberMapper.selectCount(Wrappers.<BizMember>lambdaQuery()))
        ));
    }

    @GetMapping("/report/guide")
    public Result<List<Map<String, Object>>> guideReport(@RequestParam(required = false) String startDate,
                                                         @RequestParam(required = false) String endDate) {
        LocalDate start = startDate == null ? LocalDate.now().minusDays(30) : LocalDate.parse(startDate);
        LocalDate end = endDate == null ? LocalDate.now() : LocalDate.parse(endDate);
        List<BizGuideExpense> list = expenseMapper.selectList(Wrappers.<BizGuideExpense>lambdaQuery()
                .between(BizGuideExpense::getCreatedAt, start.atStartOfDay(), end.plusDays(1).atStartOfDay()));
        Map<Long, BigDecimal> byGuide = new LinkedHashMap<>();
        Map<Long, Long> count = new LinkedHashMap<>();
        list.forEach(e -> {
            byGuide.merge(e.getGuideId(), e.getAmount(), BigDecimal::add);
            count.merge(e.getGuideId(), 1L, Long::sum);
        });
        List<Map<String, Object>> out = new ArrayList<>();
        byGuide.forEach((guideId, amount) -> out.add(Map.of("guideId", guideId, "amount", amount,
                "count", count.get(guideId), "approved", list.stream()
                        .filter(e -> e.getGuideId().equals(guideId) && e.getStatus() == 1).count())));
        return Result.ok(out);
    }

    @GetMapping("/report/supplier")
    public Result<Map<String, Object>> supplierReport() {
        List<BizPurchaseOrder> list = purchaseMapper.selectList(Wrappers.<BizPurchaseOrder>lambdaQuery());
        BigDecimal total = list.stream().map(BizPurchaseOrder::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long confirmed = list.stream().filter(p -> p.getStatus() == 1).count();
        long pending = list.stream().filter(p -> p.getStatus() == 0).count();
        return Result.ok(Map.of("purchaseAmount", total, "confirmed", confirmed, "pending", pending,
                "count", list.size()));
    }

    @GetMapping("/report/members")
    public Result<Map<String, Object>> memberReport() {
        List<BizMember> members = memberMapper.selectList(Wrappers.<BizMember>lambdaQuery());
        BigDecimal consume = members.stream().map(BizMember::getTotalConsume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Result.ok(Map.of("total", members.size(),
                "totalConsume", consume,
                "avgConsume", members.isEmpty() ? BigDecimal.ZERO
                        : consume.divide(BigDecimal.valueOf(members.size()), 2, RoundingMode.HALF_UP),
                "levels", members.stream().map(m -> m.getLevel() == null ? 1 : m.getLevel()).distinct().toList()));
    }

    /** 全国日报（平台端） */
    @GetMapping("/platform/report/daily")
    public Result<Map<String, Object>> nationalDaily(@RequestParam(required = false) String month) {
        SecurityUtils.requirePlatform();
        String m = month == null ? LocalDate.now().toString().substring(0, 7) : month;
        List<BizReportDaily> list = reportDailyMapper.selectList(Wrappers.<BizReportDaily>lambdaQuery()
                .likeRight(BizReportDaily::getStatDate, m).orderByAsc(BizReportDaily::getStatDate));
        BigDecimal sales = list.stream().map(BizReportDaily::getSalesAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long orders = list.stream().mapToLong(r -> r.getOrderCount() == null ? 0 : r.getOrderCount()).sum();
        List<SysTenant> tenants = tenantMapper.selectList(Wrappers.<SysTenant>lambdaQuery());
        return Result.ok(Map.of("month", m, "rows", list, "salesAmount", sales, "orderCount", orders,
                "tenantCount", tenants.size()));
    }

    @GetMapping("/report/export")
    public void export(@RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate,
                       HttpServletResponse response) throws Exception {
        LocalDate start = startDate == null ? LocalDate.now().minusDays(30) : LocalDate.parse(startDate);
        LocalDate end = endDate == null ? LocalDate.now() : LocalDate.parse(endDate);
        List<BizOrder> orders = orderMapper.selectList(Wrappers.<BizOrder>lambdaQuery()
                .between(BizOrder::getCreatedAt, start.atStartOfDay(), end.plusDays(1).atStartOfDay())
                .orderByDesc(BizOrder::getId));
        List<OrderReportRow> rows = orders.stream().map(o -> {
            OrderReportRow r = new OrderReportRow();
            r.setOrderNo(o.getOrderNo());
            r.setProductTitle(o.getProductTitle());
            r.setDepartDate(o.getDepartDate() == null ? "" : o.getDepartDate().toString());
            r.setPeople((o.getAdultCount() == null ? 0 : o.getAdultCount()) + (o.getChildCount() == null ? 0 : o.getChildCount()));
            r.setAmount(o.getPaidAmount());
            r.setRefund(o.getRefundAmount());
            r.setChannel(o.getChannel());
            r.setStatus(String.valueOf(o.getStatus()));
            r.setCreatedAt(o.getCreatedAt() == null ? "" : o.getCreatedAt().toString());
            return r;
        }).toList();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" +
                URLEncoder.encode("经营报表.xlsx", StandardCharsets.UTF_8));
        EasyExcel.write(response.getOutputStream(), OrderReportRow.class).sheet("订单").doWrite(rows);
    }
}
