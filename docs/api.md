# API 接口文档

> 基础路径：`/api`。统一响应 `{ code, message, data }`，`code=0` 成功。
> 鉴权：`Authorization: Bearer <JWT>`。公开端：`X-Tenant-Code: HLJ`。平台下钻：`X-Tenant-Id: <租户ID>`（只读）。
> 分页参数：`page`、`size`。

## 一、认证

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/auth/login` | 平台/租户登录 `{type: platform\|tenant, tenantCode, username, password}` |
| POST | `/auth/member-login` | 游客 UnionID 登录 `{tenantCode, code, unionId?, nickname}` |
| POST | `/auth/guide-login` | 导游登录 `{tenantCode, unionId?, phone?}` |
| POST | `/auth/scenic-login` | 景区员工登录 `{tenantCode, username, password}` |
| POST | `/auth/distributor-login` | 分销商登录 |
| POST | `/auth/supplier-login` | 供应商登录 |
| GET | `/auth/me` | 当前用户、权限、菜单 |
| POST | `/auth/logout` | 退出（前端清 token） |

## 二、平台端（system 超管）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/platform/dashboard` | 全国看板 + 租户明细 |
| GET | `/platform/tenants` | 租户分页（keyword/status） |
| POST | `/platform/tenants` | 开通租户（自动建 admin） |
| PUT | `/platform/tenants/{id}` | 编辑租户/配额 |
| POST | `/platform/tenants/{id}/status?status=` | 停用/启用 |
| POST | `/platform/tenants/{id}/renew?months=` | 续期 |
| GET/POST/DELETE | `/platform/dict`、`/platform/dict/{id}` | 全局字典 |
| GET/PUT | `/platform/params` | 平台参数 |
| GET | `/platform/audit` | 平台审计 |
| GET/POST/DELETE | `/platform/notices`、`/platform/notices/{id}` | 平台公告 |
| GET/POST | `/platform/users` | 平台用户 |
| GET | `/platform/report/daily?month=` | 全国日报 |

## 三、系统设置（租户）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET/POST/DELETE | `/system/users`、`/system/users/{id}` | 租户用户 |
| GET/POST/DELETE | `/system/roles`、`/system/roles/{id}` | 角色（含菜单 ID 列表） |
| GET | `/system/menus?scope=` | 菜单/按钮权限 |
| GET/POST/DELETE | `/system/dict`、`/system/dict/{id}` | 全局+租户字典 |
| GET/PUT | `/system/config` | 租户配置（密钥回掩码） |
| GET/PUT | `/system/nav?platform=` | 菜单可视化（admin/miniprogram/h5/app/mp） |
| GET | `/system/audit` | 租户审计 |
| GET/POST | `/system/notices` | 租户公告 |
| GET | `/system/dashboard` | 租户工作台 |

## 四、对象存储/附件

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/upload` | 上传（multipart `file`、`bizType`、`bizId?`） |
| GET | `/attachments?bizType=&bizId=` | 附件列表 |
| DELETE | `/attachments/{id}` | 删除附件 |
| GET | `/system/storage/configs` | 5 驱动配置（密钥掩码） |
| POST | `/system/storage/test` | 连接测试 |
| POST | `/system/storage/save` | 保存/启用（二次确认由前端） |

## 五、产品/目的地/排期

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/products` | 产品分页（keyword/category/status/days/destination/channel） |
| GET | `/products/{id}` | 详情（行程/渠道价/多语言） |
| POST | `/products` | 新增（含 itineraries/prices） |
| PUT | `/products/{id}` | 修改 |
| DELETE | `/products/{id}` | 删除 |
| POST | `/products/{id}/status?status=` | 上下架 |
| POST | `/products/{id}/channels` | 多渠道上架 |
| POST | `/products/{id}/i18n` | 多语言字段 |
| GET | `/products/export` | Excel 导出 |
| POST | `/products/import` | Excel 导入 |
| GET/POST/DELETE | `/destinations`、`/destinations/{id}` | 目的地/攻略 |
| GET | `/destinations/all` | 下拉 |
| GET | `/schedules` | 排期分页 |
| GET | `/schedules/calendar?startDate=&endDate=` | 日历 |
| GET | `/schedules/warnings` | 红黄灯预警 |
| POST/PUT/DELETE | `/schedules`、`/schedules/{id}` | 排期 CRUD |
| POST | `/schedules/{id}/status?status=` | 状态流转 |
| POST | `/schedules/generate` | 批量生成 |
| POST | `/schedules/{id}/guide?guideId=` | 指派导游并推送 |
| GET | `/schedules/{id}/plan` | 出团计划书 PDF |

