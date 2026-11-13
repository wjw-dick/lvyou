# 技术文档

## 1. 技术栈与版本

| 组件 | 版本/说明 |
|---|---|
| JDK | 17 |
| Spring Boot | 3.2.5 |
| MyBatis-Plus | 3.5.7（`mybatis-plus-spring-boot3-starter`） |
| MySQL | 8.x，`utf8mb4_unicode_ci` |
| Redis | 7（Lettuce，max-active=200） |
| RabbitMQ | 3.x（手动 ACK、失败重试；预留通知队列） |
| JWT | jjwt 0.12.5 |
| 对象存储 | MinIO SDK 8.5.9（本地 + S3/OSS/COS/MinIO 统一协议） |
| Excel | EasyExcel 3.3.4 |
| PDF | PDFBox 2.0.31（优先加载系统中文 TTF） |
| 后台 | Vue 3.4 + Vite 5 + TypeScript + Element Plus 2.7 + ECharts 5 + WangEditor 5 |
| 门户 | Vue 3 + Vite（响应式 / SEO） |
| 小程序 | 微信原生（WXML/WXSS/JS） |
| App | Flutter 3（http / shared_preferences） |

## 2. 工程结构

```
backend/src/main/java/net/jxdg/travel/
├── TravelApplication.java
├── common/        Result、PageResult、BizException、GlobalExceptionHandler、
│                  BaseEntity、CryptoUtil、NumberGenerator、PdfUtil、Audit/AuditAspect
├── config/        MybatisPlusConfig（租户/分页/自动填充）、SecurityConfig、WebConfig、
│                  PublicTenantInterceptor
├── security/      LoginUser、JwtService、JwtAuthFilter、SecurityUtils
├── tenant/        TenantContext（ThreadLocal）
├── storage/       StorageManager（5 驱动）
├── wechat/        WechatService（code2Session / 模板消息 / JSAPI / 菜单）
└── modules/
    ├── system/        租户/平台用户/角色/菜单/字典/配置/存储/微信/消息/审计/附件
    ├── product/       产品、行程、渠道价、排期、目的地、公开端接口
    ├── order/         订单、游客、支付、核销、退改、支付回调
    ├── guide/         导游档案、带团安排、报账、现金领款
    ├── resource/      供应商、资源库存、采购单、OP 排团匹配
    ├── finance/       结算、明细、收付款计划、发票、审批、佣金、分账、保险、质保金
    ├── distribution/  分销商、渠道价、佣金、换线、按报名人数排序、推广裂变
    ├── contract/      合同模板、合同签署/PDF/文旅局同步
    ├── member/        会员、积分、优惠券、活动、积分商城、会员卡、储值
    ├── scenic/        景区、门票、库存、验票、客流、员工、预约、区域、导览、二销、停车
    ├── supplierportal/供应商开放端（酒店/车队/餐厅/地接 + 两套账）
    ├── content/       UGC、定制游、行程规划器、直播、海报
    ├── ota/           OTA 配置与订单同步
    ├── insurance/     保险产品/投保单（实体与接口）
    └── report/        经营报表、驾驶舱、日报汇总
```

`scripts/gen-entities.js` 从 `schema.sql` 生成 91 个实体与 Mapper，保证字段与表结构严格一致；重新生成命令：`node scripts/gen-entities.js`。

## 3. 请求生命周期与认证

```
HTTP → CorsFilter → JwtAuthFilter（解析 token / 平台下钻只读 / 写 TenantContext）
     → Spring Security 授权（authenticated + @PreAuthorize）
     → TenantInterceptor（仅 /public/**、/pay/**：按 X-Tenant-Code 解析租户）
     → Controller（@Audit 切面写审计）
     → MyBatis-Plus 租户插件（自动注入 tenant_id）
     → MySQL
```

### JWT Claims

