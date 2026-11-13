package net.jxdg.travel.modules.product.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import net.jxdg.travel.common.*;
import net.jxdg.travel.modules.product.entity.BizDestination;
import net.jxdg.travel.modules.product.mapper.BizDestinationMapper;
import net.jxdg.travel.tenant.TenantContext;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/destinations")
public class DestinationController {

    private final BizDestinationMapper destinationMapper;

    @GetMapping
    public Result<PageResult<BizDestination>> page(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "20") long size,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String type) {
        Page<BizDestination> p = destinationMapper.selectPage(new Page<>(page, size),
                Wrappers.<BizDestination>lambdaQuery()
                        .like(keyword != null && !keyword.isBlank(), BizDestination::getName, keyword)
                        .eq(type != null && !type.isBlank(), BizDestination::getType, type)
                        .orderByAsc(BizDestination::getSort).orderByDesc(BizDestination::getId));
        return Result.ok(PageResult.of(p));
    }

    @GetMapping("/all")
    public Result<java.util.List<BizDestination>> all() {
        return Result.ok(destinationMapper.selectList(Wrappers.<BizDestination>lambdaQuery()
                .eq(BizDestination::getStatus, 1).orderByAsc(BizDestination::getSort)));
    }

    @GetMapping("/{id}")
    public Result<BizDestination> detail(@PathVariable Long id) {
        BizDestination d = destinationMapper.selectById(id);
        if (d == null) throw BizException.notFound("目的地不存在");
        return Result.ok(d);
    }

    @PostMapping
    @Audit(module = "目的地", action = "保存目的地")
    public Result<Void> save(@RequestBody BizDestination d) {
        if (d.getTenantId() == null) d.setTenantId(TenantContext.getTenantId());
        if (d.getId() == null) destinationMapper.insert(d);
        else destinationMapper.updateById(d);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Audit(module = "目的地", action = "删除目的地")
    public Result<Void> delete(@PathVariable Long id) {
        destinationMapper.deleteById(id);
        return Result.ok();
    }
}
