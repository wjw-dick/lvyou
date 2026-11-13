-- ============================================================
-- 初始化数据：平台参数 / 全局字典 / 菜单 / 租户与账号 / 演示业务数据
-- 默认密码见 README（admin123）
-- ============================================================
USE `travel`;

INSERT INTO `sys_tenant` (`id`,`code`,`name`,`province`,`city`,`contact`,`phone`,`status`,`open_time`,`expire_time`,`quota_user`,`quota_product`,`quota_order`,`quota_storage`) VALUES
(1,'HLJ','哈尔滨雪乡国际旅行社','黑龙江省','哈尔滨市','张经理','13800000001',2,'2026-01-01 00:00:00','2027-12-31 23:59:59',50,500,200000,53687091200),
(2,'XA','西安丝路旅行社','陕西省','西安市','李经理','13800000002',2,'2026-01-01 00:00:00','2027-06-30 23:59:59',20,200,50000,10737418240);

INSERT INTO `sys_platform_user` (`id`,`username`,`password`,`real_name`,`phone`,`status`,`role_code`) VALUES
(1,'system','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','平台超级管理员','13900000000',1,'platform_admin');

INSERT INTO `sys_platform_param` (`param_key`,`param_value`,`value_type`,`remark`) VALUES
('group.min.count','10','int','成团最低人数默认'),
('group.warn.days','3','int','出团预警默认天数'),
('settle.period','30','int','结算账期（天）'),
('contract.enabled','true','bool','电子合同模板开关'),
('ota.enabled','true','bool','OTA 对接开关'),
('sms.enabled','false','bool','短信开关'),
('tenant.expire.warn.days','30','int','租户到期预警天数'),
('order.auto.finish.days','7','int','订单自动完结天数');

INSERT INTO `sys_language` (`code`,`name`,`enabled`,`sort`) VALUES
('zh-CN','简体中文',1,1),('en-US','English',1,2);

INSERT INTO `sys_global_dict` (`dict_type`,`dict_label`,`dict_value`,`sort`) VALUES
('product_category','跟团游','line',1),('product_category','签证','visa',2),('product_category','机票','flight',3),('product_category','酒店','hotel',4),('product_category','门票','ticket',5),('product_category','租车','car',6),('product_category','邮轮','cruise',7),('product_category','特产商品','goods',8),
('order_status','待支付','1',1),('order_status','待确认','2',2),('order_status','已成团','3',3),('order_status','待出团','4',4),('order_status','已出团','5',5),('order_status','已核销','6',6),('order_status','已完结','7',7),('order_status','已取消','8',8),
('settle_type','按合作方','partner',1),('settle_type','按单团','group',2),('settle_type','按订单','order',3),
('id_type','身份证','id_card',1),('id_type','护照','passport',2),('id_type','港澳通行证','hk_macao',3),('id_type','台胞证','taiwan',4),
('refund_rule','出发前7天以上退订，全额退款','7d_full',1),('refund_rule','出发前3-7天退订，扣除30%','3d_30',2),('refund_rule','出发前3天内退订，扣除50%','72h_50',3),('refund_rule','出团后不可退','departed_no',4),
('destination','哈尔滨','HRB',1),('destination','雪乡','XX',2),('destination','西安','XA',3),('destination','三亚','SYX',4),('destination','云南','YN',5),
('departure','哈尔滨','HRB',1),('departure','西安','XA',2),('departure','北京','BJS',3),('departure','上海','SHA',4),
('line_days','1日','1',1),('line_days','2日','2',2),('line_days','3日','3',3),('line_days','5日','5',5),('line_days','7日','7',7),
('currency','人民币','CNY',1),('currency','美元','USD',2),
('price_unit','元/人','person',1),('price_unit','元/间','room',2);

