package net.jxdg.travel.modules.product.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.jxdg.travel.common.*;
import net.jxdg.travel.modules.guide.entity.BizGuide;
import net.jxdg.travel.modules.guide.entity.BizGuideAssignment;
import net.jxdg.travel.modules.guide.mapper.BizGuideAssignmentMapper;
import net.jxdg.travel.modules.guide.mapper.BizGuideMapper;
import net.jxdg.travel.modules.order.entity.BizOrder;
import net.jxdg.travel.modules.order.entity.BizOrderTourist;
import net.jxdg.travel.modules.order.mapper.BizOrderMapper;
import net.jxdg.travel.modules.order.mapper.BizOrderTouristMapper;
import net.jxdg.travel.modules.product.entity.BizProduct;
import net.jxdg.travel.modules.product.entity.BizSchedule;
import net.jxdg.travel.modules.product.mapper.BizProductMapper;
import net.jxdg.travel.modules.product.mapper.BizScheduleMapper;
import net.jxdg.travel.tenant.TenantContext;
import net.jxdg.travel.wechat.WechatService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/schedules")
public class ScheduleController {

    private final BizScheduleMapper scheduleMapper;
    private final BizProductMapper productMapper;
    private final BizGuideMapper guideMapper;
    private final BizGuideAssignmentMapper assignmentMapper;
    private final BizOrderMapper orderMapper;
    private final BizOrderTouristMapper touristMapper;
    private final NumberGenerator numberGenerator;
    private final PdfUtil pdfUtil;
    private final WechatService wechatService;

