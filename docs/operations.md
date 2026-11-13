# 部署与运维

## 1. 环境要求

| 组件 | 最低配置 | 说明 |
|---|---|---|
| 应用服务器 | 4C8G | 后端 + Nginx |
| MySQL 8 | 4C8G / 200G SSD | 建议主从、每日备份 |
| Redis 7 | 2C4G | 缓存/锁/限流/编号 |
| RabbitMQ 3 | 2C4G | 通知/延迟任务 |
| MinIO | 4C8G / 500G | 或使用 OSS/COS/S3 |
| 域名 | 已备案 + HTTPS | 小程序/公众号需合法域名 |

## 2. Docker Compose 部署

```bash
cd deploy
cp .env.example .env
# 修改所有密码、JWT_SECRET、AES_KEY、微信配置
docker compose up -d --build
docker compose ps
```

服务与端口：

| 服务 | 容器 | 端口 |
|---|---|---|
| MySQL | travel-mysql | 3306 |
| Redis | travel-redis | 6379 |
| RabbitMQ | travel-rabbitmq | 5672 / 15672 |
| MinIO | travel-minio | 9000 / 9001 |
| 后端 | travel-backend | 8080 |
| 管理后台 | travel-admin | 80 |
| H5 门户 | travel-portal | 8082 |

首次启动 MySQL 会自动执行：

- `backend/src/main/resources/db/schema.sql`
- `backend/src/main/resources/db/data.sql`

> 生产环境务必修改演示密码，并删除/替换演示数据。

## 3. 反向代理与 HTTPS

管理后台与门户均内置 Nginx（`/api` 代理到 `backend:8080`）。如需统一入口：

```nginx
server {
    listen 443 ssl http2;
    server_name admin.example.com;
    ssl_certificate     /etc/nginx/cert/fullchain.pem;
    ssl_certificate_key /etc/nginx/cert/privkey.pem;
    location / { proxy_pass http://127.0.0.1:80; }
}

server {
    listen 443 ssl http2;
    server_name www.example.com;
    ssl_certificate     /etc/nginx/cert/fullchain.pem;
    ssl_certificate_key /etc/nginx/cert/privkey.pem;
    location / { proxy_pass http://127.0.0.1:8082; }
}
```

小程序合法域名需加入：

- request：`https://api.example.com`
- uploadFile：`https://api.example.com`
- downloadFile：`https://api.example.com`
- 公众号网页授权域名 / JS 安全域名：`www.example.com`

## 4. 数据库备份（每日 02:00，保留 7 份）

Linux crontab：

```cron
0 2 * * * /opt/travel/backup.sh >> /var/log/travel-backup.log 2>&1
```

`backup.sh`：

```bash
#!/usr/bin/env bash
set -e
DATE=$(date +%F_%H%M)
DIR=/opt/travel/backup
mkdir -p "$DIR"
docker exec travel-mysql mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" \
  --single-transaction --routines --triggers --default-character-set=utf8mb4 travel \
  | gzip > "$DIR/travel_$DATE.sql.gz"
find "$DIR" -name 'travel_*.sql.gz' -mtime +7 -delete
```

恢复演练：

```bash
gunzip -c /opt/travel/backup/travel_2026-01-01_0200.sql.gz | \
  docker exec -i travel-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" travel
```

演练要求：每季度一次，在独立环境恢复并校验租户数、订单数、最近订单可查。

## 5. 健康检查与监控

| 项 | 命令/地址 |
|---|---|
| 后端存活 | `GET /api/actuator/health` |
| 容器状态 | `docker compose ps` |
| MySQL | `mysqladmin ping`、慢查询日志 |
| Redis | `redis-cli -a <pwd> ping`、内存使用 |
| RabbitMQ | `http://host:15672` 队列堆积 |
| MinIO | `http://host:9001` 容量 |
| 备份 | 备份文件存在 + 大小 + 恢复抽检 |

建议接入 Prometheus + Grafana / 云监控，告警项：

- 5xx 比例 > 1%
- P95 响应 > 2s
- 数据库连接池使用率 > 80%
- Redis 内存 > 80%
- 队列堆积 > 1000
- 磁盘剩余 < 20%
- 备份失败

## 6. 日常运维