INSERT INTO `sys_menu` (`id`,`parent_id`,`title`,`title_en`,`name`,`path`,`component`,`icon`,`perm`,`type`,`scope`,`sort`,`visible`) VALUES
(1,0,'全国数据看板','Dashboard','PlatformDashboard','/platform/dashboard','platform/dashboard/index','DataBoard','platform:dashboard','C','platform',1,1),
(2,0,'租户管理','Tenants','PlatformTenant','/platform/tenant','platform/tenant/index','OfficeBuilding','platform:tenant:list','C','platform',2,1),
(3,0,'全局标准字典','Global Dict','PlatformDict','/platform/dict','platform/dict/index','Collection','platform:dict:list','C','platform',3,1),
(4,0,'平台参数','Platform Params','PlatformParam','/platform/param','platform/param/index','Setting','platform:param:list','C','platform',4,1),
(5,0,'平台公告','Notices','PlatformNotice','/platform/notice','platform/notice/index','Bell','platform:notice:list','C','platform',5,1),
(6,0,'平台审计日志','Audit','PlatformAudit','/platform/audit','platform/audit/index','Document','platform:audit:list','C','platform',6,1),
(7,0,'系统用户','Platform Users','PlatformUser','/platform/user','platform/user/index','User','platform:user:list','C','platform',7,1),
(10,0,'工作台','Workbench','Dashboard','/dashboard','dashboard/index','HomeFilled','dashboard:view','C','tenant',1,1),
(20,0,'产品中心','Products','Product','/product','Layout','Goods','product:list','M','tenant',2,1),
(21,20,'线路产品','Product List','ProductList','/product/list','product/list/index','List','product:list','C','tenant',1,1),
(22,20,'产品编辑','Product Edit','ProductEdit','/product/edit/:id','product/edit/index','Edit','product:edit','C','tenant',2,0),
(23,20,'目的地/攻略','Destinations','Destination','/product/destination','product/destination/index','Location','destination:list','C','tenant',3,1),
(30,0,'排期管理','Schedules','Schedule','/schedule','Layout','Calendar','schedule:list','M','tenant',3,1),
(31,30,'日历排期','Calendar','ScheduleCalendar','/schedule/calendar','schedule/calendar/index','Calendar','schedule:list','C','tenant',1,1),
(32,30,'出团计划','Group Plans','ScheduleList','/schedule/list','schedule/list/index','Tickets','schedule:list','C','tenant',2,1),
(40,0,'订单管理','Orders','Order','/order','Layout','Tickets','order:list','M','tenant',4,1),
(41,40,'订单列表','Order List','OrderList','/order/list','order/list/index','Document','order:list','C','tenant',1,1),
(42,40,'订单详情','Order Detail','OrderDetail','/order/detail/:id','order/detail/index','View','order:view','C','tenant',2,0),
(50,0,'导游管理','Guides','Guide','/guide','Layout','UserFilled','guide:list','M','tenant',5,1),
(51,50,'导游档案','Guide List','GuideList','/guide/list','guide/list/index','Avatar','guide:list','C','tenant',1,1),
(52,50,'带团安排','Assignments','GuideAssignment','/guide/assignment','guide/assignment/index','Connection','guide:assign','C','tenant',2,1),
(53,50,'报账审核','Expenses','GuideExpense','/guide/expense','guide/expense/index','Money','guide:audit','C','tenant',3,1),
(60,0,'计调资源','Resources','Resource','/resource','Layout','Box','supplier:list','M','tenant',6,1),
(61,60,'供应商档案','Suppliers','Supplier','/resource/supplier','resource/supplier/index','Shop','supplier:list','C','tenant',1,1),
(62,60,'采购单','Purchase','Purchase','/resource/purchase','resource/purchase/index','ShoppingCart','purchase:list','C','tenant',2,1),
(70,0,'财务结算','Finance','Finance','/finance','Layout','Wallet','settlement:list','M','tenant',7,1),
(71,70,'结算单','Settlements','Settlement','/finance/settlement','finance/settlement/index','Money','settlement:list','C','tenant',1,1),
(72,70,'电子发票','Invoices','Invoice','/finance/invoice','finance/invoice/index','Ticket','invoice:list','C','tenant',2,1),
(73,70,'分账管理','Split Accounts','Split','/finance/split','finance/split/index','Share','split:list','C','tenant',3,1),
(80,0,'分销管理','Distribution','Distribution','/distribution','Layout','Share','distributor:list','M','tenant',8,1),
(81,80,'分销商','Distributors','Distributor','/distribution/list','distribution/list/index','User','distributor:list','C','tenant',1,1),
(82,80,'分销报价','Distributor Price','DistPrice','/distribution/price','distribution/price/index','PriceTag','distributor:price','C','tenant',2,1),
(83,80,'佣金结算','Commission','Commission','/distribution/commission','distribution/commission/index','Coin','commission:list','C','tenant',3,1),
(90,0,'电子合同','Contracts','Contract','/contract','Layout','Document','contract:list','M','tenant',9,1),
(91,90,'合同列表','Contract List','ContractList','/contract/list','contract/list/index','Document','contract:list','C','tenant',1,1),
(92,90,'合同模板','Templates','ContractTemplate','/contract/template','contract/template/index','Files','contract:template','C','tenant',2,1),
(100,0,'会员运营','Members','Member','/member','Layout','User','member:list','M','tenant',10,1),
(101,100,'会员列表','Member List','MemberList','/member/list','member/list/index','User','member:list','C','tenant',1,1),
(102,100,'优惠券','Coupons','Coupon','/member/coupon','member/coupon/index','Present','coupon:list','C','tenant',2,1),
(103,100,'营销活动','Activities','Activity','/member/activity','member/activity/index','Promotion','activity:list','C','tenant',3,1),
(104,100,'积分商城','Points Mall','PointsGoods','/member/points','member/points/index','Star','points:list','C','tenant',4,1),
(110,0,'景区管理','Scenic','Scenic','/scenic','Layout','Picture','scenic:list','M','tenant',11,1),
(111,110,'景区档案','Scenic List','ScenicList','/scenic/list','scenic/list/index','Place','scenic:list','C','tenant',1,1),
(112,110,'门票产品','Tickets','TicketList','/scenic/ticket','scenic/ticket/index','Ticket','ticket:list','C','tenant',2,1),
(113,110,'票务库存','Ticket Stock','TicketStock','/scenic/stock','scenic/stock/index','Grid','ticket:stock','C','tenant',3,1),
(114,110,'客流看板','Traffic','Traffic','/scenic/traffic','scenic/traffic/index','TrendCharts','traffic:view','C','tenant',4,1),
(115,110,'验票记录','Verify Records','VerifyRecord','/scenic/verify','scenic/verify/index','Checked','verify:list','C','tenant',5,1),
(120,0,'内容与增长','Content','Content','/content','Layout','Edit','ugc:list','M','tenant',12,1),
(121,120,'社区审核','UGC Audit','UgcAudit','/content/ugc','content/ugc/index','ChatDotSquare','ugc:audit','C','tenant',1,1),
(122,120,'定制游','Custom Tour','CustomTour','/content/custom','content/custom/index','Compass','custom:list','C','tenant',2,1),
(123,120,'直播带货','Live','Live','/content/live','content/live/index','VideoCamera','live:list','C','tenant',3,1),
(130,0,'报表中心','Reports','Report','/report','Layout','DataAnalysis','report:view','M','tenant',13,1),
(131,130,'经营报表','Business Report','BusinessReport','/report/business','report/business/index','PieChart','report:view','C','tenant',1,1),
(132,130,'经营驾驶舱','Business Cockpit','Cockpit','/report/dashboard','report/cockpit/index','Monitor','report:cockpit','C','tenant',2,1),
(140,0,'系统设置','System','System','/system','Layout','Setting','system:user:list','M','tenant',14,1),
(141,140,'用户管理','Users','SystemUser','/system/user','system/user/index','User','system:user:list','C','tenant',1,1),
(142,140,'角色权限','Roles','SystemRole','/system/role','system/role/index','Lock','system:role:list','C','tenant',2,1),
(143,140,'数据字典','Dict','SystemDict','/system/dict','system/dict/index','Collection','system:dict:list','C','tenant',3,1),
(144,140,'参数配置','Config','SystemConfig','/system/config','system/config/index','Tools','system:config','C','tenant',4,1),
(145,140,'对象存储','Storage','SystemStorage','/system/storage','system/storage/index','FolderOpened','system:storage','C','tenant',5,1),
(146,140,'微信配置','WeChat','SystemWechat','/system/wechat','system/wechat/index','ChatDotRound','system:wechat','C','tenant',6,1),
(147,140,'消息配置','Message','SystemMessage','/system/message','system/message/index','Message','system:message','C','tenant',7,1),
(148,140,'菜单可视化','Nav Config','SystemNav','/system/nav','system/nav/index','Menu','system:nav','C','tenant',8,1),
(149,140,'日志审计','Audit Logs','SystemAudit','/system/audit','system/audit/index','Document','system:audit:list','C','tenant',9,1),
(150,0,'保险与合规','Insurance','Insurance','/insurance','Layout','Umbrella','insurance:list','M','tenant',15,1),
(151,150,'旅游保险','Insurance','InsuranceList','/insurance/list','insurance/list/index','Umbrella','insurance:list','C','tenant',1,1),
(152,150,'质保金台账','Funds','GuaranteeFund','/insurance/fund','insurance/fund/index','Coin','fund:list','C','tenant',2,1),
(1001,21,'新增产品','','','','','','product:add','F','tenant',1,1),
(1002,21,'编辑产品','','','','','','product:edit','F','tenant',2,1),
(1003,21,'删除产品','','','','','','product:delete','F','tenant',3,1),
(1004,41,'核销','','','','','','order:verify','F','tenant',1,1),
(1005,41,'退改','','','','','','order:refund','F','tenant',2,1),
(1006,71,'发起结算','','','','','','settlement:add','F','tenant',1,1),
(1007,111,'景区编辑','','','','','','scenic:edit','F','tenant',1,1),
(1008,112,'门票编辑','','','','','','ticket:edit','F','tenant',1,1);

