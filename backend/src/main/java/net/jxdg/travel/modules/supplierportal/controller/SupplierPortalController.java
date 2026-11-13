package net.jxdg.travel.modules.supplierportal.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import net.jxdg.travel.common.*;
import net.jxdg.travel.modules.finance.entity.BizSettlement;
import net.jxdg.travel.modules.finance.mapper.BizSettlementMapper;
import net.jxdg.travel.modules.resource.entity.BizPurchaseOrder;
import net.jxdg.travel.modules.resource.entity.BizSupplier;
import net.jxdg.travel.modules.resource.entity.BizSupplierResource;
import net.jxdg.travel.modules.resource.mapper.BizPurchaseOrderMapper;
import net.jxdg.travel.modules.resource.mapper.BizSupplierMapper;
import net.jxdg.travel.modules.resource.mapper.BizSupplierResourceMapper;
import net.jxdg.travel.modules.supplierportal.entity.BizHotelRoom;
import net.jxdg.travel.modules.supplierportal.entity.BizVehicle;
import net.jxdg.travel.modules.supplierportal.entity.BizVehicleDispatch;
import net.jxdg.travel.modules.supplierportal.mapper.BizHotelRoomMapper;
import net.jxdg.travel.modules.supplierportal.mapper.BizVehicleDispatchMapper;
import net.jxdg.travel.modules.supplierportal.mapper.BizVehicleMapper;
import net.jxdg.travel.security.LoginUser;
import net.jxdg.travel.security.SecurityUtils;
import net.jxdg.travel.tenant.TenantContext;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 供应商开放端（酒店/车队/餐厅/地接社）：仅见自身采购单与结算，越权 403 + 审计。
 */
@RestController
@RequiredArgsConstructor
public class SupplierPortalController {

    private final BizSupplierMapper supplierMapper;
    private final BizSupplierResourceMapper resourceMapper;
    private final BizPurchaseOrderMapper purchaseMapper;
    private final BizSettlementMapper settlementMapper;
    private final BizHotelRoomMapper hotelRoomMapper;
    private final BizVehicleMapper vehicleMapper;
    private final BizVehicleDispatchMapper dispatchMapper;

    @GetMapping("/supplier/me")
    public Result<Map<String, Object>> me() {
        BizSupplier s = currentSupplier();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("supplier", s);
        out.put("resources", resourceMapper.selectList(Wrappers.<BizSupplierResource>lambdaQuery()
                .eq(BizSupplierResource::getSupplierId, s.getId())));
        out.put("hotelRooms", hotelRoomMapper.selectList(Wrappers.<BizHotelRoom>lambdaQuery()
                .eq(BizHotelRoom::getSupplierId, s.getId())));
        out.put("vehicles", vehicleMapper.selectList(Wrappers.<BizVehicle>lambdaQuery()
                .eq(BizVehicle::getSupplierId, s.getId())));
        return Result.ok(out);
    }

