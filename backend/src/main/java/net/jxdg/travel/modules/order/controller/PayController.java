package net.jxdg.travel.modules.order.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import net.jxdg.travel.common.Result;
import net.jxdg.travel.modules.order.entity.BizOrder;
import net.jxdg.travel.modules.order.mapper.BizOrderMapper;
import net.jxdg.travel.modules.order.service.OrderService;
import net.jxdg.travel.tenant.TenantContext;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 微信支付回调（模拟通道）。生产环境需验签并校验金额。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/pay")
public class PayController {

    private final BizOrderMapper orderMapper;
    private final OrderService orderService;

    @PostMapping("/notify/{orderNo}")
    public String notify(@PathVariable String orderNo) {
        BizOrder order = orderMapper.selectOne(Wrappers.<BizOrder>lambdaQuery()
                .eq(BizOrder::getOrderNo, orderNo).last("limit 1"));
        if (order == null || TenantContext.getTenantId() == null) {
            return "FAIL";
        }
        if (order.getPayStatus() != 1) {
            orderService.pay(order.getId(), "wechat", null);
        }
        return "SUCCESS";
    }

    @GetMapping("/mock/{orderNo}")
    public Result<Map<String, Object>> mock(@PathVariable String orderNo) {
        BizOrder order = orderMapper.selectOne(Wrappers.<BizOrder>lambdaQuery()
                .eq(BizOrder::getOrderNo, orderNo).last("limit 1"));
        if (order == null) return Result.fail("订单不存在");
        if (order.getPayStatus() != 1) orderService.pay(order.getId(), "wechat", null);
        return Result.ok(Map.of("paid", true, "orderNo", orderNo));
    }
}
