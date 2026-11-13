package net.jxdg.travel.modules.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import net.jxdg.travel.common.*;
import net.jxdg.travel.modules.distribution.entity.BizDistributor;
import net.jxdg.travel.modules.distribution.entity.BizDistributorPrice;
import net.jxdg.travel.modules.distribution.mapper.BizDistributorMapper;
import net.jxdg.travel.modules.distribution.mapper.BizDistributorPriceMapper;
import net.jxdg.travel.modules.member.entity.BizMember;
import net.jxdg.travel.modules.member.mapper.BizMemberMapper;
import net.jxdg.travel.modules.order.entity.*;
import net.jxdg.travel.modules.order.mapper.*;
import net.jxdg.travel.modules.product.entity.BizProduct;
import net.jxdg.travel.modules.product.entity.BizProductPrice;
import net.jxdg.travel.modules.product.entity.BizSchedule;
import net.jxdg.travel.modules.product.mapper.BizProductMapper;
import net.jxdg.travel.modules.product.mapper.BizProductPriceMapper;
import net.jxdg.travel.modules.product.mapper.BizScheduleMapper;
import net.jxdg.travel.security.LoginUser;
import net.jxdg.travel.tenant.TenantContext;
import net.jxdg.travel.wechat.WechatService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final BizOrderMapper orderMapper;
    private final BizOrderTouristMapper touristMapper;
    private final BizOrderLogMapper orderLogMapper;
    private final BizPaymentMapper paymentMapper;
    private final BizVerifyRecordMapper verifyRecordMapper;
    private final BizRefundMapper refundMapper;
    private final BizProductMapper productMapper;
    private final BizProductPriceMapper productPriceMapper;
    private final BizScheduleMapper scheduleMapper;
    private final BizMemberMapper memberMapper;
    private final BizDistributorMapper distributorMapper;
    private final BizDistributorPriceMapper distributorPriceMapper;
    private final NumberGenerator numberGenerator;
    private final CryptoUtil cryptoUtil;
    private final WechatService wechatService;
    private final PdfUtil pdfUtil;

    @Transactional
    public BizOrder create(LoginUser user, Map<String, Object> body) {
        Long productId = Long.valueOf(String.valueOf(body.get("productId")));
        BizProduct product = productMapper.selectById(productId);
        if (product == null) throw BizException.notFound("产品不存在");
        if (product.getStatus() != 1) throw new BizException("产品已下架");
        Long scheduleId = body.get("scheduleId") == null ? null : Long.valueOf(String.valueOf(body.get("scheduleId")));
        BizSchedule schedule = scheduleId == null ? null : scheduleMapper.selectById(scheduleId);
        if (schedule != null && schedule.getStatus() >= 4) throw new BizException("该排期已出团/完结，不可预订");

        int adult = body.get("adultCount") == null ? 1 : Integer.parseInt(String.valueOf(body.get("adultCount")));
        int child = body.get("childCount") == null ? 0 : Integer.parseInt(String.valueOf(body.get("childCount")));
        int people = adult + child;
        if (people <= 0) throw new BizException("出行人数必须大于 0");

        BigDecimal adultPrice = product.getAdultPrice();
        BigDecimal childPrice = product.getChildPrice();
        String channel = body.get("channel") == null ? "miniprogram" : String.valueOf(body.get("channel"));
        Long distributorId = body.get("distributorId") == null ? null : Long.valueOf(String.valueOf(body.get("distributorId")));
        if (distributorId != null) {
            BizDistributorPrice dp = distributorPriceMapper.selectOne(Wrappers.<BizDistributorPrice>lambdaQuery()
                    .eq(BizDistributorPrice::getDistributorId, distributorId)
                    .eq(BizDistributorPrice::getProductId, productId).last("limit 1"));
            if (dp != null) {
                adultPrice = dp.getAdultPrice();
                childPrice = dp.getChildPrice();
            }
        } else {
            BizProductPrice pp = productPriceMapper.selectOne(Wrappers.<BizProductPrice>lambdaQuery()
                    .eq(BizProductPrice::getProductId, productId).eq(BizProductPrice::getChannel, channel)
                    .eq(BizProductPrice::getStatus, 1).last("limit 1"));
            if (pp != null) {
                adultPrice = pp.getAdultPrice();
                childPrice = pp.getChildPrice();
            }
        }
        BigDecimal total = adultPrice.multiply(BigDecimal.valueOf(adult))
                .add(childPrice.multiply(BigDecimal.valueOf(child)))
                .setScale(2, RoundingMode.HALF_UP);

        // 防重提交
        String lockKey = "order:" + (user == null ? "guest" : user.getId()) + ":" + productId + ":" + scheduleId;
        if (!numberGenerator.tryLock(lockKey, java.time.Duration.ofSeconds(5))) {
            throw new BizException(429, "请勿重复提交订单");
        }
        String redisKey = scheduleId == null ? "stock:product:" + productId : "stock:schedule:" + scheduleId;
        long initialLeft = schedule != null
                ? Math.max(0, (schedule.getQuota() == null ? 0 : schedule.getQuota()) - (schedule.getSold() == null ? 0 : schedule.getSold()))
                : Math.max(0, (product.getStock() == null ? 0 : product.getStock()) - (product.getSold() == null ? 0 : product.getSold()));
        boolean deducted = false;
        try {
            // Redis 原子扣减（防超售第一道）；首次访问用数据库剩余量初始化
            Long left = decrementStock(redisKey, people, initialLeft);
            if (left != null && left < 0) {
                throw new BizException(409, "余位不足，请选择其他团期");
            }
            deducted = left != null;

            // 数据库事务兜底（防超售第二道）
            if (schedule != null) {
                int rows = scheduleMapper.update(null, Wrappers.<BizSchedule>lambdaUpdate()
                        .setSql("sold = sold + " + people)
                        .eq(BizSchedule::getId, schedule.getId())
                        .apply("sold + {0} <= quota", people));
                if (rows == 0) throw new BizException(409, "余位不足，请选择其他团期");
                BizSchedule latest = scheduleMapper.selectById(schedule.getId());
                if (latest.getSold() >= latest.getQuota()) latest.setStatus(3);
                else if (latest.getSold() >= latest.getMinGroup()) latest.setStatus(2);
                scheduleMapper.updateById(latest);
            } else {
                int rows = productMapper.update(null, Wrappers.<BizProduct>lambdaUpdate()
                        .setSql("sold = sold + " + people)
                        .eq(BizProduct::getId, productId)
                        .apply("sold + {0} <= stock", people));
                if (rows == 0) throw new BizException(409, "库存不足");
            }

            BizOrder order = new BizOrder();
            order.setTenantId(TenantContext.getTenantId());
            order.setOrderNo(numberGenerator.next("OD"));
            order.setMemberId(user != null && "MEMBER".equals(user.getUserType()) ? user.getId() : null);
            order.setProductId(productId);
            order.setScheduleId(scheduleId);
            order.setProductTitle(product.getTitle());
            order.setDepartDate(schedule == null ? null : schedule.getDepartDate());
            order.setContactName(str(body.get("contactName")));
            order.setContactPhone(str(body.get("contactPhone")));
            order.setAdultCount(adult);
            order.setChildCount(child);
            order.setTotalAmount(total);
            order.setPaidAmount(BigDecimal.ZERO);
            order.setRefundAmount(BigDecimal.ZERO);
            order.setCommissionAmount(BigDecimal.ZERO);
            order.setPayStatus(0);
            order.setVerifyStatus(0);
            order.setStatus(1);
            order.setChannel(channel);
            order.setSource(distributorId != null ? "distributor" : "self");
            order.setDistributorId(distributorId);
            order.setRemark(str(body.get("remark")));
            order.setDeleted(0);
            orderMapper.insert(order);

            saveTourists(order.getId(), body, adult, child);
            log(order.getId(), 1, "创建订单", "订单创建成功", order.getContactName());
            return order;
        } catch (RuntimeException e) {
            if (deducted) incrementStock(redisKey, people);
            throw e;
        } finally {
            numberGenerator.release(lockKey);
        }
    }

    private Long decrementStock(String key, int n, long initialLeft) {
        try {
            var redis = redisTemplate;
            if (!Boolean.TRUE.equals(redis.hasKey(key))) {
                redis.opsForValue().setIfAbsent(key, String.valueOf(initialLeft));
            }
            Long left = redis.opsForValue().decrement(key, n);
            if (left != null && left < 0) {
                redis.opsForValue().increment(key, n);
                return left;
            }
            return left;
        } catch (Exception e) {
            return null;
        }
    }

    private void incrementStock(String key, int n) {
        try {
            redisTemplate.opsForValue().increment(key, n);
        } catch (Exception ignored) {
        }
    }

    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @SuppressWarnings("unchecked")
    private void saveTourists(Long orderId, Map<String, Object> body, int adult, int child) {
        List<?> tourists = body.get("tourists") instanceof List<?> l ? l : List.of();
        if (tourists.isEmpty()) {
            BizOrderTourist t = new BizOrderTourist();
            t.setTenantId(TenantContext.getTenantId());
            t.setOrderId(orderId);
            t.setName(str(body.getOrDefault("contactName", "游客")));
            t.setIsAdult(1);
            touristMapper.insert(t);
            return;
        }
        for (Object o : tourists) {
            if (!(o instanceof Map<?, ?> m)) continue;
            BizOrderTourist t = new BizOrderTourist();
            t.setTenantId(TenantContext.getTenantId());
            t.setOrderId(orderId);
            t.setName(str(m.get("name")));
            t.setIdType(m.get("idType") == null ? "id_card" : str(m.get("idType")));
            String idNo = str(m.get("idNo"));
            if (idNo != null && !idNo.isBlank()) {
                t.setIdNoEnc(cryptoUtil.encrypt(idNo));
                t.setIdNoMask(CryptoUtil.maskIdCard(idNo));
            }
            String phone = str(m.get("phone"));
            if (phone != null && !phone.isBlank()) {
                t.setPhoneEnc(cryptoUtil.encrypt(phone));
                t.setPhoneMask(CryptoUtil.maskPhone(phone));
            }
            t.setIsAdult(m.get("isAdult") == null ? 1 : Integer.parseInt(String.valueOf(m.get("isAdult"))));
            touristMapper.insert(t);
        }
    }

    @Transactional
    public BizPayment pay(Long orderId, String channel, BigDecimal amount) {
        BizOrder order = orderMapper.selectById(orderId);
        if (order == null) throw BizException.notFound("订单不存在");
        if (order.getPayStatus() == 1) throw new BizException("订单已支付，请勿重复支付");
        BigDecimal payAmount = amount == null ? order.getTotalAmount() : amount;
        BizPayment pay = new BizPayment();
        pay.setTenantId(TenantContext.getTenantId());
        pay.setOrderId(orderId);
        pay.setPayNo(numberGenerator.next("PY"));
        pay.setType("pay");
        pay.setChannel(channel == null ? "wechat" : channel);
        pay.setAmount(payAmount);
        pay.setStatus(1);
        pay.setTransactionId("MOCK" + System.currentTimeMillis());
        pay.setPaidAt(LocalDateTime.now());
        paymentMapper.insert(pay);

        order.setPayStatus(1);
        order.setPaidAmount(payAmount);
        order.setStatus(2);
        orderMapper.updateById(order);
        log(orderId, 2, "支付成功", "支付 " + payAmount + " 元", order.getContactName());
        wechatService.sendTemplate(TenantContext.getTenantId(), order.getMemberId(), "booking",
                Map.of("线路名称", order.getProductTitle(), "订单号", order.getOrderNo(),
                        "人数", String.valueOf(order.getAdultCount() + order.getChildCount()), "状态", "已支付"));
        if (order.getMemberId() != null) {
            BizMember member = memberMapper.selectById(order.getMemberId());
            if (member != null) {
                member.setPoints((member.getPoints() == null ? 0 : member.getPoints()) + payAmount.intValue());
                member.setTotalConsume(member.getTotalConsume() == null ? payAmount : member.getTotalConsume().add(payAmount));
                memberMapper.updateById(member);
            }
        }
        return pay;
    }

    /** 扫码核销：支持订单号或核销码；防重复核销。 */
    @Transactional
    public Map<String, Object> verify(String code, String gate, LoginUser operator) {
        if (code == null || code.isBlank()) throw new BizException("缺少核销码");
        BizOrder order = orderMapper.selectOne(Wrappers.<BizOrder>lambdaQuery()
                .eq(BizOrder::getOrderNo, code.trim()).last("limit 1"));
        if (order == null) throw BizException.notFound("核销码无效");
        if (order.getStatus() == 8) throw new BizException("订单已取消，不可核销");
        if (order.getPayStatus() != 1) throw new BizException("订单未支付，不可核销");
        if (order.getVerifyStatus() == 1) throw new BizException("该订单已核销，请勿重复核销");
        BizOrderTourist tourist = touristMapper.selectOne(Wrappers.<BizOrderTourist>lambdaQuery()
                .eq(BizOrderTourist::getOrderId, order.getId())
                .eq(BizOrderTourist::getVerifyStatus, 0).last("limit 1"));
        BizVerifyRecord record = new BizVerifyRecord();
        record.setTenantId(TenantContext.getTenantId());
        record.setOrderId(order.getId());
        record.setTouristId(tourist == null ? null : tourist.getId());
        record.setCode(code.trim());
        record.setVerifyType("qrcode");
        record.setOperatorId(operator == null ? null : operator.getId());
        record.setOperatorName(operator == null ? null : operator.getRealName());
        record.setGate(gate);
        record.setResult("success");
        record.setVerifyAt(LocalDateTime.now());
        verifyRecordMapper.insert(record);

        if (tourist != null) {
            tourist.setVerifyStatus(1);
            tourist.setVerifyAt(LocalDateTime.now());
            touristMapper.updateById(tourist);
        }
        order.setVerifyStatus(1);
        order.setStatus(6);
        orderMapper.updateById(order);
        log(order.getId(), 6, "扫码核销", "核销入口：" + gate, operator == null ? null : operator.getRealName());
        return Map.of("orderNo", order.getOrderNo(), "verifyAt", record.getVerifyAt(),
                "tourist", tourist == null ? "" : tourist.getName());
    }

    @Transactional
    public BizRefund applyRefund(Long orderId, BigDecimal amount, String reason, String applicant) {
        BizOrder order = orderMapper.selectById(orderId);
        if (order == null) throw BizException.notFound("订单不存在");
        if (order.getPayStatus() != 1) throw new BizException("订单未支付，无法退款");
        if (order.getStatus() == 8) throw new BizException("订单已取消");
        long days = order.getDepartDate() == null ? 30
                : ChronoUnit.DAYS.between(LocalDate.now(), order.getDepartDate());
        int percent = days >= 7 ? 100 : days >= 3 ? 70 : days > 0 ? 50 : 0;
        BigDecimal maxRefund = order.getPaidAmount().multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal refundAmount = amount == null ? maxRefund : amount.min(maxRefund);
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) throw new BizException("按退改规则不可退款");
        BizRefund refund = new BizRefund();
        refund.setTenantId(TenantContext.getTenantId());
        refund.setOrderId(orderId);
        refund.setRefundNo(numberGenerator.next("RF"));
        refund.setAmount(refundAmount);
        refund.setReason(reason);
        refund.setStatus(0);
        refund.setApplyBy(applicant);
        refundMapper.insert(refund);
        log(orderId, order.getStatus(), "申请退款", "申请退款 " + refundAmount + " 元：" + reason, applicant);
        return refund;
    }

    @Transactional
    public void approveRefund(Long refundId, boolean approve, String approver) {
        BizRefund refund = refundMapper.selectById(refundId);
        if (refund == null) throw BizException.notFound("退款单不存在");
        if (refund.getStatus() != 0) throw new BizException("退款单已处理");
        BizOrder order = orderMapper.selectById(refund.getOrderId());
        if (order == null) throw BizException.notFound("订单不存在");
        refund.setApproveBy(approver);
        refund.setApproveAt(LocalDateTime.now());
        if (!approve) {
            refund.setStatus(2);
            refundMapper.updateById(refund);
            log(order.getId(), order.getStatus(), "退款驳回", "审批人：" + approver, approver);
            return;
        }
        refund.setStatus(3);
        refund.setRefundAt(LocalDateTime.now());
        refundMapper.updateById(refund);

        BizPayment pay = new BizPayment();
        pay.setTenantId(TenantContext.getTenantId());
        pay.setOrderId(order.getId());
        pay.setPayNo(numberGenerator.next("RF"));
        pay.setType("refund");
        pay.setChannel("wechat");
        pay.setAmount(refund.getAmount());
        pay.setStatus(1);
        pay.setTransactionId("MOCK_REFUND" + System.currentTimeMillis());
        pay.setPaidAt(LocalDateTime.now());
        paymentMapper.insert(pay);

        order.setRefundAmount((order.getRefundAmount() == null ? BigDecimal.ZERO : order.getRefundAmount()).add(refund.getAmount()));
        if (order.getRefundAmount().compareTo(order.getPaidAmount()) >= 0) {
            order.setPayStatus(3);
            order.setStatus(8);
        } else {
            order.setPayStatus(2);
        }
        orderMapper.updateById(order);
        // 回补余位
        if (order.getScheduleId() != null) {
            int people = order.getAdultCount() + order.getChildCount();
            scheduleMapper.update(null, Wrappers.<BizSchedule>lambdaUpdate()
                    .setSql("sold = GREATEST(sold - " + people + ", 0)")
                    .eq(BizSchedule::getId, order.getScheduleId()));
        }
        log(order.getId(), order.getStatus(), "退款成功", "退款 " + refund.getAmount() + " 元（微信原路退回）", approver);
        wechatService.sendTemplate(TenantContext.getTenantId(), order.getMemberId(), "refund",
                Map.of("订单号", order.getOrderNo(), "退款金额", refund.getAmount().toString(), "原因", String.valueOf(refund.getReason())));
    }

    @Transactional
    public void confirm(Long orderId, String operator) {
        BizOrder order = orderMapper.selectById(orderId);
        if (order == null) throw BizException.notFound("订单不存在");
        if (order.getPayStatus() != 1) throw new BizException("订单未支付，不能确认");
        int status = 2;
        if (order.getScheduleId() != null) {
            BizSchedule s = scheduleMapper.selectById(order.getScheduleId());
            if (s != null && s.getSold() >= s.getMinGroup()) status = 3;
        }
        order.setStatus(status);
        orderMapper.updateById(order);
        log(orderId, status, "确认订单", "名单已确认", operator);
        wechatService.sendTemplate(TenantContext.getTenantId(), order.getMemberId(),
                status == 3 ? "group" : "booking",
                Map.of("线路名称", order.getProductTitle(), "状态", status == 3 ? "已成团" : "待成团"));
    }

    public void log(Long orderId, Integer status, String action, String remark, String operator) {
        BizOrderLog l = new BizOrderLog();
        l.setTenantId(TenantContext.getTenantId());
        l.setOrderId(orderId);
        l.setStatus(status);
        l.setAction(action);
        l.setRemark(remark);
        l.setOperator(operator);
        l.setCreatedAt(LocalDateTime.now());
        orderLogMapper.insert(l);
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