    @GetMapping("/supplier/purchases")
    public Result<List<Map<String, Object>>> purchases(@RequestParam(required = false) Integer status) {
        BizSupplier s = currentSupplier();
        List<BizPurchaseOrder> list = purchaseMapper.selectList(Wrappers.<BizPurchaseOrder>lambdaQuery()
                .eq(BizPurchaseOrder::getSupplierId, s.getId())
                .eq(status != null, BizPurchaseOrder::getStatus, status)
                .orderByDesc(BizPurchaseOrder::getId));
        return Result.ok(list.stream().map(p -> Map.<String, Object>of("purchase", p))
                .map(m -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mm = new LinkedHashMap<>(m);
                    return mm;
                }).toList());
    }

    @PostMapping("/supplier/purchases/{id}/confirm")
    @Audit(module = "供应商端", action = "一键确认订单")
    public Result<Void> confirm(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean accept,
                                @RequestParam(required = false) String remark) {
        BizSupplier s = currentSupplier();
        BizPurchaseOrder p = purchaseMapper.selectById(id);
        if (p == null || !Objects.equals(p.getSupplierId(), s.getId())) {
            throw BizException.forbidden("无权操作其他供应商的采购单");
        }
        p.setStatus(accept ? 1 : 2);
        p.setConfirmTime(LocalDateTime.now());
        p.setConfirmRemark(remark == null ? "供应商开放端确认" : remark);
        purchaseMapper.updateById(p);
        return Result.ok();
    }

    @GetMapping("/supplier/settlements")
    public Result<List<BizSettlement>> settlements() {
        BizSupplier s = currentSupplier();
        return Result.ok(settlementMapper.selectList(Wrappers.<BizSettlement>lambdaQuery()
                .eq(BizSettlement::getPartnerType, "supplier")
                .eq(BizSettlement::getPartnerId, s.getId())
                .orderByDesc(BizSettlement::getId)));
    }

    @GetMapping("/supplier/account")
    public Result<Map<String, Object>> account() {
        BizSupplier s = currentSupplier();
        List<BizPurchaseOrder> purchases = purchaseMapper.selectList(Wrappers.<BizPurchaseOrder>lambdaQuery()
                .eq(BizPurchaseOrder::getSupplierId, s.getId()).eq(BizPurchaseOrder::getStatus, 1));
        BigDecimal receivable = purchases.stream().map(BizPurchaseOrder::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<BizSettlement> settlements = settlementMapper.selectList(Wrappers.<BizSettlement>lambdaQuery()
                .eq(BizSettlement::getPartnerType, "supplier").eq(BizSettlement::getPartnerId, s.getId()));
        BigDecimal settled = settlements.stream().map(BizSettlement::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal confirmed = settlements.stream().map(BizSettlement::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        // 两套账：供应商账（应收/已结）与旅行社账（结算单/已付）口径并列
        Map<String, Object> supplierBook = Map.of("receivable", receivable, "settled", settled,
                "pending", receivable.subtract(settled));
        Map<String, Object> agencyBook = Map.of("settlementAmount", confirmed, "paidAmount", settled,
                "pending", confirmed.subtract(settled));
        return Result.ok(Map.of("supplierBook", supplierBook, "agencyBook", agencyBook, "settlements", settlements));
    }

    @GetMapping("/supplier/resources")
    public Result<List<BizSupplierResource>> resources() {
        BizSupplier s = currentSupplier();
        return Result.ok(resourceMapper.selectList(Wrappers.<BizSupplierResource>lambdaQuery()
                .eq(BizSupplierResource::getSupplierId, s.getId())));
    }

    @PostMapping("/supplier/resources")
    @Audit(module = "供应商端", action = "维护资源库存/协议价")
    public Result<Void> saveResource(@RequestBody BizSupplierResource r) {
        BizSupplier s = currentSupplier();
        r.setSupplierId(s.getId());
        if (r.getTenantId() == null) r.setTenantId(TenantContext.getTenantId());
        if (r.getId() == null) {
            if (r.getSold() == null) r.setSold(0);
            resourceMapper.insert(r);
        } else {
            BizSupplierResource exist = resourceMapper.selectById(r.getId());
            if (exist == null || !Objects.equals(exist.getSupplierId(), s.getId()))
                throw BizException.forbidden("无权修改其他供应商资源");
            resourceMapper.updateById(r);
        }
        return Result.ok();
    }

    @GetMapping("/supplier/hotel/rooms")
    public Result<List<BizHotelRoom>> rooms() {
        BizSupplier s = currentSupplier();
        return Result.ok(hotelRoomMapper.selectList(Wrappers.<BizHotelRoom>lambdaQuery()
                .eq(BizHotelRoom::getSupplierId, s.getId())));
    }

    @PostMapping("/supplier/hotel/rooms")
    @Audit(module = "供应商端", action = "维护房态房价")
    public Result<Void> saveRoom(@RequestBody BizHotelRoom r) {
        BizSupplier s = currentSupplier();
        r.setSupplierId(s.getId());
        if (r.getTenantId() == null) r.setTenantId(TenantContext.getTenantId());
        if (r.getId() == null) hotelRoomMapper.insert(r);
        else hotelRoomMapper.updateById(r);
        return Result.ok();
    }

    @GetMapping("/supplier/vehicles")
    public Result<List<BizVehicle>> vehicles() {
        BizSupplier s = currentSupplier();
        return Result.ok(vehicleMapper.selectList(Wrappers.<BizVehicle>lambdaQuery()
                .eq(BizVehicle::getSupplierId, s.getId())));
    }

    @PostMapping("/supplier/vehicles")
    @Audit(module = "供应商端", action = "维护车辆司机")
    public Result<Void> saveVehicle(@RequestBody BizVehicle v) {
        BizSupplier s = currentSupplier();
        v.setSupplierId(s.getId());
        if (v.getTenantId() == null) v.setTenantId(TenantContext.getTenantId());
        if (v.getId() == null) vehicleMapper.insert(v);
        else vehicleMapper.updateById(v);
        return Result.ok();
    }

    @GetMapping("/supplier/dispatches")
    public Result<List<BizVehicleDispatch>> dispatches() {
        BizSupplier s = currentSupplier();
        List<Long> ids = vehicleMapper.selectList(Wrappers.<BizVehicle>lambdaQuery()
                .eq(BizVehicle::getSupplierId, s.getId())).stream().map(BizVehicle::getId).toList();
        if (ids.isEmpty()) return Result.ok(List.of());
        return Result.ok(dispatchMapper.selectList(Wrappers.<BizVehicleDispatch>lambdaQuery()
                .in(BizVehicleDispatch::getVehicleId, ids).orderByDesc(BizVehicleDispatch::getId)));
    }

    @PostMapping("/supplier/dispatches/{id}/verify")
    @Audit(module = "供应商端", action = "出车核销")
    public Result<Void> verifyDispatch(@PathVariable Long id) {
        BizSupplier s = currentSupplier();
        BizVehicleDispatch d = dispatchMapper.selectById(id);
        if (d == null) throw BizException.notFound("调度单不存在");
        BizVehicle v = vehicleMapper.selectById(d.getVehicleId());
        if (v == null || !Objects.equals(v.getSupplierId(), s.getId()))
            throw BizException.forbidden("无权操作其他供应商车辆");
        d.setStatus(2);
        d.setVerifyAt(LocalDateTime.now());
        dispatchMapper.updateById(d);
        return Result.ok();
    }

    private BizSupplier currentSupplier() {
        LoginUser u = SecurityUtils.current();
        if (!"SUPPLIER".equals(u.getUserType())) throw BizException.forbidden("仅供应商账号可访问");
        BizSupplier s = supplierMapper.selectById(u.getId());
        if (s == null) throw BizException.notFound("供应商档案不存在");
        return s;
    }
}