-- 平台角色
INSERT INTO `sys_role` (`id`,`tenant_id`,`code`,`name`,`remark`) VALUES
(1,0,'platform_admin','平台超级管理员','全国数据/租户管理/平台参数'),
(2,1,'tenant_admin','租户管理员','本租户全部权限'),
(3,1,'planner','计调','产品/排期/计调'),
(4,1,'sales','销售','订单/客户'),
(5,1,'finance','财务','结算/发票'),
(6,1,'guide','导游','带团/报账'),
(7,1,'store_manager','门店店长','门店订单'),
(8,2,'tenant_admin','租户管理员',NULL),
(9,2,'planner','计调',NULL),
(10,2,'sales','销售',NULL);

INSERT INTO `sys_user_role` (`tenant_id`,`user_id`,`role_id`,`user_type`) VALUES
(0,1,1,'platform'),(1,1,2,'tenant'),(1,2,3,'tenant'),(1,3,4,'tenant'),(1,4,5,'tenant'),(1,5,6,'tenant'),(2,6,8,'tenant');

-- 租户管理员拥有全部租户菜单；其他角色子集
INSERT INTO `sys_role_menu` (`tenant_id`,`role_id`,`menu_id`)
SELECT 1,2,id FROM `sys_menu` WHERE `scope`='tenant';
INSERT INTO `sys_role_menu` (`tenant_id`,`role_id`,`menu_id`)
SELECT 2,8,id FROM `sys_menu` WHERE `scope`='tenant';
INSERT INTO `sys_role_menu` (`tenant_id`,`role_id`,`menu_id`) VALUES
(1,3,10),(1,3,20),(1,3,21),(1,3,22),(1,3,23),(1,3,30),(1,3,31),(1,3,32),(1,3,40),(1,3,41),(1,3,42),(1,3,60),(1,3,61),(1,3,62),(1,3,140),(1,3,143),(1,3,1001),(1,3,1002),(1,3,1003),(1,3,1004),
(1,4,10),(1,4,20),(1,4,21),(1,4,40),(1,4,41),(1,4,42),(1,4,1004),(1,4,1005),
(1,5,10),(1,5,70),(1,5,71),(1,5,72),(1,5,73),(1,5,1006),(1,5,130),(1,5,131),(1,5,140),(1,5,149),
(1,6,10),(1,6,30),(1,6,31),(1,6,32),(1,6,50),(1,6,51),(1,6,52),(1,6,53),
(1,7,10),(1,7,40),(1,7,41),(1,7,42),(1,7,1004),
(2,9,10),(2,9,20),(2,9,21),(2,9,22),(2,9,30),(2,9,31),(2,9,32),(2,9,40),(2,9,41),
(2,10,10),(2,10,40),(2,10,41);