`uid`、`username`、`realName`、`userType`、`tenantId`、`tenantCode`、`perms[]`。
`userType` 取值：`PLATFORM` / `TENANT` / `MEMBER` / `GUIDE` / `SCENIC` / `DISTRIBUTOR` / `SUPPLIER`。

- 平台超管 token 的 `tenantId=null`：全国视图（跨租户）。
- 平台端请求头带 `X-Tenant-Id`：下钻指定租户，`JwtAuthFilter` 将 `readOnly=true`，对该租户的**非 GET 请求直接 403**。
- 租户用户 token 固定携带本租户 `tenantId`，无法伪造其他租户。

## 4. SaaS 多租户实现（核心）

### 4.1 租户上下文

`TenantContext` 保存 `tenantId / platform / readOnly / tenantCode`。登录成功后由 JWT 过滤器写入，请求结束 `finally` 清理，避免线程池串租户。

### 4.2 MyBatis-Plus 租户插件

`MybatisPlusConfig#mybatisPlusInterceptor` 注册 `TenantLineInnerInterceptor`：

- `getTenantId()` 返回当前 `TenantContext.tenantId`；
- `ignoreTable()`：
  - 平台超管未选租户（`tenantId == null`）→ 忽略租户条件（跨租户统计）；
  - 白名单表 → 忽略；
  - 其余业务表 → 自动追加/注入 `tenant_id`。
- 白名单：`sys_tenant`、`sys_platform_user`、`sys_global_dict`、`sys_platform_param`、`sys_language`、`sys_menu`、`sys_sensitive_word`。
- 所有业务表均含 `tenant_id` 与组合索引（详见 `schema.sql`），避免漏写 where 造成越权。

### 4.3 平台下钻只读

`JwtAuthFilter` 在 `userType=PLATFORM` 且请求头 `X-Tenant-Id` 有值时：

1. 将租户上下文设为该租户；
2. 标记只读；
3. 非 `GET` 请求返回 403（错误信息「平台超管下钻租户为只读视图」）。

### 4.4 租户状态

登录时校验租户：编码存在、状态不为停用、未过期；过期时自动置为「到期」并拒绝登录。定时任务每日 02:00 再次检查并自动停用。

## 5. 数据模型

共 91 张表，分四类：

1. **平台级**：`sys_tenant`、`sys_platform_user`、`sys_global_dict`、`sys_platform_param`、`sys_language`、`sys_menu`、`sys_sensitive_word`。
2. **租户系统级**：`sys_tenant_user`、`sys_role`、`sys_user_role`、`sys_role_menu`、`sys_audit_log`、`sys_attachment`、`sys_config`、`sys_dict_item`、`sys_storage_config`、`sys_wechat_config`、`sys_message_template`、`sys_message_log`、`sys_nav`、`sys_content_i18n`、`sys_notice`。
3. **业务域**：产品/行程/渠道价/排期/目的地、订单/游客/支付/核销/退改/轨迹、供应商/资源/采购、导游/带团/报账/领款、结算/明细/计划/发票/审批/佣金/分账、分销/渠道价/推广、合同/模板、会员/积分/券/活动/商城/会员卡、景区/门票/库存/票单/客流/员工/预约/区域/导览/二销/停车、保险/质保金、OTA、UGC/定制/直播、日报/币种。
4. **公共**：编号走 Redis，附件统一 `sys_attachment`。

金额统一 `DECIMAL(12,2)`；敏感字段存 `*_enc` + `*_mask` 两列（AES + 脱敏展示）。

## 6. 关键业务流程

### 6.1 订单创建（防超售）

1. 校验产品上架、排期可订、人数 > 0。
2. 按渠道/分销商取价（`biz_product_price` / `biz_distributor_price`），计算总额。
3. 防重：Redis `SETNX` 5 秒（用户+产品+排期）。
4. Redis 原子扣减余位（第一道）。
5. 数据库条件更新：`UPDATE biz_schedule SET sold = sold + n WHERE id=? AND sold + n <= quota`（第二道，事务兜底）。
6. 更新排期状态（收客中 → 已成团 → 已满）。
7. 写订单、游客（证件/手机加密+脱敏）、订单轨迹。
8. 失败回滚 Redis 扣减。

