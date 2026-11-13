package net.jxdg.travel.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jxdg.travel.modules.finance.entity.BizSettlement;
import net.jxdg.travel.modules.finance.mapper.BizSettlementMapper;
import net.jxdg.travel.modules.member.entity.BizMember;
import net.jxdg.travel.modules.member.mapper.BizMemberMapper;
import net.jxdg.travel.modules.order.entity.BizOrder;
import net.jxdg.travel.modules.order.mapper.BizOrderMapper;
import net.jxdg.travel.modules.product.entity.BizSchedule;
import net.jxdg.travel.modules.product.mapper.BizScheduleMapper;
import net.jxdg.travel.modules.report.entity.BizReportDaily;
import net.jxdg.travel.modules.report.mapper.BizReportDailyMapper;
import net.jxdg.travel.modules.system.entity.SysTenant;
import net.jxdg.travel.modules.system.mapper.SysTenantMapper;
import net.jxdg.travel.tenant.TenantContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务：租户到期预警、排期状态流转、订单自动完结、经营日报汇总。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTasks {

    private final SysTenantMapper tenantMapper;
    private final BizScheduleMapper scheduleMapper;
    private final BizOrderMapper orderMapper;
    private final BizMemberMapper memberMapper;
    private final BizSettlementMapper settlementMapper;
    private final BizReportDailyMapper reportDailyMapper;

    /** 每日 02:00 租户到期检查（提前 30 天预警，到期自动停用） */
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkTenantExpiry() {
        List<SysTenant> tenants = tenantMapper.selectList(Wrappers.<SysTenant>lambdaQuery());
        for (SysTenant t : tenants) {
            if (t.getExpireTime() == null) continue;
            if (t.getExpireTime().isBefore(LocalDateTime.now()) && t.getStatus() != 3) {
                t.setStatus(4);
                tenantMapper.updateById(t);
                log.warn("租户 {} 已到期，自动停用", t.getCode());
            } else if (t.getExpireTime().isBefore(LocalDateTime.now().plusDays(30))) {
                log.warn("租户 {} 将于 {} 到期", t.getCode(), t.getExpireTime());
            }
        }
    }

    /** 每小时刷新排期状态：已出团/已完结 */
    @Scheduled(cron = "0 10 * * * ?")
    public void refreshScheduleStatus() {
        for (SysTenant t : tenantMapper.selectList(Wrappers.<SysTenant>lambdaQuery().in(SysTenant::getStatus, 1, 2))) {
            TenantContext.set(t.getId(), true, false, t.getCode());
            try {
                LocalDate today = LocalDate.now();
                for (BizSchedule s : scheduleMapper.selectList(Wrappers.<BizSchedule>lambdaQuery()
                        .in(BizSchedule::getStatus, 1, 2, 3))) {
                    if (s.getReturnDate() != null && s.getReturnDate().isBefore(today)) {
                        s.setStatus(5);
                        scheduleMapper.updateById(s);
                    } else if (!s.getDepartDate().isAfter(today)) {
                        s.setStatus(4);
                        scheduleMapper.updateById(s);
                    }
                }
            } catch (Exception e) {
                log.error("refresh schedule status failed tenant={}", t.getCode(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }

    /** 每日 03:00 订单自动完结（已核销 7 天） */
    @Scheduled(cron = "0 0 3 * * ?")
    public void autoFinishOrders() {
        for (SysTenant t : tenantMapper.selectList(Wrappers.<SysTenant>lambdaQuery().in(SysTenant::getStatus, 1, 2))) {
            TenantContext.set(t.getId(), true, false, t.getCode());
            try {
                orderMapper.update(null, Wrappers.<BizOrder>lambdaUpdate()
                        .set(BizOrder::getStatus, 7)
                        .eq(BizOrder::getStatus, 6)
                        .le(BizOrder::getUpdatedAt, LocalDateTime.now().minusDays(7)));
            } catch (Exception e) {
                log.error("auto finish orders failed tenant={}", t.getCode(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }

    /** 每日 01:00 生成经营日报（全国看板数据源） */
    @Scheduled(cron = "0 0 1 * * ?")
    public void aggregateDailyReport() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        for (SysTenant t : tenantMapper.selectList(Wrappers.<SysTenant>lambdaQuery().in(SysTenant::getStatus, 1, 2))) {
            TenantContext.set(t.getId(), true, false, t.getCode());
            try {
                List<BizOrder> orders = orderMapper.selectList(Wrappers.<BizOrder>lambdaQuery()
                        .ge(BizOrder::getCreatedAt, yesterday.atStartOfDay())
                        .lt(BizOrder::getCreatedAt, yesterday.plusDays(1).atStartOfDay()));
                BigDecimal sales = orders.stream().map(o -> o.getPaidAmount() == null ? BigDecimal.ZERO : o.getPaidAmount())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                List<BizSchedule> schedules = scheduleMapper.selectList(Wrappers.<BizSchedule>lambdaQuery()
                        .eq(BizSchedule::getDepartDate, yesterday));
                long grouped = schedules.stream().filter(s -> s.getStatus() != null && s.getStatus() >= 2).count();
                BigDecimal groupRate = schedules.isEmpty() ? BigDecimal.ZERO
                        : BigDecimal.valueOf(grouped * 100.0 / schedules.size()).setScale(2, RoundingMode.HALF_UP);
                BigDecimal settle = settlementMapper.selectList(Wrappers.<BizSettlement>lambdaQuery()
                                .ge(BizSettlement::getCreatedAt, yesterday.atStartOfDay()))
                        .stream().map(BizSettlement::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                BizReportDaily row = reportDailyMapper.selectOne(Wrappers.<BizReportDaily>lambdaQuery()
                        .eq(BizReportDaily::getStatDate, yesterday).last("limit 1"));
                if (row == null) {
                    row = new BizReportDaily();
                    row.setTenantId(t.getId());
                    row.setStatDate(yesterday);
                }
                row.setOrderCount(orders.size());
                row.setSalesAmount(sales);
                row.setSettleAmount(settle);
                row.setMemberCount(memberMapper.selectCount(Wrappers.<BizMember>lambdaQuery()).intValue());
                row.setGroupRate(groupRate);
                if (row.getId() == null) reportDailyMapper.insert(row);
                else reportDailyMapper.updateById(row);
            } catch (Exception e) {
                log.error("daily report failed tenant={}", t.getCode(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }
}