## 六、公开端（门户/小程序/H5/App）

均需 `X-Tenant-Code`（query 参数 `tenantCode` 也可）。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/public/tenant` | 旅行社信息/配置 |
| GET | `/public/products` | 上架线路分页 |
| GET | `/public/products/{id}` | 线路详情（行程/团期） |
| GET | `/public/products/{id}/schedules?month=` | 团期余位 |
| GET | `/public/destinations` | 目的地 |
| GET | `/public/notices` | 公告 |
| GET | `/public/nav?platform=` | 导航配置 |
| GET | `/public/i18n?bizType=&bizId=` | 多语言内容 |
| POST | `/public/orders` | 游客下单（实名） |
| POST | `/public/order-query` | 订单查询（订单号+手机尾号，限流） |
| GET | `/public/contracts/verify?contractNo=` | 合同验真（摘要，限流） |
| POST | `/public/custom-request` | 定制需求 |
| GET | `/public/ugc?productId=` | 社区内容 |
| POST | `/public/ai/chat` | AI 客服（规则 + 转人工） |
| GET | `/public/currencies` | 币种汇率 |
| POST | `/pay/notify/{orderNo}` | 支付回调（模拟） |
| GET | `/pay/mock/{orderNo}` | 模拟支付 |

## 七、订单/支付/核销/退改

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/orders` | 订单分页 |
| GET | `/orders/{id}` | 详情（游客/轨迹/支付/退款/核销） |
| POST | `/orders` | 后台/销售下单 |
| POST | `/orders/{id}/pay?channel=` | 支付 |
| POST | `/orders/{id}/confirm` | 确认 |
| POST | `/orders/{id}/notify` | 出团通知 |
| POST | `/orders/{id}/cancel?reason=` | 取消 |
| POST | `/orders/verify` | 扫码核销 `{code, gate}` |
| POST | `/orders/{id}/refund` | 退改申请 |
| POST | `/orders/refunds/{id}/approve?approve=` | 审批退款 |
| GET | `/orders/tourists/export` | 游客名单 Excel |
| GET | `/orders/stat` | 今日/待付/待核销 |
| GET | `/member/orders` | 游客本人订单 |
| GET | `/member/orders/{id}` | 游客本人订单详情 |
| POST | `/member/orders` | 游客下单 |
| POST | `/member/orders/{id}/pay` | 游客支付 |
| POST | `/member/orders/{id}/refund` | 游客退改 |

## 八、导游

| 方法 | 路径 | 说明 |
|---|---|---|
| GET/POST/DELETE | `/guides`、`/guides/{id}` | 导游档案 |
| GET | `/guides/all` | 下拉 |
| GET | `/guide/assignments` | 带团安排 |
| POST | `/guide/assignments/{id}/confirm?accept=` | 导游确认/拒绝 |
| GET | `/guide/expenses` | 报账列表 |
| POST | `/guide/expenses/{id}/audit?approve=` | 报账审核 |
| GET | `/guide/expenses/summary` | 报账汇总 |
| GET | `/guide/app/schedules` | 导游端-我的团 |
| GET | `/guide/app/schedules/{id}/tourists` | 游客名单 |
| POST | `/guide/app/schedules/{id}/rollcall` | 客人点名 |
| GET/POST | `/guide/app/expenses` | 报账 |
| GET/POST | `/guide/app/cash` | 现金领款 |
| GET | `/guide/app/suppliers` | 供应商直拨 |
| GET | `/guide/app/report?scheduleId=` | 团报 |

## 九、计调/供应商

