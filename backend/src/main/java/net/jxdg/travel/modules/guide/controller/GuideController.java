package net.jxdg.travel.modules.guide.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import net.jxdg.travel.common.*;
import net.jxdg.travel.modules.guide.entity.*;
import net.jxdg.travel.modules.guide.mapper.*;
import net.jxdg.travel.modules.order.entity.BizOrder;
import net.jxdg.travel.modules.order.entity.BizOrderTourist;
import net.jxdg.travel.modules.order.mapper.BizOrderMapper;
import net.jxdg.travel.modules.order.mapper.BizOrderTouristMapper;
import net.jxdg.travel.modules.order.service.OrderService;
import net.jxdg.travel.modules.product.entity.BizProduct;
import net.jxdg.travel.modules.product.entity.BizSchedule;
import net.jxdg.travel.modules.product.mapper.BizProductMapper;
import net.jxdg.travel.modules.product.mapper.BizScheduleMapper;
import net.jxdg.travel.modules.resource.entity.BizSupplier;
import net.jxdg.travel.modules.resource.mapper.BizSupplierMapper;
import net.jxdg.travel.security.LoginUser;
import net.jxdg.travel.security.SecurityUtils;
import net.jxdg.travel.tenant.TenantContext;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class GuideController {

    private final BizGuideMapper guideMapper;
    private final BizGuideAssignmentMapper assignmentMapper;
    private final BizGuideExpenseMapper expenseMapper;
    private final BizGuideCashMapper cashMapper;
    private final BizScheduleMapper scheduleMapper;
    private final BizProductMapper productMapper;
    private final BizOrderMapper orderMapper;
    private final BizOrderTouristMapper touristMapper;
    private final BizSupplierMapper supplierMapper;
    private final OrderService orderService;
    private final CryptoUtil cryptoUtil;

    // ---------------- 后台导游档案 ----------------
    @GetMapping("/guides")
    public Result<PageResult<BizGuide>> page(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "20") long size,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer status) {
        Page<BizGuide> p = guideMapper.selectPage(new Page<>(page, size), Wrappers.<BizGuide>lambdaQuery()
                .and(keyword != null && !keyword.isBlank(), w -> w.like(BizGuide::getName, keyword)
                        .or().like(BizGuide::getGuideNo, keyword).or().like(BizGuide::getPhone, keyword))
                .eq(status != null, BizGuide::getStatus, status)
                .orderByDesc(BizGuide::getId));
        return Result.ok(PageResult.of(p));
    }

    @GetMapping("/guides/all")
    public Result<List<BizGuide>> all() {
        return Result.ok(guideMapper.selectList(Wrappers.<BizGuide>lambdaQuery().eq(BizGuide::getStatus, 1)));
    }

    @GetMapping("/guides/{id}")
    public Result<BizGuide> detail(@PathVariable Long id) {
        BizGuide g = guideMapper.selectById(id);
        if (g == null) throw BizException.notFound("导游不存在");
        return Result.ok(g);
    }

    @PostMapping("/guides")
    @Audit(module = "导游", action = "保存导游档案")
    public Result<Void> save(@RequestBody Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        BizGuide g = id == null ? new BizGuide() : guideMapper.selectById(id);
        if (g == null) throw BizException.notFound("导游不存在");
        g.setName(str(body.get("name")));
        g.setGuideNo(str(body.get("guideNo")));
        g.setLevel(str(body.get("level")));
        g.setPhone(str(body.get("phone")));
        g.setGender(str(body.get("gender")));
        g.setLanguages(str(body.get("languages")));
        g.setAvailableDates(str(body.get("availableDates")));
        g.setUnionId(str(body.get("unionId")));
        g.setUserId(body.get("userId") == null ? null : Long.valueOf(String.valueOf(body.get("userId"))));
        g.setStatus(body.get("status") == null ? 1 : Integer.parseInt(String.valueOf(body.get("status"))));
        g.setRemark(str(body.get("remark")));
        if (body.get("deposit") != null) g.setDeposit(new BigDecimal(String.valueOf(body.get("deposit"))));
        String idCard = str(body.get("idCard"));
        if (idCard != null && !idCard.isBlank() && !idCard.contains("*")) {
            g.setIdCardEnc(cryptoUtil.encrypt(idCard));
            g.setIdCardMask(CryptoUtil.maskIdCard(idCard));
        }
        if (id == null) {
            g.setTenantId(TenantContext.getTenantId());
            guideMapper.insert(g);
        } else {
            guideMapper.updateById(g);
        }
        return Result.ok();
    }

    @DeleteMapping("/guides/{id}")
    @Audit(module = "导游", action = "删除导游")
    public Result<Void> delete(@PathVariable Long id) {
        guideMapper.deleteById(id);
        return Result.ok();
    }

    // ---------------- 带团安排 ----------------
    @GetMapping("/guide/assignments")
    public Result<PageResult<Map<String, Object>>> assignments(@RequestParam(defaultValue = "1") long page,
                                                               @RequestParam(defaultValue = "20") long size,
                                                               @RequestParam(required = false) Long guideId,
                                                               @RequestParam(required = false) Integer status) {
        Page<BizGuideAssignment> p = assignmentMapper.selectPage(new Page<>(page, size),
                Wrappers.<BizGuideAssignment>lambdaQuery()
                        .eq(guideId != null, BizGuideAssignment::getGuideId, guideId)
                        .eq(status != null, BizGuideAssignment::getStatus, status)
                        .orderByDesc(BizGuideAssignment::getId));
        return Result.ok(PageResult.of(p.getRecords().stream().map(this::view).toList(),
                p.getTotal(), p.getCurrent(), p.getSize()));
    }

    @PostMapping("/guide/assignments/{id}/confirm")
    @Audit(module = "导游", action = "导游确认接团")
    public Result<Void> confirm(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean accept) {
        BizGuideAssignment a = assignmentMapper.selectById(id);
        if (a == null) throw BizException.notFound("带团安排不存在");
        a.setStatus(accept ? 1 : 2);
        a.setConfirmTime(LocalDateTime.now());
        assignmentMapper.updateById(a);
        return Result.ok();
    }

    // ---------------- 报账审核 ----------------
    @GetMapping("/guide/expenses")
    public Result<PageResult<Map<String, Object>>> expenses(@RequestParam(defaultValue = "1") long page,
                                                            @RequestParam(defaultValue = "20") long size,
                                                            @RequestParam(required = false) Integer status,
                                                            @RequestParam(required = false) Long scheduleId) {
        Page<BizGuideExpense> p = expenseMapper.selectPage(new Page<>(page, size),
                Wrappers.<BizGuideExpense>lambdaQuery()
                        .eq(status != null, BizGuideExpense::getStatus, status)
                        .eq(scheduleId != null, BizGuideExpense::getScheduleId, scheduleId)
                        .orderByDesc(BizGuideExpense::getId));
        return Result.ok(PageResult.of(p.getRecords().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("expense", e);
            BizGuide g = guideMapper.selectById(e.getGuideId());
            m.put("guideName", g == null ? "" : g.getName());
            BizSchedule s = scheduleMapper.selectById(e.getScheduleId());
            m.put("scheduleNo", s == null ? "" : s.getScheduleNo());
            return m;
        }).toList(), p.getTotal(), p.getCurrent(), p.getSize()));
    }

    @PostMapping("/guide/expenses/{id}/audit")
    @Audit(module = "导游", action = "报账审核")
    public Result<Void> audit(@PathVariable Long id, @RequestParam boolean approve,
                              @RequestParam(required = false) String remark) {
        BizGuideExpense e = expenseMapper.selectById(id);
        if (e == null) throw BizException.notFound("报账记录不存在");
        e.setStatus(approve ? 1 : 2);
        e.setAuditBy(SecurityUtils.username());
        e.setAuditAt(LocalDateTime.now());
        if (remark != null) e.setRemark(remark);
        expenseMapper.updateById(e);
        return Result.ok();
    }

    /** 导游报账汇总（后台） */
    @GetMapping("/guide/expenses/summary")
    public Result<List<Map<String, Object>>> expenseSummary() {
        List<BizGuide> guides = guideMapper.selectList(Wrappers.<BizGuide>lambdaQuery());
        List<Map<String, Object>> out = new ArrayList<>();
        for (BizGuide g : guides) {
            List<BizGuideExpense> list = expenseMapper.selectList(Wrappers.<BizGuideExpense>lambdaQuery()
                    .eq(BizGuideExpense::getGuideId, g.getId()));
            BigDecimal total = list.stream().map(BizGuideExpense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            out.add(Map.of("guideId", g.getId(), "guideName", g.getName(),
                    "count", list.size(), "amount", total,
                    "approved", list.stream().filter(x -> x.getStatus() == 1).count()));
        }
        return Result.ok(out);
    }

    // ---------------- 导游端（小程序/App） ----------------
    @GetMapping("/guide/app/schedules")
    public Result<List<Map<String, Object>>> mySchedules() {
        BizGuide guide = currentGuide();
        List<Map<String, Object>> out = new ArrayList<>();
        for (BizGuideAssignment a : assignmentMapper.selectList(Wrappers.<BizGuideAssignment>lambdaQuery()
                .eq(BizGuideAssignment::getGuideId, guide.getId()).orderByDesc(BizGuideAssignment::getId))) {
            BizSchedule s = scheduleMapper.selectById(a.getScheduleId());
            if (s == null) continue;
            BizProduct p = productMapper.selectById(s.getProductId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("assignmentId", a.getId());
            m.put("assignmentStatus", a.getStatus());
            m.put("scheduleId", s.getId());
            m.put("scheduleNo", s.getScheduleNo());
            m.put("productTitle", p == null ? "" : p.getTitle());
            m.put("departDate", s.getDepartDate());
            m.put("returnDate", s.getReturnDate());
            m.put("meetingPoint", s.getMeetingPoint());
            m.put("meetingTime", s.getMeetingTime());
            m.put("status", s.getStatus());
            m.put("sold", s.getSold());
            out.add(m);
        }
        return Result.ok(out);
    }

    @GetMapping("/guide/app/schedules/{scheduleId}/tourists")
    public Result<List<Map<String, Object>>> tourists(@PathVariable Long scheduleId) {
        currentGuide();
        List<Map<String, Object>> out = new ArrayList<>();
        for (BizOrder o : orderMapper.selectList(Wrappers.<BizOrder>lambdaQuery()
                .eq(BizOrder::getScheduleId, scheduleId).ne(BizOrder::getStatus, 8))) {
            for (BizOrderTourist t : touristMapper.selectList(Wrappers.<BizOrderTourist>lambdaQuery()
                    .eq(BizOrderTourist::getOrderId, o.getId()))) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("touristId", t.getId());
                m.put("orderNo", o.getOrderNo());
                m.put("name", t.getName());
                m.put("idNoMask", t.getIdNoMask());
                m.put("phoneMask", t.getPhoneMask());
                m.put("isAdult", t.getIsAdult());
                m.put("verifyStatus", t.getVerifyStatus());
                out.add(m);
            }
        }
        return Result.ok(out);
    }

    @PostMapping("/guide/app/schedules/{scheduleId}/rollcall")
    @Audit(module = "导游端", action = "客人点名")
    public Result<Map<String, Object>> rollcall(@PathVariable Long scheduleId, @RequestBody Map<String, Object> body) {
        BizGuide guide = currentGuide();
        List<BizOrderTourist> tourists = new ArrayList<>();
        for (BizOrder o : orderMapper.selectList(Wrappers.<BizOrder>lambdaQuery()
                .eq(BizOrder::getScheduleId, scheduleId).ne(BizOrder::getStatus, 8))) {
            tourists.addAll(touristMapper.selectList(Wrappers.<BizOrderTourist>lambdaQuery()
                    .eq(BizOrderTourist::getOrderId, o.getId())));
        }
        int present = body.get("present") == null ? tourists.size() : Integer.parseInt(String.valueOf(body.get("present")));
        return Result.ok(Map.of("expected", tourists.size(), "present", present,
                "guide", guide.getName(), "rollcallAt", LocalDateTime.now()));
    }

    @PostMapping("/guide/app/expenses")
    @Audit(module = "导游端", action = "提交报账")
    public Result<BizGuideExpense> submitExpense(@RequestBody Map<String, Object> body) {
        BizGuide guide = currentGuide();
        BizGuideExpense e = new BizGuideExpense();
        e.setTenantId(TenantContext.getTenantId());
        e.setGuideId(guide.getId());
        e.setScheduleId(Long.valueOf(String.valueOf(body.get("scheduleId"))));
        e.setType(str(body.getOrDefault("type", "other")));
        e.setAmount(new BigDecimal(String.valueOf(body.getOrDefault("amount", "0"))));
        e.setReceiptImages(str(body.get("receiptImages")));
        e.setRemark(str(body.get("remark")));
        e.setStatus(0);
        e.setCreatedAt(LocalDateTime.now());
        expenseMapper.insert(e);
        return Result.ok(e);
    }

    @GetMapping("/guide/app/expenses")
    public Result<List<BizGuideExpense>> myExpenses(@RequestParam(required = false) Long scheduleId) {
        BizGuide guide = currentGuide();
        return Result.ok(expenseMapper.selectList(Wrappers.<BizGuideExpense>lambdaQuery()
                .eq(BizGuideExpense::getGuideId, guide.getId())
                .eq(scheduleId != null, BizGuideExpense::getScheduleId, scheduleId)
                .orderByDesc(BizGuideExpense::getId)));
    }

    @GetMapping("/guide/app/cash")
    public Result<List<BizGuideCash>> myCash() {
        BizGuide guide = currentGuide();
        return Result.ok(cashMapper.selectList(Wrappers.<BizGuideCash>lambdaQuery()
                .eq(BizGuideCash::getGuideId, guide.getId()).orderByDesc(BizGuideCash::getId)));
    }

    @PostMapping("/guide/app/cash")
    @Audit(module = "导游端", action = "现金领款申请")
    public Result<BizGuideCash> requestCash(@RequestBody Map<String, Object> body) {
        BizGuide guide = currentGuide();
        BizGuideCash c = new BizGuideCash();
        c.setTenantId(TenantContext.getTenantId());
        c.setGuideId(guide.getId());
        c.setScheduleId(Long.valueOf(String.valueOf(body.get("scheduleId"))));
        c.setAmount(new BigDecimal(String.valueOf(body.getOrDefault("amount", "0"))));
        c.setStatus(0);
        c.setRemark(str(body.get("remark")));
        c.setCreatedAt(LocalDateTime.now());
        cashMapper.insert(c);
        return Result.ok(c);
    }

    @GetMapping("/guide/app/suppliers")
    public Result<List<Map<String, Object>>> supplierPhones() {
        currentGuide();
        return Result.ok(supplierMapper.selectList(Wrappers.<BizSupplier>lambdaQuery()
                        .eq(BizSupplier::getStatus, 1)).stream()
                .map(s -> Map.<String, Object>of("id", s.getId(), "name", s.getName(),
                        "type", s.getType(), "phone", s.getPhone() == null ? "" : s.getPhone()))
                .toList());
    }

    @GetMapping("/guide/app/report")
    public Result<Map<String, Object>> report(@RequestParam Long scheduleId) {
        BizGuide guide = currentGuide();
        List<BizGuideExpense> expenses = expenseMapper.selectList(Wrappers.<BizGuideExpense>lambdaQuery()
                .eq(BizGuideExpense::getScheduleId, scheduleId).eq(BizGuideExpense::getGuideId, guide.getId()));
        BigDecimal expTotal = expenses.stream().map(BizGuideExpense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<BizGuideCash> cash = cashMapper.selectList(Wrappers.<BizGuideCash>lambdaQuery()
                .eq(BizGuideCash::getScheduleId, scheduleId).eq(BizGuideCash::getGuideId, guide.getId()));
        BigDecimal cashTotal = cash.stream().map(BizGuideCash::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long orderCount = orderMapper.selectCount(Wrappers.<BizOrder>lambdaQuery()
                .eq(BizOrder::getScheduleId, scheduleId).ne(BizOrder::getStatus, 8));
        return Result.ok(Map.of("orderCount", orderCount, "expenseAmount", expTotal, "cashAmount", cashTotal,
                "balance", cashTotal.subtract(expTotal), "expenses", expenses));
    }

    private BizGuide currentGuide() {
        LoginUser u = SecurityUtils.current();
        if (!"GUIDE".equals(u.getUserType())) throw BizException.forbidden("仅导游账号可访问");
        BizGuide g = guideMapper.selectById(u.getId());
        if (g == null) throw BizException.notFound("导游档案不存在");
        return g;
    }

    private Map<String, Object> view(BizGuideAssignment a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("status", a.getStatus());
        m.put("pushTime", a.getPushTime());
        m.put("confirmTime", a.getConfirmTime());
        BizGuide g = guideMapper.selectById(a.getGuideId());
        m.put("guideId", a.getGuideId());
        m.put("guideName", g == null ? "" : g.getName());
        BizSchedule s = scheduleMapper.selectById(a.getScheduleId());
        if (s != null) {
            m.put("scheduleId", s.getId());
            m.put("scheduleNo", s.getScheduleNo());
            m.put("departDate", s.getDepartDate());
            m.put("meetingPoint", s.getMeetingPoint());
            BizProduct p = productMapper.selectById(s.getProductId());
            m.put("productTitle", p == null ? "" : p.getTitle());
        }
        return m;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