-- 租户用户（密码 admin123）
INSERT INTO `sys_tenant_user` (`id`,`tenant_id`,`username`,`password`,`real_name`,`phone`,`is_admin`) VALUES
(1,1,'admin','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','雪乡国旅管理员','13800000001',1),
(2,1,'planner','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','计调小王','13800000003',0),
(3,1,'sales','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','销售小李','13800000004',0),
(4,1,'finance','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','财务小赵','13800000005',0),
(5,1,'guide01','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','导游小刘','13800000006',0),
(6,2,'admin','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','丝路国旅管理员','13800000002',1);

INSERT INTO `sys_config` (`tenant_id`,`config_key`,`config_value`) VALUES
(1,'tenant.name','哈尔滨雪乡国际旅行社'),(1,'tenant.logo',''),(1,'tenant.service_phone','400-800-1234'),(1,'tenant.mp_app_id',''),(1,'tenant.notify.enabled','true'),(1,'tenant.depart_template','尊敬的游客，您的{线路}将于{日期}出团，集合地点{集合地点}，导游{导游}'),
(2,'tenant.name','西安丝路旅行社'),(2,'tenant.service_phone','400-800-5678');

INSERT INTO `sys_message_template` (`tenant_id`,`biz_type`,`template_id`,`title`,`fields_json`) VALUES
(1,'booking','','报名确认','["线路名称","出团日期","人数","订单号","状态"]'),
(1,'group','','成团通知','["线路名称","出团日期","成团状态"]'),
(1,'depart','','出团通知','["线路名称","集合地点","集合时间","导游"]'),
(1,'settle','','结算到账','["结算单号","金额","到账状态"]'),
(1,'refund','','退款通知','["订单号","退款金额","原因"]'),
(1,'marketing','','优惠营销','["活动名","券面额","有效期"]'),
(1,'contract','','合同签署','["合同编号","线路名称","签署状态"]');

