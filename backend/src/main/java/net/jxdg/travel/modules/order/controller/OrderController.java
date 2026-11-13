package net.jxdg.travel.modules.order.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.jxdg.travel.common.*;
import net.jxdg.travel.modules.order.dto.TouristRow;
import net.jxdg.travel.modules.order.entity.*;
import net.jxdg.travel.modules.order.mapper.*;
import net.jxdg.travel.modules.order.service.OrderService;
import net.jxdg.travel.modules.product.entity.BizSchedule;
import net.jxdg.travel.modules.product.mapper.BizScheduleMapper;
import net.jxdg.travel.security.LoginUser;
import net.jxdg.travel.security.SecurityUtils;
import net.jxdg.travel.wechat.WechatService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final BizOrderMapper orderMapper;
    private final BizOrderTouristMapper touristMapper;
    private final BizOrderLogMapper orderLogMapper;
    private final BizPaymentMapper paymentMapper;
    private final BizRefundMapper refundMapper;
    private final BizVerifyRecordMapper verifyRecordMapper;
    private final BizScheduleMapper scheduleMapper;
    private final OrderService orderService;
    private final WechatService wechatService;

    @GetMapping("/orders")
    @PreAuthorize("hasAnyAuthority('*','order:list')")
    public Result<PageResult<BizOrder>> page(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "20") long size,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer status,
                                             @RequestParam(required = false) Integer payStatus,
                                             @RequestParam(required = false) Integer verifyStatus,
                                             @RequestParam(required = false) String channel,
                                             @RequestParam(required = false) Long scheduleId,
                                             @RequestParam(required = false) String startDate,
                                             @RequestParam(required = false) String endDate) {
        Page<BizOrder> p = orderMapper.selectPage(new Page<>(page, size), Wrappers.<BizOrder>lambdaQuery()
                .and(keyword != null && !keyword.isBlank(), w -> w.like(BizOrder::getOrderNo, keyword)
                        .or().like(BizOrder::getContactName, keyword).or().like(BizOrder::getContactPhone, keyword))
                .eq(status != null, BizOrder::getStatus, status)
                .eq(payStatus != null, BizOrder::getPayStatus, payStatus)
                .eq(verifyStatus != null, BizOrder::getVerifyStatus, verifyStatus)
                .eq(channel != null && !channel.isBlank(), BizOrder::getChannel, channel)
                .eq(scheduleId != null, BizOrder::getScheduleId, scheduleId)
                .ge(startDate != null && !startDate.isBlank(), BizOrder::getCreatedAt, startDate)
                .le(endDate != null && !endDate.isBlank(), BizOrder::getCreatedAt, endDate + " 23:59:59")
                .orderByDesc(BizOrder::getId));
        return Result.ok(PageResult.of(p));
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("hasAnyAuthority('*','order:list','order:view')")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        BizOrder order = orderMapper.selectById(id);
        if (order == null) throw BizException.notFound("订单不存在");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("order", order);
        out.put("tourists", touristMapper.selectList(Wrappers.<BizOrderTourist>lambdaQuery()
                .eq(BizOrderTourist::getOrderId, id)));
        out.put("logs", orderLogMapper.selectList(Wrappers.<BizOrderLog>lambdaQuery()
                .eq(BizOrderLog::getOrderId, id).orderByAsc(BizOrderLog::getId)));
        out.put("payments", paymentMapper.selectList(Wrappers.<BizPayment>lambdaQuery()
                .eq(BizPayment::getOrderId, id).orderByDesc(BizPayment::getId)));
        out.put("refunds", refundMapper.selectList(Wrappers.<BizRefund>lambdaQuery()
                .eq(BizRefund::getOrderId, id).orderByDesc(BizRefund::getId)));
        out.put("verifyRecords", verifyRecordMapper.selectList(Wrappers.<BizVerifyRecord>lambdaQuery()
                .eq(BizVerifyRecord::getOrderId, id).orderByDesc(BizVerifyRecord::getId)));
        return Result.ok(out);
    }

    @PostMapping("/orders")
    @PreAuthorize("hasAnyAuthority('*','order:list','order:add')")
    @Audit(module = "订单", action = "创建订单")
    public Result<BizOrder> create(@RequestBody Map<String, Object> body) {
        return Result.ok(orderService.create(SecurityUtils.current(), body));
    }

    @PostMapping("/orders/{id}/pay")
    @PreAuthorize("hasAnyAuthority('*','order:list')")
    @Audit(module = "订单", action = "订单支付")
    public Result<BizPayment> pay(@PathVariable Long id, @RequestParam(required = false) String channel) {
        return Result.ok(orderService.pay(id, channel, null));
    }

    @PostMapping("/orders/{id}/confirm")
    @PreAuthorize("hasAnyAuthority('*','order:list')")
    @Audit(module = "订单", action = "确认订单")
    public Result<Void> confirm(@PathVariable Long id) {
        orderService.confirm(id, SecurityUtils.username());
        return Result.ok();
    }

    @PostMapping("/orders/{id}/notify")
    @PreAuthorize("hasAnyAuthority('*','order:list')")
    @Audit(module = "订单", action = "发送出团通知")
    public Result<Void> notifyDepart(@PathVariable Long id) {
        BizOrder order = orderMapper.selectById(id);
        if (order == null) throw BizException.notFound("订单不存在");
        Map<String, String> data = new LinkedHashMap<>();
        data.put("线路名称", order.getProductTitle());
        data.put("出团日期", order.getDepartDate() == null ? "" : order.getDepartDate().toString());
        data.put("集合地点", "");
        data.put("导游", "");
        if (order.getScheduleId() != null) {
            BizSchedule s = scheduleMapper.selectById(order.getScheduleId());
            if (s != null) {
                data.put("集合地点", s.getMeetingPoint() == null ? "" : s.getMeetingPoint());
                data.put("集合时间", s.getMeetingTime() == null ? "" : s.getMeetingTime());
            }
        }
        wechatService.sendTemplate(net.jxdg.travel.tenant.TenantContext.getTenantId(), order.getMemberId(), "depart", data);
        order.setStatus(Math.max(order.getStatus(), 4));
        orderMapper.updateById(order);
        orderService.log(id, order.getStatus(), "发送出团通知", "已推送公众号模板消息/站内消息", SecurityUtils.username());
        return Result.ok();
    }

    @PostMapping("/orders/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('*','order:list')")
    @Audit(module = "订单", action = "取消订单")
    public Result<Void> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        BizOrder order = orderMapper.selectById(id);
        if (order == null) throw BizException.notFound("订单不存在");
        if (order.getPayStatus() == 1) throw new BizException("已支付订单请走退改流程");
        order.setStatus(8);
        order.setCancelReason(reason);
        orderMapper.updateById(order);
        if (order.getScheduleId() != null) {
            int people = order.getAdultCount() + order.getChildCount();
            scheduleMapper.update(null, Wrappers.<BizSchedule>lambdaUpdate()
                    .setSql("sold = GREATEST(sold - " + people + ", 0)").eq(BizSchedule::getId, order.getScheduleId()));
        }
        orderService.log(id, 8, "取消订单", reason, SecurityUtils.username());
        return Result.ok();
    }

    @PostMapping("/orders/verify")
    @PreAuthorize("hasAnyAuthority('*','order:verify')")
    @Audit(module = "订单", action = "扫码核销")
    public Result<Map<String, Object>> verify(@RequestBody Map<String, Object> body) {
        String code = String.valueOf(body.getOrDefault("code", ""));
        String gate = body.get("gate") == null ? "门店" : String.valueOf(body.get("gate"));
        return Result.ok(orderService.verify(code, gate, SecurityUtils.current()));
    }

    @PostMapping("/orders/{id}/refund")
    @PreAuthorize("hasAnyAuthority('*','order:refund','order:list')")
    @Audit(module = "订单", action = "申请退改")
    public Result<BizRefund> applyRefund(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal amount = body.get("amount") == null ? null : new BigDecimal(String.valueOf(body.get("amount")));
        String reason = body.get("reason") == null ? "游客申请退改" : String.valueOf(body.get("reason"));
        return Result.ok(orderService.applyRefund(id, amount, reason, SecurityUtils.username()));
    }

    @PostMapping("/orders/refunds/{refundId}/approve")
    @PreAuthorize("hasAnyAuthority('*','order:refund')")
    @Audit(module = "订单", action = "退款审批")
    public Result<Void> approveRefund(@PathVariable Long refundId,
                                      @RequestParam(defaultValue = "true") boolean approve) {
        orderService.approveRefund(refundId, approve, SecurityUtils.username());
        return Result.ok();
    }

    @GetMapping("/orders/tourists/export")
    public void exportTourists(HttpServletResponse response, @RequestParam(required = false) Long scheduleId) throws Exception {
        List<BizOrder> orders = orderMapper.selectList(Wrappers.<BizOrder>lambdaQuery()
                .eq(scheduleId != null, BizOrder::getScheduleId, scheduleId)
                .ne(BizOrder::getStatus, 8));
        List<TouristRow> rows = new ArrayList<>();
        for (BizOrder o : orders) {
            for (BizOrderTourist t : touristMapper.selectList(Wrappers.<BizOrderTourist>lambdaQuery()
                    .eq(BizOrderTourist::getOrderId, o.getId()))) {
                TouristRow r = new TouristRow();
                r.setOrderNo(o.getOrderNo());
                r.setName(t.getName());
                r.setIdType(t.getIdType());
                r.setIdNoMask(t.getIdNoMask());
                r.setPhoneMask(t.getPhoneMask());
                r.setIsAdult(t.getIsAdult() != null && t.getIsAdult() == 1 ? "成人" : "儿童");
                r.setVerifyStatus(t.getVerifyStatus() != null && t.getVerifyStatus() == 1 ? "已核销" : "未核销");
                rows.add(r);
            }
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" +
                URLEncoder.encode("游客名单.xlsx", StandardCharsets.UTF_8));
        EasyExcel.write(response.getOutputStream(), TouristRow.class).sheet("名单").doWrite(rows);
    }

    @GetMapping("/orders/stat")
    public Result<Map<String, Object>> stat() {
        long today = orderMapper.selectCount(Wrappers.<BizOrder>lambdaQuery()
                .ge(BizOrder::getCreatedAt, java.time.LocalDate.now().atStartOfDay()));
        long pending = orderMapper.selectCount(Wrappers.<BizOrder>lambdaQuery().eq(BizOrder::getStatus, 1));
        long toVerify = orderMapper.selectCount(Wrappers.<BizOrder>lambdaQuery().eq(BizOrder::getVerifyStatus, 0)
                .eq(BizOrder::getPayStatus, 1));
        return Result.ok(Map.of("todayOrders", today, "pendingPay", pending, "toVerify", toVerify));
    }

    // ---------- 游客端（仅本人数据） ----------
    @GetMapping("/member/orders")
    public Result<List<BizOrder>> myOrders(@RequestParam(required = false) Integer status) {
        LoginUser u = SecurityUtils.current();
        if (!"MEMBER".equals(u.getUserType())) throw BizException.forbidden("仅游客账号可访问");
        return Result.ok(orderMapper.selectList(Wrappers.<BizOrder>lambdaQuery()
                .eq(BizOrder::getMemberId, u.getId())
                .eq(status != null, BizOrder::getStatus, status)
                .orderByDesc(BizOrder::getId)));
    }

    @GetMapping("/member/orders/{id}")
    public Result<Map<String, Object>> myOrder(@PathVariable Long id) {
        LoginUser u = SecurityUtils.current();
        BizOrder order = orderMapper.selectById(id);
        if (order == null || !"MEMBER".equals(u.getUserType()) || !Objects.equals(order.getMemberId(), u.getId())) {
            throw BizException.forbidden("无权查看该订单");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("order", order);
        out.put("tourists", touristMapper.selectList(Wrappers.<BizOrderTourist>lambdaQuery()
                .eq(BizOrderTourist::getOrderId, id)));
        out.put("logs", orderLogMapper.selectList(Wrappers.<BizOrderLog>lambdaQuery()
                .eq(BizOrderLog::getOrderId, id).orderByAsc(BizOrderLog::getId)));
        return Result.ok(out);
    }

    @PostMapping("/member/orders")
    public Result<BizOrder> memberCreate(@RequestBody Map<String, Object> body) {
        return Result.ok(orderService.create(SecurityUtils.current(), body));
    }

    @PostMapping("/member/orders/{id}/pay")
    public Result<BizPayment> memberPay(@PathVariable Long id) {
        LoginUser u = SecurityUtils.current();
        BizOrder order = orderMapper.selectById(id);
        if (order == null || !"MEMBER".equals(u.getUserType()) || !Objects.equals(order.getMemberId(), u.getId())) {
            throw BizException.forbidden("无权操作该订单");
        }
        return Result.ok(orderService.pay(id, "wechat", null));
    }

    @PostMapping("/member/orders/{id}/refund")
    @Audit(module = "游客端", action = "游客申请退改")
    public Result<BizRefund> memberRefund(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        LoginUser u = SecurityUtils.current();
        BizOrder order = orderMapper.selectById(id);
        if (order == null || !Objects.equals(order.getMemberId(), u.getId())) {
            throw BizException.forbidden("无权操作该订单");
        }
        BigDecimal amount = body.get("amount") == null ? null : new BigDecimal(String.valueOf(body.get("amount")));
        return Result.ok(orderService.applyRefund(id, amount,
                body.get("reason") == null ? "游客申请退改" : String.valueOf(body.get("reason")), u.getUsername()));
    }
}
