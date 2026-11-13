package net.jxdg.travel.modules.contract.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.jxdg.travel.common.*;
import net.jxdg.travel.modules.contract.entity.BizContract;
import net.jxdg.travel.modules.contract.entity.BizContractTemplate;
import net.jxdg.travel.modules.contract.mapper.BizContractMapper;
import net.jxdg.travel.modules.contract.mapper.BizContractTemplateMapper;
import net.jxdg.travel.modules.order.entity.BizOrder;
import net.jxdg.travel.modules.order.mapper.BizOrderMapper;
import net.jxdg.travel.security.SecurityUtils;
import net.jxdg.travel.tenant.TenantContext;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class ContractController {

    private final BizContractTemplateMapper templateMapper;
    private final BizContractMapper contractMapper;
    private final BizOrderMapper orderMapper;
    private final NumberGenerator numberGenerator;
    private final PdfUtil pdfUtil;

    @GetMapping("/contract/templates")
    public Result<List<BizContractTemplate>> templates() {
        return Result.ok(templateMapper.selectList(Wrappers.<BizContractTemplate>lambdaQuery().orderByDesc(BizContractTemplate::getId)));
    }

    @PostMapping("/contract/templates")
    @Audit(module = "合同", action = "保存合同模板")
    public Result<Void> saveTemplate(@RequestBody BizContractTemplate t) {
        if (t.getTenantId() == null) t.setTenantId(TenantContext.getTenantId());
        if (t.getId() == null) templateMapper.insert(t);
        else templateMapper.updateById(t);
        return Result.ok();
    }

    @DeleteMapping("/contract/templates/{id}")
    @Audit(module = "合同", action = "删除合同模板")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        templateMapper.deleteById(id);
        return Result.ok();
    }

    @GetMapping("/contracts")
    public Result<PageResult<BizContract>> page(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "20") long size,
                                                @RequestParam(required = false) Integer signStatus,
                                                @RequestParam(required = false) String keyword) {
        Page<BizContract> p = contractMapper.selectPage(new Page<>(page, size),
                Wrappers.<BizContract>lambdaQuery()
                        .eq(signStatus != null, BizContract::getSignStatus, signStatus)
                        .like(keyword != null && !keyword.isBlank(), BizContract::getContractNo, keyword)
                        .orderByDesc(BizContract::getId));
        return Result.ok(PageResult.of(p));
    }

    @GetMapping("/contracts/{id}")
    public Result<BizContract> detail(@PathVariable Long id) {
        BizContract c = contractMapper.selectById(id);
        if (c == null) throw BizException.notFound("合同不存在");
        return Result.ok(c);
    }

    @PostMapping("/contracts")
    @Audit(module = "合同", action = "生成电子合同")
    public Result<BizContract> create(@RequestBody Map<String, Object> body) {
        BizOrder order = body.get("orderId") == null ? null
                : orderMapper.selectById(Long.valueOf(String.valueOf(body.get("orderId"))));
        BizContractTemplate tpl = body.get("templateId") == null ? null
                : templateMapper.selectById(Long.valueOf(String.valueOf(body.get("templateId"))));
        if (tpl == null) {
            tpl = templateMapper.selectOne(Wrappers.<BizContractTemplate>lambdaQuery()
                    .eq(BizContractTemplate::getStatus, 1).last("limit 1"));
        }
        BizContract c = new BizContract();
        c.setTenantId(TenantContext.getTenantId());
        c.setContractNo(numberGenerator.next("CT"));
        c.setOrderId(order == null ? null : order.getId());
        c.setMemberId(order == null ? null : order.getMemberId());
        c.setTemplateId(tpl == null ? null : tpl.getId());
        c.setTitle("国内旅游合同 - " + (order == null ? "" : order.getProductTitle()));
        String content = tpl == null ? "<p>旅游合同</p>" : tpl.getContent();
        if (content != null && order != null) {
            content = content.replace("{{contact}}", order.getContactName() == null ? "" : order.getContactName())
                    .replace("{{product}}", order.getProductTitle() == null ? "" : order.getProductTitle())
                    .replace("{{date}}", order.getDepartDate() == null ? "" : order.getDepartDate().toString())
                    .replace("{{people}}", String.valueOf(order.getAdultCount() + order.getChildCount()));
        }
        c.setContent(content);
        c.setSignStatus(0);
        c.setFaceVerified(0);
        c.setSyncedGov(0);
        contractMapper.insert(c);
        return Result.ok(c);
    }

    /** 在线签署：签字 + 人脸校验（模拟）+ CA 证书（模拟），生成 PDF 归档 */
    @PostMapping("/contracts/{id}/sign")
    @Audit(module = "合同", action = "在线签署合同")
    public Result<Map<String, Object>> sign(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        BizContract c = contractMapper.selectById(id);
        if (c == null) throw BizException.notFound("合同不存在");
        if (c.getSignStatus() == 1) throw new BizException("合同已签署");
        boolean face = body == null || body.get("faceVerified") == null
                || Boolean.parseBoolean(String.valueOf(body.get("faceVerified")));
        if (!face) throw new BizException("人脸识别未通过，无法签署");
        c.setSignStatus(1);
        c.setSignTime(LocalDateTime.now());
        c.setFaceVerified(1);
        c.setCaCert("CA-MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        c.setSignUrl("/contracts/" + id + "/pdf");
        c.setPdfUrl("/api/contracts/" + id + "/pdf");
        c.setSyncedGov(1);
        contractMapper.updateById(c);
        return Result.ok(Map.of("contractNo", c.getContractNo(), "signTime", c.getSignTime(),
                "caCert", c.getCaCert(), "pdfUrl", c.getPdfUrl(), "mock", true));
    }

    @GetMapping("/contracts/{id}/pdf")
    public void pdf(@PathVariable Long id, HttpServletResponse response) throws Exception {
        BizContract c = contractMapper.selectById(id);
        if (c == null) throw BizException.notFound("合同不存在");
        List<String> lines = new ArrayList<>();
        lines.add("合同编号：" + c.getContractNo());
        lines.add("合同标题：" + c.getTitle());
        lines.add("签署状态：" + (c.getSignStatus() == 1 ? "已签署" : "待签署"));
        if (c.getSignTime() != null) lines.add("签署时间：" + c.getSignTime());
        if (c.getCaCert() != null) lines.add("CA 证书：" + c.getCaCert());
        lines.add("--------------------------------------------------");
        String plain = c.getContent() == null ? "" : c.getContent()
                .replaceAll("<[^>]+>", "\n").replace("&nbsp;", " ");
        for (String line : plain.split("\n")) {
            if (!line.isBlank()) lines.add(line.trim());
        }
        byte[] pdf = pdfUtil.render("电子旅游合同", lines);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", PdfUtil.attachment("电子合同-" + c.getContractNo() + ".pdf"));
        response.getOutputStream().write(pdf);
    }

    @PostMapping("/contracts/{id}/sync-gov")
    @Audit(module = "合同", action = "同步国家旅游局电子合同系统")
    public Result<Map<String, Object>> syncGov(@PathVariable Long id) {
        BizContract c = contractMapper.selectById(id);
        if (c == null) throw BizException.notFound("合同不存在");
        c.setSyncedGov(1);
        contractMapper.updateById(c);
        return Result.ok(Map.of("synced", true, "mock", true, "contractNo", c.getContractNo()));
    }

    @GetMapping("/contracts/warnings")
    public Result<List<BizContract>> unsignedWarnings() {
        return Result.ok(contractMapper.selectList(Wrappers.<BizContract>lambdaQuery()
                .eq(BizContract::getSignStatus, 0).orderByAsc(BizContract::getId)));
    }
}
