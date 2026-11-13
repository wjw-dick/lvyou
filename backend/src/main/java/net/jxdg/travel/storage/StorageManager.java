package net.jxdg.travel.storage;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jxdg.travel.common.BizException;
import net.jxdg.travel.common.CryptoUtil;
import net.jxdg.travel.modules.system.entity.SysStorageConfig;
import net.jxdg.travel.modules.system.mapper.SysStorageConfigMapper;
import net.jxdg.travel.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 对象存储抽象层：本地 / S3 / 阿里云 OSS / 腾讯云 COS / MinIO。
 * OSS/COS/S3 使用 S3 兼容协议（MinIO SDK）实现，配置 endpoint + bucket + AK/SK。
 * 驱动配置按租户存储，密钥整体 AES 加密；切换驱动后历史文件仍按原 URL 可读（URL 持久化在附件表）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageManager {

    private final SysStorageConfigMapper storageConfigMapper;
    private final CryptoUtil cryptoUtil;
    private final ObjectMapper objectMapper;

    @Value("${app.storage.local-dir:./data/upload}")
    private String localDir;
    @Value("${app.storage.public-prefix:/upload}")
    private String publicPrefix;

    public record UploadResult(String url, String name, long size, String driver) {
    }

    public UploadResult upload(MultipartFile file, String bizType) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }
        Long tenantId = TenantContext.getTenantId();
        SysStorageConfig cfg = activeConfig(tenantId);
        String driver = cfg == null ? "local" : cfg.getDriver();
        Map<String, String> conf = parseConfig(cfg);
        String ext = extension(file.getOriginalFilename());
        String objectName = "%s/%s/%s.%s".formatted(
                bizType == null ? "common" : bizType,
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                UUID.randomUUID().toString().replace("-", ""), ext);
        try {
            String url;
            if ("local".equals(driver)) {
                Path target = Paths.get(localDir).resolve(objectName).toAbsolutePath().normalize();
                Files.createDirectories(target.getParent());
                try (InputStream in = file.getInputStream()) {
                    Files.copy(in, target);
                }
                url = publicPrefix + "/" + objectName.replace("\\", "/");
            } else {
                MinioClient client = client(conf);
                String bucket = conf.getOrDefault("bucket", "travel");
                if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                    client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                }
                try (InputStream in = file.getInputStream()) {
                    client.putObject(PutObjectArgs.builder()
                            .bucket(bucket).object(objectName)
                            .stream(in, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
                }
                String endpoint = conf.getOrDefault("endpoint", "");
                if (endpoint.startsWith("http")) {
                    url = endpoint.replaceAll("/$", "") + "/" + bucket + "/" + objectName;
                } else {
                    url = "http://" + endpoint.replaceAll("/$", "") + "/" + bucket + "/" + objectName;
                }
            }
            return new UploadResult(url, file.getOriginalFilename(), file.getSize(), driver);
        } catch (Exception e) {
            log.error("upload failed", e);
            throw new BizException("文件上传失败：" + e.getMessage());
        }
    }

    public void delete(String url) {
        if (url == null) return;
        Long tenantId = TenantContext.getTenantId();
        SysStorageConfig cfg = activeConfig(tenantId);
        if (cfg == null || "local".equals(cfg.getDriver())) {
            String prefix = publicPrefix + "/";
            if (url.startsWith(prefix)) {
                Path p = Paths.get(localDir).resolve(url.substring(prefix.length())).normalize();
                try {
                    Files.deleteIfExists(p);
                } catch (Exception ignored) {
                }
            }
            return;
        }
        Map<String, String> conf = parseConfig(cfg);
        try {
            MinioClient client = client(conf);
            String bucket = conf.getOrDefault("bucket", "travel");
            String object = url.substring(url.indexOf("/" + bucket + "/") + bucket.length() + 2);
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(object).build());
        } catch (Exception e) {
            log.warn("delete object failed: {}", e.getMessage());
        }
    }

    /** 连接测试：本地直接可写；云存储验证 bucket 可达。 */
    public boolean test(String driver, Map<String, String> conf) {
        try {
            if ("local".equals(driver)) {
                Path dir = Paths.get(conf.getOrDefault("baseDir", localDir)).toAbsolutePath();
                Files.createDirectories(dir);
                return Files.isWritable(dir);
            }
            MinioClient client = client(conf);
            return client.bucketExists(BucketExistsArgs.builder()
                    .bucket(conf.getOrDefault("bucket", "travel")).build());
        } catch (Exception e) {
            throw new BizException("连接测试失败：" + e.getMessage());
        }
    }

    private MinioClient client(Map<String, String> conf) {
        String endpoint = conf.getOrDefault("endpoint", "");
        if (endpoint.isBlank()) {
            throw new BizException("存储端点未配置");
        }
        if (!endpoint.startsWith("http")) endpoint = "http://" + endpoint;
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(conf.getOrDefault("accessKey", ""), conf.getOrDefault("secretKey", ""))
                .build();
    }

    private SysStorageConfig activeConfig(Long tenantId) {
        if (tenantId == null) return null;
        return storageConfigMapper.selectOne(Wrappers.<SysStorageConfig>lambdaQuery()
                .eq(SysStorageConfig::getTenantId, tenantId)
                .eq(SysStorageConfig::getActive, 1).last("limit 1"));
    }

    public Map<String, String> parseConfig(SysStorageConfig cfg) {
        if (cfg == null || cfg.getConfigJson() == null) return new HashMap<>();
        try {
            String plain = cryptoUtil.decrypt(cfg.getConfigJson());
            if (plain == null) plain = cfg.getConfigJson();
            return objectMapper.readValue(plain, new TypeReference<>() {
            });
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private static String extension(String name) {
        if (name == null || !name.contains(".")) return "bin";
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase();
    }

    public String encryptConfig(Map<String, String> config) {
        try {
            return cryptoUtil.encrypt(objectMapper.writeValueAsString(config));
        } catch (Exception e) {
            throw new BizException("存储配置解析失败");
        }
    }
}