| 方法 | 路径 | 说明 |
|---|---|---|
| GET/POST/DELETE | `/suppliers`、`/suppliers/{id}` | 供应商 |
| GET | `/suppliers/all` | 下拉 |
| GET | `/suppliers/{id}/resources` | 资源 |
| GET/POST | `/resources` | 资源库存/协议价 |
| GET/POST | `/purchases` | 采购单 |
| POST | `/purchases/{id}/confirm?accept=` | 供应商确认 |
| GET | `/resource/match?scheduleId=` | OP 排团建议 |
| GET | `/supplier/me` | 供应商开放端资料 |
| GET | `/supplier/purchases` | 自身采购单 |
| POST | `/supplier/purchases/{id}/confirm` | 一键确认 |
| GET | `/supplier/settlements` | 自身结算 |
| GET | `/supplier/account` | 两套账 |
| GET/POST | `/supplier/resources` | 资源维护 |
| GET/POST | `/supplier/hotel/rooms` | 房态房价 |
| GET/POST | `/supplier/vehicles` | 车辆司机 |
| GET | `/supplier/dispatches` | 出车调度 |
| POST | `/supplier/dispatches/{id}/verify` | 出车核销 |

## 十、财务/结算/分账/保险

| 方法 | 路径 | 说明 |
|---|---|---|
| GET/POST | `/settlements` | 结算单 |
| GET | `/settlements/{id}` | 详情 |
| POST | `/settlements/{id}/confirm` | 确认 |
| POST | `/settlements/{id}/approve?approve=` | 审批 |
| POST | `/settlements/{id}/pay` | 打款 |
| GET | `/settlements/export` | 导出 |
| GET/POST | `/invoices` | 发票 |
| POST | `/invoices/{id}/issue` | 开票 |
| GET | `/split/accounts` | 分账接收方 |
| POST | `/split/accounts` | 保存 |
| GET | `/split/records` | 分账明细 |
| POST | `/split/execute?orderId=` | 执行分账 |
| POST | `/split/records/{id}/retry` | 重试 |
| GET/POST | `/insurance/products` | 保险产品 |
| GET/POST | `/insurance/orders` | 投保单 |
| POST | `/insurance/orders/{id}/issue` | 出单 |
| GET/POST | `/funds` | 质保金 |
| GET | `/funds/warnings` | 到期预警 |
| GET | `/commissions` | 佣金 |
| POST | `/commissions/{id}/settle` | 佣金入账 |
| GET | `/finance/summary` | 财务概要 |

## 十一、分销

| 方法 | 路径 | 说明 |
|---|---|---|
| GET/POST/DELETE | `/distributors`、`/distributors/{id}` | 分销商 |
| GET/POST | `/distributors/{id}/prices` | 分销报价 |
| GET | `/distributor/products` | 分销端报价 |
| GET | `/distributor/orders` | 分销订单 |
| GET | `/distributor/commission` | 佣金 |
| POST | `/distribution/orders/{orderId}/change-product?productId=` | 换线 |
| GET | `/distribution/schedules?sort=signups` | 按报名人数排序 |
| GET/POST | `/promotion/links` | 推广链接/二维码 |
| GET | `/public/promotion/{code}` | 推广落地/点击统计 |
| GET | `/promotion/stats` | 裂变统计 |

## 十二、合同/会员/营销/内容

