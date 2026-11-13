# 旅游数字化平台（SaaS）多端系统

> 面向全国中小型旅行社 / 地接社 / 组团社 / 旅游门店的 SaaS 多租户旅游经营管理平台。
> 以「线路产品」为主数据，覆盖产品上架、线路排期、游客预订、订单核销、导游带团、计调调度、供应商资源、财务结算、分销、电子合同、会员运营、景区票务、经营驾驶舱全流程。

## 技术栈

| 层 | 选型 |
|---|---|
| 后端 | Java 17 · Spring Boot 3.2.5 · MyBatis-Plus 3.5.7（TenantLineInnerInterceptor） · Spring Security + JWT |
| 数据 | MySQL 8（utf8mb4） · Redis 7 · RabbitMQ 3 |
| 存储 | 抽象对象存储层：本地 / S3 / 阿里云 OSS / 腾讯云 COS / MinIO |
| 管理后台 | Vue 3 + Vite + TypeScript + Element Plus（`admin-web/`） |
| H5 门户 | Vue 3 + Vite（`portal-web/`） |
| 小程序 | 微信原生小程序：游客端 + 导游端 + 景区员工端（`miniprogram/`） |
| 独立 App | Flutter：游客端 + 导游端（`app/`） |
| 部署 | Docker Compose（`deploy/`） |

## 目录结构

```
backend/      Spring Boot 后端（含 schema.sql / data.sql）
admin-web/    平台端 + 租户端管理后台
portal-web/   H5 公开门户 + 游客端 Web + 分销端 Web
miniprogram/  微信原生小程序（游客/导游/景区）
app/          Flutter 独立 App
deploy/       Docker Compose / nginx / 环境变量
docs/         用户文档、项目文档、技术文档、验收与运维文档
scripts/      开发辅助脚本（实体生成、数据库初始化）
```

## 本地启动

### 1. 数据库

```bash
# 方式一：使用自带初始化工具（读取 .env.local / 环境变量）
cd backend
mvn test-compile exec:java -Dexec.classpathScope=test -Dexec.mainClass=net.jxdg.travel.DbInit

# 方式二：手动执行
mysql -h DB_HOST -u DB_USER -p travel < backend/src/main/resources/db/schema.sql
mysql -h DB_HOST -u DB_USER -p travel < backend/src/main/resources/db/data.sql
```

### 2. 后端

```bash
cd backend
mvn spring-boot:run
# http://127.0.0.1:8080/api
```

### 3. 管理后台

```bash
cd admin-web
npm install
npm run dev     # http://127.0.0.1:80
```

### 4. H5 门户

```bash
cd portal-web
npm install
npm run dev     # http://127.0.0.1:8082
```

### 5. 小程序

用微信开发者工具导入 `miniprogram/`，修改 `app.js` 中 `baseUrl` 指向后端地址；本地开发需关闭「合法域名校验」。

### 6. Flutter App

```bash
cd app
flutter pub get
flutter run --dart-define=API_BASE=http://10.0.2.2:8080/api --dart-define=TENANT_CODE=HLJ
```

## 演示账号

| 端 | 租户编码 | 账号 | 密码 |
|---|---|---|---|
| 平台端（system 超管） | - | system | admin123 |
| 租户端（雪乡国旅） | HLJ | admin | admin123 |
| 租户端（西安丝路） | XA | admin | admin123 |
| 计调 | HLJ | planner | admin123 |
| 销售 | HLJ | sales | admin123 |
| 财务 | HLJ | finance | admin123 |
| 导游端 | HLJ | 手机号 13800000006 或 UnionID 绑定 | - |
| 景区员工端 | HLJ | scenic01 | admin123 |
| 供应商开放端 | HLJ | hotel01 / bus01 / rest01 | admin123 |
| 分销商 | HLJ | md001 | admin123 |

> 演示密码统一为 `admin123`（BCrypt 存储）；生产环境请立即修改，并更换 `JWT_SECRET` / `AES_KEY`。

## 文档

- [用户使用文档](docs/user-guide.md)
- [项目文档](docs/project.md)
- [技术文档](docs/technical.md)
- [API 接口文档](docs/api.md)
- [SaaS 多租户隔离说明](docs/saas-isolation.md)
- [验收对照](docs/acceptance.md)
- [部署与运维](docs/operations.md)

## 重要说明

- 微信真实 AppID/AppSecret、模板消息 ID、合法域名、微信支付、分账、OTA、保险、国家旅游局电子合同、直播平台、大模型 API 等属于**外部依赖**，代码已预留统一入口与配置项；未配置时使用 mock 通道，界面会明确标注「模拟/预留」。
- `checklist.md` 是验收依据；`docs/acceptance.md` 记录当前实现状态与外部条件项。