INSERT INTO `sys_storage_config` (`tenant_id`,`driver`,`config_json`,`active`) VALUES
(1,'local','{"baseDir":"./data/upload"}',1),
(1,'minio','{"endpoint":"172.16.28.201:9000","accessKey":"","secretKey":"","bucket":"travel"}',0);

INSERT INTO `sys_wechat_config` (`tenant_id`,`config_key`,`config_value`,`encrypted`) VALUES
(1,'miniprogram.appid','',0),(1,'miniprogram.secret','',1),(1,'mp.appid','',0),(1,'mp.secret','',1),(1,'mp.token','',0),(1,'mp.aes_key','',1),(1,'open.platform_appid','',0);

INSERT INTO `sys_nav` (`tenant_id`,`platform`,`content`) VALUES
(1,'miniprogram','[{"title":"首页","icon":"home","path":"/pages/index/index"},{"title":"线路","icon":"list","path":"/pages/product/list"},{"title":"订单","icon":"order","path":"/pages/order/list"},{"title":"我的","icon":"user","path":"/pages/mine/index"}]'),
(1,'h5','[{"title":"首页","link":"/"},{"title":"全部线路","link":"/products"},{"title":"目的地","link":"/destinations"},{"title":"关于我们","link":"/about"}]'),
(1,'app','[{"title":"首页","icon":"home","path":"/home"},{"title":"线路","icon":"explore","path":"/products"},{"title":"订单","icon":"receipt","path":"/orders"},{"title":"我的","icon":"person","path":"/mine"}]'),
(1,'admin','[]');

INSERT INTO `sys_notice` (`tenant_id`,`title`,`content`,`type`,`created_by`) VALUES
(0,'平台上线公告','旅游数字化平台 SaaS 正式上线，多端统一管理。','notice',1),
(1,'雪乡冬季线路已上架','雪乡2日游、哈尔滨冰雪3日游已开放预订。','notice',1);

INSERT INTO `sys_sensitive_word` (`word`,`level`) VALUES ('赌博',1),('毒品',1),('诈骗',1),('色情',1),('暴力',1);

-- ---------- 演示业务数据 ----------
INSERT INTO `biz_destination` (`id`,`tenant_id`,`name`,`name_en`,`type`,`province`,`city`,`summary`,`longitude`,`latitude`) VALUES
(1,1,'雪乡','Snow Town','scenic','黑龙江省','牡丹江市','中国雪乡，童话世界',128.900000,44.300000),
(2,1,'哈尔滨冰雪大世界','Harbin Ice World','scenic','黑龙江省','哈尔滨市','世界最大冰雪主题乐园',126.600000,45.780000),
(3,1,'中央大街','Central Street','strategy','黑龙江省','哈尔滨市','百年欧式建筑步行街',126.610000,45.770000);

