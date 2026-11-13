# SaaS 多租户隔离说明

## 1. 隔离目标

1. 租户 A 与租户 B 的产品、订单、排期、游客、导游、供应商、结算、会员、景区、门票等数据完全不可见、不可操作。
2. 平台超管可跨租户统计，并可下钻任意租户**只读**查看。
3. 游客/导游/景区员工/供应商/分销商只能访问本人/本社/本景区/本供应商数据。
4. 任何越权返回 403，并写入审计日志。

## 2. 实现机制

| 机制 | 实现 |
|---|---|
| 租户上下文 | `TenantContext`（ThreadLocal），由 `JwtAuthFilter` 从 JWT 解析，`finally` 清理 |
| 自动 SQL 过滤 | `TenantLineInnerInterceptor` 对非白名单表自动追加 `tenant_id = ?`，插入自动补 `tenant_id` |
| 平台白名单 | `sys_tenant`、`sys_platform_user`、`sys_global_dict`、`sys_platform_param`、`sys_language`、`sys_menu`、`sys_sensitive_word` |
| 全国视图 | 平台超管 token `tenantId=null`，插件跳过租户条件 |
| 下钻只读 | 平台端请求头 `X-Tenant-Id` → 上下文设为该租户且 `readOnly=true`；`JwtAuthFilter` 对非 GET 直接 403 |
| 登录校验 | 租户编码 + 状态 + 到期时间；停用/到期拒绝登录 |
| 业务归属校验 | 游客/导游/景区员工/供应商/分销商接口先校验 `userType`，再按 `id` 校验数据归属 |
| 唯一约束 | 订单号/产品编号/出团编号/结算单号/核销码均按 `(tenant_id, xxx)` 唯一 |
| 审计 | 写操作 `@Audit` 记录操作人、租户、IP、对象、结果 |

## 3. 租户上下文切换规则

```
租户用户登录        → tenantId = 本租户，platform = false
平台超管（全国）     → tenantId = null，platform = true
平台超管（下钻）     → tenantId = X-Tenant-Id，platform = true，readOnly = true
公开端（门户/小程序） → tenantId = X-Tenant-Code 解析结果
```

## 4. 越权拦截点

| 场景 | 处理 |
|---|---|
| 租户账号访问其他租户数据（即使伪造 ID） | 插件追加 `tenant_id`，查不到数据 → 404；写操作受影响行数为 0 |
| 租户账号调用平台接口 | `SecurityUtils.requirePlatform()` + 权限 → 403，审计 |
| 平台超管下钻后修改 | `JwtAuthFilter` 非 GET → 403，不进入业务 |
| 游客访问他人订单 | `member/orders/{id}` 校验 `order.memberId == token.uid` → 403 |
| 导游访问其他社团 | 导游端按 `guideId = token.uid` 查询，插件再按租户过滤 |
| 景区员工访问其他景区 | `scenicId` 来自身份，查询按员工所属景区收敛 |
| 供应商确认他人采购单 | 校验 `purchase.supplierId == token.uid` → 403 |
| 分销商查看他人佣金 | 按 `distributorId = token.uid` 过滤 |
| 公开订单查询撞库 | 订单号 + 手机尾号 + 10 次/分钟/IP 限流 |
| 合同验真批量探测 | 摘要返回 + 20 次/分钟/IP 限流 |

## 5. 数据表要求

所有业务表必须包含：

```sql
`tenant_id` BIGINT NOT NULL,
KEY `idx_tenant_xxx` (`tenant_id`, ...)
```

新增业务表时：

1. 在 `schema.sql` 添加 `tenant_id` 与索引；
2. 运行 `node scripts/gen-entities.js` 重新生成实体；
3. **不要**把业务表加入 `MybatisPlusConfig.PLATFORM_TABLES`；
4. 为列表/详情接口编写租户内唯一校验（编号、核销码等）。

## 6. 验证证据（本地实测）

```bash
# 租户登录
POST /api/auth/login {"type":"tenant","tenantCode":"HLJ","username":"admin","password":"admin123"}
→ 200，54 项权限、68 个菜单

# 租户访问平台接口
GET /api/platform/dashboard（携带租户 token）
→ 403 "无权访问该资源"

# 公开端租户隔离
GET /api/public/products（X-Tenant-Code: HLJ）
→ 200，仅返回 HLJ 的 3 条线路
```

建议上线前执行专项用例：

1. 租户 A/B 各建产品、订单、会员、结算，互相查询/修改全部失败。
2. 平台超管全国视图数据 = A + B 之和；下钻 A 只读，修改返回 403。
3. 停用租户 A 后，其全部账号登录失败，数据仍在。
4. 并发下单 100 次，余位不出现负数，订单号无重复。
5. 游客 A 访问游客 B 订单/合同/核销凭证，全部 403/404。
6. 供应商 A 确认供应商 B 的采购单，403。
7. 景区 A 员工扫描景区 B 门票，验票失败。

## 7. 注意事项

- 平台超管跨租户统计使用实时聚合；数据量大时建议读取 `biz_report_daily` 汇总表。
- 平台超管默认只读下钻；如需“代运营修改”，必须显式新增受控接口并二次确认 + 审计，当前未开放。
- 跨租户唯一（如全局订单号）不采用，编号按租户内唯一，前缀含租户编码便于识别。
- 如果使用多数据源/分库，需要将租户插件替换为分库路由，并保持 `TenantContext` 语义不变。
