-- ----------------------------------------------------------------------------
-- 用密成果提供使用逐级核准单（政务服务和监管处规矩落库）
--
-- 口径一句话：一份用密成果申请，全程只凭系统里这一张核准单，四步走——
-- 窗口受理、市局核用途、省厅两人会签（密级脱密一名、用途核准一名）、窗口凭单放成果。
-- 签署链只增不改：经办、时辰在签署那一下由系统写死，事后由不得人回头补录，
-- 也不许挪时辰；省厅发回市局，前各阶签一笔勾销，从市局重新攒起，废票与新票不并堂；
-- 核准之后各栏锁住，要动另起新件；核准件一键生成领用清单，图幅代号、密级、
-- 出图年份、批准文号统由核准那一份带过；核准后顺拉倒拉各验一遍，少一笔、
-- 次序错一处即不合格。
-- ----------------------------------------------------------------------------

-- 核准单：四阶流转；已签人数由件自己记（页面与单据同一处算，不许手填）
CREATE TABLE IF NOT EXISTS `t_mgis_use_bill` (
  `id`                BIGINT       NOT NULL COMMENT '主键',
  `bill_no`           VARCHAR(64)  NOT NULL COMMENT '核准单号(系统给号,YM打头)',
  `applicant_unit`    VARCHAR(128) NOT NULL COMMENT '申请单位(受理五样之一)',
  `purpose`           VARCHAR(500) NOT NULL COMMENT '用途(受理五样之一;市局核真假与作业范围搭不搭)',
  `secret_level`      VARCHAR(32)  NOT NULL COMMENT '密级(受理五样之一;省厅一名盯密级与脱密)',
  `map_year`          INT          NOT NULL COMMENT '出图年份(受理五样之一)',
  `station`           VARCHAR(16)  NOT NULL COMMENT '眼下停在哪一阶 WINDOW窗口受理 CITY市局核用途 PROVINCE省厅会签 ISSUE窗口放成果',
  `round_no`          INT          NOT NULL DEFAULT 1 COMMENT '第几轮(省厅发回市局一轮加一)',
  `need_count`        INT          NOT NULL DEFAULT 1 COMMENT '本阶应签人数(窗口、市局1;省厅2;放成果1)',
  `sign_count`        INT          NOT NULL DEFAULT 0 COMMENT '本阶已签人数(件自己记,不许手填;与链上同口径)',
  `status`            INT          NOT NULL DEFAULT 0 COMMENT '情形 0在核 1已核准 2已否决(否决即定死)',
  `approved_doc_no`   VARCHAR(64)  NULL COMMENT '批准文号(省厅两签齐时系统给号,清单照带)',
  `approved_by`       VARCHAR(64)  NULL COMMENT '核准定稿经办(末笔签署人)',
  `approved_at`       DATETIME     NULL COMMENT '核准时辰(系统写死,以秒计)',
  `checklist_no`      VARCHAR(64)  NULL COMMENT '领用清单号(一键生成时给号;一家单位一册图对一条清单)',
  `checklist_gen`     INT          NOT NULL DEFAULT 0 COMMENT '清单已生成 0否 1是(核准后只生成一次)',
  `issued_by`         VARCHAR(64)  NULL COMMENT '放成果经办(窗口凭单发放)',
  `issued_at`         DATETIME     NULL COMMENT '放成果时辰(系统写死,以秒计)',
  `create_by`         VARCHAR(64)  NULL,
  `create_time`       DATETIME     NULL,
  `update_by`         VARCHAR(64)  NULL,
  `update_time`       DATETIME     NULL,
  `remark`            VARCHAR(500) NULL,
  `del_flag`          INT          NOT NULL DEFAULT 0 COMMENT '删除标记 0正常 1删除(服务层不辟删门)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_use_bill_no` (`bill_no`),
  KEY `idx_use_bill_station` (`station`),
  KEY `idx_use_bill_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用密成果提供使用逐级核准单';

-- 签署链：只增不改——不设 update 列，服务层无改删入口；
-- 发回勾销不是删笔：旧轮各笔原样留在链上，页面只显新一轮，废票与新票不并堂
CREATE TABLE IF NOT EXISTS `t_mgis_use_entry` (
  `id`            BIGINT      NOT NULL COMMENT '主键',
  `bill_id`       BIGINT      NOT NULL COMMENT '核准单主键',
  `seq`           INT         NOT NULL COMMENT '链上序号(自1连排)',
  `round_no`      INT         NOT NULL DEFAULT 1 COMMENT '第几轮(发回重审一轮加一)',
  `station`       VARCHAR(16) NOT NULL COMMENT '签署阶 WINDOW/CITY/PROVINCE/ISSUE',
  `action`        VARCHAR(16) NOT NULL COMMENT '签署动作 ACCEPT受理 VERIFY核用途 CONFIRM核准会签 REJECT驳回 VETO否决 SEND_BACK发回 ISSUE放成果',
  `sign_role`     VARCHAR(24) NULL COMMENT '省厅会签分工 SECRET密级脱密 PURPOSE用途(其余阶为空)',
  `payload`       TEXT        NULL COMMENT '这一笔写的话(JSON,签署即定)',
  `penned_by`     VARCHAR(64) NOT NULL COMMENT '签署人(签署那一下系统写死,替签不作数)',
  `penned_at`     DATETIME    NOT NULL COMMENT '签署时辰(系统写死,以秒计)',
  `prev_hash`     CHAR(64)    NOT NULL COMMENT '前一笔哈希(头一笔为64个0)',
  `hash`          CHAR(64)    NOT NULL COMMENT '本笔哈希',
  `create_by`     VARCHAR(64) NULL,
  `create_time`   DATETIME    NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_use_entry_seq` (`bill_id`, `seq`),
  UNIQUE KEY `uk_use_entry_point` (`bill_id`, `station`, `action`, `sign_role`, `round_no`),
  KEY `idx_use_entry_bill` (`bill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用密核准签署链(只增不改)';

-- 申请图幅行：受理时一次挂齐图幅代号（密级、出图年份是受理五样里的单件口径，挂在核准单上）；受理即锁
CREATE TABLE IF NOT EXISTS `t_mgis_use_sheet` (
  `id`            BIGINT      NOT NULL COMMENT '主键',
  `bill_id`       BIGINT      NOT NULL COMMENT '核准单主键',
  `line_no`       INT         NOT NULL COMMENT '册内行次(自1连排)',
  `sheet_code`    VARCHAR(64) NOT NULL COMMENT '图幅代号',
  `create_by`     VARCHAR(64) NULL,
  `create_time`   DATETIME     NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_use_sheet_line` (`bill_id`, `line_no`),
  UNIQUE KEY `uk_use_sheet_code` (`bill_id`, `sheet_code`),
  KEY `idx_use_sheet_bill` (`bill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用密核准申请图幅行(受理即锁)';

-- 领用清单行：核准后一键生成，图幅代号、密级、出图年份、批准文号照核准那一份带过，
-- 窗口不再誊第二遍；一家单位一册图对一条清单
CREATE TABLE IF NOT EXISTS `t_mgis_use_checklist` (
  `id`               BIGINT      NOT NULL COMMENT '主键',
  `bill_id`          BIGINT      NOT NULL COMMENT '核准单主键',
  `checklist_no`     VARCHAR(64) NOT NULL COMMENT '领用清单号(同单一个号)',
  `line_no`          INT         NOT NULL COMMENT '清单行次(照申请册内行次)',
  `sheet_code`       VARCHAR(64) NOT NULL COMMENT '图幅代号(核准件带过)',
  `secret_level`     VARCHAR(32) NOT NULL COMMENT '密级(核准单带过)',
  `map_year`         INT         NOT NULL COMMENT '出图年份(核准单带过)',
  `approved_doc_no`  VARCHAR(64) NOT NULL COMMENT '批准文号(核准件带过)',
  `create_by`        VARCHAR(64) NULL,
  `create_time`      DATETIME     NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_use_checklist_line` (`bill_id`, `line_no`),
  KEY `idx_use_checklist_bill` (`bill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用密成果领用清单行(核准件一键带出)';

-- ----------------------------------------------------------------------------
-- 老库升级：早期 t_mgis_use_bill 只有 node_no/sign_mode/need_count/sign_count/status
-- 五栏的占位骨架。新库按本文件建表已带全字段，跳过；老库执行下列补列语句后，
-- 再补建 t_mgis_use_entry / t_mgis_use_sheet / t_mgis_use_checklist 三张表。
-- ----------------------------------------------------------------------------
-- ALTER TABLE `t_mgis_use_bill`
--   ADD COLUMN `applicant_unit`  VARCHAR(128) NOT NULL COMMENT '申请单位' AFTER `bill_no`,
--   ADD COLUMN `purpose`         VARCHAR(500) NOT NULL COMMENT '用途' AFTER `applicant_unit`,
--   ADD COLUMN `secret_level`    VARCHAR(32)  NOT NULL COMMENT '密级' AFTER `purpose`,
--   ADD COLUMN `map_year`        INT          NOT NULL COMMENT '出图年份' AFTER `secret_level`,
--   ADD COLUMN `station`         VARCHAR(16)  NOT NULL DEFAULT 'CITY' COMMENT '眼下停在哪一阶' AFTER `map_year`,
--   ADD COLUMN `round_no`        INT          NOT NULL DEFAULT 1 COMMENT '第几轮' AFTER `station`,
--   ADD COLUMN `approved_doc_no` VARCHAR(64)  NULL COMMENT '批准文号' AFTER `status`,
--   ADD COLUMN `approved_by`     VARCHAR(64)  NULL COMMENT '核准定稿经办' AFTER `approved_doc_no`,
--   ADD COLUMN `approved_at`     DATETIME     NULL COMMENT '核准时辰' AFTER `approved_by`,
--   ADD COLUMN `checklist_no`    VARCHAR(64)  NULL COMMENT '领用清单号' AFTER `approved_at`,
--   ADD COLUMN `checklist_gen`   INT          NOT NULL DEFAULT 0 COMMENT '清单已生成' AFTER `checklist_no`,
--   ADD COLUMN `issued_by`       VARCHAR(64)  NULL COMMENT '放成果经办' AFTER `checklist_gen`,
--   ADD COLUMN `issued_at`       DATETIME     NULL COMMENT '放成果时辰' AFTER `issued_by`;
-- ALTER TABLE `t_mgis_use_bill` ADD UNIQUE KEY `uk_use_bill_no` (`bill_no`);