INSERT INTO `biz_supplier` (`id`,`tenant_id`,`name`,`type`,`contact`,`phone`,`username`,`password`,`address`,`settlement_period`) VALUES
(1,1,'雪乡雪韵阁酒店','hotel','王经理','13900000001','hotel01','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','雪乡景区内',30),
(2,1,'龙运旅游车队','bus','赵师傅','13900000002','bus01','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','哈尔滨市南岗区',15),
(3,1,'东北人家餐厅','restaurant','孙经理','13900000003','rest01','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','雪乡景区',15);

INSERT INTO `biz_scenic_staff` (`tenant_id`,`scenic_id`,`name`,`username`,`password`,`gate`,`phone`) VALUES
(1,1,'雪乡闸口管理员','scenic01','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','1号闸口','13800000008');

INSERT INTO `biz_supplier_resource` (`tenant_id`,`supplier_id`,`name`,`type`,`spec`,`unit`,`stock`,`cost_price`,`sale_price`,`available_date`) VALUES
(1,1,'标准双人间','room','2人/间','间',50,380,580,'2026-12-20'),
(1,2,'33座旅游大巴','bus','33座','辆',5,1800,2600,'2026-12-20'),
(1,3,'东北特色团餐','meal','10人/桌','桌',30,300,480,'2026-12-20');

INSERT INTO `biz_product` (`id`,`tenant_id`,`product_no`,`category`,`title`,`subtitle`,`days`,`departure`,`destination`,`destination_id`,`adult_price`,`child_price`,`single_room_diff`,`cost_price`,`include_text`,`exclude_text`,`refund_rule`,`meeting_point`,`stock`,`sold`,`channels`,`status`) VALUES
(1,1,'HLJ-TR-2026-0001','line','雪乡童话世界2日游','纯玩无购物 · 含雪韵大街门票','2','哈尔滨','雪乡',1,899,499,300,620,'往返交通、1晚住宿、1早2正、门票、导游服务','个人消费、旅游意外险','出发前7天以上退订全额退款；3-7天扣30%；3天内扣50%','哈尔滨火车站广场',100,12,'miniprogram,mp,h5,app','1' ),
(2,1,'HLJ-TR-2026-0002','line','哈尔滨冰雪大世界3日游','冰雪大世界+中央大街+索菲亚教堂','3','哈尔滨','哈尔滨',2,1599,899,450,1100,'2晚酒店、行程内门票、交通、导游','正餐酒水、个人消费','出发前7天以上退订全额退款；3天内扣50%','哈尔滨太平国际机场',80,30,'miniprogram,mp,h5,app,ota','1'),
(3,1,'HLJ-TR-2026-0003','ticket','雪乡景区门票','成人票 · 当日有效','1','哈尔滨','雪乡',1,120,60,0,80,'景区大门票','园内二销','未使用可退','雪乡景区入口',500,120,'miniprogram,mp,h5,app','1');

INSERT INTO `biz_product_itinerary` (`tenant_id`,`product_id`,`day_no`,`title`,`spots`,`hotel`,`meals`,`transport`,`guide`,`accommodation`,`content`) VALUES
(1,1,1,'哈尔滨—雪乡','中央大街、雪韵大街','雪韵阁酒店','午/晚','旅游大巴','小刘','标准双人间','早集合出发，沿途欣赏林海雪原，下午抵达雪乡。'),
(1,1,2,'雪乡—哈尔滨','雪乡日出、雪雕群','无','早/午','旅游大巴','小刘','无','清晨观雪乡日出，午餐后返回哈尔滨。'),
(1,2,1,'抵达哈尔滨','中央大街、索菲亚教堂','中央大街酒店','晚','接机巴士','小刘','标准双人间','接机入住，夜游中央大街。'),
(1,2,2,'冰雪大世界','冰雪大世界','中央大街酒店','早/晚','旅游大巴','小刘','标准双人间','全天畅玩冰雪大世界。'),
(1,2,3,'返程','太阳岛','无','早','送机巴士','小刘','无','上午太阳岛，下午送机。');