### 6.2 支付

`POST /orders/{id}/pay`（或 `/member/orders/{id}/pay`）：
- 写支付流水（`biz_payment`），更新订单为已支付/待确认；
- 累积会员积分与消费；
- 发送「报名确认」模板消息（mock 通道落 `sys_message_log`）。

真实微信支付回调入口：`POST /pay/notify/{orderNo}`（需补充验签与金额校验）。

### 6.3 核销

`POST /orders/verify {code, gate}`：
- 按订单号查订单，校验已支付、未核销；
- 游客逐一核销，写 `biz_verify_record`（`tenant_id + code` 唯一，防重复）；
- 全部核销后订单状态 6（已核销）；
- 记录操作人、闸口、时间；写审计。

### 6.4 退改

- 按出团日期计算可退比例（≥7 天 100%、3-7 天 70%、3 天内 50%、出团后 0）；
- 提交 `biz_refund`（待审批）；
- 审批通过写退款流水、更新订单退款金额/支付状态、回补余位；
- 发送退款模板消息。

### 6.5 门票验票

`POST /scenic/verify`：
- 按二维码查 `biz_ticket_order`；
- 校验退款/过期/未到游玩日/已用次数（多日票按 `valid_days`）；
- 写 `biz_verify_record`（多日票 code 追加 `#n` 保证唯一）；
- 更新票单状态、写 `biz_traffic_stat`（分时/闸口客流）、预约状态；
- 每租户每分钟验票次数限流（>300 直接 429），防批量撞码。

### 6.6 编号

`NumberGenerator`：Redis `INCR num:{tenantId}:{type}:{year}`，格式 `{租户编码}-{类型}-{年}-{4位}`（如 `HLJ-TR-2026-0001`），Redis 不可用时降级时间戳；下单/结算接口另有 `SETNX` 防重。

### 6.7 对象存储

`StorageManager`：
- 每租户 `sys_storage_config` 保存 5 种驱动配置，`active=1` 为当前驱动；
- 配置 JSON 整体 AES 加密；
- 本地驱动写 `app.storage.local-dir` 并映射 `/upload/**`；
- 云驱动使用 MinIO SDK（S3 兼容）上传/删除/连接测试；
- 历史文件 URL 已持久化，切换驱动后仍可读。

### 6.8 定时任务

| 任务 | Cron | 说明 |
|---|---|---|
| 租户到期检查 | 每日 02:00 | 到期自动停用，提前 30 天预警日志 |
| 排期状态刷新 | 每小时 | 到出团日 → 已出团；过返程日 → 已完结 |
| 订单自动完结 | 每日 03:00 | 已核销 7 天 → 已完结 |
| 经营日报 | 每日 01:00 | 按租户写入 `biz_report_daily`，供全国看板 |

## 7. 安全设计

- 密码 BCrypt；JWT 有效期 720 分钟，密钥 ≥32 字节。
- 敏感字段 AES（密钥来自 `AES_KEY`，SHA-256 派生），展示脱敏（身份证保留前 4 后 4、手机前 3 后 4）。
- 配置密钥页面回掩码（`****`），未修改不覆盖。
- 审计切面记录模块/动作/对象/IP/结果；平台与租户均可检索。
- 接口限流：公开订单查询 10 次/分钟/IP、合同验真 20 次/分钟/IP、验票 300 次/分钟/租户。
- 防重：下单 `SETNX`、核销唯一索引、结算/退款唯一单号。
- 平台下钻只读；租户越权访问返回 403 并审计。
- UGC 敏感词过滤 + 人工审核 + 举报下架。
- AI 客服对退改/退款/订单结论强制转人工，并提示「仅供参考」。

## 8. 接口约定

