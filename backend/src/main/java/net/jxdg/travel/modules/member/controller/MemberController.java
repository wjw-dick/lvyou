package net.jxdg.travel.modules.member.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import net.jxdg.travel.common.*;
import net.jxdg.travel.modules.member.entity.*;
import net.jxdg.travel.modules.member.mapper.*;
import net.jxdg.travel.modules.order.entity.BizOrder;
import net.jxdg.travel.modules.order.mapper.BizOrderMapper;
import net.jxdg.travel.modules.system.entity.SysMessageLog;
import net.jxdg.travel.modules.system.mapper.SysMessageLogMapper;
import net.jxdg.travel.security.LoginUser;
import net.jxdg.travel.security.SecurityUtils;
import net.jxdg.travel.tenant.TenantContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final BizMemberMapper memberMapper;
    private final BizMemberPointsLogMapper pointsLogMapper;
    private final BizCouponMapper couponMapper;
    private final BizMemberCouponMapper memberCouponMapper;
    private final BizActivityMapper activityMapper;
    private final BizPointsGoodsMapper pointsGoodsMapper;
    private final BizPointsExchangeMapper pointsExchangeMapper;
    private final BizMemberCardMapper memberCardMapper;
    private final BizWalletLogMapper walletLogMapper;
    private final BizOrderMapper orderMapper;
    private final SysMessageLogMapper messageLogMapper;

    // ---------------- 会员档案 ----------------
    @GetMapping("/members")
    public Result<PageResult<BizMember>> page(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "20") long size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Integer level) {
        Page<BizMember> p = memberMapper.selectPage(new Page<>(page, size), Wrappers.<BizMember>lambdaQuery()
                .and(keyword != null && !keyword.isBlank(), w -> w.like(BizMember::getNickname, keyword)
                        .or().like(BizMember::getPhoneMask, keyword))
                .eq(level != null, BizMember::getLevel, level)
                .orderByDesc(BizMember::getId));
        return Result.ok(PageResult.of(p));
    }

    @GetMapping("/members/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        BizMember m = memberMapper.selectById(id);
        if (m == null) throw BizException.notFound("会员不存在");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("member", m);
        out.put("orders", orderMapper.selectList(Wrappers.<BizOrder>lambdaQuery()
                .eq(BizOrder::getMemberId, id).orderByDesc(BizOrder::getId)));
        out.put("pointsLogs", pointsLogMapper.selectList(Wrappers.<BizMemberPointsLog>lambdaQuery()
                .eq(BizMemberPointsLog::getMemberId, id).orderByDesc(BizMemberPointsLog::getId)));
        out.put("coupons", memberCouponMapper.selectList(Wrappers.<BizMemberCoupon>lambdaQuery()
                .eq(BizMemberCoupon::getMemberId, id)));
        return Result.ok(out);
    }

    @PutMapping("/members/{id}")
    @Audit(module = "会员", action = "调整会员等级/积分")
    public Result<Void> updateMember(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BizMember m = memberMapper.selectById(id);
        if (m == null) throw BizException.notFound("会员不存在");
        if (body.get("level") != null) m.setLevel(Integer.parseInt(String.valueOf(body.get("level"))));
        if (body.get("points") != null) {
            int delta = Integer.parseInt(String.valueOf(body.get("points")));
            m.setPoints((m.getPoints() == null ? 0 : m.getPoints()) + delta);
            BizMemberPointsLog log = new BizMemberPointsLog();
            log.setTenantId(TenantContext.getTenantId());
            log.setMemberId(id);
            log.setPoints(delta);
            log.setType("adjust");
            log.setRemark("后台调整积分");
            log.setCreatedAt(LocalDateTime.now());
            pointsLogMapper.insert(log);
        }
        memberMapper.updateById(m);
        return Result.ok();
    }

    /** 复购分析 */
    @GetMapping("/members/analysis")
    public Result<Map<String, Object>> analysis() {
        List<BizMember> members = memberMapper.selectList(Wrappers.<BizMember>lambdaQuery());
        long active = members.stream().filter(m -> m.getLastActiveAt() != null
                && m.getLastActiveAt().isAfter(LocalDateTime.now().minusDays(30))).count();
        long silent = members.size() - active;
        Map<Integer, Long> levels = new TreeMap<>();
        members.forEach(m -> levels.merge(m.getLevel() == null ? 1 : m.getLevel(), 1L, Long::sum));
        List<Map<String, Object>> top = members.stream()
                .sorted(Comparator.comparing((BizMember m) -> m.getTotalConsume() == null ? BigDecimal.ZERO : m.getTotalConsume()).reversed())
                .limit(10)
                .map(m -> Map.<String, Object>of("id", m.getId(), "nickname", m.getNickname() == null ? "" : m.getNickname(),
                        "totalConsume", m.getTotalConsume() == null ? BigDecimal.ZERO : m.getTotalConsume(),
                        "points", m.getPoints() == null ? 0 : m.getPoints()))
                .toList();
        return Result.ok(Map.of("total", members.size(), "active", active, "silent", silent,
                "levels", levels, "top", top));
    }

    // ---------------- 优惠券/营销 ----------------
    @GetMapping("/coupons")
    public Result<List<BizCoupon>> coupons() {
        return Result.ok(couponMapper.selectList(Wrappers.<BizCoupon>lambdaQuery().orderByDesc(BizCoupon::getId)));
    }

    @PostMapping("/coupons")
    @Audit(module = "会员", action = "保存优惠券")
    public Result<Void> saveCoupon(@RequestBody BizCoupon c) {
        if (c.getTenantId() == null) c.setTenantId(TenantContext.getTenantId());
        if (c.getId() == null) couponMapper.insert(c);
        else couponMapper.updateById(c);
        return Result.ok();
    }

    @PostMapping("/coupons/{id}/issue")
    @Audit(module = "会员", action = "发放优惠券")
    public Result<Map<String, Object>> issueCoupon(@PathVariable Long id,
                                                   @RequestParam(required = false) Long memberId,
                                                   @RequestParam(defaultValue = "false") boolean all) {
        BizCoupon c = couponMapper.selectById(id);
        if (c == null) throw BizException.notFound("优惠券不存在");
        List<BizMember> targets = all ? memberMapper.selectList(Wrappers.<BizMember>lambdaQuery())
                : memberId == null ? List.of() : List.of(memberMapper.selectById(memberId));
        int issued = 0;
        for (BizMember m : targets) {
            if (m == null) continue;
            BizMemberCoupon mc = new BizMemberCoupon();
            mc.setTenantId(TenantContext.getTenantId());
            mc.setCouponId(id);
            mc.setMemberId(m.getId());
            mc.setStatus(0);
            mc.setCreatedAt(LocalDateTime.now());
            memberCouponMapper.insert(mc);
            issued++;
        }
        c.setReceived((c.getReceived() == null ? 0 : c.getReceived()) + issued);
        couponMapper.updateById(c);
        return Result.ok(Map.of("issued", issued));
    }

    @GetMapping("/activities")
    public Result<List<BizActivity>> activities() {
        return Result.ok(activityMapper.selectList(Wrappers.<BizActivity>lambdaQuery().orderByDesc(BizActivity::getId)));
    }

    @PostMapping("/activities")
    @Audit(module = "营销", action = "保存营销活动")
    public Result<Void> saveActivity(@RequestBody BizActivity a) {
        if (a.getTenantId() == null) a.setTenantId(TenantContext.getTenantId());
        if (a.getId() == null) activityMapper.insert(a);
        else activityMapper.updateById(a);
        return Result.ok();
    }

    @GetMapping("/activities/stats")
    public Result<List<Map<String, Object>>> activityStats() {
        return Result.ok(activityMapper.selectList(Wrappers.<BizActivity>lambdaQuery()).stream()
                .map(a -> Map.<String, Object>of("id", a.getId(), "name", a.getName(), "type", a.getType(),
                        "status", a.getStatus(), "startTime", String.valueOf(a.getStartTime()),
                        "endTime", String.valueOf(a.getEndTime()))).toList());
    }

    // ---------------- 积分商城 ----------------
    @GetMapping("/points/goods")
    public Result<List<BizPointsGoods>> goods() {
        return Result.ok(pointsGoodsMapper.selectList(Wrappers.<BizPointsGoods>lambdaQuery().orderByDesc(BizPointsGoods::getId)));
    }

    @PostMapping("/points/goods")
    @Audit(module = "积分商城", action = "保存积分商品")
    public Result<Void> saveGoods(@RequestBody BizPointsGoods g) {
        if (g.getTenantId() == null) g.setTenantId(TenantContext.getTenantId());
        if (g.getId() == null) pointsGoodsMapper.insert(g);
        else pointsGoodsMapper.updateById(g);
        return Result.ok();
    }

    @PostMapping("/member/points/exchange")
    @Transactional
    @Audit(module = "积分商城", action = "积分兑换")
    public Result<BizPointsExchange> exchange(@RequestBody Map<String, Object> body) {
        LoginUser u = requireMember();
        BizPointsGoods g = pointsGoodsMapper.selectById(Long.valueOf(String.valueOf(body.get("goodsId"))));
        if (g == null) throw BizException.notFound("积分商品不存在");
        BizMember m = memberMapper.selectById(u.getId());
        if (m.getPoints() == null || m.getPoints() < g.getPoints()) throw new BizException("积分不足");
        int rows = pointsGoodsMapper.update(null, Wrappers.<BizPointsGoods>lambdaUpdate()
                .setSql("stock = stock - 1").eq(BizPointsGoods::getId, g.getId())
                .apply("stock > 0"));
        if (rows == 0) throw new BizException("商品已兑完");
        m.setPoints(m.getPoints() - g.getPoints());
        memberMapper.updateById(m);
        BizMemberPointsLog log = new BizMemberPointsLog();
        log.setTenantId(TenantContext.getTenantId());
        log.setMemberId(m.getId());
        log.setPoints(-g.getPoints());
        log.setType("exchange");
        log.setRemark("兑换：" + g.getName());
        log.setCreatedAt(LocalDateTime.now());
        pointsLogMapper.insert(log);
        BizPointsExchange e = new BizPointsExchange();
        e.setTenantId(TenantContext.getTenantId());
        e.setGoodsId(g.getId());
        e.setMemberId(m.getId());
        e.setPoints(g.getPoints());
        e.setStatus(0);
        e.setCreatedAt(LocalDateTime.now());
        pointsExchangeMapper.insert(e);
        return Result.ok(e);
    }

    @GetMapping("/points/exchanges")
    public Result<List<Map<String, Object>>> exchanges() {
        return Result.ok(pointsExchangeMapper.selectList(Wrappers.<BizPointsExchange>lambdaQuery()
                .orderByDesc(BizPointsExchange::getId)).stream().map(e -> {
            BizMember m = memberMapper.selectById(e.getMemberId());
            BizPointsGoods g = pointsGoodsMapper.selectById(e.getGoodsId());
            return Map.<String, Object>of("id", e.getId(), "goodName", g == null ? "" : g.getName(),
                    "memberName", m == null ? "" : m.getNickname(), "points", e.getPoints(),
                    "status", e.getStatus(), "createdAt", String.valueOf(e.getCreatedAt()));
        }).toList());
    }

    @PostMapping("/points/exchanges/{id}/verify")
    @Audit(module = "积分商城", action = "兑换核销")
    public Result<Void> verifyExchange(@PathVariable Long id) {
        BizPointsExchange e = pointsExchangeMapper.selectById(id);
        if (e == null) throw BizException.notFound("兑换记录不存在");
        e.setStatus(1);
        pointsExchangeMapper.updateById(e);
        return Result.ok();
    }

    // ---------------- 会员卡/储值 ----------------
    @GetMapping("/member/cards")
    public Result<List<BizMemberCard>> cards(@RequestParam(required = false) Long memberId) {
        return Result.ok(memberCardMapper.selectList(Wrappers.<BizMemberCard>lambdaQuery()
                .eq(memberId != null, BizMemberCard::getMemberId, memberId)));
    }

    @PostMapping("/member/cards")
    @Audit(module = "会员卡", action = "开卡")
    public Result<BizMemberCard> openCard(@RequestBody Map<String, Object> body) {
        BizMemberCard c = new BizMemberCard();
        c.setTenantId(TenantContext.getTenantId());
        c.setMemberId(Long.valueOf(String.valueOf(body.get("memberId"))));
        c.setCardNo("CARD" + System.currentTimeMillis());
        c.setType(String.valueOf(body.getOrDefault("type", "stored")));
        c.setBalance(new BigDecimal(String.valueOf(body.getOrDefault("balance", "0"))));
        c.setStatus(1);
        if (body.get("validEnd") != null) c.setValidEnd(java.time.LocalDate.parse(String.valueOf(body.get("validEnd"))));
        c.setCreatedAt(LocalDateTime.now());
        memberCardMapper.insert(c);
        return Result.ok(c);
    }

    @PostMapping("/member/cards/{id}/recharge")
    @Transactional
    @Audit(module = "会员卡", action = "储值充值")
    public Result<Void> recharge(@PathVariable Long id, @RequestParam BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) throw new BizException("充值金额必须大于 0");
        BizMemberCard c = memberCardMapper.selectById(id);
        if (c == null) throw BizException.notFound("会员卡不存在");
        c.setBalance((c.getBalance() == null ? BigDecimal.ZERO : c.getBalance()).add(amount));
        memberCardMapper.updateById(c);
        BizWalletLog log = new BizWalletLog();
        log.setTenantId(TenantContext.getTenantId());
        log.setMemberId(c.getMemberId());
        log.setAmount(amount);
        log.setType("recharge");
        log.setRemark("会员卡充值");
        log.setCreatedAt(LocalDateTime.now());
        walletLogMapper.insert(log);
        return Result.ok();
    }

    @GetMapping("/member/wallet/logs")
    public Result<List<BizWalletLog>> walletLogs(@RequestParam(required = false) Long memberId) {
        return Result.ok(walletLogMapper.selectList(Wrappers.<BizWalletLog>lambdaQuery()
                .eq(memberId != null, BizWalletLog::getMemberId, memberId)
                .orderByDesc(BizWalletLog::getId)));
    }

    // ---------------- 会员自助端 ----------------
    @GetMapping("/member/profile")
    public Result<Map<String, Object>> profile() {
        LoginUser u = requireMember();
        BizMember m = memberMapper.selectById(u.getId());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("member", m);
        out.put("coupons", memberCouponMapper.selectList(Wrappers.<BizMemberCoupon>lambdaQuery()
                .eq(BizMemberCoupon::getMemberId, u.getId())));
        out.put("cards", memberCardMapper.selectList(Wrappers.<BizMemberCard>lambdaQuery()
                .eq(BizMemberCard::getMemberId, u.getId())));
        return Result.ok(out);
    }

    @GetMapping("/member/coupons")
    public Result<List<Map<String, Object>>> myCoupons() {
        LoginUser u = requireMember();
        List<BizMemberCoupon> list = memberCouponMapper.selectList(Wrappers.<BizMemberCoupon>lambdaQuery()
                .eq(BizMemberCoupon::getMemberId, u.getId()));
        return Result.ok(list.stream().map(mc -> {
            BizCoupon c = couponMapper.selectById(mc.getCouponId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", mc.getId());
            m.put("status", mc.getStatus());
            m.put("name", c == null ? "" : c.getName());
            m.put("amount", c == null ? BigDecimal.ZERO : c.getAmount());
            m.put("minAmount", c == null ? BigDecimal.ZERO : c.getMinAmount());
            m.put("endTime", c == null ? null : c.getEndTime());
            return m;
        }).toList());
    }

    @GetMapping("/member/points/logs")
    public Result<List<BizMemberPointsLog>> myPoints() {
        LoginUser u = requireMember();
        return Result.ok(pointsLogMapper.selectList(Wrappers.<BizMemberPointsLog>lambdaQuery()
                .eq(BizMemberPointsLog::getMemberId, u.getId()).orderByDesc(BizMemberPointsLog::getId)));
    }

    @GetMapping("/member/messages")
    public Result<List<SysMessageLog>> myMessages() {
        LoginUser u = requireMember();
        return Result.ok(messageLogMapper.selectList(Wrappers.<SysMessageLog>lambdaQuery()
                .eq(SysMessageLog::getMemberId, u.getId()).orderByDesc(SysMessageLog::getId)));
    }

    private LoginUser requireMember() {
        LoginUser u = SecurityUtils.current();
        if (!"MEMBER".equals(u.getUserType())) throw BizException.forbidden("仅游客账号可访问");
        return u;
    }
}
