package net.jxdg.travel.modules.system.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import net.jxdg.travel.common.*;
import net.jxdg.travel.common.CryptoUtil;
import net.jxdg.travel.modules.system.entity.SysAttachment;
import net.jxdg.travel.modules.system.entity.SysStorageConfig;
import net.jxdg.travel.modules.system.mapper.SysAttachmentMapper;
import net.jxdg.travel.modules.system.mapper.SysStorageConfigMapper;
import net.jxdg.travel.security.SecurityUtils;
import net.jxdg.travel.storage.StorageManager;
import net.jxdg.travel.tenant.TenantContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequiredArgsConstructor
public class StorageController {

    private final StorageManager storageManager;
    private final SysStorageConfigMapper storageConfigMapper;
    private final SysAttachmentMapper attachmentMapper;

    /** 上传文件（图片/合同 PDF/结算单附件） */
    @PostMapping("/upload")
    @Audit(module = "对象存储", action = "上传文件")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                              @RequestParam(defaultValue = "common") String bizType,
                                              @RequestParam(required = false) Long bizId) {
        StorageManager.UploadResult r = storageManager.upload(file, bizType);
        SysAttachment a = new SysAttachment();
        a.setTenantId(TenantContext.getTenantId() == null ? 0L : TenantContext.getTenantId());
        a.setBizType(bizType);
        a.setBizId(bizId);
        a.setName(r.name());
        a.setUrl(r.url());
        a.setSize(r.size());
        a.setExt(r.name() != null && r.name().contains(".") ? r.name().substring(r.name().lastIndexOf('.') + 1) : null);
        a.setStorageDriver(r.driver());
        a.setCreatedBy(SecurityUtils.currentOrNull() == null ? null : SecurityUtils.userId());
        attachmentMapper.insert(a);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", a.getId());
        map.put("url", r.url());
        map.put("name", r.name());
        map.put("size", r.size());
        map.put("driver", r.driver());
        return Result.ok(map);
    }

    @GetMapping("/attachments")
    public Result<List<SysAttachment>> list(@RequestParam String bizType,
                                            @RequestParam(required = false) Long bizId) {
        return Result.ok(attachmentMapper.selectList(Wrappers.<SysAttachment>lambdaQuery()
                .eq(SysAttachment::getBizType, bizType)
                .eq(bizId != null, SysAttachment::getBizId, bizId)
                .orderByDesc(SysAttachment::getId)));
    }

    @DeleteMapping("/attachments/{id}")
    @Audit(module = "对象存储", action = "删除附件")
    public Result<Void> delete(@PathVariable Long id) {
        SysAttachment a = attachmentMapper.selectById(id);
        if (a != null) {
            storageManager.delete(a.getUrl());
            attachmentMapper.deleteById(id);
        }
        return Result.ok();
    }

    /** 5 驱动配置卡片数据（密钥回掩码） */
    @GetMapping("/system/storage/configs")
    public Result<List<Map<String, Object>>> configs() {
        List<SysStorageConfig> list = storageConfigMapper.selectList(Wrappers.<SysStorageConfig>lambdaQuery()
                .eq(SysStorageConfig::getTenantId, TenantContext.getTenantId()));
        List<Map<String, Object>> out = new ArrayList<>();
        for (SysStorageConfig c : list) {
            Map<String, String> conf = storageManager.parseConfig(c);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("driver", c.getDriver());
            m.put("active", c.getActive());
            Map<String, Object> safe = new LinkedHashMap<>();
            conf.forEach((k, v) -> safe.put(k, k.toLowerCase().contains("secret") || k.toLowerCase().contains("key")
                    ? CryptoUtil.maskSecret(v) : v));
            m.put("config", safe);
            out.add(m);
        }
        return Result.ok(out);
    }

    @PostMapping("/system/storage/test")
    public Result<Boolean> test(@RequestBody Map<String, Object> body) {
        String driver = String.valueOf(body.getOrDefault("driver", "local"));
        @SuppressWarnings("unchecked")
        Map<String, String> conf = (Map<String, String>) body.getOrDefault("config", new HashMap<>());
        return Result.ok(storageManager.test(driver, conf));
    }

    @PostMapping("/system/storage/save")
    @Audit(module = "对象存储", action = "保存存储配置")
    public Result<Void> save(@RequestBody Map<String, Object> body) {
        String driver = String.valueOf(body.getOrDefault("driver", "local"));
        @SuppressWarnings("unchecked")
        Map<String, String> conf = (Map<String, String>) body.getOrDefault("config", new HashMap<>());
        boolean activate = Boolean.parseBoolean(String.valueOf(body.getOrDefault("activate", false)));
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw BizException.forbidden("请先选择租户");
        SysStorageConfig exist = storageConfigMapper.selectOne(Wrappers.<SysStorageConfig>lambdaQuery()
                .eq(SysStorageConfig::getTenantId, tenantId).eq(SysStorageConfig::getDriver, driver).last("limit 1"));
        // 回掩码字段（****）不覆盖旧密钥
        Map<String, String> merged = new HashMap<>();
        if (exist != null) merged.putAll(storageManager.parseConfig(exist));
        conf.forEach((k, v) -> {
            if (v != null && !v.contains("****")) merged.put(k, v);
        });
        if (exist == null) {
            exist = new SysStorageConfig();
            exist.setTenantId(tenantId);
            exist.setDriver(driver);
            exist.setConfigJson(storageManager.encryptConfig(merged));
            exist.setActive(activate ? 1 : 0);
            storageConfigMapper.insert(exist);
        } else {
            exist.setConfigJson(storageManager.encryptConfig(merged));
            if (activate) exist.setActive(1);
            storageConfigMapper.updateById(exist);
        }
        if (activate) {
            // 切换驱动：其余驱动置为未启用
            final Long activeId = exist.getId();
            storageConfigMapper.selectList(Wrappers.<SysStorageConfig>lambdaQuery()
                            .eq(SysStorageConfig::getTenantId, tenantId))
                    .forEach(c -> {
                        if (!c.getId().equals(activeId) && c.getActive() == 1) {
                            c.setActive(0);
                            storageConfigMapper.updateById(c);
                        }
                    });
        }
        return Result.ok();
    }
}
