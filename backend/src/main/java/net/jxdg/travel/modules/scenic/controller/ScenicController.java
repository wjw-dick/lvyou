package net.jxdg.travel.modules.scenic.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import net.jxdg.travel.common.*;
import net.jxdg.travel.modules.guide.entity.BizGuideAudio;
import net.jxdg.travel.modules.guide.mapper.BizGuideAudioMapper;
import net.jxdg.travel.modules.order.entity.BizVerifyRecord;
import net.jxdg.travel.modules.order.mapper.BizVerifyRecordMapper;
import net.jxdg.travel.modules.scenic.entity.*;
import net.jxdg.travel.modules.scenic.mapper.*;
import net.jxdg.travel.security.LoginUser;
import net.jxdg.travel.security.SecurityUtils;
import net.jxdg.travel.tenant.TenantContext;
import net.jxdg.travel.wechat.WechatService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class ScenicController {

    private final BizScenicMapper scenicMapper;
    private final BizTicketMapper ticketMapper;
    private final BizTicketStockMapper stockMapper;
    private final BizTicketOrderMapper ticketOrderMapper;
    private final BizVerifyRecordMapper verifyRecordMapper;
    private final BizTrafficStatMapper trafficStatMapper;
    private final BizScenicStaffMapper staffMapper;
    private final BizScenicReservationMapper reservationMapper;
    private final BizScenicAreaMapper areaMapper;
    private final BizGuideAudioMapper audioMapper;
    private final BizScenicMerchantMapper merchantMapper;
    private final BizParkingRecordMapper parkingMapper;
    private final WechatService wechatService;
    private final PasswordEncoder passwordEncoder;
    private final NumberGenerator numberGenerator;

    // ---------------- 景区档案 ----------------
    @GetMapping("/scenic/list")
    public Result<PageResult<BizScenic>> scenicList(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "20") long size,
                                                    @RequestParam(required = false) String keyword) {
        return Result.ok(PageResult.of(scenicMapper.selectPage(new Page<>(page, size),
                Wrappers.<BizScenic>lambdaQuery().like(keyword != null && !keyword.isBlank(), BizScenic::getName, keyword)
                        .orderByDesc(BizScenic::getId))));
    }

    @GetMapping("/scenic/all")
    public Result<List<BizScenic>> scenicAll() {
        return Result.ok(scenicMapper.selectList(Wrappers.<BizScenic>lambdaQuery().eq(BizScenic::getStatus, 1)));
    }

    @GetMapping("/scenic/{id}")
    public Result<BizScenic> scenicDetail(@PathVariable Long id) {
        BizScenic s = scenicMapper.selectById(id);
        if (s == null) throw BizException.notFound("景区不存在");
        return Result.ok(s);
    }

    @PostMapping("/scenic")
    @Audit(module = "景区", action = "保存景区档案")
    public Result<Void> saveScenic(@RequestBody BizScenic s) {
        if (s.getTenantId() == null) s.setTenantId(TenantContext.getTenantId());
        if (s.getId() == null) scenicMapper.insert(s);
        else scenicMapper.updateById(s);
        return Result.ok();
    }

    // ---------------- 门票产品 ----------------
    @GetMapping("/scenic/tickets")
    public Result<List<Map<String, Object>>> tickets(@RequestParam(required = false) Long scenicId) {
        List<BizTicket> list = ticketMapper.selectList(Wrappers.<BizTicket>lambdaQuery()
                .eq(scenicId != null, BizTicket::getScenicId, scenicId).orderByDesc(BizTicket::getId));
        return Result.ok(list.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ticket", t);
            BizScenic s = scenicMapper.selectById(t.getScenicId());
            m.put("scenicName", s == null ? "" : s.getName());
            return m;
        }).toList());
    }

    @PostMapping("/scenic/tickets")
    @Audit(module = "景区", action = "保存门票产品")
    public Result<Void> saveTicket(@RequestBody BizTicket t) {
        if (t.getTenantId() == null) t.setTenantId(TenantContext.getTenantId());
        if (t.getId() == null) ticketMapper.insert(t);
        else ticketMapper.updateById(t);
        return Result.ok();
    }

    @GetMapping("/scenic/tickets/{id}")
    public Result<BizTicket> ticket(@PathVariable Long id) {
        BizTicket t = ticketMapper.selectById(id);
        if (t == null) throw BizException.notFound("门票不存在");
        return Result.ok(t);
    }

    // ---------------- 库存/限流 ----------------
    @GetMapping("/scenic/stock")
    public Result<List<Map<String, Object>>> stock(@RequestParam Long ticketId,
                                                    @RequestParam(required = false) String startDate,
                                                    @RequestParam(required = false) String endDate) {
        LocalDate start = startDate == null ? LocalDate.now() : LocalDate.parse(startDate);
        LocalDate end = endDate == null ? start.plusDays(6) : LocalDate.parse(endDate);
        return Result.ok(stockMapper.selectList(Wrappers.<BizTicketStock>lambdaQuery()
                        .eq(BizTicketStock::getTicketId, ticketId)
                        .between(BizTicketStock::getStockDate, start, end)
                        .orderByAsc(BizTicketStock::getStockDate)).stream()
                .map(s -> Map.<String, Object>of("id", s.getId(), "date", s.getStockDate(),
                        "timeSlot", s.getTimeSlot(), "stock", s.getStock(), "sold", s.getSold(),
                        "reserved", s.getReserved(), "left", Math.max(0, s.getStock() - s.getSold() - s.getReserved())))
                .toList());
    }

    @PostMapping("/scenic/stock")
    @Audit(module = "景区", action = "配置分时库存")
    public Result<Void> saveStock(@RequestBody BizTicketStock s) {
        BizTicketStock exist = stockMapper.selectOne(Wrappers.<BizTicketStock>lambdaQuery()
                .eq(BizTicketStock::getTicketId, s.getTicketId())
                .eq(BizTicketStock::getStockDate, s.getStockDate())
                .eq(BizTicketStock::getTimeSlot, s.getTimeSlot()).last("limit 1"));
        if (exist == null) {
            s.setTenantId(TenantContext.getTenantId());
            if (s.getSold() == null) s.setSold(0);
            if (s.getReserved() == null) s.setReserved(0);
            stockMapper.insert(s);
        } else {
            exist.setStock(s.getStock());
            exist.setReserved(s.getReserved());
            stockMapper.updateById(exist);
        }
        return Result.ok();
    }

    // ---------------- 门票下单（公开） ----------------
    @PostMapping("/public/tickets/order")
    @Transactional
    public Result<Map<String, Object>> createTicketOrder(@RequestBody Map<String, Object> body) {
        if (TenantContext.getTenantId() == null) throw new BizException("缺少租户标识");
        BizTicket ticket = ticketMapper.selectById(Long.valueOf(String.valueOf(body.get("ticketId"))));
        if (ticket == null || ticket.getStatus() != 1) throw BizException.notFound("门票不存在或已停售");
        LocalDate visitDate = LocalDate.parse(String.valueOf(body.get("visitDate")));
        String slot = String.valueOf(body.getOrDefault("timeSlot", "all"));
        int qty = Integer.parseInt(String.valueOf(body.getOrDefault("quantity", "1")));
        if (qty <= 0) throw new BizException("购票数量必须大于 0");
        // 分时库存原子扣减 + 超售校验
        BizTicketStock stock = stockMapper.selectOne(Wrappers.<BizTicketStock>lambdaQuery()
                .eq(BizTicketStock::getTicketId, ticket.getId())
                .eq(BizTicketStock::getStockDate, visitDate)
                .eq(BizTicketStock::getTimeSlot, slot).last("limit 1"));
        if (stock != null) {
            int rows = stockMapper.update(null, Wrappers.<BizTicketStock>lambdaUpdate()
                    .setSql("sold = sold + " + qty)
                    .eq(BizTicketStock::getId, stock.getId())
                    .apply("sold + reserved + {0} <= stock", qty));
            if (rows == 0) throw new BizException(409, "该时段门票已售罄，请选择其他时段");
        } else if (ticket.getDailyLimit() != null && ticket.getDailyLimit() > 0) {
            long sold = ticketOrderMapper.selectCount(Wrappers.<BizTicketOrder>lambdaQuery()
                    .eq(BizTicketOrder::getTicketId, ticket.getId())
                    .eq(BizTicketOrder::getVisitDate, visitDate)
                    .ne(BizTicketOrder::getStatus, 3));
            if (sold + qty > ticket.getDailyLimit()) throw new BizException(409, "当日限流已达上限");
        }
        BigDecimal amount = ticket.getPrice().multiply(BigDecimal.valueOf(qty));
        BizTicketOrder o = new BizTicketOrder();
        o.setTenantId(TenantContext.getTenantId());
        o.setTicketNo(numberGenerator.next("TK"));
        o.setTicketId(ticket.getId());
        o.setMemberId(body.get("memberId") == null ? null : Long.valueOf(String.valueOf(body.get("memberId"))));
        o.setVisitorName(str(body.get("visitorName")));
        o.setVisitDate(visitDate);
        o.setTimeSlot(slot);
        o.setQuantity(qty);
        o.setAmount(amount);
        o.setStatus(1);
        o.setVerifyCount(0);
        o.setQrCode(UUID.randomUUID().toString().replace("-", ""));
        o.setCreatedAt(LocalDateTime.now());
        ticketOrderMapper.insert(o);
        if (!"all".equals(slot)) {
            BizScenicReservation r = new BizScenicReservation();
            r.setTenantId(TenantContext.getTenantId());
            r.setTicketOrderId(o.getId());
            r.setTicketId(ticket.getId());
            r.setVisitDate(visitDate);
            r.setTimeSlot(slot);
            r.setQuantity(qty);
            r.setStatus(0);
            r.setCreatedAt(LocalDateTime.now());
            reservationMapper.insert(r);
        }
        return Result.ok(Map.of("ticketNo", o.getTicketNo(), "qrCode", o.getQrCode(),
                "amount", amount, "visitDate", visitDate.toString(), "timeSlot", slot));
    }

    @GetMapping("/scenic/ticket-orders")
    public Result<PageResult<BizTicketOrder>> ticketOrders(@RequestParam(defaultValue = "1") long page,
                                                           @RequestParam(defaultValue = "20") long size,
                                                           @RequestParam(required = false) Integer status,
                                                           @RequestParam(required = false) String date) {
        return Result.ok(PageResult.of(ticketOrderMapper.selectPage(new Page<>(page, size),
                Wrappers.<BizTicketOrder>lambdaQuery()
                        .eq(status != null, BizTicketOrder::getStatus, status)
                        .eq(date != null && !date.isBlank(), BizTicketOrder::getVisitDate, date)
                        .orderByDesc(BizTicketOrder::getId))));
    }

    // ---------------- 扫码验票 ----------------
    @PostMapping("/scenic/verify")
    @Transactional
    @Audit(module = "景区", action = "扫码验票")
    public Result<Map<String, Object>> verify(@RequestBody Map<String, Object> body) {
        String code = str(body.get("code"));
        String gate = str(body.getOrDefault("gate", "1号闸口"));
        String type = str(body.getOrDefault("verifyType", "qrcode"));
        if (code == null) throw new BizException("缺少门票二维码");
        // 限流防撞码
        String key = "rate:scenic-verify:" + TenantContext.getTenantId();
        Long count = verifyRecordMapper.selectCount(Wrappers.<BizVerifyRecord>lambdaQuery()
                .ge(BizVerifyRecord::getVerifyAt, LocalDateTime.now().minusMinutes(1)));
        if (count > 300) throw new BizException(429, "验票请求过于频繁，已限流");
        BizTicketOrder order = ticketOrderMapper.selectOne(Wrappers.<BizTicketOrder>lambdaQuery()
                .eq(BizTicketOrder::getQrCode, code).last("limit 1"));
        if (order == null) throw BizException.notFound("门票无效或二维码不存在");
        BizTicket ticket = ticketMapper.selectById(order.getTicketId());
        if (ticket == null) throw new BizException("门票产品不存在");
        if (order.getStatus() == 3) throw new BizException("门票已退款");
        if (order.getVisitDate() != null && order.getVisitDate().isBefore(LocalDate.now().minusDays(
                ticket.getValidDays() == null ? 0 : ticket.getValidDays() - 1)))
            throw new BizException("门票已过期");
        if (order.getVisitDate().isAfter(LocalDate.now())) throw new BizException("未到游玩日期");
        int used = order.getVerifyCount() == null ? 0 : order.getVerifyCount();
        if (used >= (ticket.getValidDays() == null ? 1 : ticket.getValidDays())) {
            throw new BizException("该门票已使用完毕，请勿重复核销");
        }
        String recordCode = used == 0 ? code : code + "#" + (used + 1);
        BizVerifyRecord record = new BizVerifyRecord();
        record.setTenantId(TenantContext.getTenantId());
        record.setTicketOrderId(order.getId());
        record.setCode(recordCode);
        record.setVerifyType(type);
        LoginUser u = SecurityUtils.currentOrNull();
        record.setOperatorId(u == null ? null : u.getId());
        record.setOperatorName(u == null ? "闸机" : u.getRealName());
        record.setGate(gate);
        record.setResult("success");
        record.setVerifyAt(LocalDateTime.now());
        verifyRecordMapper.insert(record);

        order.setVerifyCount(used + 1);
        order.setGate(gate);
        if (order.getVerifyCount() >= (ticket.getValidDays() == null ? 1 : ticket.getValidDays())) {
            order.setStatus(2);
        }
        ticketOrderMapper.updateById(order);

        BizTrafficStat stat = new BizTrafficStat();
        stat.setTenantId(TenantContext.getTenantId());
        stat.setScenicId(ticket.getScenicId());
        stat.setStatDate(LocalDate.now());
        stat.setTimeSlot(String.valueOf(LocalDateTime.now().getHour()));
        stat.setArea(gate);
        stat.setInCount(order.getQuantity());
        stat.setOutCount(0);
        stat.setCreatedAt(LocalDateTime.now());
        trafficStatMapper.insert(stat);

        reservationMapper.update(null, Wrappers.<BizScenicReservation>lambdaUpdate()
                .set(BizScenicReservation::getStatus, 1)
                .eq(BizScenicReservation::getTicketOrderId, order.getId()));

        return Result.ok(Map.of("ticketNo", order.getTicketNo(), "visitor", order.getVisitorName() == null ? "" : order.getVisitorName(),
                "gate", gate, "verifyCount", order.getVerifyCount(), "multiDay",
                (ticket.getValidDays() != null && ticket.getValidDays() > 1),
                "verifiedAt", record.getVerifyAt()));
    }

    @GetMapping("/scenic/verify-records")
    public Result<PageResult<BizVerifyRecord>> verifyRecords(@RequestParam(defaultValue = "1") long page,
                                                             @RequestParam(defaultValue = "20") long size,
                                                             @RequestParam(required = false) String date) {
        return Result.ok(PageResult.of(verifyRecordMapper.selectPage(new Page<>(page, size),
                Wrappers.<BizVerifyRecord>lambdaQuery()
                        .isNotNull(BizVerifyRecord::getTicketOrderId)
                        .ge(date != null && !date.isBlank(), BizVerifyRecord::getVerifyAt, date + " 00:00:00")
                        .le(date != null && !date.isBlank(), BizVerifyRecord::getVerifyAt, date + " 23:59:59")
                        .orderByDesc(BizVerifyRecord::getId))));
    }

    // ---------------- 客流看板 ----------------
    @GetMapping("/scenic/traffic")
    public Result<Map<String, Object>> traffic(@RequestParam(required = false) Long scenicId,
                                                @RequestParam(required = false) String date) {
        LocalDate d = date == null ? LocalDate.now() : LocalDate.parse(date);
        var stats = trafficStatMapper.selectList(Wrappers.<BizTrafficStat>lambdaQuery()
                .eq(BizTrafficStat::getStatDate, d)
                .eq(scenicId != null, BizTrafficStat::getScenicId, scenicId));
        int total = stats.stream().mapToInt(s -> s.getInCount() == null ? 0 : s.getInCount()).sum();
        Map<String, Integer> byHour = new TreeMap<>();
        Map<String, Integer> byArea = new LinkedHashMap<>();
        stats.forEach(s -> {
            byHour.merge(s.getTimeSlot() == null ? "0" : s.getTimeSlot(), s.getInCount() == null ? 0 : s.getInCount(), Integer::sum);
            byArea.merge(s.getArea() == null ? "默认" : s.getArea(), s.getInCount() == null ? 0 : s.getInCount(), Integer::sum);
        });
        List<Map<String, Object>> areas = areaMapper.selectList(Wrappers.<BizScenicArea>lambdaQuery()
                .eq(scenicId != null, BizScenicArea::getScenicId, scenicId)).stream()
                .map(a -> Map.<String, Object>of("name", a.getName(), "capacity", a.getCapacity(),
                        "inCount", a.getInCount(), "congested",
                        a.getCapacity() != null && a.getCapacity() > 0 && a.getInCount() > a.getCapacity() * 0.8))
                .toList();
        long congestionAlerts = areas.stream().filter(a -> Boolean.TRUE.equals(a.get("congested"))).count();
        return Result.ok(Map.of("date", d.toString(), "inCount", total, "byHour", byHour, "byArea", byArea,
                "areas", areas, "congestionAlerts", congestionAlerts));
    }

    // ---------------- 员工账号 ----------------
    @GetMapping("/scenic/staff")
    public Result<List<BizScenicStaff>> staff(@RequestParam(required = false) Long scenicId) {
        List<BizScenicStaff> list = staffMapper.selectList(Wrappers.<BizScenicStaff>lambdaQuery()
                .eq(scenicId != null, BizScenicStaff::getScenicId, scenicId));
        list.forEach(s -> s.setPassword(null));
        return Result.ok(list);
    }

    @PostMapping("/scenic/staff")
    @Audit(module = "景区", action = "保存员工账号")
    public Result<Void> saveStaff(@RequestBody Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        BizScenicStaff s = id == null ? new BizScenicStaff() : staffMapper.selectById(id);
        if (s == null) throw BizException.notFound("员工不存在");
        s.setScenicId(Long.valueOf(String.valueOf(body.get("scenicId"))));
        s.setName(str(body.get("name")));
        s.setUsername(str(body.get("username")));
        s.setGate(str(body.get("gate")));
        s.setPhone(str(body.get("phone")));
        s.setStatus(1);
        if (id == null) {
            s.setTenantId(TenantContext.getTenantId());
            s.setPassword(passwordEncoder.encode(str(body.getOrDefault("password", "admin123"))));
            staffMapper.insert(s);
        } else {
            if (body.get("password") != null && !String.valueOf(body.get("password")).isBlank())
                s.setPassword(passwordEncoder.encode(String.valueOf(body.get("password"))));
            staffMapper.updateById(s);
        }
        return Result.ok();
    }

    /** 景区员工移动端 */
    @GetMapping("/scenic/app/me")
    public Result<Map<String, Object>> staffMe() {
        BizScenicStaff s = currentStaff();
        BizScenic scenic = scenicMapper.selectById(s.getScenicId());
        return Result.ok(Map.of("staffId", s.getId(), "name", s.getName(), "gate", s.getGate() == null ? "" : s.getGate(),
                "scenicName", scenic == null ? "" : scenic.getName(), "scenicId", s.getScenicId()));
    }

    @GetMapping("/scenic/app/traffic")
    public Result<Map<String, Object>> staffTraffic() {
        BizScenicStaff s = currentStaff();
        return Result.ok(traffic(s.getScenicId(), null).getData());
    }

    @GetMapping("/scenic/app/records")
    public Result<List<BizVerifyRecord>> staffRecords() {
        BizScenicStaff s = currentStaff();
        List<Long> orderIds = ticketOrderMapper.selectList(Wrappers.<BizTicketOrder>lambdaQuery()
                        .eq(BizTicketOrder::getStatus, 2)).stream().map(BizTicketOrder::getId).toList();
        if (orderIds.isEmpty()) return Result.ok(List.of());
        return Result.ok(verifyRecordMapper.selectList(Wrappers.<BizVerifyRecord>lambdaQuery()
                .in(BizVerifyRecord::getTicketOrderId, orderIds)
                .orderByDesc(BizVerifyRecord::getId).last("limit 100")));
    }

    @GetMapping("/scenic/app/notices")
    public Result<List<String>> staffNotices() {
        currentStaff();
        return Result.ok(List.of("今日客流较大，请提前开启备用闸口。", "请核对团队票入园人数。",
                "对账提醒：昨日门票收入已生成结算单，请查看。"));
    }

    // ---------------- 预约/区域/导览/二销/停车 ----------------
    @GetMapping("/scenic/reservations")
    public Result<List<BizScenicReservation>> reservations(@RequestParam(required = false) String date) {
        return Result.ok(reservationMapper.selectList(Wrappers.<BizScenicReservation>lambdaQuery()
                .eq(date != null && !date.isBlank(), BizScenicReservation::getVisitDate, date)
                .orderByDesc(BizScenicReservation::getId)));
    }

    @PostMapping("/scenic/reservations/absent")
    @Audit(module = "景区", action = "爽约处理")
    public Result<Map<String, Object>> markAbsent(@RequestParam String date) {
        int rows = reservationMapper.update(null, Wrappers.<BizScenicReservation>lambdaUpdate()
                .set(BizScenicReservation::getStatus, 2)
                .eq(BizScenicReservation::getVisitDate, date)
                .eq(BizScenicReservation::getStatus, 0)
                .lt(BizScenicReservation::getVisitDate, LocalDate.now()));
        return Result.ok(Map.of("absent", rows));
    }

    @GetMapping("/scenic/areas")
    public Result<List<BizScenicArea>> areas(@RequestParam Long scenicId) {
        return Result.ok(areaMapper.selectList(Wrappers.<BizScenicArea>lambdaQuery()
                .eq(BizScenicArea::getScenicId, scenicId)));
    }

    @PostMapping("/scenic/areas")
    @Audit(module = "景区", action = "保存区域")
    public Result<Void> saveArea(@RequestBody BizScenicArea a) {
        if (a.getTenantId() == null) a.setTenantId(TenantContext.getTenantId());
        if (a.getId() == null) areaMapper.insert(a);
        else areaMapper.updateById(a);
        return Result.ok();
    }

    @GetMapping("/public/scenic/{id}/guide")
    public Result<List<BizGuideAudio>> guide(@PathVariable Long id) {
        return Result.ok(audioMapper.selectList(Wrappers.<BizGuideAudio>lambdaQuery()
                .eq(BizGuideAudio::getScenicId, id)));
    }

    @PostMapping("/scenic/guide-audio")
    @Audit(module = "景区", action = "保存导览点")
    public Result<Void> saveAudio(@RequestBody BizGuideAudio a) {
        if (a.getTenantId() == null) a.setTenantId(TenantContext.getTenantId());
        if (a.getId() == null) audioMapper.insert(a);
        else audioMapper.updateById(a);
        return Result.ok();
    }

    @GetMapping("/scenic/merchants")
    public Result<List<BizScenicMerchant>> merchants(@RequestParam Long scenicId) {
        return Result.ok(merchantMapper.selectList(Wrappers.<BizScenicMerchant>lambdaQuery()
                .eq(BizScenicMerchant::getScenicId, scenicId)));
    }

    @PostMapping("/scenic/merchants")
    @Audit(module = "景区", action = "保存二销商户")
    public Result<Void> saveMerchant(@RequestBody BizScenicMerchant m) {
        if (m.getTenantId() == null) m.setTenantId(TenantContext.getTenantId());
        if (m.getId() == null) merchantMapper.insert(m);
        else merchantMapper.updateById(m);
        return Result.ok();
    }

    @GetMapping("/scenic/parking")
    public Result<List<BizParkingRecord>> parking(@RequestParam(required = false) Integer status) {
        return Result.ok(parkingMapper.selectList(Wrappers.<BizParkingRecord>lambdaQuery()
                .eq(status != null, BizParkingRecord::getStatus, status)
                .orderByDesc(BizParkingRecord::getId)));
    }

    @PostMapping("/scenic/parking/entry")
    @Audit(module = "景区", action = "车辆入场")
    public Result<BizParkingRecord> parkingEntry(@RequestBody Map<String, Object> body) {
        BizParkingRecord r = new BizParkingRecord();
        r.setTenantId(TenantContext.getTenantId());
        r.setScenicId(Long.valueOf(String.valueOf(body.get("scenicId"))));
        r.setPlateNo(String.valueOf(body.get("plateNo")));
        r.setEntryTime(LocalDateTime.now());
        r.setFee(BigDecimal.ZERO);
        r.setStatus(0);
        parkingMapper.insert(r);
        return Result.ok(r);
    }

    @PostMapping("/scenic/parking/{id}/exit")
    @Audit(module = "景区", action = "车辆出场核销")
    public Result<BizParkingRecord> parkingExit(@PathVariable Long id, @RequestParam(defaultValue = "5") BigDecimal hourly) {
        BizParkingRecord r = parkingMapper.selectById(id);
        if (r == null) throw BizException.notFound("停车记录不存在");
        r.setExitTime(LocalDateTime.now());
        long hours = Math.max(1, java.time.Duration.between(r.getEntryTime(), r.getExitTime()).toHours());
        r.setFee(hourly.multiply(BigDecimal.valueOf(hours)));
        r.setStatus(1);
        parkingMapper.updateById(r);
        return Result.ok(r);
    }

    /** 门票收入/对账报表 */
    @GetMapping("/scenic/revenue")
    public Result<Map<String, Object>> revenue(@RequestParam(required = false) String startDate,
                                               @RequestParam(required = false) String endDate) {
        LocalDate start = startDate == null ? LocalDate.now().minusDays(30) : LocalDate.parse(startDate);
        LocalDate end = endDate == null ? LocalDate.now() : LocalDate.parse(endDate);
        List<BizTicketOrder> orders = ticketOrderMapper.selectList(Wrappers.<BizTicketOrder>lambdaQuery()
                .between(BizTicketOrder::getVisitDate, start, end).ne(BizTicketOrder::getStatus, 3));
        BigDecimal total = orders.stream().map(BizTicketOrder::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, BigDecimal> byDate = new TreeMap<>();
        orders.forEach(o -> byDate.merge(o.getVisitDate().toString(), o.getAmount(), BigDecimal::add));
        long refunds = ticketOrderMapper.selectCount(Wrappers.<BizTicketOrder>lambdaQuery()
                .between(BizTicketOrder::getVisitDate, start, end).eq(BizTicketOrder::getStatus, 3));
        return Result.ok(Map.of("total", total, "orderCount", orders.size(), "refundCount", refunds,
                "byDate", byDate, "startDate", start.toString(), "endDate", end.toString()));
    }

    private BizScenicStaff currentStaff() {
        LoginUser u = SecurityUtils.current();
        if (!"SCENIC".equals(u.getUserType())) throw BizException.forbidden("仅景区员工可访问");
        BizScenicStaff s = staffMapper.selectById(u.getId());
        if (s == null) throw BizException.notFound("员工账号不存在");
        return s;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