INSERT INTO `biz_product_price` (`tenant_id`,`product_id`,`channel`,`adult_price`,`child_price`,`single_room_diff`) VALUES
(1,1,'distributor',849,469,300),(1,1,'ota',879,479,300),(1,2,'distributor',1499,849,450);

INSERT INTO `biz_schedule` (`id`,`tenant_id`,`schedule_no`,`product_id`,`depart_date`,`return_date`,`meeting_point`,`meeting_time`,`guide_id`,`status`,`quota`,`sold`,`min_group`) VALUES
(1,1,'HLJ-SC-2026-0001',1,'2026-12-20','2026-12-21','哈尔滨火车站广场','07:00',1,1,30,12,10),
(2,1,'HLJ-SC-2026-0002',2,'2026-12-24','2026-12-26','哈尔滨太平国际机场','10:00',1,2,30,30,10),
(3,1,'HLJ-SC-2026-0003',1,'2027-01-05','2027-01-06','哈尔滨火车站广场','07:00',NULL,1,30,3,10);

INSERT INTO `biz_guide` (`id`,`tenant_id`,`name`,`guide_no`,`level`,`phone`,`id_card_mask`,`languages`,`available_dates`,`deposit`,`user_id`) VALUES
(1,1,'刘洋','DHLJ2020001','中级','13800000006','2301**********1234','普通话/英语','2026-12-01~2027-02-28',2000,5),
(2,1,'王雪','DHLJ2021002','初级','13800000007','2301**********5678','普通话','2026-12-15~2027-01-15',1000,NULL);

INSERT INTO `biz_order` (`id`,`tenant_id`,`order_no`,`member_id`,`product_id`,`schedule_id`,`product_title`,`depart_date`,`contact_name`,`contact_phone`,`adult_count`,`child_count`,`total_amount`,`paid_amount`,`pay_status`,`status`,`channel`,`source`) VALUES
(1,1,'HLJ-OD-2026-0001',1,1,1,'雪乡童话世界2日游','2026-12-20','张伟','13700000001',2,1,2297.00,2297.00,1,3,'miniprogram','self'),
(2,1,'HLJ-OD-2026-0002',2,2,2,'哈尔滨冰雪大世界3日游','2026-12-24','李娜','13700000002',2,0,3198.00,3198.00,1,4,'miniprogram','self');

INSERT INTO `biz_order_tourist` (`tenant_id`,`order_id`,`name`,`id_type`,`id_no_enc`,`id_no_mask`,`phone_enc`,`phone_mask`,`is_adult`) VALUES
(1,1,'张伟','id_card','','2301**********1234','','137****0001',1),
(1,1,'王芳','id_card','','2301**********2345','','137****0002',1),
(1,1,'张小明','id_card','','2301**********3456','','',0),
(1,2,'李娜','id_card','','6101**********1234','','137****0002',1),
(1,2,'刘强','id_card','','6101**********2345','','137****0003',1);

INSERT INTO `biz_member` (`id`,`tenant_id`,`union_id`,`open_id`,`nickname`,`phone_mask`,`level`,`points`,`total_consume`) VALUES
(1,1,'union_demo_001','open_demo_001','雪乡旅行家','137****0001',2,380,2297),
(2,1,'union_demo_002','open_demo_002','爱旅行的小娜','137****0002',1,120,3198);

INSERT INTO `biz_scenic` (`id`,`tenant_id`,`name`,`level`,`province`,`city`,`address`,`phone`,`intro`,`open_time`,`notice`) VALUES
(1,1,'雪乡国家森林公园','4A','黑龙江省','牡丹江市','海林市长汀镇','0453-1234567','中国雪乡，冬季积雪厚度可达2米，是国内著名的冰雪旅游目的地。','08:00-17:00','冬季路滑，请注意保暖与安全。'),
(2,1,'哈尔滨冰雪大世界','5A','黑龙江省','哈尔滨市','松北区太阳岛西区','0451-1234567','世界最大冰雪主题乐园。','11:00-21:30','请提前预约分时段入园。');

