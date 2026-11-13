-- ============================================================
-- 旅游数字化平台（SaaS）MySQL 8 初始化脚本
-- 字符集 utf8mb4 / utf8mb4_unicode_ci
-- 说明：除平台级白名单表外，所有业务表含 tenant_id 并由租户插件强制注入
-- ============================================================
CREATE DATABASE IF NOT EXISTS `travel` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `travel`;

-- ---------- 平台级表（租户插件白名单） ----------
DROP TABLE IF EXISTS `sys_tenant`;
CREATE TABLE `sys_tenant` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(32) NOT NULL COMMENT '租户编码，用于编号前缀',
  `name` VARCHAR(128) NOT NULL,
  `province` VARCHAR(64) DEFAULT NULL,
  `city` VARCHAR(64) DEFAULT NULL,
  `contact` VARCHAR(64) DEFAULT NULL,
  `phone` VARCHAR(32) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1试用 2正常 3停用 4到期',
  `open_time` DATETIME DEFAULT NULL,
  `expire_time` DATETIME DEFAULT NULL,
  `quota_user` INT NOT NULL DEFAULT 20,
  `quota_product` INT NOT NULL DEFAULT 200,
  `quota_order` INT NOT NULL DEFAULT 100000,
  `quota_storage` BIGINT NOT NULL DEFAULT 10737418240 COMMENT '字节',
  `remark` VARCHAR(255) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';

DROP TABLE IF EXISTS `sys_platform_user`;
CREATE TABLE `sys_platform_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password` VARCHAR(128) NOT NULL,
  `real_name` VARCHAR(64) DEFAULT NULL,
  `phone` VARCHAR(32) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `role_code` VARCHAR(32) NOT NULL DEFAULT 'platform_admin',
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台用户（system 超管）表';

DROP TABLE IF EXISTS `sys_global_dict`;
CREATE TABLE `sys_global_dict` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `dict_type` VARCHAR(64) NOT NULL,
  `dict_label` VARCHAR(128) NOT NULL,
  `dict_value` VARCHAR(128) NOT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  `remark` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_global_dict` (`dict_type`,`dict_value`),
  KEY `idx_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全局标准字典（平台维护）';

DROP TABLE IF EXISTS `sys_platform_param`;
CREATE TABLE `sys_platform_param` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `param_key` VARCHAR(64) NOT NULL,
  `param_value` VARCHAR(512) DEFAULT NULL,
  `value_type` VARCHAR(16) NOT NULL DEFAULT 'string',
  `remark` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_param_key` (`param_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台参数';

DROP TABLE IF EXISTS `sys_language`;
CREATE TABLE `sys_language` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(16) NOT NULL,
  `name` VARCHAR(64) NOT NULL,
  `enabled` TINYINT NOT NULL DEFAULT 1,
  `sort` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lang_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多语言列表';

DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `parent_id` BIGINT NOT NULL DEFAULT 0,
  `title` VARCHAR(64) NOT NULL,
  `title_en` VARCHAR(64) DEFAULT NULL,
  `name` VARCHAR(64) DEFAULT NULL COMMENT '路由名',
  `path` VARCHAR(128) DEFAULT NULL,
  `component` VARCHAR(128) DEFAULT NULL,
  `icon` VARCHAR(64) DEFAULT NULL,
  `perm` VARCHAR(128) DEFAULT NULL COMMENT '权限标识 product:add',
  `type` CHAR(1) NOT NULL DEFAULT 'M' COMMENT 'M目录 C菜单 F按钮',
  `scope` VARCHAR(16) NOT NULL DEFAULT 'tenant' COMMENT 'platform/tenant',
  `sort` INT NOT NULL DEFAULT 0,
  `visible` TINYINT NOT NULL DEFAULT 1,
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`),
  KEY `idx_scope` (`scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单/按钮权限（全局定义）';

DROP TABLE IF EXISTS `sys_sensitive_word`;
CREATE TABLE `sys_sensitive_word` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `word` VARCHAR(64) NOT NULL,
  `level` TINYINT NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='UGC 敏感词（平台级）';

