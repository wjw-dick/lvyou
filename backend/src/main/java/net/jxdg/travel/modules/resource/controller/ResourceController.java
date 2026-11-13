package net.jxdg.travel.modules.resource.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import net.jxdg.travel.common.*;
import net.jxdg.travel.modules.guide.entity.BizGuide;
import net.jxdg.travel.modules.guide.mapper.BizGuideMapper;
import net.jxdg.travel.modules.product.entity.BizSchedule;
import net.jxdg.travel.modules.product.mapper.BizScheduleMapper;
import net.jxdg.travel.modules.resource.entity.BizPurchaseOrder;
import net.jxdg.travel.modules.resource.entity.BizSupplier;
import net.jxdg.travel.modules.resource.entity.BizSupplierResource;
import net.jxdg.travel.modules.resource.mapper.BizPurchaseOrderMapper;
import net.jxdg.travel.modules.resource.mapper.BizSupplierMapper;
import net.jxdg.travel.modules.resource.mapper.BizSupplierResourceMapper;
import net.jxdg.travel.security.SecurityUtils;
import net.jxdg.travel.tenant.TenantContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class ResourceController {

    private final BizSupplierMapper supplierMapper;
    private final BizSupplierResourceMapper resourceMapper;
    private final BizPurchaseOrderMapper purchaseMapper;
    private final BizScheduleMapper scheduleMapper;
    private final BizGuideMapper guideMapper;
    private final NumberGenerator numberGenerator;

    // ---------------- 供应商 ----------------
    @GetMapping("/suppliers")
    public Result<PageResult<BizSupplier>> suppliers(@RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "20") long size,
                                                     @RequestParam(required = false) String type,
                                                     @RequestParam(required = false) String keyword) {
        Page<BizSupplier> p = supplierMapper.selectPage(new Page<>(page, size), Wrappers.<BizSupplier>lambdaQuery()
                .eq(type != null && !type.isBlank(), BizSupplier::getType, type)
                .like(keyword != null && !keyword.isBlank(), BizSupplier::getName, keyword)
                .orderByDesc(BizSupplier::getId));
        return Result.ok(PageResult.of(p));
    }

    @GetMapping("/suppliers/all")
    public Result<List<BizSupplier>> allSuppliers() {
        return Result.ok(supplierMapper.selectList(Wrappers.<BizSupplier>lambdaQuery().eq(BizSupplier::getStatus, 1)));
    }

    @PostMapping("/suppliers")
    @Audit(module = "计调", action = "保存供应商")
    public Result<Void> saveSupplier(@RequestBody BizSupplier s) {
        if (s.getTenantId() == null) s.setTenantId(TenantContext.getTenantId());
        if (s.getStatus() == null) s.setStatus(1);
        if (s.getId() == null) supplierMapper.insert(s);
        else supplierMapper.updateById(s);
        return Result.ok();
    }

    @DeleteMapping("/suppliers/{id}")
    @Audit(module = "计调", action = "删除供应商")
    public Result<Void> deleteSupplier(@PathVariable Long id) {
        supplierMapper.deleteById(id);
        return Result.ok();
    }

    // ---------------- 资源/库存/协议价 ----------------
    @GetMapping("/suppliers/{supplierId}/resources")
    public Result<List<BizSupplierResource>> resources(@PathVariable Long supplierId) {
        return Result.ok(resourceMapper.selectList(Wrappers.<BizSupplierResource>lambdaQuery()
                .eq(BizSupplierResource::getSupplierId, supplierId).orderByAsc(BizSupplierResource::getId)));
    }

    @GetMapping("/resources")
    public Result<List<Map<String, Object>>> resources(@RequestParam(required = false) String date,
                                                       @RequestParam(required = false) String type) {
        List<BizSupplierResource> list = resourceMapper.selectList(Wrappers.<BizSupplierResource>lambdaQuery()
                .eq(date != null && !date.isBlank(), BizSupplierResource::getAvailableDate, date)
                .eq(type != null && !type.isBlank(), BizSupplierResource::getType, type)
                .orderByAsc(BizSupplierResource::getAvailableDate));
        return Result.ok(list.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("resource", r);
            BizSupplier s = supplierMapper.selectById(r.getSupplierId());
            m.put("supplierName", s == null ? "" : s.getName());
            m.put("left", Math.max(0, r.getStock() - r.getSold()));
            return m;
        }).toList());
    }

    @PostMapping("/resources")
    @Audit(module = "计调", action = "保存资源")
    public Result<Void> saveResource(@RequestBody BizSupplierResource r) {
        if (r.getTenantId() == null) r.setTenantId(TenantContext.getTenantId());
        if (r.getId() == null) {
            if (r.getSold() == null) r.setSold(0);
            resourceMapper.insert(r);
        } else {
            resourceMapper.updateById(r);
        }
        return Result.ok();
    }

    // ---------------- 采购单 ----------------
    @GetMapping("/purchases")
    public Result<PageResult<Map<String, Object>>> purchases(@RequestParam(defaultValue = "1") long page,
                                                             @RequestParam(defaultValue = "20") long size,
                                                             @RequestParam(required = false) Integer status,
                                                             @RequestParam(required = false) Long supplierId) {
        Page<BizPurchaseOrder> p = purchaseMapper.selectPage(new Page<>(page, size),
                Wrappers.<BizPurchaseOrder>lambdaQuery()
                        .eq(status != null, BizPurchaseOrder::getStatus, status)
                        .eq(supplierId != null, BizPurchaseOrder::getSupplierId, supplierId)
                        .orderByDesc(BizPurchaseOrder::getId));
        return Result.ok(PageResult.of(p.getRecords().stream().map(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("purchase", o);
            BizSupplier s = supplierMapper.selectById(o.getSupplierId());
            m.put("supplierName", s == null ? "" : s.getName());
            BizSchedule sc = o.getScheduleId() == null ? null : scheduleMapper.selectById(o.getScheduleId());
            m.put("scheduleNo", sc == null ? "" : sc.getScheduleNo());
            return m;
        }).toList(), p.getTotal(), p.getCurrent(), p.getSize()));
    }

    @PostMapping("/purchases")
    @Transactional
    @Audit(module = "计调", action = "创建采购单")
    public Result<BizPurchaseOrder> createPurchase(@RequestBody Map<String, Object> body) {
        BizSupplierResource resource = null;
        int qty = body.get("quantity") == null ? 1 : Integer.parseInt(String.valueOf(body.get("quantity")));
        Long resourceId = body.get("resourceId") == null ? null : Long.valueOf(String.valueOf(body.get("resourceId")));
        if (resourceId != null) {
            resource = resourceMapper.selectById(resourceId);
            if (resource == null) throw BizException.notFound("资源不存在");
            int rows = resourceMapper.update(null, Wrappers.<BizSupplierResource>lambdaUpdate()
                    .setSql("sold = sold + " + qty)
                    .eq(BizSupplierResource::getId, resourceId)
                    .apply("sold + {0} <= stock", qty));
            if (rows == 0) throw new BizException(409, "资源库存不足（超售校验拦截）");
        }
        BizPurchaseOrder p = new BizPurchaseOrder();
        p.setTenantId(TenantContext.getTenantId());
        p.setPurchaseNo(numberGenerator.next("PO"));
        p.setScheduleId(body.get("scheduleId") == null ? null : Long.valueOf(String.valueOf(body.get("scheduleId"))));
        p.setSupplierId(Long.valueOf(String.valueOf(body.get("supplierId"))));
        p.setResourceId(resourceId);
        p.setResourceName(resource == null ? str(body.get("resourceName")) : resource.getName());
        p.setQuantity(qty);
        p.setUnitPrice(new BigDecimal(String.valueOf(body.getOrDefault("unitPrice", resource == null ? "0" : resource.getCostPrice().toString()))));
        p.setAmount(p.getUnitPrice().multiply(BigDecimal.valueOf(qty)));
        p.setStatus(0);
        p.setSettleStatus(0);
        p.setCreatedAt(LocalDateTime.now());
        purchaseMapper.insert(p);
        return Result.ok(p);
    }

    @PostMapping("/purchases/{id}/confirm")
    @Audit(module = "计调", action = "供应商确认采购单")
    public Result<Void> confirm(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean accept,
                                @RequestParam(required = false) String remark) {
        BizPurchaseOrder p = purchaseMapper.selectById(id);
        if (p == null) throw BizException.notFound("采购单不存在");
        p.setStatus(accept ? 1 : 2);
        p.setConfirmTime(LocalDateTime.now());
        p.setConfirmRemark(remark);
        purchaseMapper.updateById(p);
        return Result.ok();
    }

    /** OP 自动匹配资源与导游（排团建议） */
    @GetMapping("/resource/match")
    public Result<Map<String, Object>> match(@RequestParam Long scheduleId) {
        BizSchedule s = scheduleMapper.selectById(scheduleId);
        if (s == null) throw BizException.notFound("排期不存在");
        List<Map<String, Object>> resources = new ArrayList<>();
        for (BizSupplierResource r : resourceMapper.selectList(Wrappers.<BizSupplierResource>lambdaQuery()
                .ge(BizSupplierResource::getAvailableDate, s.getDepartDate())
                .orderByAsc(BizSupplierResource::getCostPrice))) {
            BizSupplier sup = supplierMapper.selectById(r.getSupplierId());
            resources.add(Map.of("resourceId", r.getId(), "name", r.getName(), "type", r.getType(),
                    "costPrice", r.getCostPrice(), "left", Math.max(0, r.getStock() - r.getSold()),
                    "supplierName", sup == null ? "" : sup.getName()));
        }
        List<Map<String, Object>> guides = guideMapper.selectList(Wrappers.<BizGuide>lambdaQuery()
                        .eq(BizGuide::getStatus, 1)).stream()
                .map(g -> Map.<String, Object>of("guideId", g.getId(), "name", g.getName(),
                        "level", g.getLevel() == null ? "" : g.getLevel(),
                        "phone", g.getPhone() == null ? "" : g.getPhone())).toList();
        return Result.ok(Map.of("scheduleId", scheduleId, "resources", resources, "guides", guides,
                "suggestedGuide", guides.isEmpty() ? "" : guides.get(0)));
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