INSERT INTO `biz_ticket` (`id`,`tenant_id`,`scenic_id`,`name`,`type`,`price`,`child_price`,`team_price`,`valid_days`,`valid_start`,`valid_end`,`daily_limit`,`verify_rule`) VALUES
(1,1,1,'雪乡景区成人票','single',120,60,100,1,'2026-11-01','2027-03-31',8000,'一次性核销，闸口绑定'),
(2,1,1,'雪乡景区年卡','annual',680,0,0,365,'2026-01-01','2026-12-31',200,'实名年卡，每日一次'),
(3,1,2,'冰雪大世界日场票','single',330,180,290,1,'2026-11-15','2027-02-28',15000,'一次性核销，时段有效'),
(4,1,2,'冰雪大世界联票','combo',480,260,420,2,'2026-11-15','2027-02-28',5000,'两日内二次入园有效');

INSERT INTO `biz_ticket_stock` (`tenant_id`,`ticket_id`,`stock_date`,`time_slot`,`stock`,`sold`,`reserved`) VALUES
(1,1,'2026-12-20','08:00-12:00',4000,120,200),
(1,1,'2026-12-20','12:00-17:00',4000,80,100),
(1,3,'2026-12-24','11:00-16:00',7000,300,500),
(1,3,'2026-12-24','16:00-21:30',8000,500,500);

INSERT INTO `biz_member_coupon` (`tenant_id`,`coupon_id`,`member_id`,`status`) VALUES (1,1,1,0);
INSERT INTO `biz_coupon` (`id`,`tenant_id`,`name`,`type`,`amount`,`min_amount`,`total`,`received`,`start_time`,`end_time`) VALUES
(1,1,'新客立减50','cash',50,300,1000,1,'2026-10-01 00:00:00','2027-03-31 23:59:59'),
(2,1,'冬季特惠9折','discount',0,0,500,0,'2026-11-01 00:00:00','2027-02-28 23:59:59');

INSERT INTO `biz_distributor` (`id`,`tenant_id`,`name`,`type`,`contact`,`phone`,`username`,`password`,`shop_name`,`commission_rate`) VALUES
(1,1,'中央大街门市','shop','陈店长','13700000009','md001','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','中央大街旅游商城',0.05);

INSERT INTO `biz_settlement` (`tenant_id`,`settle_no`,`type`,`partner_type`,`partner_id`,`partner_name`,`schedule_id`,`amount`,`paid_amount`,`real_amount`,`status`) VALUES
(1,'HLJ-ST-2026-0001','partner','supplier',1,'雪乡雪韵阁酒店',1,7600,7600,7600,2),
(1,'HLJ-ST-2026-0002','group',NULL,NULL,'雪乡2日游单团结算',1,2297,2297,2297,1);

INSERT INTO `biz_invoice` (`tenant_id`,`title`,`tax_no`,`amount`,`order_id`,`status`) VALUES (1,'哈尔滨雪乡国际旅行社',NULL,2297,1,0);

INSERT INTO `biz_contract_template` (`tenant_id`,`name`,`content`) VALUES (1,'国内旅游合同模板','<p>甲方（旅游者）：{{contact}}<br/>乙方（旅行社）：哈尔滨雪乡国际旅行社</p><p>线路：{{product}}，出团日期：{{date}}，人数：{{people}}</p>');

INSERT INTO `biz_ugc_post` (`tenant_id`,`member_id`,`type`,`title`,`content`,`product_id`,`status`,`likes`) VALUES
(1,1,'travel','雪乡两日，遇见最美童话世界','雪韵大街的夜景太美了，推荐大家住雪乡里面。',1,1,12);

INSERT INTO `biz_points_goods` (`tenant_id`,`name`,`type`,`points`,`stock`) VALUES
(1,'雪乡明信片套装','goods',200,100),(1,'50元优惠券','coupon',500,50);

INSERT INTO `biz_split_account` (`tenant_id`,`name`,`type`,`target_id`,`split_rate`) VALUES
(1,'平台服务费','platform',NULL,0.05),(1,'雪乡景区','scenic',1,0.30);

INSERT INTO `biz_insurance_product` (`tenant_id`,`name`,`company`,`scope`,`coverage_json`,`premium`) VALUES
(1,'境内旅游意外险','中国人保','domestic','{"意外身故":"30万","医疗":"5万"}',15);

INSERT INTO `biz_guarantee_fund` (`tenant_id`,`name`,`type`,`amount`,`paid_amount`,`expire_date`) VALUES
(1,'旅行社质量保证金','quality',200000,200000,'2027-12-31');