### 6.1 租户生命周期
- 开通：平台端 → 租户管理；自动创建 `admin/admin123`。
- 续期：到期前 30 天平台会有日志预警，手动续期。
- 停用：停用后全部用户不可登录，数据保留；恢复需启用。
- 配额：产品/用户/订单达到配额时业务接口拦截，需平台扩容。

### 6.2 对象存储切换
1. 平台/租户 → 系统设置 → 对象存储。
2. 选择驱动卡片 → 填参数 → 连接测试通过 → 保存并应用 → 二次确认。
3. 历史文件 URL 不变，仍可访问；新文件写新驱动。

### 6.3 微信配置
- 小程序/公众号 AppID、Secret、Token、EncodingAESKey、网页授权域名。
- 密钥 AES 加密存储，页面回掩码；修改全部审计。
- 模板消息模板 ID 需在微信公众平台申请；未配置时降级站内消息。
- 本地/验收环境保持 `WECHAT_MOCK=true`。

### 6.4 日志
- 应用日志：`docker compose logs -f backend`
- 审计日志：后台「日志审计」或 `sys_audit_log`
- 消息日志：`sys_message_log`（含失败原因、重试次数）
- 分账失败：`biz_split_record.status=2`，后台可重试

## 7. 故障排查

| 现象 | 排查 |
|---|---|
| 后端启动失败 `Unsupported character encoding` | JDBC URL 使用 `characterEncoding=UTF-8`（非 utf8mb4） |
| 登录 500 / 无法获取连接 | 检查 DB_HOST/账号/网络/连接池 |
| 租户数据串号 | 检查 `TenantContext` 是否清理、是否误加白名单 |
| 平台下钻无法保存 | 设计即只读；切回租户账号 |
| 编号重复 | 检查 Redis 是否可用、是否被清空导致序号回退；生产建议保留 Redis 持久化 |
| 下单提示余位不足 | 检查 `stock:schedule:{id}` 与数据库 sold/quota；清理错误缓存需谨慎 |
| 上传失败 | 存储连接测试、Bucket、AK/SK、文件大小、Nginx `client_max_body_size` |
| PDF 中文乱码 | 安装 `simhei.ttf` / Noto CJK，或把字体放入镜像 |
| 小程序请求被拦截 | 配置合法域名 / 开发工具关闭校验 / HTTPS |
| 公众号回调失败 | 服务器 URL、Token、EncodingAESKey、防火墙 |
| 模板消息失败 | 模板 ID、粉丝 openid、频率限制、日志 |

## 8. 安全加固

- 修改 `JWT_SECRET`、`AES_KEY`、数据库/Redis/RabbitMQ/MinIO 密码。
- 关闭不必要的公网端口（3306/6379/5672/9000 仅内网）。
- 全站 HTTPS；管理后台限制 IP 或加 VPN。
- 定期轮换微信 Secret、支付密钥；密钥不落代码库。
- 平台超管账号启用强密码 + 限制登录来源。
- 关闭 Swagger/Actuator 敏感端点或加鉴权。
- 每日备份 + 每季度恢复演练。
- 关注依赖漏洞（`mvn dependency-check`、`npm audit`）。

## 9. 上线检查清单

- [ ] 环境变量全部改为生产值，演示账号密码已修改
- [ ] 数据库初始化 + 演示数据清理
- [ ] HTTPS 证书、域名备案、小程序合法域名
- [ ] 微信 AppID/Secret、模板 ID、服务器配置
- [ ] 对象存储连接测试通过并切换
- [ ] 消息通道（模板消息/站内/短信）验证
- [ ] 租户 A/B 隔离用例通过
- [ ] 平台下钻只读验证
- [ ] 下单并发/余位/编号防重压测
- [ ] 备份任务 + 恢复演练
- [ ] 监控告警接入
- [ ] 真机（iOS/Android/微信开发者工具）回归
- [ ] 用户协议/隐私协议/资质公示
- [ ] 客服与应急联系人

## 10. 扩容建议

- 后端无状态，可多副本 + Nginx 负载均衡；WebSocket/文件上传注意会话粘性（当前无强依赖）。
- Redis 使用哨兵/集群；编号与锁依赖 Redis，需持久化。
- MySQL 读写分离；全国报表读 `biz_report_daily` 汇总表，避免实时全表扫描。
- 对象存储走 CDN；图片/合同 PDF 加缓存。
- RabbitMQ 消费者独立部署，通知失败进入重试队列/死信队列。
