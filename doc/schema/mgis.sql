-- mgis 测绘地理信息成果提供使用与测绘资质监督管理 -- schema (chen-011)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_011

CREATE TABLE IF NOT EXISTS t_mgis_check_card (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '不合格成果整改回执号',
  stage int DEFAULT NULL COMMENT '当前关口 0..5',
  status int DEFAULT NULL COMMENT '回执落定 0在办 1已销号 2已封存',
  content varchar(255) DEFAULT NULL COMMENT '整改记事',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次过口动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='不合格成果整改回执单';

CREATE TABLE IF NOT EXISTS t_mgis_duty_task (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) DEFAULT NULL COMMENT '到期提醒单号',
  due_at datetime DEFAULT NULL COMMENT '本项应当到期的日终截止时刻',
  amount decimal(12,2) DEFAULT NULL COMMENT '单条提前提醒的自然日数',
  status int DEFAULT NULL COMMENT '条目情形 0待提醒 1已提醒 2送不出去',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='到期提醒派发条目';

CREATE TABLE IF NOT EXISTS t_mgis_qual_line (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(64) DEFAULT NULL COMMENT '成果质检判定线代号',
  rule_name varchar(128) DEFAULT NULL COMMENT '判定线名称',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '一档中误差限差上限(米)',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '二档中误差限差上限(米)',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '三档中误差限差上限(米)',
  eff_start datetime DEFAULT NULL COMMENT '启用之日',
  eff_end datetime DEFAULT NULL COMMENT '交棒之日(不含)',
  priority int DEFAULT NULL COMMENT '抢档顺位(数值越大越优先)',
  status int DEFAULT NULL COMMENT '线的情形 0当家 1已让位',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='测绘成果质检判定线';

CREATE TABLE IF NOT EXISTS t_mgis_scope_card (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '作业范围标签卡号(系统给号)',
  source_bill_no varchar(64) DEFAULT NULL COMMENT '来文单号(人工填写;同一单号并一条,账上不留两笔)',
  source_from varchar(16) DEFAULT NULL COMMENT '来文来源 CITY_FORWARD市局抄送 UNIT_SELF单位自报',
  site_id int DEFAULT NULL COMMENT '所属资质单位基础档案',
  site_no varchar(64) DEFAULT NULL COMMENT '资质单位编号',
  scope_seg varchar(64) DEFAULT NULL COMMENT '作业范围段代号(同一单位同一段范围只许一张卡)',
  qty decimal(12,2) DEFAULT NULL COMMENT '本卡挂用专业类别总数(项)',
  fine_amt decimal(12,2) DEFAULT NULL COMMENT '本年度新增挂用类别数(项)',
  grade_level decimal(5,2) DEFAULT NULL COMMENT '标签层级档(系统撞三道顶线折算,留两位小数,不许人手填)',
  status int DEFAULT NULL COMMENT '进展 0待核对 1已核对 2已定档',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者(立卡那一下写死,重录不许挪)',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_scope_card_source_bill (source_bill_no),
  UNIQUE KEY uk_scope_card_site_scope (site_no, scope_seg)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作业范围标签建档卡';

CREATE TABLE IF NOT EXISTS t_mgis_scope_category (
  id bigint NOT NULL COMMENT '主键',
  cate_code varchar(32) NOT NULL COMMENT '专业类别代号',
  cate_name varchar(128) NOT NULL COMMENT '专业类别名称',
  lvl int NOT NULL COMMENT '层级 1大类 2末级(卡上只许挂末级)',
  parent_code varchar(32) DEFAULT NULL COMMENT '所属大类代号(末级必填;大类为空)',
  merged_into varchar(32) DEFAULT NULL COMMENT '并走去向(非空=旧类别已并入别类,不许再挂)',
  status int NOT NULL DEFAULT '0' COMMENT '0现行 1停用',
  del_flag int NOT NULL DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_scope_category_code (cate_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='测绘专业类别目录树(一级大类,二级末级)';

CREATE TABLE IF NOT EXISTS t_mgis_scope_card_item (
  id bigint NOT NULL COMMENT '主键',
  card_id bigint NOT NULL COMMENT '卡主键',
  cate_code varchar(32) NOT NULL COMMENT '挂用的末级类别代号',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_scope_card_item (card_id, cate_code),
  KEY idx_scope_item_card (card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作业范围标签卡在挂类别(只记眼下在挂)';

CREATE TABLE IF NOT EXISTS t_mgis_scope_card_entry (
  id bigint NOT NULL COMMENT '主键',
  card_id bigint NOT NULL COMMENT '卡主键',
  seq int NOT NULL COMMENT '链上序号(自1连排)',
  proc varchar(16) NOT NULL COMMENT '手续 REGISTER立卡 CHECK核对 GRADE定档 ADJUST类别增减',
  payload varchar(500) DEFAULT NULL COMMENT '这一笔要说的话(落笔即定,往后只读)',
  penned_by varchar(64) NOT NULL COMMENT '经手人(落笔那一下系统写死)',
  penned_at datetime NOT NULL COMMENT '落笔时辰(系统写死,以秒计)',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_scope_entry_seq (card_id, seq),
  KEY idx_scope_entry_card (card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标签卡经手人落笔链(只增不改)';

CREATE TABLE IF NOT EXISTS t_mgis_submit_row (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '成果目录汇交册号',
  row_no int DEFAULT NULL COMMENT '原册内行次',
  item_code varchar(64) DEFAULT NULL COMMENT '图幅成果代号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本行填报图幅幅数(幅)',
  status int DEFAULT NULL COMMENT '行进展 0待销 1已入账 2挂驳回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成果目录汇交核收行';

CREATE TABLE IF NOT EXISTS t_mgis_unit_doc (
  id bigint NOT NULL COMMENT '主键',
  site_no varchar(64) DEFAULT NULL COMMENT '资质单位编号',
  site_name varchar(128) DEFAULT NULL COMMENT '资质单位基础档案名称',
  site_type varchar(32) DEFAULT NULL COMMENT '单位性质类',
  road_name varchar(128) DEFAULT NULL COMMENT '注册地(省—市—区县)',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '挂用专业类别数一档上限(项)',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '二档上限(项)',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '三档上限(项)',
  status int DEFAULT NULL COMMENT '档案情形 0在册 1已注销',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资质单位基础档案';

CREATE TABLE IF NOT EXISTS t_mgis_use_bill (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '成果提供使用核准单号',
  node_no int DEFAULT NULL COMMENT '当前所在层 0..3',
  sign_mode int DEFAULT NULL COMMENT '同层核准方式 0任一人 1两名点齐',
  need_count int DEFAULT NULL COMMENT '本层应签人数',
  sign_count int DEFAULT NULL COMMENT '本层已签人数',
  status int DEFAULT NULL COMMENT '报批情形 0在核 1已核讫 2已打回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成果提供使用逐级核准单';

-- 初始档案数据（在册 / 注销两种状态各一条）
-- t_mgis_unit_doc 两条种子档案：id=0 在册、id=1 已注销（在册与注销的联动样例）。
-- 挂用专业类别数上限取 30/120/360 项，取值达到上限时按高一档折算。
INSERT IGNORE INTO t_mgis_unit_doc (id, site_no, site_name, site_type, road_name, th1_max, th2_max, th3_max, status, del_flag, create_by, create_time)
VALUES (0, 'ZZ00', '江南省测绘院第一地理信息工程院基础档案', '事业单位', '江南省—临川市—城东区', 30.00, 120.00, 360.00, 0, 0, 'seed', NOW()),
       (1, 'ZZ01', '旧临川市城乡测绘服务中心基础档案（已注销）', '企业单位', '江南省—临川市—城西区', 30.00, 120.00, 360.00, 1, 0, 'seed', NOW());

-- 专业类别目录种子：现行目录（三个大类各带末级）+ 一条已并走的旧类别样例。
-- GC09 旧工程测量已并入 GC01 控制测量——挂它立卡，当场顶回并指明去向。
INSERT IGNORE INTO t_mgis_scope_category (id, cate_code, cate_name, lvl, parent_code, merged_into, status, del_flag, create_by, create_time)
VALUES (101, 'GC',   '工程测量',                 1, NULL, NULL,   0, 0, 'seed', NOW()),
       (102, 'GC01', '控制测量',                 2, 'GC', NULL,   0, 0, 'seed', NOW()),
       (103, 'GC02', '地形测量',                 2, 'GC', NULL,   0, 0, 'seed', NOW()),
       (104, 'GC03', '规划测量',                 2, 'GC', NULL,   0, 0, 'seed', NOW()),
       (105, 'JX',   '界线与不动产测绘',         1, NULL, NULL,   0, 0, 'seed', NOW()),
       (106, 'JX01', '界线测绘',                 2, 'JX', NULL,   0, 0, 'seed', NOW()),
       (107, 'JX02', '不动产测绘',               2, 'JX', NULL,   0, 0, 'seed', NOW()),
       (108, 'DT',   '地图编制',                 1, NULL, NULL,   0, 0, 'seed', NOW()),
       (109, 'DT01', '地形图编制',               2, 'DT', NULL,   0, 0, 'seed', NOW()),
       (110, 'DT02', '专题地图编制',             2, 'DT', NULL,   0, 0, 'seed', NOW()),
       (111, 'GC09', '旧工程测量（已并入控制测量）', 2, 'GC', 'GC01', 1, 0, 'seed', NOW());