-- ---------- 租户级系统表 ----------
DROP TABLE IF EXISTS `sys_tenant_user`;
CREATE TABLE `sys_tenant_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `username` VARCHAR(64) NOT NULL,
  `password` VARCHAR(128) NOT NULL,
  `real_name` VARCHAR(64) DEFAULT NULL,
  `phone` VARCHAR(32) DEFAULT NULL,
  `email` VARCHAR(128) DEFAULT NULL,
  `avatar` VARCHAR(255) DEFAULT NULL,
  `is_admin` TINYINT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_user` (`tenant_id`,`username`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户用户表';

DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `code` VARCHAR(64) NOT NULL,
  `name` VARCHAR(64) NOT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role` (`tenant_id`,`code`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表（租户级/平台级）';

DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  `user_type` VARCHAR(16) NOT NULL DEFAULT 'tenant',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`,`user_type`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `role_id` BIGINT NOT NULL,
  `menu_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`,`menu_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联';

DROP TABLE IF EXISTS `sys_audit_log`;
CREATE TABLE `sys_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `operator_id` BIGINT DEFAULT NULL,
  `operator_name` VARCHAR(64) DEFAULT NULL,
  `user_type` VARCHAR(16) DEFAULT NULL,
  `module` VARCHAR(64) DEFAULT NULL,
  `action` VARCHAR(64) DEFAULT NULL,
  `target` VARCHAR(255) DEFAULT NULL,
  `detail` TEXT,
  `ip` VARCHAR(64) DEFAULT NULL,
  `result` VARCHAR(16) DEFAULT 'success',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_time` (`tenant_id`,`created_at`),
  KEY `idx_operator` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志';

DROP TABLE IF EXISTS `sys_attachment`;
CREATE TABLE `sys_attachment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `biz_type` VARCHAR(32) DEFAULT NULL,
  `biz_id` BIGINT DEFAULT NULL,
  `name` VARCHAR(255) NOT NULL,
  `url` VARCHAR(512) NOT NULL,
  `size` BIGINT DEFAULT 0,
  `ext` VARCHAR(32) DEFAULT NULL,
  `storage_driver` VARCHAR(32) DEFAULT NULL,
  `created_by` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_biz` (`tenant_id`,`biz_type`,`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件表';

DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `config_key` VARCHAR(128) NOT NULL,
  `config_value` TEXT,
  `encrypted` TINYINT NOT NULL DEFAULT 0,
  `remark` VARCHAR(255) DEFAULT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_config` (`tenant_id`,`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户配置（含密钥加密）';

DROP TABLE IF EXISTS `sys_dict_item`;
CREATE TABLE `sys_dict_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `dict_type` VARCHAR(64) NOT NULL,
  `label` VARCHAR(128) NOT NULL,
  `value` VARCHAR(128) NOT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_dict` (`tenant_id`,`dict_type`,`value`),
  KEY `idx_tenant_type` (`tenant_id`,`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户自定义字典';

DROP TABLE IF EXISTS `sys_storage_config`;
CREATE TABLE `sys_storage_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `driver` VARCHAR(32) NOT NULL COMMENT 'local/s3/oss/cos/minio',
  `config_json` TEXT NOT NULL COMMENT 'AK/SK 等加密后的配置',
  `active` TINYINT NOT NULL DEFAULT 0,
  `updated_by` BIGINT DEFAULT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_driver` (`tenant_id`,`driver`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对象存储配置';

DROP TABLE IF EXISTS `sys_wechat_config`;
CREATE TABLE `sys_wechat_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `config_key` VARCHAR(128) NOT NULL,
  `config_value` TEXT,
  `encrypted` TINYINT NOT NULL DEFAULT 0,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_wechat` (`tenant_id`,`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信配置（密钥 AES 加密）';

DROP TABLE IF EXISTS `sys_message_template`;
CREATE TABLE `sys_message_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `biz_type` VARCHAR(64) NOT NULL COMMENT 'booking/group/depart/settle/refund/marketing/contract',
  `template_id` VARCHAR(128) DEFAULT NULL,
  `title` VARCHAR(128) DEFAULT NULL,
  `fields_json` TEXT,
  `channel` VARCHAR(32) NOT NULL DEFAULT 'mp',
  `enabled` TINYINT NOT NULL DEFAULT 1,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_biz` (`tenant_id`,`biz_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息模板配置';

DROP TABLE IF EXISTS `sys_message_log`;
CREATE TABLE `sys_message_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `member_id` BIGINT DEFAULT NULL,
  `open_id` VARCHAR(64) DEFAULT NULL,
  `biz_type` VARCHAR(64) DEFAULT NULL,
  `channel` VARCHAR(32) DEFAULT NULL,
  `content` TEXT,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待发 1成功 2失败',
  `retry_count` INT NOT NULL DEFAULT 0,
  `error_msg` VARCHAR(512) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息发送日志';

DROP TABLE IF EXISTS `sys_nav`;
CREATE TABLE `sys_nav` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `platform` VARCHAR(32) NOT NULL COMMENT 'admin/miniprogram/h5/app/scenic',
  `content` LONGTEXT,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_platform` (`tenant_id`,`platform`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单/导航可视化配置';

DROP TABLE IF EXISTS `sys_content_i18n`;
CREATE TABLE `sys_content_i18n` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `biz_type` VARCHAR(32) NOT NULL,
  `biz_id` BIGINT NOT NULL,
  `field` VARCHAR(64) NOT NULL,
  `lang` VARCHAR(16) NOT NULL,
  `content` LONGTEXT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_i18n` (`tenant_id`,`biz_type`,`biz_id`,`field`,`lang`),
  KEY `idx_biz` (`biz_type`,`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容多语言字段';

DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0,
  `title` VARCHAR(255) NOT NULL,
  `content` LONGTEXT,
  `type` VARCHAR(32) DEFAULT 'notice',
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_by` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台/租户公告';

-- ---------- 主数据 ----------
DROP TABLE IF EXISTS `biz_destination`;
CREATE TABLE `biz_destination` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `parent_id` BIGINT NOT NULL DEFAULT 0,
  `name` VARCHAR(128) NOT NULL,
  `name_en` VARCHAR(128) DEFAULT NULL,
  `type` VARCHAR(32) NOT NULL DEFAULT 'region' COMMENT 'region/scenic/strategy',
  `province` VARCHAR(64) DEFAULT NULL,
  `city` VARCHAR(64) DEFAULT NULL,
  `cover` VARCHAR(512) DEFAULT NULL,
  `summary` VARCHAR(512) DEFAULT NULL,
  `content` LONGTEXT,
  `longitude` DECIMAL(10,6) DEFAULT NULL,
  `latitude` DECIMAL(10,6) DEFAULT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`),
  KEY `idx_tenant_type` (`tenant_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='目的地/景点/攻略';

DROP TABLE IF EXISTS `biz_supplier`;
CREATE TABLE `biz_supplier` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `type` VARCHAR(32) NOT NULL COMMENT 'hotel/bus/restaurant/land/ticket',
  `contact` VARCHAR(64) DEFAULT NULL,
  `phone` VARCHAR(32) DEFAULT NULL,
  `email` VARCHAR(128) DEFAULT NULL,
  `username` VARCHAR(64) DEFAULT NULL COMMENT '供应商开放端账号',
  `password` VARCHAR(128) DEFAULT NULL,
  `address` VARCHAR(255) DEFAULT NULL,
  `settlement_period` INT NOT NULL DEFAULT 30,
  `commission_rate` DECIMAL(8,4) NOT NULL DEFAULT 0,
  `account_name` VARCHAR(128) DEFAULT NULL,
  `bank_account` VARCHAR(128) DEFAULT NULL,
  `account_no` VARCHAR(64) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `remark` VARCHAR(512) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_type` (`tenant_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商档案';

DROP TABLE IF EXISTS `biz_supplier_resource`;
CREATE TABLE `biz_supplier_resource` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `supplier_id` BIGINT NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `type` VARCHAR(32) NOT NULL COMMENT 'room/bus/meal/ticket',
  `spec` VARCHAR(128) DEFAULT NULL,
  `unit` VARCHAR(32) DEFAULT '间',
  `stock` INT NOT NULL DEFAULT 0,
  `sold` INT NOT NULL DEFAULT 0,
  `cost_price` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `sale_price` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `available_date` DATE DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `remark` VARCHAR(255) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_supplier` (`tenant_id`,`supplier_id`),
  KEY `idx_tenant_date` (`tenant_id`,`available_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商资源/库存/协议价';

DROP TABLE IF EXISTS `biz_product`;
CREATE TABLE `biz_product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `product_no` VARCHAR(64) NOT NULL,
  `category` VARCHAR(32) NOT NULL DEFAULT 'line' COMMENT 'line/visa/flight/hotel/ticket/car/cruise/goods',
  `title` VARCHAR(255) NOT NULL,
  `subtitle` VARCHAR(255) DEFAULT NULL,
  `days` INT NOT NULL DEFAULT 1,
  `departure` VARCHAR(64) DEFAULT NULL,
  `destination` VARCHAR(128) DEFAULT NULL,
  `destination_id` BIGINT DEFAULT NULL,
  `adult_price` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `child_price` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `single_room_diff` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `cost_price` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `include_text` TEXT,
  `exclude_text` TEXT,
  `refund_rule` TEXT,
  `meeting_point` VARCHAR(255) DEFAULT NULL,
  `cover` VARCHAR(512) DEFAULT NULL,
  `images` TEXT COMMENT 'JSON 数组',
  `guide_content` LONGTEXT COMMENT '富文本攻略',
  `stock` INT NOT NULL DEFAULT 0,
  `sold` INT NOT NULL DEFAULT 0,
  `min_group` INT NOT NULL DEFAULT 10,
  `channels` VARCHAR(128) NOT NULL DEFAULT 'miniprogram,mp,h5,app' COMMENT '上架渠道',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0下架 1上架',
  `sort` INT NOT NULL DEFAULT 0,
  `remark` VARCHAR(512) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_by` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_product_no` (`tenant_id`,`product_no`),
  KEY `idx_tenant_status` (`tenant_id`,`status`),
  KEY `idx_tenant_category` (`tenant_id`,`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品/线路主表';

DROP TABLE IF EXISTS `biz_product_itinerary`;
CREATE TABLE `biz_product_itinerary` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `day_no` INT NOT NULL,
  `title` VARCHAR(255) DEFAULT NULL,
  `spots` VARCHAR(512) DEFAULT NULL,
  `hotel` VARCHAR(255) DEFAULT NULL,
  `meals` VARCHAR(255) DEFAULT NULL,
  `transport` VARCHAR(255) DEFAULT NULL,
  `guide` VARCHAR(128) DEFAULT NULL,
  `accommodation` VARCHAR(128) DEFAULT NULL,
  `content` LONGTEXT,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product` (`tenant_id`,`product_id`,`day_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行程 D1~Dn';

DROP TABLE IF EXISTS `biz_product_price`;
CREATE TABLE `biz_product_price` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `channel` VARCHAR(32) NOT NULL COMMENT 'miniprogram/mp/h5/app/ota/distributor',
  `channel_id` BIGINT DEFAULT NULL COMMENT '分销商ID或OTA ID',
  `adult_price` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `child_price` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `single_room_diff` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_channel` (`tenant_id`,`product_id`,`channel`,`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多渠道报价';

DROP TABLE IF EXISTS `biz_schedule`;
CREATE TABLE `biz_schedule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `schedule_no` VARCHAR(64) NOT NULL,
  `product_id` BIGINT NOT NULL,
  `depart_date` DATE NOT NULL,
  `return_date` DATE DEFAULT NULL,
  `meeting_point` VARCHAR(255) DEFAULT NULL,
  `meeting_time` VARCHAR(32) DEFAULT NULL,
  `guide_id` BIGINT DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1收客中 2已成团 3已满 4已出团 5已完结',
  `quota` INT NOT NULL DEFAULT 30,
  `sold` INT NOT NULL DEFAULT 0,
  `min_group` INT NOT NULL DEFAULT 10,
  `warn_days` INT NOT NULL DEFAULT 3,
  `plan_pdf` VARCHAR(512) DEFAULT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_schedule_no` (`tenant_id`,`schedule_no`),
  KEY `idx_tenant_date` (`tenant_id`,`depart_date`),
  KEY `idx_product` (`tenant_id`,`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排期/出团计划';

-- ---------- 订单 ----------
DROP TABLE IF EXISTS `biz_order`;
CREATE TABLE `biz_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `order_no` VARCHAR(64) NOT NULL,
  `member_id` BIGINT DEFAULT NULL,
  `product_id` BIGINT NOT NULL,
  `schedule_id` BIGINT DEFAULT NULL,
  `product_title` VARCHAR(255) DEFAULT NULL,
  `depart_date` DATE DEFAULT NULL,
  `contact_name` VARCHAR(64) DEFAULT NULL,
  `contact_phone` VARCHAR(32) DEFAULT NULL,
  `adult_count` INT NOT NULL DEFAULT 1,
  `child_count` INT NOT NULL DEFAULT 0,
  `total_amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `paid_amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `refund_amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `commission_amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0未付 1已付 2部分退款 3已退款',
  `verify_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0未核销 1已核销',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1待支付 2待确认 3已成团 4待出团 5已出团 6已核销 7已完结 8已取消',
  `channel` VARCHAR(32) NOT NULL DEFAULT 'miniprogram',
  `source` VARCHAR(32) DEFAULT NULL COMMENT 'self/ota/distributor',
  `distributor_id` BIGINT DEFAULT NULL,
  `ota_order_no` VARCHAR(64) DEFAULT NULL,
  `remark` VARCHAR(512) DEFAULT NULL,
  `cancel_reason` VARCHAR(255) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_order_no` (`tenant_id`,`order_no`),
  KEY `idx_tenant_status` (`tenant_id`,`status`),
  KEY `idx_tenant_schedule` (`tenant_id`,`schedule_id`),
  KEY `idx_member` (`tenant_id`,`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

DROP TABLE IF EXISTS `biz_order_tourist`;
CREATE TABLE `biz_order_tourist` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `order_id` BIGINT NOT NULL,
  `name` VARCHAR(64) NOT NULL,
  `id_type` VARCHAR(32) DEFAULT 'id_card',
  `id_no_enc` VARCHAR(255) DEFAULT NULL COMMENT 'AES 密文',
  `id_no_mask` VARCHAR(64) DEFAULT NULL,
  `phone_enc` VARCHAR(255) DEFAULT NULL,
  `phone_mask` VARCHAR(32) DEFAULT NULL,
  `is_adult` TINYINT NOT NULL DEFAULT 1,
  `verify_status` TINYINT NOT NULL DEFAULT 0,
  `verify_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`tenant_id`,`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单游客名单（敏感字段加密）';

DROP TABLE IF EXISTS `biz_order_log`;
CREATE TABLE `biz_order_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `order_id` BIGINT NOT NULL,
  `status` TINYINT DEFAULT NULL,
  `action` VARCHAR(64) DEFAULT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `operator` VARCHAR(64) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`tenant_id`,`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单状态轨迹';

DROP TABLE IF EXISTS `biz_payment`;
CREATE TABLE `biz_payment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `order_id` BIGINT NOT NULL,
  `pay_no` VARCHAR(64) NOT NULL,
  `type` VARCHAR(16) NOT NULL DEFAULT 'pay' COMMENT 'pay/refund',
  `channel` VARCHAR(32) NOT NULL DEFAULT 'wechat',
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付 1成功 2失败',
  `transaction_id` VARCHAR(128) DEFAULT NULL,
  `paid_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pay_no` (`tenant_id`,`pay_no`),
  KEY `idx_order` (`tenant_id`,`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付/退款流水';

DROP TABLE IF EXISTS `biz_verify_record`;
CREATE TABLE `biz_verify_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `order_id` BIGINT DEFAULT NULL,
  `tourist_id` BIGINT DEFAULT NULL,
  `ticket_order_id` BIGINT DEFAULT NULL,
  `code` VARCHAR(128) NOT NULL,
  `verify_type` VARCHAR(32) NOT NULL DEFAULT 'qrcode' COMMENT 'qrcode/barcode/face/idcard/nfc',
  `operator_id` BIGINT DEFAULT NULL,
  `operator_name` VARCHAR(64) DEFAULT NULL,
  `gate` VARCHAR(64) DEFAULT NULL,
  `result` VARCHAR(16) NOT NULL DEFAULT 'success',
  `remark` VARCHAR(255) DEFAULT NULL,
  `verify_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_verify_code` (`tenant_id`,`code`),
  KEY `idx_order` (`tenant_id`,`order_id`),
  KEY `idx_time` (`tenant_id`,`verify_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='核销记录（防重复）';

DROP TABLE IF EXISTS `biz_refund`;
CREATE TABLE `biz_refund` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `order_id` BIGINT NOT NULL,
  `refund_no` VARCHAR(64) NOT NULL,
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `reason` VARCHAR(255) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待审批 1已通过 2已拒绝 3已退款',
  `apply_by` VARCHAR(64) DEFAULT NULL,
  `approve_by` VARCHAR(64) DEFAULT NULL,
  `approve_at` DATETIME DEFAULT NULL,
  `refund_at` DATETIME DEFAULT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`tenant_id`,`refund_no`),
  KEY `idx_order` (`tenant_id`,`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退改记录';

-- ---------- 计调/采购 ----------
DROP TABLE IF EXISTS `biz_purchase_order`;
CREATE TABLE `biz_purchase_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `purchase_no` VARCHAR(64) NOT NULL,
  `schedule_id` BIGINT DEFAULT NULL,
  `supplier_id` BIGINT NOT NULL,
  `resource_id` BIGINT DEFAULT NULL,
  `resource_name` VARCHAR(128) DEFAULT NULL,
  `quantity` INT NOT NULL DEFAULT 1,
  `unit_price` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待确认 1已确认 2已拒绝 3已完结',
  `confirm_time` DATETIME DEFAULT NULL,
  `confirm_remark` VARCHAR(255) DEFAULT NULL,
  `settle_status` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_purchase_no` (`tenant_id`,`purchase_no`),
  KEY `idx_tenant_supplier` (`tenant_id`,`supplier_id`),
  KEY `idx_schedule` (`tenant_id`,`schedule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购单/供应商确认';

DROP TABLE IF EXISTS `biz_ota_config`;
CREATE TABLE `biz_ota_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `ota` VARCHAR(32) NOT NULL COMMENT 'fliggy/ctrip/tuniu/meituan/qunar/mfw',
  `app_key` VARCHAR(128) DEFAULT NULL,
  `app_secret` VARCHAR(255) DEFAULT NULL,
  `enabled` TINYINT NOT NULL DEFAULT 0,
  `commission_rate` DECIMAL(8,4) NOT NULL DEFAULT 0,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_ota` (`tenant_id`,`ota`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OTA 对接配置';

DROP TABLE IF EXISTS `biz_ota_order`;
CREATE TABLE `biz_ota_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `ota` VARCHAR(32) NOT NULL,
  `ota_order_no` VARCHAR(128) NOT NULL,
  `order_id` BIGINT DEFAULT NULL,
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `settle_amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `commission` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `fee` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `refund_amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `supplement_amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `status` VARCHAR(32) DEFAULT NULL,
  `raw_json` TEXT,
  `synced_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ota_order` (`tenant_id`,`ota`,`ota_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OTA 同步订单/结算';

-- ---------- 导游 ----------
DROP TABLE IF EXISTS `biz_guide`;
CREATE TABLE `biz_guide` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `name` VARCHAR(64) NOT NULL,
  `guide_no` VARCHAR(64) NOT NULL,
  `level` VARCHAR(32) DEFAULT NULL COMMENT '初级/中级/高级/领队',
  `phone` VARCHAR(32) DEFAULT NULL,
  `id_card_enc` VARCHAR(255) DEFAULT NULL,
  `id_card_mask` VARCHAR(64) DEFAULT NULL,
  `gender` VARCHAR(8) DEFAULT NULL,
  `languages` VARCHAR(128) DEFAULT NULL,
  `available_dates` VARCHAR(512) DEFAULT NULL,
  `deposit` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  `user_id` BIGINT DEFAULT NULL COMMENT '绑定导游端账号',
  `union_id` VARCHAR(64) DEFAULT NULL,
  `remark` VARCHAR(512) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_guide_no` (`tenant_id`,`guide_no`),
  KEY `idx_tenant` (`tenant_id`),
  KEY `idx_union` (`tenant_id`,`union_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导游档案';

DROP TABLE IF EXISTS `biz_guide_assignment`;
CREATE TABLE `biz_guide_assignment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `schedule_id` BIGINT NOT NULL,
  `guide_id` BIGINT NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待确认 1已确认 2已拒绝',
  `push_time` DATETIME DEFAULT NULL,
  `confirm_time` DATETIME DEFAULT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_schedule_guide` (`tenant_id`,`schedule_id`,`guide_id`),
  KEY `idx_guide` (`tenant_id`,`guide_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='带团安排';

DROP TABLE IF EXISTS `biz_guide_expense`;
CREATE TABLE `biz_guide_expense` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `schedule_id` BIGINT NOT NULL,
  `guide_id` BIGINT NOT NULL,
  `type` VARCHAR(32) NOT NULL COMMENT 'ticket/meal/other',
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `receipt_images` TEXT,
  `remark` VARCHAR(512) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待审核 1已通过 2已驳回',
  `audit_by` VARCHAR(64) DEFAULT NULL,
  `audit_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_schedule` (`tenant_id`,`schedule_id`),
  KEY `idx_guide` (`tenant_id`,`guide_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导游报账';

DROP TABLE IF EXISTS `biz_guide_cash`;
CREATE TABLE `biz_guide_cash` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `schedule_id` BIGINT NOT NULL,
  `guide_id` BIGINT NOT NULL,
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 0,
  `remark` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_schedule` (`tenant_id`,`schedule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导游现金领款';

-- ---------- 财务 ----------
DROP TABLE IF EXISTS `biz_settlement`;
CREATE TABLE `biz_settlement` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `settle_no` VARCHAR(64) NOT NULL,
  `type` VARCHAR(32) NOT NULL COMMENT 'partner/group/order',
  `partner_type` VARCHAR(32) DEFAULT NULL COMMENT 'supplier/distributor/guide/member',
  `partner_id` BIGINT DEFAULT NULL,
  `partner_name` VARCHAR(128) DEFAULT NULL,
  `schedule_id` BIGINT DEFAULT NULL,
  `order_id` BIGINT DEFAULT NULL,
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `paid_amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `refund_amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `commission` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `real_amount` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '真实到账金额',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待确认 1已确认 2已结算 3已归档',
  `period_start` DATE DEFAULT NULL,
  `period_end` DATE DEFAULT NULL,
  `approval_status` TINYINT NOT NULL DEFAULT 0,
  `remark` VARCHAR(255) DEFAULT NULL,
  `created_by` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_settle_no` (`tenant_id`,`settle_no`),
  KEY `idx_tenant_type` (`tenant_id`,`type`),
  KEY `idx_partner` (`tenant_id`,`partner_type`,`partner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算单';

DROP TABLE IF EXISTS `biz_settlement_item`;
CREATE TABLE `biz_settlement_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `settlement_id` BIGINT NOT NULL,
  `biz_type` VARCHAR(32) DEFAULT NULL,
  `biz_id` BIGINT DEFAULT NULL,
  `biz_no` VARCHAR(64) DEFAULT NULL,
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `remark` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_settlement` (`tenant_id`,`settlement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算明细';

DROP TABLE IF EXISTS `biz_payment_plan`;
CREATE TABLE `biz_payment_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `settlement_id` BIGINT DEFAULT NULL,
  `direction` VARCHAR(16) NOT NULL DEFAULT 'out' COMMENT 'in/out',
  `plan_date` DATE DEFAULT NULL,
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 0,
  `remark` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_settlement` (`tenant_id`,`settlement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收付款计划';

DROP TABLE IF EXISTS `biz_invoice`;
CREATE TABLE `biz_invoice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `invoice_no` VARCHAR(64) DEFAULT NULL,
  `title` VARCHAR(255) NOT NULL COMMENT '抬头',
  `tax_no` VARCHAR(64) DEFAULT NULL,
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `type` VARCHAR(32) DEFAULT 'normal',
  `order_id` BIGINT DEFAULT NULL,
  `settlement_id` BIGINT DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 0,
  `apply_by` VARCHAR(64) DEFAULT NULL,
  `issued_at` DATETIME DEFAULT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_order` (`tenant_id`,`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子发票';

DROP TABLE IF EXISTS `biz_approval`;
CREATE TABLE `biz_approval` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `biz_type` VARCHAR(32) NOT NULL,
  `biz_id` BIGINT NOT NULL,
  `title` VARCHAR(255) DEFAULT NULL,
  `amount` DECIMAL(12,2) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 0,
  `apply_by` VARCHAR(64) DEFAULT NULL,
  `approve_by` VARCHAR(64) DEFAULT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `approve_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_biz` (`tenant_id`,`biz_type`,`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录';

DROP TABLE IF EXISTS `biz_commission`;
CREATE TABLE `biz_commission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `order_id` BIGINT NOT NULL,
  `distributor_id` BIGINT DEFAULT NULL,
  `member_id` BIGINT DEFAULT NULL,
  `type` VARCHAR(32) NOT NULL DEFAULT 'distributor',
  `rate` DECIMAL(8,4) NOT NULL DEFAULT 0,
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待入账 1已入账',
  `settle_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`,`status`),
  KEY `idx_order` (`tenant_id`,`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='佣金（完成单入账）';

-- ---------- 分销 ----------
DROP TABLE IF EXISTS `biz_distributor`;
CREATE TABLE `biz_distributor` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `type` VARCHAR(32) NOT NULL DEFAULT 'shop' COMMENT 'shop/agency/individual',
  `contact` VARCHAR(64) DEFAULT NULL,
  `phone` VARCHAR(32) DEFAULT NULL,
  `username` VARCHAR(64) DEFAULT NULL,
  `password` VARCHAR(128) DEFAULT NULL,
  `shop_name` VARCHAR(128) DEFAULT NULL,
  `commission_rate` DECIMAL(8,4) NOT NULL DEFAULT 0.05,
  `parent_id` BIGINT DEFAULT NULL COMMENT '二级分销上级',
  `status` TINYINT NOT NULL DEFAULT 1,
  `remark` VARCHAR(255) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`),
  UNIQUE KEY `uk_tenant_username` (`tenant_id`,`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分销商/门市';

DROP TABLE IF EXISTS `biz_distributor_price`;
CREATE TABLE `biz_distributor_price` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `distributor_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `adult_price` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `child_price` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dist_product` (`tenant_id`,`distributor_id`,`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分销报价';

DROP TABLE IF EXISTS `biz_promotion_link`;
CREATE TABLE `biz_promotion_link` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `member_id` BIGINT DEFAULT NULL,
  `distributor_id` BIGINT DEFAULT NULL,
  `product_id` BIGINT DEFAULT NULL,
  `code` VARCHAR(64) NOT NULL,
  `type` VARCHAR(16) NOT NULL DEFAULT 'cps',
  `clicks` INT NOT NULL DEFAULT 0,
  `orders` INT NOT NULL DEFAULT 0,
  `commission` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_promo_code` (`tenant_id`,`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推广链接/二维码（裂变）';

-- ---------- 合同/会员/营销 ----------
DROP TABLE IF EXISTS `biz_contract_template`;
CREATE TABLE `biz_contract_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `content` LONGTEXT,
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同模板';

DROP TABLE IF EXISTS `biz_contract`;
CREATE TABLE `biz_contract` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `contract_no` VARCHAR(64) NOT NULL,
  `order_id` BIGINT DEFAULT NULL,
  `member_id` BIGINT DEFAULT NULL,
  `template_id` BIGINT DEFAULT NULL,
  `title` VARCHAR(255) DEFAULT NULL,
  `content` LONGTEXT,
  `sign_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待签 1已签 2已作废',
  `sign_url` VARCHAR(512) DEFAULT NULL,
  `sign_time` DATETIME DEFAULT NULL,
  `face_verified` TINYINT NOT NULL DEFAULT 0,
  `ca_cert` VARCHAR(255) DEFAULT NULL,
  `pdf_url` VARCHAR(512) DEFAULT NULL,
  `synced_gov` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_no` (`tenant_id`,`contract_no`),
  KEY `idx_order` (`tenant_id`,`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子合同';

DROP TABLE IF EXISTS `biz_member`;
CREATE TABLE `biz_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `union_id` VARCHAR(64) DEFAULT NULL,
  `open_id` VARCHAR(64) DEFAULT NULL,
  `nickname` VARCHAR(64) DEFAULT NULL,
  `avatar` VARCHAR(512) DEFAULT NULL,
  `phone_enc` VARCHAR(255) DEFAULT NULL,
  `phone_mask` VARCHAR(32) DEFAULT NULL,
  `level` INT NOT NULL DEFAULT 1,
  `points` INT NOT NULL DEFAULT 0,
  `balance` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `total_consume` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  `last_active_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_union` (`tenant_id`,`union_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游客会员（UnionID）';

DROP TABLE IF EXISTS `biz_member_points_log`;
CREATE TABLE `biz_member_points_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `member_id` BIGINT NOT NULL,
  `points` INT NOT NULL DEFAULT 0,
  `type` VARCHAR(32) DEFAULT NULL COMMENT 'order/exchange/refund',
  `remark` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_member` (`tenant_id`,`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分流水';

DROP TABLE IF EXISTS `biz_coupon`;
CREATE TABLE `biz_coupon` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `type` VARCHAR(32) NOT NULL DEFAULT 'cash' COMMENT 'cash/discount',
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `discount` DECIMAL(5,2) NOT NULL DEFAULT 1,
  `min_amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `total` INT NOT NULL DEFAULT 0,
  `received` INT NOT NULL DEFAULT 0,
  `start_time` DATETIME DEFAULT NULL,
  `end_time` DATETIME DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券';

DROP TABLE IF EXISTS `biz_member_coupon`;
CREATE TABLE `biz_member_coupon` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `coupon_id` BIGINT NOT NULL,
  `member_id` BIGINT NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0未使用 1已使用 2已过期',
  `order_id` BIGINT DEFAULT NULL,
  `used_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_member` (`tenant_id`,`member_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员优惠券';

DROP TABLE IF EXISTS `biz_activity`;
CREATE TABLE `biz_activity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `type` VARCHAR(32) NOT NULL DEFAULT 'promotion' COMMENT 'coupon/fullcut/limit',
  `rule_json` TEXT,
  `start_time` DATETIME DEFAULT NULL,
  `end_time` DATETIME DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `stat_json` TEXT,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营销活动';

-- ---------- 景区端 ----------
DROP TABLE IF EXISTS `biz_scenic`;
CREATE TABLE `biz_scenic` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `level` VARCHAR(32) DEFAULT NULL COMMENT '5A/4A...',
  `province` VARCHAR(64) DEFAULT NULL,
  `city` VARCHAR(64) DEFAULT NULL,
  `address` VARCHAR(255) DEFAULT NULL,
  `phone` VARCHAR(32) DEFAULT NULL,
  `intro` LONGTEXT,
  `open_time` VARCHAR(128) DEFAULT NULL,
  `notice` TEXT,
  `strategy` LONGTEXT,
  `cover` VARCHAR(512) DEFAULT NULL,
  `longitude` DECIMAL(10,6) DEFAULT NULL,
  `latitude` DECIMAL(10,6) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='景区档案';

DROP TABLE IF EXISTS `biz_ticket`;
CREATE TABLE `biz_ticket` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `scenic_id` BIGINT NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `type` VARCHAR(32) NOT NULL DEFAULT 'single' COMMENT 'single/combo/annual/team/preferential',
  `price` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `child_price` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `team_price` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `valid_days` INT NOT NULL DEFAULT 1,
  `valid_start` DATE DEFAULT NULL,
  `valid_end` DATE DEFAULT NULL,
  `daily_limit` INT NOT NULL DEFAULT 0,
  `refund_rule` TEXT,
  `verify_rule` TEXT COMMENT '一次性/时间窗/闸口绑定',
  `status` TINYINT NOT NULL DEFAULT 1,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_scenic` (`tenant_id`,`scenic_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门票产品';

DROP TABLE IF EXISTS `biz_ticket_stock`;
CREATE TABLE `biz_ticket_stock` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `ticket_id` BIGINT NOT NULL,
  `stock_date` DATE NOT NULL,
  `time_slot` VARCHAR(32) NOT NULL DEFAULT 'all',
  `stock` INT NOT NULL DEFAULT 0,
  `sold` INT NOT NULL DEFAULT 0,
  `reserved` INT NOT NULL DEFAULT 0 COMMENT '团队预留',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ticket_slot` (`tenant_id`,`ticket_id`,`stock_date`,`time_slot`),
  KEY `idx_date` (`tenant_id`,`stock_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门票分时库存/限流';

DROP TABLE IF EXISTS `biz_ticket_order`;
CREATE TABLE `biz_ticket_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `ticket_no` VARCHAR(64) NOT NULL,
  `ticket_id` BIGINT NOT NULL,
  `member_id` BIGINT DEFAULT NULL,
  `visitor_name` VARCHAR(64) DEFAULT NULL,
  `id_no_enc` VARCHAR(255) DEFAULT NULL,
  `visit_date` DATE NOT NULL,
  `time_slot` VARCHAR(32) DEFAULT NULL,
  `quantity` INT NOT NULL DEFAULT 1,
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1待使用 2已使用 3已退款 4已过期',
  `verify_count` INT NOT NULL DEFAULT 0,
  `qr_code` VARCHAR(128) NOT NULL,
  `gate` VARCHAR(64) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ticket_no` (`tenant_id`,`ticket_no`),
  UNIQUE KEY `uk_qr` (`tenant_id`,`qr_code`),
  KEY `idx_date` (`tenant_id`,`visit_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门票订单/电子票';

DROP TABLE IF EXISTS `biz_traffic_stat`;
CREATE TABLE `biz_traffic_stat` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `scenic_id` BIGINT NOT NULL,
  `stat_date` DATE NOT NULL,
  `time_slot` VARCHAR(32) DEFAULT NULL,
  `area` VARCHAR(64) DEFAULT NULL,
  `in_count` INT NOT NULL DEFAULT 0,
  `out_count` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_scenic_date` (`tenant_id`,`scenic_id`,`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客流统计';

DROP TABLE IF EXISTS `biz_scenic_staff`;
CREATE TABLE `biz_scenic_staff` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `scenic_id` BIGINT NOT NULL,
  `name` VARCHAR(64) NOT NULL,
  `username` VARCHAR(64) NOT NULL,
  `password` VARCHAR(128) NOT NULL,
  `gate` VARCHAR(64) DEFAULT NULL COMMENT '闸口/入口',
  `phone` VARCHAR(32) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_username` (`tenant_id`,`username`),
  KEY `idx_scenic` (`tenant_id`,`scenic_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='景区员工账号（多入口/闸口）';

DROP TABLE IF EXISTS `biz_scenic_reservation`;
CREATE TABLE `biz_scenic_reservation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `ticket_order_id` BIGINT DEFAULT NULL,
  `ticket_id` BIGINT NOT NULL,
  `visit_date` DATE NOT NULL,
  `time_slot` VARCHAR(32) NOT NULL,
  `quantity` INT NOT NULL DEFAULT 1,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0已预约 1已入园 2爽约',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_slot` (`tenant_id`,`ticket_id`,`visit_date`,`time_slot`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分时预约';

DROP TABLE IF EXISTS `biz_scenic_area`;
CREATE TABLE `biz_scenic_area` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `scenic_id` BIGINT NOT NULL,
  `name` VARCHAR(64) NOT NULL,
  `capacity` INT NOT NULL DEFAULT 0,
  `in_count` INT NOT NULL DEFAULT 0,
  `longitude` DECIMAL(10,6) DEFAULT NULL,
  `latitude` DECIMAL(10,6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_scenic` (`tenant_id`,`scenic_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='景区区域/拥堵预警';

DROP TABLE IF EXISTS `biz_guide_audio`;
CREATE TABLE `biz_guide_audio` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `scenic_id` BIGINT NOT NULL,
  `spot_name` VARCHAR(128) NOT NULL,
  `audio_url` VARCHAR(512) DEFAULT NULL,
  `map_url` VARCHAR(512) DEFAULT NULL,
  `qr_code` VARCHAR(128) DEFAULT NULL,
  `latitude` DECIMAL(10,6) DEFAULT NULL,
  `longitude` DECIMAL(10,6) DEFAULT NULL,
  `content` TEXT,
  PRIMARY KEY (`id`),
  KEY `idx_scenic` (`tenant_id`,`scenic_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智慧导览';

DROP TABLE IF EXISTS `biz_scenic_merchant`;
CREATE TABLE `biz_scenic_merchant` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `scenic_id` BIGINT NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `type` VARCHAR(32) DEFAULT NULL COMMENT 'food/shop/transport/show',
  `split_rate` DECIMAL(8,4) NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_scenic` (`tenant_id`,`scenic_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='景区二销商户';

DROP TABLE IF EXISTS `biz_parking_record`;
CREATE TABLE `biz_parking_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `scenic_id` BIGINT NOT NULL,
  `plate_no` VARCHAR(32) NOT NULL,
  `entry_time` DATETIME DEFAULT NULL,
  `exit_time` DATETIME DEFAULT NULL,
  `fee` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0在场 1已出场',
  PRIMARY KEY (`id`),
  KEY `idx_plate` (`tenant_id`,`plate_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='停车记录';

-- ---------- 供应商端 ----------
DROP TABLE IF EXISTS `biz_hotel_room`;
CREATE TABLE `biz_hotel_room` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `supplier_id` BIGINT NOT NULL,
  `room_type` VARCHAR(64) NOT NULL,
  `contract_price` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `stock` INT NOT NULL DEFAULT 0,
  `sold` INT NOT NULL DEFAULT 0,
  `stock_date` DATE DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_supplier_date` (`tenant_id`,`supplier_id`,`stock_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='酒店房态/房量/协议价';

DROP TABLE IF EXISTS `biz_vehicle`;
CREATE TABLE `biz_vehicle` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `supplier_id` BIGINT NOT NULL,
  `plate_no` VARCHAR(32) NOT NULL,
  `model` VARCHAR(64) DEFAULT NULL,
  `seats` INT NOT NULL DEFAULT 0,
  `driver_name` VARCHAR(64) DEFAULT NULL,
  `driver_phone` VARCHAR(32) DEFAULT NULL,
  `price_per_day` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `fuel_per_100km` DECIMAL(8,2) DEFAULT NULL,
  `mileage` DECIMAL(12,2) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_supplier` (`tenant_id`,`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆/司机';

DROP TABLE IF EXISTS `biz_vehicle_dispatch`;
CREATE TABLE `biz_vehicle_dispatch` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `vehicle_id` BIGINT NOT NULL,
  `schedule_id` BIGINT DEFAULT NULL,
  `driver_name` VARCHAR(64) DEFAULT NULL,
  `depart_time` DATETIME DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待出车 1已出车 2已核销',
  `verify_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_vehicle` (`tenant_id`,`vehicle_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出车调度/核销';

-- ---------- 分账/保险/质保金 ----------
DROP TABLE IF EXISTS `biz_split_account`;
CREATE TABLE `biz_split_account` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `type` VARCHAR(32) NOT NULL COMMENT 'tenant/scenic/supplier/platform',
  `target_id` BIGINT DEFAULT NULL,
  `wx_account` VARCHAR(128) DEFAULT NULL,
  `split_rate` DECIMAL(8,4) NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分账接收方/资金托管';

DROP TABLE IF EXISTS `biz_split_record`;
CREATE TABLE `biz_split_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `order_id` BIGINT NOT NULL,
  `account_id` BIGINT NOT NULL,
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `rate` DECIMAL(8,4) NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待分账 1成功 2失败',
  `retry_count` INT NOT NULL DEFAULT 0,
  `error_msg` VARCHAR(512) DEFAULT NULL,
  `split_no` VARCHAR(64) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`tenant_id`,`order_id`),
  KEY `idx_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分账明细';

DROP TABLE IF EXISTS `biz_insurance_product`;
CREATE TABLE `biz_insurance_product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `company` VARCHAR(128) DEFAULT NULL,
  `scope` VARCHAR(32) DEFAULT 'domestic' COMMENT 'domestic/overseas',
  `coverage_json` TEXT COMMENT '保障项/保额',
  `premium` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保险产品';

DROP TABLE IF EXISTS `biz_insurance_order`;
CREATE TABLE `biz_insurance_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `policy_no` VARCHAR(64) DEFAULT NULL,
  `insurance_id` BIGINT NOT NULL,
  `order_id` BIGINT DEFAULT NULL,
  `insured_name` VARCHAR(64) DEFAULT NULL,
  `id_no_enc` VARCHAR(255) DEFAULT NULL,
  `phone_enc` VARCHAR(255) DEFAULT NULL,
  `premium` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `coverage` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待投保 1已投保 2已退保',
  `start_date` DATE DEFAULT NULL,
  `end_date` DATE DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`tenant_id`,`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投保单/保单';

DROP TABLE IF EXISTS `biz_guarantee_fund`;
CREATE TABLE `biz_guarantee_fund` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `type` VARCHAR(32) DEFAULT 'quality' COMMENT 'quality/margin',
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `paid_amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `expire_date` DATE DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `remark` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质保金/保证金台账';

-- ---------- 内容增长 ----------
DROP TABLE IF EXISTS `biz_ugc_post`;
CREATE TABLE `biz_ugc_post` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `member_id` BIGINT DEFAULT NULL,
  `type` VARCHAR(32) NOT NULL DEFAULT 'note' COMMENT 'travel/strategy/review/qa/show',
  `title` VARCHAR(255) NOT NULL,
  `content` LONGTEXT,
  `images` TEXT,
  `video_url` VARCHAR(512) DEFAULT NULL,
  `product_id` BIGINT DEFAULT NULL,
  `destination_id` BIGINT DEFAULT NULL,
  `likes` INT NOT NULL DEFAULT 0,
  `comments` INT NOT NULL DEFAULT 0,
  `favorites` INT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待审核 1已通过 2已拒绝',
  `audit_remark` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`,`status`),
  KEY `idx_product` (`tenant_id`,`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='UGC 游记/攻略/点评';

DROP TABLE IF EXISTS `biz_ugc_comment`;
CREATE TABLE `biz_ugc_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `post_id` BIGINT NOT NULL,
  `member_id` BIGINT DEFAULT NULL,
  `content` VARCHAR(1000) NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_post` (`tenant_id`,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='UGC 评论';

DROP TABLE IF EXISTS `biz_ugc_like`;
CREATE TABLE `biz_ugc_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `post_id` BIGINT NOT NULL,
  `member_id` BIGINT NOT NULL,
  `type` VARCHAR(16) NOT NULL DEFAULT 'like' COMMENT 'like/favorite',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_member_type` (`tenant_id`,`post_id`,`member_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞/收藏';

DROP TABLE IF EXISTS `biz_ugc_report`;
CREATE TABLE `biz_ugc_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `post_id` BIGINT NOT NULL,
  `member_id` BIGINT DEFAULT NULL,
  `reason` VARCHAR(255) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_post` (`tenant_id`,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='UGC 举报';

DROP TABLE IF EXISTS `biz_custom_request`;
CREATE TABLE `biz_custom_request` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `member_id` BIGINT DEFAULT NULL,
  `contact_name` VARCHAR(64) DEFAULT NULL,
  `contact_phone` VARCHAR(32) DEFAULT NULL,
  `destination` VARCHAR(128) DEFAULT NULL,
  `days` INT DEFAULT NULL,
  `budget` DECIMAL(12,2) DEFAULT NULL,
  `people` INT DEFAULT NULL,
  `requirement` TEXT,
  `draft_json` TEXT COMMENT '行程草案',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1已报价 2已成单 3已关闭',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定制游需求';

DROP TABLE IF EXISTS `biz_custom_quote`;
CREATE TABLE `biz_custom_quote` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `request_id` BIGINT NOT NULL,
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `content` TEXT,
  `status` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_request` (`tenant_id`,`request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定制游报价';

DROP TABLE IF EXISTS `biz_points_goods`;
CREATE TABLE `biz_points_goods` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `type` VARCHAR(32) DEFAULT 'ticket' COMMENT 'ticket/goods/coupon',
  `points` INT NOT NULL DEFAULT 0,
  `stock` INT NOT NULL DEFAULT 0,
  `image` VARCHAR(512) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分商品';

DROP TABLE IF EXISTS `biz_points_exchange`;
CREATE TABLE `biz_points_exchange` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `goods_id` BIGINT NOT NULL,
  `member_id` BIGINT NOT NULL,
  `points` INT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待核销 1已核销',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_member` (`tenant_id`,`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分兑换';

DROP TABLE IF EXISTS `biz_member_card`;
CREATE TABLE `biz_member_card` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `member_id` BIGINT NOT NULL,
  `card_no` VARCHAR(64) NOT NULL,
  `type` VARCHAR(32) NOT NULL DEFAULT 'stored' COMMENT 'stored/annual',
  `balance` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `valid_end` DATE DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_card_no` (`tenant_id`,`card_no`),
  KEY `idx_member` (`tenant_id`,`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员卡/储值卡';

DROP TABLE IF EXISTS `biz_wallet_log`;
CREATE TABLE `biz_wallet_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `member_id` BIGINT NOT NULL,
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `type` VARCHAR(32) DEFAULT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_member` (`tenant_id`,`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='余额流水';

DROP TABLE IF EXISTS `biz_live_room`;
CREATE TABLE `biz_live_room` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `cover` VARCHAR(512) DEFAULT NULL,
  `product_id` BIGINT DEFAULT NULL,
  `url` VARCHAR(512) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 0,
  `view_count` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播/短视频带货（预留）';

-- ---------- 驾驶舱/AI/多币种 ----------
DROP TABLE IF EXISTS `biz_ai_conversation`;
CREATE TABLE `biz_ai_conversation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `member_id` BIGINT DEFAULT NULL,
  `question` TEXT,
  `answer` TEXT,
  `intent` VARCHAR(64) DEFAULT NULL,
  `transferred` TINYINT NOT NULL DEFAULT 0 COMMENT '转人工',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 客服会话';

DROP TABLE IF EXISTS `biz_currency_rate`;
CREATE TABLE `biz_currency_rate` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `currency` VARCHAR(8) NOT NULL DEFAULT 'CNY',
  `rate` DECIMAL(12,6) NOT NULL DEFAULT 1,
  `symbol` VARCHAR(8) DEFAULT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_currency` (`tenant_id`,`currency`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='币种汇率';

DROP TABLE IF EXISTS `biz_report_daily`;
CREATE TABLE `biz_report_daily` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `stat_date` DATE NOT NULL,
  `order_count` INT NOT NULL DEFAULT 0,
  `sales_amount` DECIMAL(14,2) NOT NULL DEFAULT 0,
  `settle_amount` DECIMAL(14,2) NOT NULL DEFAULT 0,
  `member_count` INT NOT NULL DEFAULT 0,
  `group_rate` DECIMAL(6,2) NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_date` (`tenant_id`,`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='经营日报汇总（全国看板用）';