- 统一前缀 `/api`。
- 响应：`{ "code": 0, "message": "success", "data": ... }`；业务错误 HTTP 4xx + code。
- 分页：`page`、`size`，返回 `{ records, total, current, size }`。
- 鉴权：`Authorization: Bearer <token>`；公开端加 `X-Tenant-Code`；平台下钻加 `X-Tenant-Id`。
- 审计：写操作 Controller 方法标注 `@Audit(module, action)`。

## 9. 前端实现要点

### 9.1 管理后台
- 左侧折叠菜单 + 面包屑 + 多页签 + 平台租户切换器；
- 菜单来自后端（按角色过滤），平台端/租户端同一套路由；
- 通用 `CrudPage` 组件：筛选区、斑马纹表格、右下分页、弹窗表单、图片/富文本字段；
- `RichEditor`（WangEditor，粘贴图片自动走对象存储）；
- `ImageUpload`（独立卡片块、拖拽、多图、预览、删除、进度）；
- 对象存储页严格按「5 卡片 → 选中滑出表单 → 连接测试通过才可保存 → 二次确认」；
- 报表用 ECharts；平台端租户切换后自动带 `X-Tenant-Id` 只读。

### 9.2 H5 门户
- 公开接口全部走 `X-Tenant-Code`；SEO 标题/描述/hreflang；
- 页面：首页、线路列表/详情/预订、目的地攻略、机构官网、订单查询、登录、会员中心、分销中心、分享落地页；
- 移动端自适应，预订表单统一输入框 token。

### 9.3 小程序
- `tp-input` 统一组件：高 88rpx、字号 28rpx、文字 #303133、placeholder #909399、聚焦主色描边、`adjust-position` 防键盘遮挡；
- tabBar 文案从 `/public/nav` 拉取并 `wx.setTabBarItem` 更新（页面路径随包固定）；
- 游客/导游/景区三种登录入口，登录态存本地并注入 `Authorization`；
- 验票支持 `wx.scanCode` 扫码与手动输入。

### 9.4 Flutter App
- 底部导航四 Tab（首页/线路/订单/我的），主题色 #1f6feb；
- 统一 `InputDecorationTheme`（圆角 8、聚焦主色、提示 #909399）；
- 游客端预订、订单、会员；导游端带团/名单/点名/团报；
- `API_BASE` / `TENANT_CODE` 通过 `--dart-define` 注入。

## 10. 验证与测试

已执行：

```bash
cd backend && mvn -q -DskipTests compile        # 编译通过
cd backend && mvn test-compile exec:java ...    # schema + data 初始化成功
cd admin-web && npm run build                   # 类型检查 + 构建通过
node scripts/gen-entities.js                    # 91 实体/Mapper 生成
```

接口回归（本地）：

| 用例 | 结果 |
|---|---|
| 租户登录 HLJ/admin | 200，返回 54 权限 / 68 菜单 |
| 工作台/产品/排期/订单/会员/景区/报表 | 200 |
| 租户访问 `/platform/dashboard` | 403（越权拦截） |
| 公开线路 `/public/products` | 200，3 条线路 |
| 平台登录并下钻租户 | 只读（非 GET 403） |

建议补充：JUnit + MockMvc 集成测试、租户 A/B 隔离用例、并发下单压力测试、真机回归。

## 11. 扩展点

- 微信真实对接：`WechatService`（code2Session、access_token、模板消息、JSAPI、菜单）。
- 微信支付/分账：`PayController` 回调验签 + `FinanceController#executeSplit` 对接分账 API。
- OTA：`OtaController#sync` 统一字段映射。
- 保险：`FinanceController` 投保接口对接保司 API。
- 电子合同：`ContractController#syncGov` 对接国家旅游局系统。
- AI：`PublicController#aiChat` 替换为真实大模型调用，保留转人工边界。
- 推送：`sys_message_log` 增加 APNs/FCM/短信通道实现。