| 方法 | 路径 | 说明 |
|---|---|---|
| GET/POST/DELETE | `/contract/templates`、`/contract/templates/{id}` | 合同模板 |
| GET/POST | `/contracts` | 合同 |
| GET | `/contracts/{id}` | 详情 |
| POST | `/contracts/{id}/sign` | 在线签署（人脸/CA 模拟） |
| GET | `/contracts/{id}/pdf` | PDF 下载 |
| POST | `/contracts/{id}/sync-gov` | 文旅局同步（预留） |
| GET | `/contracts/warnings` | 未签预警 |
| GET | `/members` | 会员分页 |
| GET | `/members/{id}` | 会员详情 |
| PUT | `/members/{id}` | 等级/积分调整 |
| GET | `/members/analysis` | 复购分析 |
| GET/POST | `/coupons` | 优惠券 |
| POST | `/coupons/{id}/issue?memberId=&all=` | 发放 |
| GET/POST | `/activities` | 营销活动 |
| GET | `/activities/stats` | 活动统计 |
| GET/POST | `/points/goods` | 积分商品 |
| POST | `/member/points/exchange` | 积分兑换 |
| GET | `/points/exchanges` | 兑换记录 |
| POST | `/points/exchanges/{id}/verify` | 核销 |
| GET/POST | `/member/cards` | 会员卡 |
| POST | `/member/cards/{id}/recharge` | 充值 |
| GET | `/member/profile` | 会员自助 |
| GET | `/member/coupons` | 我的券 |
| GET | `/member/points/logs` | 积分流水 |
| GET | `/member/messages` | 站内消息 |
| GET | `/ugc/posts` | UGC 后台 |
| POST | `/ugc/posts/{id}/audit?approve=` | 审核 |
| DELETE | `/ugc/posts/{id}` | 删除 |
| GET | `/ugc/reports` | 举报 |
| POST | `/ugc/reports/{id}/handle?takeDown=` | 处理 |
| POST | `/content/ugc` | 发布（敏感词） |
| POST | `/content/ugc/{id}/like?type=` | 点赞/收藏 |
| POST | `/content/ugc/{id}/comment` | 评论 |
| POST | `/content/ugc/{id}/report` | 举报 |
| GET | `/custom/requests` | 定制需求 |
| POST | `/custom/requests/{id}/quote` | 报价 |
| POST | `/custom/quotes/{id}/accept` | 接受报价 |
| POST | `/content/planner` | 行程规划器 |
| GET/POST | `/content/live` | 直播 |
| POST | `/content/poster?productId=` | 海报信息 |

## 十三、景区

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/scenic/list`、`/scenic/all`、`/scenic/{id}` | 景区 |
| POST | `/scenic` | 保存景区 |
| GET/POST | `/scenic/tickets` | 门票产品 |
| GET | `/scenic/tickets/{id}` | 门票详情 |
| GET/POST | `/scenic/stock` | 分时库存 |
| POST | `/public/tickets/order` | 门票下单（公开） |
| GET | `/scenic/ticket-orders` | 门票订单 |
| POST | `/scenic/verify` | 扫码验票 |
| GET | `/scenic/verify-records` | 验票记录 |
| GET | `/scenic/traffic` | 客流看板 |
| GET/POST | `/scenic/staff` | 员工账号 |
| GET | `/scenic/app/me` | 员工端资料 |
| GET | `/scenic/app/traffic` | 员工端客流 |
| GET | `/scenic/app/records` | 员工端记录 |
| GET | `/scenic/app/notices` | 员工端公告 |
| GET | `/scenic/reservations` | 分时预约 |
| POST | `/scenic/reservations/absent?date=` | 爽约处理 |
| GET/POST | `/scenic/areas` | 区域/拥堵 |
| GET | `/public/scenic/{id}/guide` | 智慧导览 |
| POST | `/scenic/guide-audio` | 导览点位 |
| GET/POST | `/scenic/merchants` | 二销商户 |
| GET | `/scenic/parking` | 停车记录 |
| POST | `/scenic/parking/entry`、`/scenic/parking/{id}/exit` | 进出场 |
| GET | `/scenic/revenue` | 门票营收 |

## 十四、OTA/报表/微信

| 方法 | 路径 | 说明 |
|---|---|---|
| GET/POST | `/ota/configs` | OTA 配置 |
| GET | `/ota/orders` | OTA 订单 |
| POST | `/ota/sync` | 同步 OTA 订单 |
| GET | `/ota/stats` | OTA 统计 |
| GET | `/report/business` | 经营报表 |
| GET | `/report/cockpit` | 驾驶舱 |
| GET | `/report/guide` | 导游报账 |
| GET | `/report/supplier` | 供应商 |
| GET | `/report/members` | 会员 |
| GET | `/report/export` | Excel 导出 |
| GET | `/system/wechat/configs`、POST/PUT | 微信配置 |
| GET/POST | `/system/message/templates` | 模板消息 |
| GET | `/system/message/logs` | 发送日志 |
| POST | `/system/wechat/menu/sync` | 同步菜单 |
| GET | `/system/wechat/materials`、`tags`、`fans` | 内容运营 |
| POST | `/system/wechat/mass` | 群发（模拟） |
| GET | `/public/wechat/oauth-url` | 网页授权 URL |
| GET | `/public/wechat/jsapi` | JS-SDK 签名 |
| GET/POST | `/wechat/callback` | 公众号服务器校验/事件 |