    @GetMapping
    public Result<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "20") long size,
                                                        @RequestParam(required = false) Long productId,
                                                        @RequestParam(required = false) Long guideId,
                                                        @RequestParam(required = false) Integer status,
                                                        @RequestParam(required = false) String startDate,
                                                        @RequestParam(required = false) String endDate) {
        Page<BizSchedule> p = scheduleMapper.selectPage(new Page<>(page, size), Wrappers.<BizSchedule>lambdaQuery()
                .eq(productId != null, BizSchedule::getProductId, productId)
                .eq(guideId != null, BizSchedule::getGuideId, guideId)
                .eq(status != null, BizSchedule::getStatus, status)
                .ge(startDate != null && !startDate.isBlank(), BizSchedule::getDepartDate, startDate)
                .le(endDate != null && !endDate.isBlank(), BizSchedule::getDepartDate, endDate)
                .orderByAsc(BizSchedule::getDepartDate));
        return Result.ok(PageResult.of(p.getRecords().stream().map(this::toView).toList(),
                p.getTotal(), p.getCurrent(), p.getSize()));
    }

    @GetMapping("/calendar")
    public Result<List<Map<String, Object>>> calendar(@RequestParam String startDate,
                                                      @RequestParam String endDate,
                                                      @RequestParam(required = false) Long productId,
                                                      @RequestParam(required = false) Long guideId) {
        return Result.ok(scheduleMapper.selectList(Wrappers.<BizSchedule>lambdaQuery()
                        .between(BizSchedule::getDepartDate, startDate, endDate)
                        .eq(productId != null, BizSchedule::getProductId, productId)
                        .eq(guideId != null, BizSchedule::getGuideId, guideId)
                        .orderByAsc(BizSchedule::getDepartDate))
                .stream().map(this::toView).toList());
    }

    /** 成团/出团预警：红黄灯清单 */
    @GetMapping("/warnings")
    public Result<List<Map<String, Object>>> warnings() {
        LocalDate today = LocalDate.now();
        List<BizSchedule> list = scheduleMapper.selectList(Wrappers.<BizSchedule>lambdaQuery()
                .in(BizSchedule::getStatus, 1, 2, 3)
                .between(BizSchedule::getDepartDate, today, today.plusDays(15))
                .orderByAsc(BizSchedule::getDepartDate));
        List<Map<String, Object>> out = new ArrayList<>();
        for (BizSchedule s : list) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(today, s.getDepartDate());
            boolean shortGroup = s.getSold() < s.getMinGroup();
            Map<String, Object> m = toView(s);
            m.put("daysLeft", days);
            m.put("shortGroup", shortGroup);
            String level;
            if (days <= 1 || (shortGroup && days <= s.getWarnDays())) level = "red";
            else if (days <= s.getWarnDays() || shortGroup) level = "yellow";
            else level = "normal";
            m.put("level", level);
            out.add(m);
        }
        return Result.ok(out);
    }

    @PostMapping
    @Audit(module = "排期管理", action = "新增排期")
    public Result<BizSchedule> create(@RequestBody Map<String, Object> body) {
        BizProduct product = productMapper.selectById(Long.valueOf(String.valueOf(body.get("productId"))));
        if (product == null) throw BizException.notFound("产品不存在");
        BizSchedule s = new BizSchedule();
        apply(s, body);
        s.setScheduleNo(numberGenerator.next("SC"));
        s.setTenantId(TenantContext.getTenantId());
        s.setSold(0);
        s.setStatus(1);
        if (s.getQuota() == null) s.setQuota(product.getStock());
        if (s.getMinGroup() == null) s.setMinGroup(product.getMinGroup());
        if (s.getWarnDays() == null) s.setWarnDays(3);
        scheduleMapper.insert(s);
        return Result.ok(s);
    }

    @PutMapping("/{id}")
    @Audit(module = "排期管理", action = "修改排期")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BizSchedule s = scheduleMapper.selectById(id);
        if (s == null) throw BizException.notFound("排期不存在");
        apply(s, body);
        scheduleMapper.updateById(s);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Audit(module = "排期管理", action = "删除排期")
    public Result<Void> delete(@PathVariable Long id) {
        scheduleMapper.deleteById(id);
        return Result.ok();
    }

    @PostMapping("/{id}/status")
    @Audit(module = "排期管理", action = "变更出团状态")
    public Result<Void> status(@PathVariable Long id, @RequestParam Integer status) {
        BizSchedule s = scheduleMapper.selectById(id);
        if (s == null) throw BizException.notFound("排期不存在");
        s.setStatus(status);
        scheduleMapper.updateById(s);
        return Result.ok();
    }

    /** 日历期间批量生成出团计划（weekdays: 1-7，逗号分隔） */
    @PostMapping("/generate")
    @Audit(module = "排期管理", action = "批量生成出团计划")
    public Result<Map<String, Object>> generate(@RequestBody Map<String, Object> body) {
        Long productId = Long.valueOf(String.valueOf(body.get("productId")));
        BizProduct product = productMapper.selectById(productId);
        if (product == null) throw BizException.notFound("产品不存在");
        LocalDate start = LocalDate.parse(String.valueOf(body.get("startDate")));
        LocalDate end = LocalDate.parse(String.valueOf(body.get("endDate")));
        Set<Integer> weekdays = new HashSet<>();
        Object w = body.get("weekdays");
        if (w instanceof List<?> list) list.forEach(x -> weekdays.add(Integer.valueOf(String.valueOf(x))));
        else if (w != null) Arrays.stream(String.valueOf(w).split(",")).forEach(x -> weekdays.add(Integer.parseInt(x.trim())));
        if (weekdays.isEmpty()) weekdays.addAll(List.of(1, 2, 3, 4, 5, 6, 7));
        int created = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (!weekdays.contains(d.getDayOfWeek().getValue())) continue;
            if (scheduleMapper.selectCount(Wrappers.<BizSchedule>lambdaQuery()
                    .eq(BizSchedule::getProductId, productId).eq(BizSchedule::getDepartDate, d)) > 0) continue;
            BizSchedule s = new BizSchedule();
            s.setTenantId(TenantContext.getTenantId());
            s.setProductId(productId);
            s.setDepartDate(d);
            s.setReturnDate(d.plusDays(Math.max(0, product.getDays() - 1)));
            s.setScheduleNo(numberGenerator.next("SC"));
            s.setMeetingPoint(product.getMeetingPoint());
            s.setQuota(product.getStock());
            s.setSold(0);
            s.setMinGroup(product.getMinGroup());
            s.setWarnDays(3);
            s.setStatus(1);
            scheduleMapper.insert(s);
            created++;
        }
        return Result.ok(Map.of("created", created));
    }

    @PostMapping("/{id}/guide")
    @Audit(module = "排期管理", action = "指派导游并推送")
    public Result<Void> assignGuide(@PathVariable Long id, @RequestParam Long guideId) {
        BizSchedule s = scheduleMapper.selectById(id);
        BizGuide guide = guideMapper.selectById(guideId);
        if (s == null || guide == null) throw BizException.notFound("排期或导游不存在");
        s.setGuideId(guideId);
        scheduleMapper.updateById(s);
        BizGuideAssignment a = assignmentMapper.selectOne(Wrappers.<BizGuideAssignment>lambdaQuery()
                .eq(BizGuideAssignment::getScheduleId, id).eq(BizGuideAssignment::getGuideId, guideId).last("limit 1"));
        if (a == null) {
            a = new BizGuideAssignment();
            a.setTenantId(TenantContext.getTenantId());
            a.setScheduleId(id);
            a.setGuideId(guideId);
            a.setStatus(0);
            a.setPushTime(LocalDateTime.now());
            assignmentMapper.insert(a);
        } else {
            a.setPushTime(LocalDateTime.now());
            assignmentMapper.updateById(a);
        }
        wechatService.sendTemplate(TenantContext.getTenantId(), null, "depart",
                Map.of("线路", String.valueOf(s.getProductId()), "集合地点",
                        s.getMeetingPoint() == null ? "" : s.getMeetingPoint(), "导游", guide.getName()));
        return Result.ok();
    }

    /** 出团计划书 PDF（含行程/名单/计划明细） */
    @GetMapping("/{id}/plan")
    public void plan(@PathVariable Long id, HttpServletResponse response) throws Exception {
        BizSchedule s = scheduleMapper.selectById(id);
        if (s == null) throw BizException.notFound("排期不存在");
        BizProduct p = productMapper.selectById(s.getProductId());
        BizGuide guide = s.getGuideId() == null ? null : guideMapper.selectById(s.getGuideId());
        List<String> lines = new ArrayList<>();
        lines.add("出团编号：" + s.getScheduleNo());
        lines.add("线路名称：" + (p == null ? "" : p.getTitle()));
        lines.add("出团日期：" + s.getDepartDate() + "  集合地点：" + s.getMeetingPoint() + "  集合时间：" + s.getMeetingTime());
        lines.add("导游：" + (guide == null ? "待指派" : guide.getName() + "（" + guide.getPhone() + "）"));
        lines.add("收客：" + s.getSold() + "/" + s.getQuota() + " 人；成团最低：" + s.getMinGroup() + " 人");
        lines.add("--------------------------------------------------");
        lines.add("游客名单：");
        List<BizOrder> orders = orderMapper.selectList(Wrappers.<BizOrder>lambdaQuery()
                .eq(BizOrder::getScheduleId, id).notIn(BizOrder::getStatus, 8));
        int i = 1;
        for (BizOrder o : orders) {
            List<BizOrderTourist> tourists = touristMapper.selectList(Wrappers.<BizOrderTourist>lambdaQuery()
                    .eq(BizOrderTourist::getOrderId, o.getId()));
            for (BizOrderTourist t : tourists) {
                lines.add(i++ + ". " + t.getName() + "  " + (t.getIdNoMask() == null ? "" : t.getIdNoMask())
                        + "  " + (t.getPhoneMask() == null ? "" : t.getPhoneMask()));
            }
        }
        byte[] pdf = pdfUtil.render("出团计划书 - " + (p == null ? "" : p.getTitle()), lines);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", PdfUtil.attachment("出团计划书-" + s.getScheduleNo() + ".pdf"));
        response.getOutputStream().write(pdf);
    }

    private Map<String, Object> toView(BizSchedule s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("scheduleNo", s.getScheduleNo());
        m.put("productId", s.getProductId());
        BizProduct p = productMapper.selectById(s.getProductId());
        m.put("productTitle", p == null ? "" : p.getTitle());
        m.put("days", p == null ? null : p.getDays());
        m.put("departDate", s.getDepartDate());
        m.put("returnDate", s.getReturnDate());
        m.put("meetingPoint", s.getMeetingPoint());
        m.put("meetingTime", s.getMeetingTime());
        m.put("guideId", s.getGuideId());
        if (s.getGuideId() != null) {
            BizGuide g = guideMapper.selectById(s.getGuideId());
            m.put("guideName", g == null ? "" : g.getName());
            m.put("guidePhone", g == null ? "" : g.getPhone());
        }
        m.put("status", s.getStatus());
        m.put("quota", s.getQuota());
        m.put("sold", s.getSold());
        m.put("minGroup", s.getMinGroup());
        m.put("warnDays", s.getWarnDays());
        return m;
    }

    private void apply(BizSchedule s, Map<String, Object> b) {
        if (b.get("productId") != null) s.setProductId(Long.valueOf(String.valueOf(b.get("productId"))));
        if (b.get("departDate") != null) s.setDepartDate(LocalDate.parse(String.valueOf(b.get("departDate"))));
        if (b.get("returnDate") != null && !String.valueOf(b.get("returnDate")).isBlank())
            s.setReturnDate(LocalDate.parse(String.valueOf(b.get("returnDate"))));
        if (b.get("meetingPoint") != null) s.setMeetingPoint(String.valueOf(b.get("meetingPoint")));
        if (b.get("meetingTime") != null) s.setMeetingTime(String.valueOf(b.get("meetingTime")));
        if (b.get("guideId") != null) s.setGuideId(Long.valueOf(String.valueOf(b.get("guideId"))));
        if (b.get("quota") != null) s.setQuota(Integer.parseInt(String.valueOf(b.get("quota"))));
        if (b.get("minGroup") != null) s.setMinGroup(Integer.parseInt(String.valueOf(b.get("minGroup"))));
        if (b.get("warnDays") != null) s.setWarnDays(Integer.parseInt(String.valueOf(b.get("warnDays"))));
        if (b.get("remark") != null) s.setRemark(String.valueOf(b.get("remark")));
    }
}
