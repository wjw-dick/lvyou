package net.jxdg.travel.modules.product.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.jxdg.travel.common.*;
import net.jxdg.travel.modules.product.dto.ProductExcelRow;
import net.jxdg.travel.modules.product.entity.BizDestination;
import net.jxdg.travel.modules.product.entity.BizProduct;
import net.jxdg.travel.modules.product.entity.BizProductItinerary;
import net.jxdg.travel.modules.product.entity.BizProductPrice;
import net.jxdg.travel.modules.product.mapper.BizDestinationMapper;
import net.jxdg.travel.modules.product.mapper.BizProductItineraryMapper;
import net.jxdg.travel.modules.product.mapper.BizProductMapper;
import net.jxdg.travel.modules.product.mapper.BizProductPriceMapper;
import net.jxdg.travel.modules.system.entity.SysContentI18n;
import net.jxdg.travel.modules.system.entity.SysTenant;
import net.jxdg.travel.modules.system.mapper.SysContentI18nMapper;
import net.jxdg.travel.modules.system.mapper.SysTenantMapper;
import net.jxdg.travel.security.SecurityUtils;
import net.jxdg.travel.tenant.TenantContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final BizProductMapper productMapper;
    private final BizProductItineraryMapper itineraryMapper;
    private final BizProductPriceMapper priceMapper;
    private final BizDestinationMapper destinationMapper;
    private final SysTenantMapper tenantMapper;
    private final SysContentI18nMapper contentI18nMapper;
    private final NumberGenerator numberGenerator;

    @GetMapping("/products")
    public Result<PageResult<BizProduct>> page(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "20") long size,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String category,
                                               @RequestParam(required = false) Integer status,
                                               @RequestParam(required = false) String destination,
                                               @RequestParam(required = false) Integer days,
                                               @RequestParam(required = false) String channel) {
        Page<BizProduct> p = productMapper.selectPage(new Page<>(page, size), Wrappers.<BizProduct>lambdaQuery()
                .and(keyword != null && !keyword.isBlank(), w -> w.like(BizProduct::getTitle, keyword)
                        .or().like(BizProduct::getProductNo, keyword))
                .eq(category != null && !category.isBlank(), BizProduct::getCategory, category)
                .eq(status != null, BizProduct::getStatus, status)
                .eq(days != null, BizProduct::getDays, days)
                .like(destination != null && !destination.isBlank(), BizProduct::getDestination, destination)
                .like(channel != null && !channel.isBlank(), BizProduct::getChannels, channel)
                .orderByDesc(BizProduct::getId));
        return Result.ok(PageResult.of(p));
    }

    @GetMapping("/products/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        BizProduct product = productMapper.selectById(id);
        if (product == null) throw BizException.notFound("产品不存在");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("product", product);
        out.put("itineraries", itineraryMapper.selectList(Wrappers.<BizProductItinerary>lambdaQuery()
                .eq(BizProductItinerary::getProductId, id).orderByAsc(BizProductItinerary::getDayNo)));
        out.put("prices", priceMapper.selectList(Wrappers.<BizProductPrice>lambdaQuery()
                .eq(BizProductPrice::getProductId, id)));
        if (product.getDestinationId() != null) {
            out.put("destination", destinationMapper.selectById(product.getDestinationId()));
        }
        out.put("i18n", contentI18nMapper.selectList(Wrappers.<SysContentI18n>lambdaQuery()
                .eq(SysContentI18n::getBizType, "product").eq(SysContentI18n::getBizId, id)));
        return Result.ok(out);
    }

    /** 内容多语言字段保存（产品标题/攻略等） */
    @PostMapping("/products/{id}/i18n")
    @Audit(module = "产品管理", action = "保存多语言内容")
    public Result<Void> saveI18n(@PathVariable Long id, @RequestBody List<Map<String, String>> items) {
        for (Map<String, String> item : items) {
            String lang = item.get("lang");
            String field = item.get("field");
            String content = item.get("content");
            if (lang == null || field == null) continue;
            SysContentI18n exist = contentI18nMapper.selectOne(Wrappers.<SysContentI18n>lambdaQuery()
                    .eq(SysContentI18n::getBizType, "product").eq(SysContentI18n::getBizId, id)
                    .eq(SysContentI18n::getField, field).eq(SysContentI18n::getLang, lang).last("limit 1"));
            if (exist == null) {
                SysContentI18n row = new SysContentI18n();
                row.setTenantId(TenantContext.getTenantId());
                row.setBizType("product");
                row.setBizId(id);
                row.setField(field);
                row.setLang(lang);
                row.setContent(content);
                contentI18nMapper.insert(row);
            } else {
                exist.setContent(content);
                contentI18nMapper.updateById(exist);
            }
        }
        return Result.ok();
    }

    @PostMapping("/products")
    @Transactional
    @Audit(module = "产品管理", action = "新增产品")
    public Result<BizProduct> create(@RequestBody Map<String, Object> body) {
        checkQuota();
        BizProduct p = new BizProduct();
        apply(p, body);
        p.setProductNo(numberGenerator.next("TR"));
        p.setTenantId(TenantContext.getTenantId());
        p.setCreatedBy(SecurityUtils.userId());
        p.setSold(0);
        p.setStatus(body.get("status") == null ? 0 : Integer.parseInt(String.valueOf(body.get("status"))));
        p.setDeleted(0);
        productMapper.insert(p);
        saveChildren(p.getId(), body);
        return Result.ok(p);
    }

    @PutMapping("/products/{id}")
    @Transactional
    @Audit(module = "产品管理", action = "修改产品")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BizProduct p = productMapper.selectById(id);
        if (p == null) throw BizException.notFound("产品不存在");
        apply(p, body);
        if (body.get("status") != null) p.setStatus(Integer.parseInt(String.valueOf(body.get("status"))));
        productMapper.updateById(p);
        saveChildren(id, body);
        return Result.ok();
    }

    @DeleteMapping("/products/{id}")
    @Audit(module = "产品管理", action = "删除产品")
    public Result<Void> delete(@PathVariable Long id) {
        productMapper.deleteById(id);
        itineraryMapper.delete(Wrappers.<BizProductItinerary>lambdaQuery().eq(BizProductItinerary::getProductId, id));
        priceMapper.delete(Wrappers.<BizProductPrice>lambdaQuery().eq(BizProductPrice::getProductId, id));
        return Result.ok();
    }

    @PostMapping("/products/{id}/status")
    @Audit(module = "产品管理", action = "产品上下架")
    public Result<Void> status(@PathVariable Long id, @RequestParam Integer status) {
        BizProduct p = productMapper.selectById(id);
        if (p == null) throw BizException.notFound("产品不存在");
        p.setStatus(status);
        productMapper.updateById(p);
        return Result.ok();
    }

    @PostMapping("/products/{id}/channels")
    @Audit(module = "产品管理", action = "配置上架渠道")
    public Result<Void> channels(@PathVariable Long id, @RequestBody List<String> channels) {
        BizProduct p = productMapper.selectById(id);
        if (p == null) throw BizException.notFound("产品不存在");
        p.setChannels(String.join(",", channels));
        productMapper.updateById(p);
        return Result.ok();
    }

    @GetMapping("/products/export")
    public void export(HttpServletResponse response) throws Exception {
        List<BizProduct> list = productMapper.selectList(Wrappers.<BizProduct>lambdaQuery().orderByDesc(BizProduct::getId));
        List<ProductExcelRow> rows = list.stream().map(p -> {
            ProductExcelRow r = new ProductExcelRow();
            r.setTitle(p.getTitle());
            r.setCategory(p.getCategory());
            r.setDays(p.getDays());
            r.setDeparture(p.getDeparture());
            r.setDestination(p.getDestination());
            r.setAdultPrice(p.getAdultPrice());
            r.setChildPrice(p.getChildPrice());
            r.setSingleRoomDiff(p.getSingleRoomDiff());
            r.setStock(p.getStock());
            r.setIncludeText(p.getIncludeText());
            r.setExcludeText(p.getExcludeText());
            return r;
        }).toList();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" +
                URLEncoder.encode("线路产品.xlsx", StandardCharsets.UTF_8));
        EasyExcel.write(response.getOutputStream(), ProductExcelRow.class).sheet("产品").doWrite(rows);
    }

    @PostMapping("/products/import")
    @Transactional
    @Audit(module = "产品管理", action = "批量导入产品")
    public Result<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) throws Exception {
        checkQuota();
        List<ProductExcelRow> rows = EasyExcel.read(file.getInputStream()).head(ProductExcelRow.class)
                .sheet().doReadSync();
        int success = 0;
        for (ProductExcelRow r : rows) {
            if (r.getTitle() == null || r.getTitle().isBlank()) continue;
            BizProduct p = new BizProduct();
            p.setTitle(r.getTitle());
            p.setCategory(r.getCategory() == null ? "line" : r.getCategory());
            p.setDays(r.getDays() == null ? 1 : r.getDays());
            p.setDeparture(r.getDeparture());
            p.setDestination(r.getDestination());
            p.setAdultPrice(r.getAdultPrice() == null ? BigDecimal.ZERO : r.getAdultPrice());
            p.setChildPrice(r.getChildPrice() == null ? BigDecimal.ZERO : r.getChildPrice());
            p.setSingleRoomDiff(r.getSingleRoomDiff() == null ? BigDecimal.ZERO : r.getSingleRoomDiff());
            p.setStock(r.getStock() == null ? 0 : r.getStock());
            p.setIncludeText(r.getIncludeText());
            p.setExcludeText(r.getExcludeText());
            p.setProductNo(numberGenerator.next("TR"));
            p.setTenantId(TenantContext.getTenantId());
            p.setSold(0);
            p.setStatus(0);
            p.setChannels("miniprogram,mp,h5,app");
            p.setDeleted(0);
            p.setCreatedBy(SecurityUtils.userId());
            productMapper.insert(p);
            success++;
        }
        return Result.ok(Map.of("total", rows.size(), "success", success));
    }

    private void checkQuota() {
        Long tenantId = TenantContext.getTenantId();
        SysTenant t = tenantMapper.selectById(tenantId);
        if (t != null && t.getQuotaProduct() != null) {
            long count = productMapper.selectCount(Wrappers.<BizProduct>lambdaQuery());
            if (count >= t.getQuotaProduct()) {
                throw new BizException(400, "已达租户产品配额上限（" + t.getQuotaProduct() + "），请联系平台扩容");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void saveChildren(Long productId, Map<String, Object> body) {
        if (body.get("itineraries") instanceof List<?> list) {
            itineraryMapper.delete(Wrappers.<BizProductItinerary>lambdaQuery().eq(BizProductItinerary::getProductId, productId));
            int day = 1;
            for (Object o : list) {
                if (!(o instanceof Map<?, ?> m)) continue;
                BizProductItinerary it = new BizProductItinerary();
                it.setTenantId(TenantContext.getTenantId());
                it.setProductId(productId);
                it.setDayNo(m.get("dayNo") == null ? day : Integer.parseInt(String.valueOf(m.get("dayNo"))));
                it.setTitle(str(m.get("title")));
                it.setSpots(str(m.get("spots")));
                it.setHotel(str(m.get("hotel")));
                it.setMeals(str(m.get("meals")));
                it.setTransport(str(m.get("transport")));
                it.setGuide(str(m.get("guide")));
                it.setAccommodation(str(m.get("accommodation")));
                it.setContent(str(m.get("content")));
                itineraryMapper.insert(it);
                day++;
            }
        }
        if (body.get("prices") instanceof List<?> list) {
            priceMapper.delete(Wrappers.<BizProductPrice>lambdaQuery().eq(BizProductPrice::getProductId, productId));
            for (Object o : list) {
                if (!(o instanceof Map<?, ?> m)) continue;
                BizProductPrice price = new BizProductPrice();
                price.setTenantId(TenantContext.getTenantId());
                price.setProductId(productId);
                price.setChannel(m.get("channel") == null ? "h5" : str(m.get("channel")));
                price.setChannelId(m.get("channelId") == null ? null : Long.valueOf(String.valueOf(m.get("channelId"))));
                price.setAdultPrice(decimal(m.get("adultPrice")));
                price.setChildPrice(decimal(m.get("childPrice")));
                price.setSingleRoomDiff(decimal(m.get("singleRoomDiff")));
                price.setStatus(1);
                priceMapper.insert(price);
            }
        }
    }

    private void apply(BizProduct p, Map<String, Object> b) {
        if (b.get("title") != null) p.setTitle(String.valueOf(b.get("title")));
        if (b.get("category") != null) p.setCategory(String.valueOf(b.get("category")));
        if (b.get("subtitle") != null) p.setSubtitle(str(b.get("subtitle")));
        if (b.get("days") != null) p.setDays(Integer.parseInt(String.valueOf(b.get("days"))));
        if (b.get("departure") != null) p.setDeparture(str(b.get("departure")));
        if (b.get("destination") != null) p.setDestination(str(b.get("destination")));
        if (b.get("destinationId") != null) p.setDestinationId(Long.valueOf(String.valueOf(b.get("destinationId"))));
        if (b.get("adultPrice") != null) p.setAdultPrice(decimal(b.get("adultPrice")));
        if (b.get("childPrice") != null) p.setChildPrice(decimal(b.get("childPrice")));
        if (b.get("singleRoomDiff") != null) p.setSingleRoomDiff(decimal(b.get("singleRoomDiff")));
        if (b.get("costPrice") != null) p.setCostPrice(decimal(b.get("costPrice")));
        if (b.get("includeText") != null) p.setIncludeText(str(b.get("includeText")));
        if (b.get("excludeText") != null) p.setExcludeText(str(b.get("excludeText")));
        if (b.get("refundRule") != null) p.setRefundRule(str(b.get("refundRule")));
        if (b.get("meetingPoint") != null) p.setMeetingPoint(str(b.get("meetingPoint")));
        if (b.get("cover") != null) p.setCover(str(b.get("cover")));
        if (b.get("images") != null) p.setImages(b.get("images") instanceof String s ? s : b.get("images").toString());
        if (b.get("guideContent") != null) p.setGuideContent(str(b.get("guideContent")));
        if (b.get("stock") != null) p.setStock(Integer.parseInt(String.valueOf(b.get("stock"))));
        if (b.get("minGroup") != null) p.setMinGroup(Integer.parseInt(String.valueOf(b.get("minGroup"))));
        if (b.get("channels") != null) {
            p.setChannels(b.get("channels") instanceof List<?> l ? String.join(",", l.stream().map(String::valueOf).toList())
                    : String.valueOf(b.get("channels")));
        }
        if (b.get("remark") != null) p.setRemark(str(b.get("remark")));
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static BigDecimal decimal(Object o) {
        if (o == null || String.valueOf(o).isBlank()) return BigDecimal.ZERO;
        return new BigDecimal(String.valueOf(o));
    }
}
