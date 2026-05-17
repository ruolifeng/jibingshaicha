-- ======================================================
-- 疾病监控系统 初始化 SQL
-- ======================================================

CREATE DATABASE IF NOT EXISTS `disease_monitor`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `disease_monitor`;

-- ==================== 系统表 ====================

CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(128) NOT NULL COMMENT '密码',
    `real_name`   VARCHAR(64)  DEFAULT NULL COMMENT '真实姓名',
    `role`        TINYINT      NOT NULL DEFAULT 6 COMMENT '角色：1=超级管理员 2=一级 3=二级 4=三级 5=四级 6=五级',
    `org_name`    VARCHAR(128) DEFAULT NULL COMMENT '所属机构名称',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 初始密码均为 123456，已使用 BCrypt(strength=10) 加密
INSERT INTO `user` (`username`, `password`, `real_name`, `role`, `org_name`) VALUES
('admin',     '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '超级管理员', 1, '市疾控中心'),
('level4user','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '四级操作员', 5, '区疾控中心'),
('level5user','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '五级操作员', 6, '社区卫生服务中心');

CREATE TABLE IF NOT EXISTS `sys_message` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `sender_id`    BIGINT       DEFAULT NULL COMMENT '发送人ID（系统消息为空）',
    `receiver_id`  BIGINT       NOT NULL COMMENT '接收人ID',
    `title`        VARCHAR(256) NOT NULL COMMENT '消息标题',
    `content`      TEXT         DEFAULT NULL COMMENT '消息内容',
    `type`         VARCHAR(32)  NOT NULL COMMENT '消息类型：notice_timeout/supervision_timeout/visit_timeout',
    `biz_id`       BIGINT       DEFAULT NULL COMMENT '关联业务ID',
    `is_read`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读：0未读 1已读',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_receiver` (`receiver_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统消息表';

-- ==================== 学校人群筛查表 ====================
-- V4 变更：移除胸片/诊断/痰涂片/分子生物字段（移至潜伏感染追踪阶段录入）；新增结构化预防性治疗字段

CREATE TABLE IF NOT EXISTS `screening_school` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT,
    `year`                  VARCHAR(10)  DEFAULT NULL COMMENT '年份',
    `city`                  VARCHAR(64)  DEFAULT NULL COMMENT '市（州）',
    `district`              VARCHAR(64)  DEFAULT NULL COMMENT '县（市、区）',
    `name`                  VARCHAR(64)  DEFAULT NULL COMMENT '姓名',
    `gender`                VARCHAR(10)  DEFAULT NULL COMMENT '性别',
    `birth_date`            DATE         DEFAULT NULL COMMENT '出生日期',
    `age`                   INT          DEFAULT NULL COMMENT '年龄',
    `id_type`               VARCHAR(32)  DEFAULT NULL COMMENT '证件类型',
    `id_number`             VARCHAR(64)  DEFAULT NULL COMMENT '证件号',
    `ethnicity`             VARCHAR(32)  DEFAULT NULL COMMENT '民族',
    `phone`                 VARCHAR(32)  DEFAULT NULL COMMENT '联系电话',
    `household_address`     VARCHAR(256) DEFAULT NULL COMMENT '户籍所在地',
    `current_address`       VARCHAR(256) DEFAULT NULL COMMENT '现地址',
    `school_type`           VARCHAR(64)  DEFAULT NULL COMMENT '学校类型',
    `school_name`           VARCHAR(128) DEFAULT NULL COMMENT '学校名称',
    `class_name`            VARCHAR(128) DEFAULT NULL COMMENT '班级（院系）',
    `tb_history`            VARCHAR(64)  DEFAULT NULL COMMENT '既往结核病史',
    `close_contact_history` VARCHAR(64)  DEFAULT NULL COMMENT '密切接触史',
    `suspicious_symptoms`   VARCHAR(128) DEFAULT NULL COMMENT '结核病可疑症状',
    `has_infection_screen`  VARCHAR(10)  DEFAULT NULL COMMENT '是否进行感染筛',
    `screen_date`           DATE         DEFAULT NULL COMMENT '感染筛查日期',
    `screen_method`         VARCHAR(64)  DEFAULT NULL COMMENT '方法（PPD/EC/IGRA）',
    `screen_result`         VARCHAR(128) DEFAULT NULL COMMENT '结果（mmXmm/EC阴性/EC阳性/IGRA阴性/IGRA阳性）',
    `infection_result`      VARCHAR(128) DEFAULT NULL COMMENT '感染筛查结果（V4：PPD阴性/PPD+/PPD++/PPD+++/EC阴性/EC阳性/IGRA阴性/IGRA阳性）',
    -- 胸片与诊断（追踪到位后系统回写）
    `has_chest_xray`        VARCHAR(10)  DEFAULT NULL COMMENT '是否进行胸片检查',
    `chest_xray_date`       DATE         DEFAULT NULL COMMENT '胸片检查日期',
    `chest_xray_result`     VARCHAR(128) DEFAULT NULL COMMENT '胸片结果',
    `diagnosis_first`       VARCHAR(128) DEFAULT NULL COMMENT '诊断结果',
    `diagnosis_half_year`   VARCHAR(128) DEFAULT NULL COMMENT '诊断结果（半年后）',
    `diagnosis_one_year`        VARCHAR(128) DEFAULT NULL COMMENT '诊断结果（一年后）',
    -- 预防性治疗情况（督导表归档后同步，V4新增结构化字段）
    `has_preventive_treatment` VARCHAR(10)  DEFAULT NULL COMMENT '是否进行预防性治疗',
    `preventive_plan`          VARCHAR(128) DEFAULT NULL COMMENT '预防性治疗方案',
    `preventive_start_date`    DATE         DEFAULT NULL COMMENT '预防性治疗开始时间',
    `preventive_end_date`      DATE         DEFAULT NULL COMMENT '预防性治疗完成时间',
    `preventive_result`        VARCHAR(64)  DEFAULT NULL COMMENT '预防性治疗结果：规范完成/失访/自行中断治疗/确诊肺结核',
    `preventive_manager`       VARCHAR(256) DEFAULT NULL COMMENT '预防性治疗期间随访管理人员',
    `remark`                   TEXT         DEFAULT NULL COMMENT '备注',
    `is_latent`                TINYINT      NOT NULL DEFAULT 0 COMMENT '是否潜伏管理者：0否 1是',
    `upload_batch`             VARCHAR(64)  DEFAULT NULL COMMENT '上传批次号',
    `create_time`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                  TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_id_number` (`id_number`),
    KEY `idx_school` (`school_name`, `district`),
    KEY `idx_latent` (`is_latent`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学校人群筛查数据表（V4）';

-- ==================== 重点人群筛查表 ====================
-- V4 变更：移除胸片/诊断/结果判定/是否转诊等字段（移至潜伏感染追踪阶段录入）；新增结构化预防性治疗字段

CREATE TABLE IF NOT EXISTS `screening_key_population` (
    `id`                       BIGINT       NOT NULL AUTO_INCREMENT,
    `year`                     VARCHAR(10)  DEFAULT NULL COMMENT '年份',
    `city`                     VARCHAR(64)  DEFAULT NULL COMMENT '市（州）',
    `district`                 VARCHAR(64)  DEFAULT NULL COMMENT '县（市、区）',
    `name`                     VARCHAR(64)  DEFAULT NULL COMMENT '姓名',
    `gender`                   VARCHAR(10)  DEFAULT NULL COMMENT '性别',
    `birth_date`               DATE         DEFAULT NULL COMMENT '出生日期',
    `age`                      INT          DEFAULT NULL COMMENT '年龄',
    `id_type`                  VARCHAR(32)  DEFAULT NULL COMMENT '证件类型',
    `id_number`                VARCHAR(64)  DEFAULT NULL COMMENT '证件号',
    `ethnicity`                VARCHAR(32)  DEFAULT NULL COMMENT '民族',
    `phone`                    VARCHAR(32)  DEFAULT NULL COMMENT '联系电话',
    `household_address`        VARCHAR(256) DEFAULT NULL COMMENT '户籍所在地',
    `township_community`       VARCHAR(128) DEFAULT NULL COMMENT '乡镇/社区',
    `current_address`          VARCHAR(256) DEFAULT NULL COMMENT '现住址',
    -- 人群分类（多选，V4 每项独立列）
    `crowd_category_close`     VARCHAR(10)  DEFAULT NULL COMMENT '人群分类-密接（是/否）',
    `crowd_category_student`   VARCHAR(10)  DEFAULT NULL COMMENT '人群分类-学生（是/否）',
    `crowd_category_teacher`   VARCHAR(10)  DEFAULT NULL COMMENT '人群分类-教职工（是/否）',
    `crowd_category_elder`     VARCHAR(10)  DEFAULT NULL COMMENT '人群分类-老年人（是/否）',
    `crowd_category_diabetes`  VARCHAR(10)  DEFAULT NULL COMMENT '人群分类-糖尿病（是/否）',
    `crowd_category_dual`      VARCHAR(10)  DEFAULT NULL COMMENT '人群分类-双感（是/否）',
    `crowd_category_tb_hist`   VARCHAR(10)  DEFAULT NULL COMMENT '人群分类-既往结核史（是/否）',
    `crowd_category_normal`    VARCHAR(10)  DEFAULT NULL COMMENT '人群分类-非重点人群（是/否）',
    -- 症状筛查
    `has_suspicious_symptoms`  VARCHAR(10)  DEFAULT NULL COMMENT '是否有可疑症状',
    `cough`                    VARCHAR(10)  DEFAULT NULL COMMENT '咳嗽咳痰',
    `hemoptysis`               VARCHAR(10)  DEFAULT NULL COMMENT '咯血或血痰',
    `fever`                    VARCHAR(10)  DEFAULT NULL COMMENT '发热',
    `chest_pain`               VARCHAR(10)  DEFAULT NULL COMMENT '胸痛',
    `night_sweats`             VARCHAR(10)  DEFAULT NULL COMMENT '夜间盗汗',
    `appetite_loss`            VARCHAR(10)  DEFAULT NULL COMMENT '食欲不振',
    `fatigue`                  VARCHAR(10)  DEFAULT NULL COMMENT '乏力',
    `weight_loss`              VARCHAR(10)  DEFAULT NULL COMMENT '体重减轻',
    -- 感染筛查（V4 方法改为单列）
    `has_infection_screen`     VARCHAR(10)  DEFAULT NULL COMMENT '是否进行感染筛',
    `screen_date`              DATE         DEFAULT NULL COMMENT '感染筛查日期',
    `screen_method`            VARCHAR(64)  DEFAULT NULL COMMENT '感染筛查方法（PPD/EC/IGRA）',
    `screen_result`            VARCHAR(128) DEFAULT NULL COMMENT '结果（mmXmm/EC阴性/EC阳性/IGRA阴性/IGRA阳性）',
    `infection_result`         VARCHAR(128) DEFAULT NULL COMMENT '感染筛查结果',
    -- 胸片与诊断（追踪到位后系统回写）
    `has_chest_xray`           VARCHAR(10)  DEFAULT NULL COMMENT '是否进行胸片检查',
    `chest_xray_date`          DATE         DEFAULT NULL COMMENT '胸片检查日期',
    `chest_xray_result`        VARCHAR(128) DEFAULT NULL COMMENT '胸片结果',
    `diagnosis_first`          VARCHAR(128) DEFAULT NULL COMMENT '诊断结果',
    `diagnosis_half_year`      VARCHAR(128) DEFAULT NULL COMMENT '诊断结果（半年后）',
    `diagnosis_one_year`           VARCHAR(128) DEFAULT NULL COMMENT '诊断结果（一年后）',
    -- 预防性治疗情况（督导表归档后同步，V4新增结构化字段）
    `has_preventive_treatment`    VARCHAR(10)  DEFAULT NULL COMMENT '是否进行预防性治疗',
    `preventive_plan`             VARCHAR(128) DEFAULT NULL COMMENT '预防性治疗方案',
    `preventive_start_date`       DATE         DEFAULT NULL COMMENT '预防性治疗开始时间',
    `preventive_end_date`         DATE         DEFAULT NULL COMMENT '预防性治疗完成时间',
    `preventive_result`           VARCHAR(64)  DEFAULT NULL COMMENT '预防性治疗结果：规范完成/失访/自行中断治疗/确诊肺结核',
    `preventive_manager`          VARCHAR(256) DEFAULT NULL COMMENT '预防性治疗期间随访管理人员',
    `remark`                      TEXT         DEFAULT NULL COMMENT '备注',
    `is_latent`                   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否潜伏管理者：0否 1是',
    `upload_batch`                VARCHAR(64)  DEFAULT NULL COMMENT '上传批次号',
    `create_time`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                  TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_id_number` (`id_number`),
    KEY `idx_latent` (`is_latent`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='重点人群筛查数据表（V4）';

-- ==================== 密接人群筛查表 ====================
-- V4 重大重构：三轮独立筛查（首次/半年后/一年后），每轮含感染筛查+胸片+诊断

CREATE TABLE IF NOT EXISTS `screening_close_contact` (
    `id`                              BIGINT       NOT NULL AUTO_INCREMENT,
    -- ===== 原患者信息 =====
    `city`                            VARCHAR(64)  DEFAULT NULL COMMENT '市/州',
    `district`                        VARCHAR(64)  DEFAULT NULL COMMENT '区/县',
    `source_patient_name`             VARCHAR(64)  DEFAULT NULL COMMENT '原患者姓名',
    `source_patient_case_no`          VARCHAR(64)  DEFAULT NULL COMMENT '原患者病案号',
    `source_patient_bacteriology_result` VARCHAR(64) DEFAULT NULL COMMENT '原患者病原学结果',
    `source_patient_phone`            VARCHAR(32)  DEFAULT NULL COMMENT '原患者电话',
    `source_patient_id_number`        VARCHAR(64)  DEFAULT NULL COMMENT '原患者身份证号',
    `report_date`                     DATE         DEFAULT NULL COMMENT '填表日期',
    `registration_date`               DATE         DEFAULT NULL COMMENT '密切接触者登记日期（6/12/24月随访基准）',
    -- ===== 接触者基本信息 =====
    `name`                            VARCHAR(64)  DEFAULT NULL COMMENT '接触者姓名',
    `id_number`                       VARCHAR(64)  DEFAULT NULL COMMENT '接触者身份证号',
    `age`                             INT          DEFAULT NULL COMMENT '年龄',
    `phone`                           VARCHAR(32)  DEFAULT NULL COMMENT '接触者电话',
    `contact_type`                    VARCHAR(32)  DEFAULT NULL COMMENT '接触类型：家庭内/家庭外',
    `contact_place`                   VARCHAR(64)  DEFAULT NULL COMMENT '接触场所',
    -- ===== 初次筛查（S-AE）=====
    `first_screen_date`               DATE         DEFAULT NULL COMMENT '首次筛查日期',
    `symptom1`                        VARCHAR(128) DEFAULT NULL COMMENT '结核症状1',
    `symptom2`                        VARCHAR(128) DEFAULT NULL COMMENT '结核症状2',
    `infection_check_date`            DATE         DEFAULT NULL COMMENT '感染检测日期',
    `infection_check_method`          VARCHAR(64)  DEFAULT NULL COMMENT '感染检测方法（EC/PPD/IGRA）',
    `infection_check_result`          VARCHAR(64)  DEFAULT NULL COMMENT '结果判定（阴性/阳性）',
    `imaging_date`                    DATE         DEFAULT NULL COMMENT '影像检查日期',
    `imaging_method`                  VARCHAR(64)  DEFAULT NULL COMMENT '影像方法（胸部X光片/胸部CT）',
    `imaging_result`                  VARCHAR(128) DEFAULT NULL COMMENT '影像结果',
    `sputum_check_date`               DATE         DEFAULT NULL COMMENT '痰检留标日期',
    `sputum_check_method`             VARCHAR(64)  DEFAULT NULL COMMENT '痰检方法',
    `sputum_check_result`             VARCHAR(64)  DEFAULT NULL COMMENT '痰检结果',
    `final_screening_result`          VARCHAR(32)  DEFAULT NULL COMMENT '最终筛查结果：活动性肺结核/潜伏感染者/未做/未发现异常',
    -- ===== 预防性治疗信息（AF-AM）=====
    `has_contraindication`            VARCHAR(32)  DEFAULT NULL COMMENT '有无禁忌症',
    `no_treatment_reason`             VARCHAR(128) DEFAULT NULL COMMENT '不接受预防治疗的原因',
    `contraindication_remark`         VARCHAR(256) DEFAULT NULL COMMENT '禁忌症备注',
    `has_preventive_treatment`        VARCHAR(10)  DEFAULT NULL COMMENT '是否开展预防治疗：开展/未开展',
    `preventive_plan`                 VARCHAR(128) DEFAULT NULL COMMENT '预防性治疗方案',
    `preventive_plan_remark`          VARCHAR(256) DEFAULT NULL COMMENT '其他方案备注',
    `treatment_completed`             VARCHAR(10)  DEFAULT NULL COMMENT '是否完成治疗：是/否',
    `incomplete_reason`               VARCHAR(128) DEFAULT NULL COMMENT '未完成原因',
    -- ===== 6月随访（AN-AX）=====
    `followup6_due_date`              DATE         DEFAULT NULL COMMENT '6月随访到期日期',
    `followup6_screen_date`           DATE         DEFAULT NULL COMMENT '6月-症状筛查日期',
    `followup6_symptom1`              VARCHAR(128) DEFAULT NULL COMMENT '6月-症状1',
    `followup6_symptom2`              VARCHAR(128) DEFAULT NULL COMMENT '6月-症状2',
    `followup6_imaging_date`          DATE         DEFAULT NULL COMMENT '6月-影像检查日期',
    `followup6_imaging_method`        VARCHAR(64)  DEFAULT NULL COMMENT '6月-影像方法',
    `followup6_imaging_result`        VARCHAR(128) DEFAULT NULL COMMENT '6月-影像结果',
    `followup6_sputum_date`           DATE         DEFAULT NULL COMMENT '6月-痰检日期',
    `followup6_sputum_method`         VARCHAR(64)  DEFAULT NULL COMMENT '6月-病原学方法',
    `followup6_sputum_result`         VARCHAR(64)  DEFAULT NULL COMMENT '6月-病原学结果',
    `followup6_result`                VARCHAR(32)  DEFAULT NULL COMMENT '6月随访筛查结果',
    -- ===== 12月随访（AY-BI）=====
    `followup12_due_date`             DATE         DEFAULT NULL COMMENT '12月随访到期日期',
    `followup12_screen_date`          DATE         DEFAULT NULL COMMENT '12月-症状筛查日期',
    `followup12_symptom1`             VARCHAR(128) DEFAULT NULL COMMENT '12月-症状1',
    `followup12_symptom2`             VARCHAR(128) DEFAULT NULL COMMENT '12月-症状2',
    `followup12_imaging_date`         DATE         DEFAULT NULL COMMENT '12月-影像检查日期',
    `followup12_imaging_method`       VARCHAR(64)  DEFAULT NULL COMMENT '12月-影像方法',
    `followup12_imaging_result`       VARCHAR(128) DEFAULT NULL COMMENT '12月-影像结果',
    `followup12_sputum_date`          DATE         DEFAULT NULL COMMENT '12月-痰检日期',
    `followup12_sputum_method`        VARCHAR(64)  DEFAULT NULL COMMENT '12月-病原学方法',
    `followup12_sputum_result`        VARCHAR(64)  DEFAULT NULL COMMENT '12月-病原学结果',
    `followup12_result`               VARCHAR(32)  DEFAULT NULL COMMENT '12月随访筛查结果',
    -- ===== 24月随访（BJ-BT）=====
    `followup24_due_date`             DATE         DEFAULT NULL COMMENT '24月随访到期日期',
    `followup24_screen_date`          DATE         DEFAULT NULL COMMENT '24月-症状筛查日期',
    `followup24_symptom1`             VARCHAR(128) DEFAULT NULL COMMENT '24月-症状1',
    `followup24_symptom2`             VARCHAR(128) DEFAULT NULL COMMENT '24月-症状2',
    `followup24_imaging_date`         DATE         DEFAULT NULL COMMENT '24月-影像检查日期',
    `followup24_imaging_method`       VARCHAR(64)  DEFAULT NULL COMMENT '24月-影像方法',
    `followup24_imaging_result`       VARCHAR(128) DEFAULT NULL COMMENT '24月-影像结果',
    `followup24_sputum_date`          DATE         DEFAULT NULL COMMENT '24月-痰检日期',
    `followup24_sputum_method`        VARCHAR(64)  DEFAULT NULL COMMENT '24月-病原学方法',
    `followup24_sputum_result`        VARCHAR(64)  DEFAULT NULL COMMENT '24月-病原学结果',
    `followup24_result`               VARCHAR(32)  DEFAULT NULL COMMENT '24月随访筛查结果',
    `remark`                          TEXT         DEFAULT NULL COMMENT '备注',
    -- ===== 系统字段 =====
    `year`                            VARCHAR(10)  DEFAULT NULL COMMENT '年份（从登记日期提取）',
    `gender`                          VARCHAR(10)  DEFAULT NULL COMMENT '性别',
    `ethnicity`                       VARCHAR(32)  DEFAULT NULL COMMENT '民族',
    `household_address`               VARCHAR(256) DEFAULT NULL COMMENT '户籍地址',
    `current_address`                 VARCHAR(256) DEFAULT NULL COMMENT '现住址',
    `cc_status`                       TINYINT      NOT NULL DEFAULT 0 COMMENT '密接流程状态：0待处理 1活动性肺结核-患者管理 2潜伏感染者-管理中 3潜伏感染者-归档 4随访监测中 5随访监测归档 6未发现异常-待3月复查 7-3月复查阴性结束 8-3月复查阳性转潜伏流程',
    `expected_treatment_end_date`     DATE         DEFAULT NULL COMMENT '系统设定的预计完成治疗时间（用于到期提醒）',
    `three_month_check_date`          DATE         DEFAULT NULL COMMENT '3月复查感染检测日期（未发现异常流程）',
    `three_month_check_result`        VARCHAR(64)  DEFAULT NULL COMMENT '3月复查感染检测结果',
    `three_month_final_result`        VARCHAR(16)  DEFAULT NULL COMMENT '3月复查最终判定：阴性/阳性',
    `upload_batch`                    VARCHAR(64)  DEFAULT NULL COMMENT '上传批次号',
    `create_time`                     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`                     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_id_number` (`id_number`),
    KEY `idx_cc_status` (`cc_status`),
    KEY `idx_final_result` (`final_screening_result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密接人群筛查数据表（新模板73列）';

-- ==================== 潜伏感染管理表 ====================
-- V4 新增：胸片检查字段（追踪到位后录入）、首次诊断字段、密接阳性轮次

CREATE TABLE IF NOT EXISTS `latent_infection` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT,
    `screening_id`        BIGINT       NOT NULL COMMENT '关联筛查数据ID',
    `population_type`     VARCHAR(32)  NOT NULL COMMENT '人群类型：school/keyPopulation/closeContact',
    `name`                VARCHAR(64)  DEFAULT NULL COMMENT '姓名',
    `id_number`           VARCHAR(64)  DEFAULT NULL COMMENT '证件号',
    `gender`              VARCHAR(10)  DEFAULT NULL COMMENT '性别',
    `age`                 INT          DEFAULT NULL COMMENT '年龄',
    `phone`               VARCHAR(32)  DEFAULT NULL COMMENT '联系电话',
    `infection_result`    VARCHAR(128) DEFAULT NULL COMMENT '感染筛查结果',
    `tracking_status`     TINYINT      NOT NULL DEFAULT 0 COMMENT '追踪状态：0待追踪 1到位 2未到位 3其他 4强制结束',
    `not_in_place_count`  INT          NOT NULL DEFAULT 0 COMMENT '未到位次数',
    `tracking_remark`     TEXT         DEFAULT NULL COMMENT '追踪备注原因',
    -- 追踪到位后录入胸片与诊断（V4新增步骤）
    `has_chest_xray`      VARCHAR(10)  DEFAULT NULL COMMENT '是否进行胸片检查（是/否）',
    `chest_xray_date`     DATE         DEFAULT NULL COMMENT '胸片检查日期',
    `chest_xray_result`   VARCHAR(128) DEFAULT NULL COMMENT '胸片检查结果：正常/异常/未查',
    `diagnosis_first`     VARCHAR(64)  DEFAULT NULL COMMENT '首次诊断：排除/疑似肺结核/潜伏感染者/确诊患者/其他',
    -- 密接人群阳性轮次（来自筛查表同步）
    `active_round`        TINYINT      DEFAULT NULL COMMENT '密接阳性轮次：1首次 2半年后 3一年后',
    `referral_result`     VARCHAR(32)  DEFAULT NULL COMMENT '转诊结果：excluded/other/confirmed/suspected/latent',
    `referral_remark`     TEXT         DEFAULT NULL COMMENT '转诊备注',
    `diagnosis_result`    VARCHAR(64)  DEFAULT NULL COMMENT '诊断结果展示列值',
    `treatment_phase`     TINYINT      NOT NULL DEFAULT 0 COMMENT '治疗阶段：0未开始 1预防治疗中 2已结案',
    `medication_status`   TINYINT      DEFAULT NULL COMMENT '服药状态：1按要求服药 2不服药',
    `archived`            TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已归档：0否 1是',
    `archived_time`       DATETIME     DEFAULT NULL COMMENT '结案归档时间',
    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`             TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_screening` (`screening_id`),
    KEY `idx_population` (`population_type`),
    KEY `idx_tracking` (`tracking_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='潜伏感染管理表（V4）';

-- ==================== 通知单表（潜伏者/患者通用） ====================

CREATE TABLE IF NOT EXISTS `notice` (
    `id`                     BIGINT       NOT NULL AUTO_INCREMENT,
    `notice_type`            VARCHAR(16)  NOT NULL COMMENT '通知单类型：latent=潜伏者通知单 patient=患者通知单',
    `population_type`        VARCHAR(32)  NOT NULL COMMENT '人群类型',
    `biz_id`                 BIGINT       NOT NULL COMMENT '关联业务ID（latent_infection.id 或 patient.id）',
    -- 基本信息（两类共用）
    `patient_name`           VARCHAR(64)  DEFAULT NULL COMMENT '患者/潜伏者姓名',
    `id_number`              VARCHAR(64)  DEFAULT NULL COMMENT '身份证',
    `gender`                 VARCHAR(10)  DEFAULT NULL COMMENT '性别',
    `birth_date`             DATE         DEFAULT NULL COMMENT '出生日期',
    `age`                    INT          DEFAULT NULL COMMENT '年龄',
    `phone`                  VARCHAR(32)  DEFAULT NULL COMMENT '联系方式',
    `crowd_category`         VARCHAR(128) DEFAULT NULL COMMENT '人群分类',
    `ethnicity`              VARCHAR(32)  DEFAULT NULL COMMENT '民族',
    `current_address`        VARCHAR(256) DEFAULT NULL COMMENT '现居住地址',
    `household_address`      VARCHAR(256) DEFAULT NULL COMMENT '户籍地址',
    -- 检查信息（两类共用）
    `chest_xray_date`        DATE         DEFAULT NULL COMMENT '胸片检查时间',
    `chest_xray_result`      VARCHAR(32)  DEFAULT NULL COMMENT '胸片检查结果：正常/异常/未查',
    `treatment_institution`  VARCHAR(256) DEFAULT NULL COMMENT '治疗机构',
    `issued_time`            DATE         DEFAULT NULL COMMENT '下发时间',
    -- 潜伏感染者通知单专用
    `infection_date`         DATE         DEFAULT NULL COMMENT '感染检测时间',
    `infection_method`       VARCHAR(64)  DEFAULT NULL COMMENT '感染检查方法：PPD/EC/IGRA',
    `infection_result_value` VARCHAR(128) DEFAULT NULL COMMENT '感染检查结果',
    `latent_treatment_option` VARCHAR(64) DEFAULT NULL COMMENT '废弃字段，治疗方案已统一使用 treatment_plan',
    -- 患者通知单专用
    `patient_type`           VARCHAR(32)  DEFAULT NULL COMMENT '患者类型：初治/复治',
    `management_method`      VARCHAR(64)  DEFAULT NULL COMMENT '管理方式：全程督导/强化督导/全程管理/未管理',
    `treatment_plan`         VARCHAR(256) DEFAULT NULL COMMENT '治疗方案（患者，FDC等7个方案）',
    `custom_plan_detail`     TEXT         DEFAULT NULL COMMENT '个体化方案详情',
    `sputum_smear`           VARCHAR(32)  DEFAULT NULL COMMENT '痰涂片：未出结果/阴性/阳性/未做/未知',
    `sputum_culture`         VARCHAR(32)  DEFAULT NULL COMMENT '痰培养',
    `molecular_test`         VARCHAR(32)  DEFAULT NULL COMMENT '分子检查',
    `pathology_test`         VARCHAR(32)  DEFAULT NULL COMMENT '病理学检查',
    `other_notes`            TEXT         DEFAULT NULL COMMENT '其他注意事项',
    -- 流转字段
    `sender_id`              BIGINT       NOT NULL COMMENT '发送人ID（4级）',
    `receiver_org_id`        BIGINT       DEFAULT NULL COMMENT '接收单位ID（5级）',
    `status`                 TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1已发送 2已确认',
    `sent_time`              DATETIME     DEFAULT NULL COMMENT '发送时间',
    `confirmed_time`         DATETIME     DEFAULT NULL COMMENT '确认接收时间',
    `timeout_notified`              TINYINT NOT NULL DEFAULT 0 COMMENT '是否已发送通知单48h超时提醒',
    `supervision_timeout_notified`  TINYINT NOT NULL DEFAULT 0 COMMENT '是否已发送督导表72h超时提醒',
    `visit_timeout_notified`        TINYINT NOT NULL DEFAULT 0 COMMENT '是否已发送首次随访72h超时提醒',
    `create_time`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_biz` (`biz_id`, `notice_type`),
    KEY `idx_status` (`status`, `sent_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知单表';

-- ==================== 督导表 ====================
-- V5 重大改造：按照 Excel 模板《潜伏感染预防性治疗督导表》字段完整重构

CREATE TABLE IF NOT EXISTS `supervision_form` (
    `id`                     BIGINT       NOT NULL AUTO_INCREMENT,
    `latent_infection_id`    BIGINT       NOT NULL COMMENT '关联潜伏感染ID',
    `population_type`        VARCHAR(32)  NOT NULL COMMENT '人群类型',
    `patient_name`           VARCHAR(64)  DEFAULT NULL COMMENT '患者姓名',
    -- V5 新增基本信息
    `category`               VARCHAR(64)  DEFAULT NULL COMMENT '类别：密接/新生筛查/65岁以上老年人/糖尿病人/双感/其他',
    `gender`                 VARCHAR(10)  DEFAULT NULL COMMENT '性别',
    `age`                    INT          DEFAULT NULL COMMENT '年龄',
    `phone`                  VARCHAR(32)  DEFAULT NULL COMMENT '电话号码',
    `current_address`        VARCHAR(256) DEFAULT NULL COMMENT '现住址',
    `treatment_start_date`   DATE         DEFAULT NULL COMMENT '预防性治疗开始日期',
    `treatment_plan`         VARCHAR(256) DEFAULT NULL COMMENT '治疗方案（含新增"不服药"）',
    -- V4 旧字段兼容保留（实体字段 supervisionContent 映射至此列）
    `supervision_content`    TEXT         DEFAULT NULL COMMENT '督导内容（V4旧字段，兼容保留）',
    -- V5 改造：督导记录改为 JSON 数组（督导时间/内容/方式/备注）
    `supervision_records`    TEXT         DEFAULT NULL COMMENT '督导记录（JSON数组：time/content/method/remark）',
    -- V5 新增：全疗程规律治疗评价
    `interrupt_medication`   VARCHAR(16)  DEFAULT NULL COMMENT '中断用药：有/无',
    `interrupt_count`        INT          DEFAULT NULL COMMENT '中断次数',
    `total_doses`            INT          DEFAULT NULL COMMENT '全程应用药次数',
    `actual_doses`           INT          DEFAULT NULL COMMENT '实际用药次数',
    `medication_rate`        VARCHAR(16)  DEFAULT NULL COMMENT '用药率（%）',
    `treatment_end_date`     DATE         DEFAULT NULL COMMENT '预防性治疗完成（结束疗程）时间',
    -- V4 旧字段兼容保留
    `preventive_result`      VARCHAR(64)  DEFAULT NULL COMMENT '预防性治疗结果：规范完成/失访/自行中断治疗/确诊肺结核（V4旧字段）',
    `preventive_manager`     VARCHAR(256) DEFAULT NULL COMMENT '预防性治疗期间随访管理人员（V4旧字段）',
    -- V5 新增：督导管理人员
    `manager_type`           VARCHAR(64)  DEFAULT NULL COMMENT '督导管理人员类型',
    `manager_name`           VARCHAR(64)  DEFAULT NULL COMMENT '督导管理人员姓名',
    `remark`                 TEXT         DEFAULT NULL COMMENT '备注',
    `attachment_urls`        TEXT         DEFAULT NULL COMMENT '附件（JSON数组，存储图片/文件URL）',
    `filled_by`              BIGINT       DEFAULT NULL COMMENT '填写人ID',
    `status`                 TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0未填写 1已填写 2已归档',
    `archived_time`          DATETIME     DEFAULT NULL COMMENT '归档时间',
    `create_time`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_latent` (`latent_infection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预防性治疗督导表（V5）';

-- ==================== 潜伏感染者电话随访表 ====================

CREATE TABLE IF NOT EXISTS `latent_follow_up` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT,
    `latent_infection_id`   BIGINT       NOT NULL COMMENT '关联潜伏感染ID',
    `follow_up_date`        DATE         NOT NULL COMMENT '随访日期',
    `follow_up_type`        VARCHAR(32)  NOT NULL DEFAULT '电话随访' COMMENT '随访方式',
    `content`               TEXT         DEFAULT NULL COMMENT '随访内容',
    `result`                VARCHAR(256) DEFAULT NULL COMMENT '随访结果',
    `operator`              VARCHAR(64)  DEFAULT NULL COMMENT '操作人',
    `create_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`               TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_latent` (`latent_infection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='潜伏感染者电话随访记录表';

-- ==================== 潜伏感染者按期检查表 ====================

CREATE TABLE IF NOT EXISTS `latent_check` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT,
    `latent_infection_id`   BIGINT       NOT NULL COMMENT '关联潜伏感染ID',
    `check_date`            DATE         NOT NULL COMMENT '检查日期',
    `check_period`          VARCHAR(32)  NOT NULL COMMENT '检查周期：3个月/6个月/12个月',
    `check_result`          VARCHAR(128) NOT NULL COMMENT '检查结果：未发病/发病/其他',
    `content`               TEXT         DEFAULT NULL COMMENT '检查详情',
    `operator`              VARCHAR(64)  DEFAULT NULL COMMENT '操作人',
    `create_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`               TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_latent` (`latent_infection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='潜伏感染者按期检查记录表';

-- ==================== 患者管理表 ====================

CREATE TABLE IF NOT EXISTS `patient` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT,
    `screening_id`        BIGINT       DEFAULT NULL COMMENT '关联筛查数据ID',
    `latent_infection_id` BIGINT       DEFAULT NULL COMMENT '关联潜伏感染ID（确诊来源）',
    `population_type`     VARCHAR(32)  NOT NULL COMMENT '人群类型',
    `name`                VARCHAR(64)  DEFAULT NULL COMMENT '姓名',
    `gender`              VARCHAR(10)  DEFAULT NULL COMMENT '性别',
    `birth_date`          DATE         DEFAULT NULL COMMENT '出生日期',
    `age`                 INT          DEFAULT NULL COMMENT '年龄',
    `id_type`             VARCHAR(32)  DEFAULT NULL COMMENT '证件类型',
    `id_number`           VARCHAR(64)  DEFAULT NULL COMMENT '证件号',
    `ethnicity`           VARCHAR(32)  DEFAULT NULL COMMENT '民族',
    `phone`               VARCHAR(32)  DEFAULT NULL COMMENT '联系电话',
    `household_address`   VARCHAR(256) DEFAULT NULL COMMENT '户籍所在地',
    `current_address`     VARCHAR(256) DEFAULT NULL COMMENT '现住址',
    `diagnosis_result`    VARCHAR(128) DEFAULT NULL COMMENT '诊断结果',
    `source`              VARCHAR(32)  NOT NULL DEFAULT 'confirmed' COMMENT '来源：confirmed=转诊确诊 epidemic=大疫情导入',
    `archived`            TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已归档（历史患者）',
    `archived_time`       DATETIME     DEFAULT NULL COMMENT '归档时间',
    `epidemic_data`       JSON         DEFAULT NULL COMMENT '大疫情表额外字段（JSON）',
    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`             TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_id_number` (`id_number`),
    KEY `idx_population` (`population_type`, `archived`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者管理表';

-- ==================== 首次入户随访记录表 ====================

-- 首次入户随访表字段与线下《肺结核患者第一次入户随访记录表》一致，详细字段存入 visit_content(JSON)
CREATE TABLE IF NOT EXISTS `first_visit` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT,
    `patient_id`            BIGINT       NOT NULL COMMENT '关联患者ID',
    `population_type`       VARCHAR(32)  NOT NULL COMMENT '人群类型',
    `visit_date`            DATE         DEFAULT NULL COMMENT '随访时间',
    `visit_method`          VARCHAR(16)  DEFAULT NULL COMMENT '随访方式：门诊/家庭',
    `patient_type`          VARCHAR(16)  DEFAULT NULL COMMENT '患者类型：初治/复治',
    `sputum_status`         VARCHAR(16)  DEFAULT NULL COMMENT '痰菌情况：阳性/阴性/未查痰',
    `drug_resistance`       VARCHAR(16)  DEFAULT NULL COMMENT '耐药情况：耐药/非耐药/未检测',
    `symptoms`              VARCHAR(256) DEFAULT NULL COMMENT '症状及体征（多选，逗号分隔编号）',
    `other_symptoms`        VARCHAR(256) DEFAULT NULL COMMENT '其他症状',
    `chemotherapy`          VARCHAR(256) DEFAULT NULL COMMENT '化疗方案',
    `medication_usage`      VARCHAR(16)  DEFAULT NULL COMMENT '用法：每日/间歇',
    `drug_form`             VARCHAR(64)  DEFAULT NULL COMMENT '药品剂型',
    `supervisor`            VARCHAR(32)  DEFAULT NULL COMMENT '督导人员：医生/家属/自服药/其他',
    `separate_room`         VARCHAR(8)   DEFAULT NULL COMMENT '单独的居室：有/无',
    `ventilation`           VARCHAR(8)   DEFAULT NULL COMMENT '通风情况：良好/一般/差',
    `smoking_amount`        VARCHAR(32)  DEFAULT NULL COMMENT '吸烟量（支/天）',
    `drinking_amount`       VARCHAR(32)  DEFAULT NULL COMMENT '饮酒量（两/天）',
    `medication_location`   VARCHAR(256) DEFAULT NULL COMMENT '取药地点',
    `medication_pick_time`  VARCHAR(64)  DEFAULT NULL COMMENT '取药时间',
    -- 健康教育9项，存 JSON：{"item": "掌握/未掌握"}
    `education_items`       JSON         DEFAULT NULL COMMENT '健康教育及培训各项掌握情况',
    `next_visit_date`       DATE         DEFAULT NULL COMMENT '下次随访时间',
    `doctor_signature`      VARCHAR(64)  DEFAULT NULL COMMENT '评估医生签名',
    `filled_by`             BIGINT       DEFAULT NULL COMMENT '填写人ID',
    `create_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`               TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_patient` (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首次入户随访记录表';

-- ==================== 后续随访记录表（患者随访汇总表）====================

CREATE TABLE IF NOT EXISTS `follow_up_visit` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT,
    `patient_id`            BIGINT       NOT NULL COMMENT '关联患者ID',
    `population_type`       VARCHAR(32)  NOT NULL COMMENT '人群类型',
    `visit_seq`             INT          DEFAULT NULL COMMENT '随访次数（第几次）',
    `visit_date`            DATE         DEFAULT NULL COMMENT '随访时间',
    `visit_method`          VARCHAR(16)  DEFAULT NULL COMMENT '随访方式：门诊/家庭',
    `visit_situation`       TEXT         DEFAULT NULL COMMENT '随访情况',
    `remarks`               TEXT         DEFAULT NULL COMMENT '备注',
    `attachment_url`        VARCHAR(512) DEFAULT NULL COMMENT '附件图片URL',
    `filled_by`             BIGINT       DEFAULT NULL COMMENT '填写人ID',
    `create_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`               TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_patient` (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后续随访记录表（患者随访汇总表）';

-- ==================== 服药管理表 ====================

CREATE TABLE IF NOT EXISTS `medication_management` (
    `id`                      BIGINT       NOT NULL AUTO_INCREMENT,
    `patient_id`              BIGINT       NOT NULL COMMENT '关联患者ID',
    `population_type`         VARCHAR(32)  NOT NULL COMMENT '人群类型',
    `management_method`       VARCHAR(32)  DEFAULT NULL COMMENT '管理方式',
    `supervisor`              VARCHAR(32)  DEFAULT NULL COMMENT '督导人员',
    `sputum_result`           VARCHAR(32)  DEFAULT NULL COMMENT '治疗前痰菌检查结果',
    `medication_records`      JSON         DEFAULT NULL COMMENT '每日服药记录（JSON：{日期:是否服药}）',
    `stop_date`               DATE         DEFAULT NULL COMMENT '停止完成时间',
    `create_time`             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                 TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_patient` (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服药管理表';

-- ==================== 大疫情导入表 ====================

CREATE TABLE IF NOT EXISTS `epidemic_report` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `population_type`   VARCHAR(32)  NOT NULL COMMENT '人群类型',
    `patient_id`        BIGINT       DEFAULT NULL COMMENT '匹配到的患者ID',
    `raw_data`          JSON         NOT NULL COMMENT '原始导入数据（JSON）',
    `matched`           TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已匹配',
    `upload_batch`      VARCHAR(64)  DEFAULT NULL COMMENT '上传批次号',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_patient` (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大疫情导入数据表';

-- ==================== 权限表 ====================

CREATE TABLE IF NOT EXISTS `permission` (
    `id`        BIGINT       NOT NULL AUTO_INCREMENT,
    `code`      VARCHAR(128) NOT NULL COMMENT '权限编码',
    `name`      VARCHAR(128) NOT NULL COMMENT '权限名称',
    `type`      TINYINT      NOT NULL COMMENT '类型：1=菜单 2=按钮/操作',
    `parent_id` BIGINT       NOT NULL DEFAULT 0 COMMENT '父权限ID，0为顶级',
    `sort`      INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- ==================== 角色-权限关联表 ====================

CREATE TABLE IF NOT EXISTS `role_permission` (
    `id`            BIGINT  NOT NULL AUTO_INCREMENT,
    `role`          TINYINT NOT NULL COMMENT '角色编号：1-6',
    `permission_id` BIGINT  NOT NULL COMMENT '权限ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_perm` (`role`, `permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ==================== 初始化权限数据 ====================

INSERT INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`) VALUES
-- 一级菜单
(1,  'school',          '学校人群',   1, 0, 1),
(2,  'keyPopulation',   '重点人群',   1, 0, 2),
(3,  'closeContact',    '密接人群',   1, 0, 3),
(4,  'statistics',      '统计分析',   1, 0, 4),
(5,  'message',         '系统消息',   1, 0, 5),
(6,  'system',          '系统管理',   1, 0, 6),
(7,  'dataCleaning',    '数据清洗',   1, 0, 7),
-- 学校人群子菜单
(10, 'school:screening',        '筛查管理',     1, 1, 1),
(11, 'school:latent',           '潜伏感染',     1, 1, 2),
(12, 'school:patient',          '患者管理',     1, 1, 3),
(13, 'school:history',          '历史患者',     1, 1, 4),
-- 重点人群子菜单
(20, 'keyPopulation:screening', '筛查管理',     1, 2, 1),
(21, 'keyPopulation:latent',    '潜伏感染',     1, 2, 2),
(22, 'keyPopulation:patient',   '患者管理',     1, 2, 3),
(23, 'keyPopulation:history',   '历史患者',     1, 2, 4),
-- 密接人群子菜单
(30, 'closeContact:screening',  '筛查管理',     1, 3, 1),
(31, 'closeContact:latent',     '潜伏感染',     1, 3, 2),
(32, 'closeContact:patient',    '患者管理',     1, 3, 3),
(33, 'closeContact:history',    '历史患者',     1, 3, 4),
-- 系统管理子菜单
(60, 'system:users',            '用户管理',     1, 6, 1),
(61, 'system:permissions',      '权限管理',     1, 6, 2),
-- 筛查操作按钮（挂在 school:screening=10 下，三条主线共用此权限）
(100, 'screening:upload',       '上传筛查数据',       2, 10, 1),
(101, 'screening:create',       '新增筛查数据',       2, 10, 2),
(102, 'screening:export',       '导出筛查数据',       2, 10, 3),
(103, 'screening:edit',         '编辑筛查数据',       2, 10, 4),
(104, 'screening:delete',       '删除筛查数据',       2, 10, 5),
-- 重点人群筛查操作按钮（挂在 keyPopulation:screening=20 下）
(210, 'keyPopulation:screening:upload', '上传筛查数据', 2, 20, 1),
(211, 'keyPopulation:screening:create', '新增筛查数据', 2, 20, 2),
(212, 'keyPopulation:screening:export', '导出筛查数据', 2, 20, 3),
(213, 'keyPopulation:screening:edit',   '编辑筛查数据', 2, 20, 4),
(214, 'keyPopulation:screening:delete', '删除筛查数据', 2, 20, 5),
-- 密接人群筛查操作按钮（挂在 closeContact:screening=30 下）
(310, 'closeContact:screening:upload', '上传筛查数据', 2, 30, 1),
(311, 'closeContact:screening:create', '新增筛查数据', 2, 30, 2),
(312, 'closeContact:screening:export', '导出筛查数据', 2, 30, 3),
(313, 'closeContact:screening:edit',   '编辑筛查数据', 2, 30, 4),
(314, 'closeContact:screening:delete', '删除筛查数据', 2, 30, 5),
-- 潜伏感染操作按钮（挂在 school:latent=11 下，三条主线共用）
(110, 'latent:track',           '追踪',               2, 11, 1),
(111, 'latent:referral',        '转诊',               2, 11, 2),
(112, 'latent:sendNotice',      '发送潜伏者通知单',   2, 11, 3),
(113, 'latent:confirmNotice',   '确认接收通知单',     2, 11, 4),
(114, 'latent:supervision',     '填写督导表',         2, 11, 5),
-- 患者管理操作按钮（挂在 school:patient=12 下，三条主线共用）
(120, 'patient:importEpidemic', '导入大疫情表',       2, 12, 1),
(121, 'patient:sendNotice',     '发送患者通知单',     2, 12, 2),
(122, 'patient:confirmNotice',  '确认接收患者通知单', 2, 12, 3),
(123, 'patient:firstVisit',     '首次随访',           2, 12, 4),
(124, 'patient:followUp',       '后续随访',           2, 12, 5),
(125, 'patient:medication',     '服药管理',           2, 12, 6),
-- 统计操作（挂在 statistics=4 下）
(130, 'statistics:export',      '导出统计',           2, 4,  1),
-- 用户管理操作（挂在 system:users=60 下）
(140, 'user:create',            '创建用户',           2, 60, 1),
(141, 'user:edit',              '编辑用户',           2, 60, 2),
(142, 'user:delete',            '删除用户',           2, 60, 3),
-- 权限管理操作（挂在 system:permissions=61 下）
(143, 'permission:assign',      '分配权限',           2, 61, 1);

-- ==================== 默认角色权限分配 ====================
-- 超级管理员(1)：全部权限
INSERT INTO `role_permission` (`role`, `permission_id`)
SELECT 1, `id` FROM `permission`;

-- 一级(2)：除系统管理外所有菜单 + 所有业务操作
INSERT INTO `role_permission` (`role`, `permission_id`)
SELECT 2, `id` FROM `permission` WHERE `code` NOT IN ('system', 'system:users', 'system:permissions', 'user:create', 'user:edit', 'user:delete', 'permission:assign');

-- 二级(3)：同一级
INSERT INTO `role_permission` (`role`, `permission_id`)
SELECT 3, `id` FROM `permission` WHERE `code` NOT IN ('system', 'system:users', 'system:permissions', 'user:create', 'user:edit', 'user:delete', 'permission:assign');

-- 三级(4)：所有业务菜单 + 大部分操作（不含导出统计）
INSERT INTO `role_permission` (`role`, `permission_id`)
SELECT 4, `id` FROM `permission` WHERE `code` NOT IN ('system', 'system:users', 'system:permissions', 'user:create', 'user:edit', 'user:delete', 'permission:assign', 'statistics:export');

-- 四级(5)：业务菜单 + 发送通知单/追踪/转诊/督导/随访/服药/上传
INSERT INTO `role_permission` (`role`, `permission_id`)
SELECT 5, `id` FROM `permission` WHERE `code` IN (
  'school','keyPopulation','closeContact','statistics','message',
  'school:screening','school:latent','school:patient','school:history',
  'keyPopulation:screening','keyPopulation:latent','keyPopulation:patient','keyPopulation:history',
  'closeContact:screening','closeContact:latent','closeContact:patient','closeContact:history',
  'screening:upload','screening:create','screening:export','screening:edit','screening:delete',
  'keyPopulation:screening:upload','keyPopulation:screening:create','keyPopulation:screening:export','keyPopulation:screening:edit','keyPopulation:screening:delete',
  'closeContact:screening:upload','closeContact:screening:create','closeContact:screening:export','closeContact:screening:edit','closeContact:screening:delete',
  'latent:track','latent:referral','latent:sendNotice','latent:supervision',
  'patient:importEpidemic','patient:sendNotice','patient:firstVisit','patient:followUp','patient:medication'
);

-- 五级(6)：仅消息页面 + 通知确认/督导/随访/服药操作权限（无业务页面菜单权限）
INSERT INTO `role_permission` (`role`, `permission_id`)
SELECT 6, `id` FROM `permission` WHERE `code` IN (
  'message',
  'latent:confirmNotice','latent:supervision','patient:confirmNotice','patient:firstVisit','patient:followUp','patient:medication'
);

-- ==================== 修复操作按钮 parent_id（数据库已存在时执行） ====================
-- 若数据库已初始化，运行以下语句将操作权限挂到正确的父菜单下
UPDATE `permission` SET `parent_id` = 10 WHERE `code` IN ('screening:upload','screening:create','screening:export','screening:edit','screening:delete');
UPDATE `permission` SET `parent_id` = 20 WHERE `code` IN ('keyPopulation:screening:upload','keyPopulation:screening:create','keyPopulation:screening:export','keyPopulation:screening:edit','keyPopulation:screening:delete');
UPDATE `permission` SET `parent_id` = 30 WHERE `code` IN ('closeContact:screening:upload','closeContact:screening:create','closeContact:screening:export','closeContact:screening:edit','closeContact:screening:delete');
UPDATE `permission` SET `parent_id` = 11 WHERE `code` IN ('latent:track','latent:referral','latent:sendNotice','latent:confirmNotice','latent:supervision');
UPDATE `permission` SET `parent_id` = 12 WHERE `code` IN ('patient:importEpidemic','patient:sendNotice','patient:confirmNotice','patient:firstVisit','patient:followUp','patient:medication');
UPDATE `permission` SET `parent_id` = 4  WHERE `code` = 'statistics:export';
UPDATE `permission` SET `parent_id` = 60 WHERE `code` IN ('user:create','user:edit','user:delete');
UPDATE `permission` SET `parent_id` = 61 WHERE `code` = 'permission:assign';

-- ==================== 潜伏治疗扩展（已有数据库升级用） ====================
ALTER TABLE `latent_infection` ADD COLUMN IF NOT EXISTS `treatment_phase` TINYINT NOT NULL DEFAULT 0 COMMENT '治疗阶段：0未开始 1预防治疗中 2已结案' AFTER `diagnosis_result`;
ALTER TABLE `latent_infection` ADD COLUMN IF NOT EXISTS `medication_status` TINYINT DEFAULT NULL COMMENT '服药状态：1按要求服药 2不服药' AFTER `treatment_phase`;
ALTER TABLE `latent_infection` ADD COLUMN IF NOT EXISTS `archived_time` DATETIME DEFAULT NULL COMMENT '结案归档时间' AFTER `archived`;

-- V4 扩展：潜伏感染表新增胸片/诊断/轮次字段
ALTER TABLE `latent_infection` ADD COLUMN IF NOT EXISTS `has_chest_xray`    VARCHAR(10)  DEFAULT NULL COMMENT '是否进行胸片检查' AFTER `tracking_remark`;
ALTER TABLE `latent_infection` ADD COLUMN IF NOT EXISTS `chest_xray_date`   DATE         DEFAULT NULL COMMENT '胸片检查日期' AFTER `has_chest_xray`;
ALTER TABLE `latent_infection` ADD COLUMN IF NOT EXISTS `chest_xray_result` VARCHAR(128) DEFAULT NULL COMMENT '胸片检查结果：正常/异常/未查' AFTER `chest_xray_date`;
ALTER TABLE `latent_infection` ADD COLUMN IF NOT EXISTS `diagnosis_first`   VARCHAR(64)  DEFAULT NULL COMMENT '首次诊断结果' AFTER `chest_xray_result`;
ALTER TABLE `latent_infection` ADD COLUMN IF NOT EXISTS `active_round`      TINYINT      DEFAULT NULL COMMENT '密接阳性轮次：1首次 2半年后 3一年后' AFTER `diagnosis_first`;
-- referral_result 新增 suspected 值，无需迁移，注释说明即可
-- V4 增量迁移（用存储过程忽略 1060 重复列错误，兼容 MySQL）
DROP PROCEDURE IF EXISTS _v4_migrate;
DELIMITER $$
CREATE PROCEDURE _v4_migrate()
BEGIN
    DECLARE CONTINUE HANDLER FOR 1060 BEGIN END;

    -- 督导表新增治疗完成时间/结果/管理人员
    ALTER TABLE `supervision_form` ADD COLUMN `treatment_end_date` DATE         DEFAULT NULL COMMENT '预防性治疗完成时间' AFTER `treatment_start_date`;
    ALTER TABLE `supervision_form` ADD COLUMN `preventive_result`  VARCHAR(64)  DEFAULT NULL COMMENT '预防性治疗结果' AFTER `supervision_content`;
    ALTER TABLE `supervision_form` ADD COLUMN `preventive_manager` VARCHAR(256) DEFAULT NULL COMMENT '预防性治疗期间随访管理人员' AFTER `preventive_result`;

    -- screening_school 加回胸片/诊断/预防治疗系统回写列
    ALTER TABLE `screening_school` ADD COLUMN `has_chest_xray`            VARCHAR(8)   DEFAULT NULL COMMENT '是否进行胸片检查' AFTER `infection_result`;
    ALTER TABLE `screening_school` ADD COLUMN `chest_xray_date`           DATE         DEFAULT NULL COMMENT '胸片检查日期' AFTER `has_chest_xray`;
    ALTER TABLE `screening_school` ADD COLUMN `chest_xray_result`         VARCHAR(128) DEFAULT NULL COMMENT '胸片结果' AFTER `chest_xray_date`;
    ALTER TABLE `screening_school` ADD COLUMN `diagnosis_first`           VARCHAR(64)  DEFAULT NULL COMMENT '诊断结果-首次' AFTER `chest_xray_result`;
    ALTER TABLE `screening_school` ADD COLUMN `diagnosis_half_year`       VARCHAR(64)  DEFAULT NULL COMMENT '诊断结果-半年后' AFTER `diagnosis_first`;
    ALTER TABLE `screening_school` ADD COLUMN `diagnosis_one_year`        VARCHAR(64)  DEFAULT NULL COMMENT '诊断结果-一年后' AFTER `diagnosis_half_year`;
    ALTER TABLE `screening_school` ADD COLUMN `has_preventive_treatment`  VARCHAR(10)  DEFAULT NULL COMMENT '是否进行预防性治疗' AFTER `diagnosis_one_year`;

    -- screening_key_population 加回胸片/诊断/预防治疗系统回写列
    ALTER TABLE `screening_key_population` ADD COLUMN `has_chest_xray`            VARCHAR(8)   DEFAULT NULL COMMENT '是否进行胸片检查' AFTER `infection_result`;
    ALTER TABLE `screening_key_population` ADD COLUMN `chest_xray_date`           DATE         DEFAULT NULL COMMENT '胸片检查日期' AFTER `has_chest_xray`;
    ALTER TABLE `screening_key_population` ADD COLUMN `chest_xray_result`         VARCHAR(128) DEFAULT NULL COMMENT '胸片结果' AFTER `chest_xray_date`;
    ALTER TABLE `screening_key_population` ADD COLUMN `diagnosis_first`           VARCHAR(64)  DEFAULT NULL COMMENT '诊断结果-首次' AFTER `chest_xray_result`;
    ALTER TABLE `screening_key_population` ADD COLUMN `diagnosis_half_year`       VARCHAR(64)  DEFAULT NULL COMMENT '诊断结果-半年后' AFTER `diagnosis_first`;
    ALTER TABLE `screening_key_population` ADD COLUMN `diagnosis_one_year`        VARCHAR(64)  DEFAULT NULL COMMENT '诊断结果-一年后' AFTER `diagnosis_half_year`;
    ALTER TABLE `screening_key_population` ADD COLUMN `has_preventive_treatment`  VARCHAR(10)  DEFAULT NULL COMMENT '是否进行预防性治疗' AFTER `diagnosis_one_year`;
END$$
DELIMITER ;
CALL _v4_migrate();
DROP PROCEDURE IF EXISTS _v4_migrate;

-- 新增潜伏治疗相关操作权限
INSERT IGNORE INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`) VALUES
(115, 'latent:followUp',    '潜伏电话随访',   2, 11, 6),
(116, 'latent:check',       '潜伏按期检查',   2, 11, 7),
(117, 'latent:closeCase',   '潜伏结案归档',   2, 11, 8),
-- V4 新增：胸片录入权限
(118, 'latent:xray',        '录入胸片诊断',   2, 11, 9);

-- 四级(5)和五级(6)获得新权限
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`) VALUES (5, 115), (5, 116), (5, 117), (5, 118), (6, 115), (6, 116), (6, 117);
-- 上级角色也获得
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`) SELECT r.`role`, p.`id` FROM (SELECT 1 AS `role` UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) r CROSS JOIN `permission` p WHERE p.`code` IN ('latent:followUp','latent:check','latent:closeCase','latent:xray');

-- 系统管理：数据备份权限（admin 专属，parent_id=6 对应系统管理节点）
INSERT IGNORE INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`) VALUES
(119, 'system:backup', '数据备份', 2, 6, 10);
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`) VALUES (1, 119);

-- V6 新增：筛查模块按钮级权限（用于重点/密接筛查页面操作）
INSERT IGNORE INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`) VALUES
(101, 'screening:create', '新增筛查数据', 2, 10, 2),
(102, 'screening:export', '导出筛查数据', 2, 10, 3),
(103, 'screening:edit',   '编辑筛查数据', 2, 10, 4),
(104, 'screening:delete', '删除筛查数据', 2, 10, 5);
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`) VALUES
(5, 101), (5, 102), (5, 103), (5, 104);

-- V7 新增：重点/密接筛查独立按钮权限（权限树分别展示到各自主线下）
INSERT IGNORE INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`) VALUES
(210, 'keyPopulation:screening:upload', '上传筛查数据', 2, 20, 1),
(211, 'keyPopulation:screening:create', '新增筛查数据', 2, 20, 2),
(212, 'keyPopulation:screening:export', '导出筛查数据', 2, 20, 3),
(213, 'keyPopulation:screening:edit',   '编辑筛查数据', 2, 20, 4),
(214, 'keyPopulation:screening:delete', '删除筛查数据', 2, 20, 5),
(310, 'closeContact:screening:upload', '上传筛查数据', 2, 30, 1),
(311, 'closeContact:screening:create', '新增筛查数据', 2, 30, 2),
(312, 'closeContact:screening:export', '导出筛查数据', 2, 30, 3),
(313, 'closeContact:screening:edit',   '编辑筛查数据', 2, 30, 4),
(314, 'closeContact:screening:delete', '删除筛查数据', 2, 30, 5);
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`) VALUES
(5, 210), (5, 211), (5, 212), (5, 213), (5, 214),
(5, 310), (5, 311), (5, 312), (5, 313), (5, 314);
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.`role`, p.`id`
FROM (SELECT 1 AS `role` UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) r
CROSS JOIN `permission` p
WHERE p.`code` IN (
  'keyPopulation:screening:upload','keyPopulation:screening:create','keyPopulation:screening:export','keyPopulation:screening:edit','keyPopulation:screening:delete',
  'closeContact:screening:upload','closeContact:screening:create','closeContact:screening:export','closeContact:screening:edit','closeContact:screening:delete'
);

-- V5 迁移：notice 表补充 ethnicity 字段
ALTER TABLE `notice` ADD COLUMN IF NOT EXISTS `ethnicity` VARCHAR(32) DEFAULT NULL COMMENT '民族' AFTER `crowd_category`;

-- V5 迁移：重点人群筛查表补充乡镇/社区字段
ALTER TABLE `screening_key_population` ADD COLUMN `township_community` VARCHAR(128) DEFAULT NULL COMMENT '乡镇/社区' AFTER `household_address`;

-- ==================== V5 迁移：督导表字段完整重构 ====================
DROP PROCEDURE IF EXISTS _v5_migrate_supervision;
DELIMITER $$
CREATE PROCEDURE _v5_migrate_supervision()
BEGIN
    DECLARE CONTINUE HANDLER FOR 1060 BEGIN END;

    -- V5 新增基本信息字段
    ALTER TABLE `supervision_form` ADD COLUMN `category`               VARCHAR(64)  DEFAULT NULL COMMENT '类别：密接/新生筛查/65岁以上老年人/糖尿病人/双感/其他' AFTER `patient_name`;
    ALTER TABLE `supervision_form` ADD COLUMN `gender`                 VARCHAR(10)  DEFAULT NULL COMMENT '性别' AFTER `category`;
    ALTER TABLE `supervision_form` ADD COLUMN `age`                    INT          DEFAULT NULL COMMENT '年龄' AFTER `gender`;
    ALTER TABLE `supervision_form` ADD COLUMN `phone`                  VARCHAR(32)  DEFAULT NULL COMMENT '电话号码' AFTER `age`;
    ALTER TABLE `supervision_form` ADD COLUMN `current_address`        VARCHAR(256) DEFAULT NULL COMMENT '现住址' AFTER `phone`;

    -- V4 旧字段兼容保留：若旧库无此列则补加（重复时 CONTINUE HANDLER 静默忽略）
    ALTER TABLE `supervision_form` ADD COLUMN `supervision_content`    TEXT         DEFAULT NULL COMMENT '督导内容（V4旧字段，兼容保留）' AFTER `treatment_plan`;
    -- V5 改造：supervision_content → supervision_records（保留原字段兼容）
    ALTER TABLE `supervision_form` ADD COLUMN `supervision_records`    TEXT         DEFAULT NULL COMMENT '督导记录（JSON数组：time/content/method/remark）' AFTER `supervision_content`;

    -- V5 新增：全疗程规律治疗评价
    ALTER TABLE `supervision_form` ADD COLUMN `interrupt_medication`   VARCHAR(16)  DEFAULT NULL COMMENT '中断用药：有/无' AFTER `supervision_records`;
    ALTER TABLE `supervision_form` ADD COLUMN `interrupt_count`        INT          DEFAULT NULL COMMENT '中断次数' AFTER `interrupt_medication`;
    ALTER TABLE `supervision_form` ADD COLUMN `total_doses`            INT          DEFAULT NULL COMMENT '全程应用药次数' AFTER `interrupt_count`;
    ALTER TABLE `supervision_form` ADD COLUMN `actual_doses`           INT          DEFAULT NULL COMMENT '实际用药次数' AFTER `total_doses`;
    ALTER TABLE `supervision_form` ADD COLUMN `medication_rate`        VARCHAR(16)  DEFAULT NULL COMMENT '用药率（%）' AFTER `actual_doses`;

    -- V5 新增：督导管理人员
    ALTER TABLE `supervision_form` ADD COLUMN `manager_type`           VARCHAR(64)  DEFAULT NULL COMMENT '督导管理人员类型' AFTER `treatment_end_date`;
    ALTER TABLE `supervision_form` ADD COLUMN `manager_name`           VARCHAR(64)  DEFAULT NULL COMMENT '督导管理人员姓名' AFTER `manager_type`;

    -- V5 新增：备注与附件
    ALTER TABLE `supervision_form` ADD COLUMN `remark`                 TEXT         DEFAULT NULL COMMENT '备注' AFTER `manager_name`;
    ALTER TABLE `supervision_form` ADD COLUMN `attachment_urls`        TEXT         DEFAULT NULL COMMENT '附件（JSON数组，存储图片/文件URL）' AFTER `remark`;
END$$
DELIMITER ;
CALL _v5_migrate_supervision();
DROP PROCEDURE IF EXISTS _v5_migrate_supervision;

-- ==================== V8 迁移：部门隔离 + 五级权限收敛 ====================

-- 新建部门表
CREATE TABLE IF NOT EXISTS `department` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(64)  NOT NULL COMMENT '部门名称',
    `description` VARCHAR(256) DEFAULT NULL COMMENT '描述',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 为各主表添加 department_id 字段（若列已存在会报错，忽略即可）
ALTER TABLE `user`                     ADD COLUMN `department_id` BIGINT DEFAULT NULL COMMENT '所属部门ID';
ALTER TABLE `screening_school`         ADD COLUMN `department_id` BIGINT DEFAULT NULL COMMENT '所属部门ID';
ALTER TABLE `screening_key_population` ADD COLUMN `department_id` BIGINT DEFAULT NULL COMMENT '所属部门ID';
ALTER TABLE `screening_close_contact`  ADD COLUMN `department_id` BIGINT DEFAULT NULL COMMENT '所属部门ID';
ALTER TABLE `latent_infection`         ADD COLUMN `department_id` BIGINT DEFAULT NULL COMMENT '所属部门ID';
ALTER TABLE `patient`                  ADD COLUMN `department_id` BIGINT DEFAULT NULL COMMENT '所属部门ID';

-- 新增部门管理权限码（id=62，挂在 system=6 下）
INSERT IGNORE INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`) VALUES
(62, 'system:department', '部门管理', 1, 6, 3);

-- 超级管理员(1) 绑定部门管理权限
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`) VALUES (1, 62);

-- 修复已存在数据库中 role=6 的权限：移除业务菜单，仅保留消息+操作权限
DELETE FROM `role_permission`
WHERE `role` = 6
  AND `permission_id` IN (
    SELECT `id` FROM `permission`
    WHERE `code` IN (
      'school','keyPopulation','closeContact','statistics',
      'school:screening','school:suspected','school:latent','school:patient','school:history',
      'keyPopulation:screening','keyPopulation:suspected','keyPopulation:latent','keyPopulation:patient','keyPopulation:history',
      'closeContact:screening','closeContact:followUp','closeContact:latent','closeContact:patient','closeContact:history',
      'screening:upload','screening:create','screening:export','screening:edit','screening:delete',
      'keyPopulation:screening:upload','keyPopulation:screening:create','keyPopulation:screening:export','keyPopulation:screening:edit','keyPopulation:screening:delete',
      'closeContact:screening:upload','closeContact:screening:create','closeContact:screening:export','closeContact:screening:edit','closeContact:screening:delete',
      'latent:track','latent:referral','latent:sendNotice','latent:xray',
      'patient:importEpidemic','patient:sendNotice',
      'statistics:export',
      'latent:closeCase'
    )
  );

-- 确保 role=6 拥有所需的消息页面操作权限（幂等）
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT 6, `id` FROM `permission`
WHERE `code` IN (
  'message',
  'latent:confirmNotice','latent:supervision','latent:followUp','latent:check',
  'patient:confirmNotice','patient:firstVisit','patient:followUp','patient:medication'
);

-- 修复 V5 全新安装数据库中缺失 supervision_content 列导致 SELECT 报错的问题
-- （V4 → V5 迁移的数据库该列已存在，IF NOT EXISTS 可安全幂等执行）
ALTER TABLE `supervision_form` ADD COLUMN IF NOT EXISTS `supervision_content` TEXT DEFAULT NULL COMMENT '督导内容（V4旧字段，兼容保留）';

-- ==================== 分级诊疗表 ====================
CREATE TABLE IF NOT EXISTS `referral` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `biz_id`          BIGINT       NOT NULL COMMENT '关联业务记录ID',
    `biz_type`        VARCHAR(64)  NOT NULL COMMENT '业务类型：screening_school/screening_key/screening_close/suspected_school/suspected_key/suspected_close/latent_school/latent_key/latent_close/patient_school/patient_key/patient_close',
    `population_type` VARCHAR(32)  NOT NULL COMMENT '人群类型：school/key/close',
    `module_type`     VARCHAR(32)  NOT NULL COMMENT '模块类型：screening/suspected/latent/patient',
    `subject_name`    VARCHAR(64)  DEFAULT NULL COMMENT '对象姓名（用于展示）',
    `summary`         TEXT         DEFAULT NULL COMMENT '推送的业务摘要（JSON格式）',
    `sender_id`       BIGINT       NOT NULL COMMENT '发送方用户ID',
    `receiver_org_id` BIGINT       DEFAULT NULL COMMENT '接收方用户/部门ID',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1=待确认 2=已接收 3=已拒绝',
    `sent_time`       DATETIME     DEFAULT NULL COMMENT '发送时间',
    `confirmed_time`  DATETIME     DEFAULT NULL COMMENT '接收时间',
    `rejected_time`   DATETIME     DEFAULT NULL COMMENT '拒绝时间',
    `reject_reason`   VARCHAR(256) DEFAULT NULL COMMENT '拒绝原因',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_biz` (`biz_id`, `biz_type`),
    KEY `idx_sender` (`sender_id`),
    KEY `idx_receiver` (`receiver_org_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分级诊疗推送记录表';

-- 为分级诊疗操作权限添加预设（各模块均可配置；挂到系统消息下便于权限树展示与分配）
INSERT IGNORE INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`)
VALUES ('referral', '分级诊疗', 2, 5, 50);

-- 确保全部角色均拥有分级诊疗权限（幂等）
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id FROM
  (SELECT 2 AS role UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) r,
  `permission` p
WHERE p.code = 'referral';

-- ==================== V8：重点人群/密接人群 潜伏感染 & 患者管理 独立按钮级权限 ====================
INSERT IGNORE INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`) VALUES
-- 重点人群 潜伏感染 按钮（挂在 keyPopulation:latent=21 下）
(220, 'keyPopulation:latent:sendNotice',    '发送潜伏者通知单', 2, 21, 1),
(221, 'keyPopulation:latent:confirmNotice', '确认接收通知单',   2, 21, 2),
(222, 'keyPopulation:latent:supervision',   '填写督导表',       2, 21, 3),
(223, 'keyPopulation:latent:followUp',      '潜伏电话随访',     2, 21, 4),
(224, 'keyPopulation:latent:check',         '潜伏按期检查',     2, 21, 5),
(225, 'keyPopulation:latent:closeCase',     '潜伏结案归档',     2, 21, 6),
-- 密接人群 潜伏感染 按钮（挂在 closeContact:latent=31 下）
(320, 'closeContact:latent:treatmentDecision', '确认预防治疗',     2, 31, 1),
(321, 'closeContact:latent:sendNotice',        '发送通知单',       2, 31, 2),
(322, 'closeContact:latent:confirmNotice',     '确认接收通知单',   2, 31, 3),
(323, 'closeContact:latent:supervision',       '填写督导表',       2, 31, 4),
(324, 'closeContact:latent:setExpectedDate',   '设置预计完成时间', 2, 31, 5),
(325, 'closeContact:latent:confirmTreatment',  '确认治疗完成',     2, 31, 6),
(326, 'closeContact:latent:check',             '录入随访复查',     2, 31, 7),
-- 重点人群 患者管理 按钮（挂在 keyPopulation:patient=22 下）
(230, 'keyPopulation:patient:importEpidemic', '导入大疫情表',         2, 22, 1),
(231, 'keyPopulation:patient:sendNotice',     '发送患者通知单',       2, 22, 2),
(232, 'keyPopulation:patient:confirmNotice',  '确认接收患者通知单',   2, 22, 3),
(233, 'keyPopulation:patient:firstVisit',     '首次随访',             2, 22, 4),
(234, 'keyPopulation:patient:followUp',       '后续随访',             2, 22, 5),
(235, 'keyPopulation:patient:medication',     '服药管理',             2, 22, 6),
-- 密接人群 患者管理 按钮（挂在 closeContact:patient=32 下）
(330, 'closeContact:patient:importEpidemic', '导入大疫情表',         2, 32, 1),
(331, 'closeContact:patient:sendNotice',     '发送患者通知单',       2, 32, 2),
(332, 'closeContact:patient:confirmNotice',  '确认接收患者通知单',   2, 32, 3),
(333, 'closeContact:patient:firstVisit',     '首次随访',             2, 32, 4),
(334, 'closeContact:patient:followUp',       '后续随访',             2, 32, 5),
(335, 'closeContact:patient:medication',     '服药管理',             2, 32, 6);

-- 超级管理员及一~三级：获得全部新按钮权限
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id
FROM (SELECT 1 AS role UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) r
CROSS JOIN `permission` p
WHERE p.code IN (
  'keyPopulation:latent:sendNotice','keyPopulation:latent:confirmNotice','keyPopulation:latent:supervision',
  'keyPopulation:latent:followUp','keyPopulation:latent:check','keyPopulation:latent:closeCase',
  'closeContact:latent:treatmentDecision','closeContact:latent:sendNotice','closeContact:latent:confirmNotice',
  'closeContact:latent:supervision','closeContact:latent:setExpectedDate','closeContact:latent:confirmTreatment','closeContact:latent:check',
  'keyPopulation:patient:importEpidemic','keyPopulation:patient:sendNotice','keyPopulation:patient:confirmNotice',
  'keyPopulation:patient:firstVisit','keyPopulation:patient:followUp','keyPopulation:patient:medication',
  'closeContact:patient:importEpidemic','closeContact:patient:sendNotice','closeContact:patient:confirmNotice',
  'closeContact:patient:firstVisit','closeContact:patient:followUp','closeContact:patient:medication'
);

-- 四级(5)：操作权限（发送/督导/随访/检查/结案/治疗决策/导入）
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT 5, p.id FROM `permission` p
WHERE p.code IN (
  'keyPopulation:latent:sendNotice','keyPopulation:latent:supervision',
  'keyPopulation:latent:followUp','keyPopulation:latent:check','keyPopulation:latent:closeCase',
  'closeContact:latent:treatmentDecision','closeContact:latent:sendNotice',
  'closeContact:latent:supervision','closeContact:latent:setExpectedDate','closeContact:latent:check',
  'keyPopulation:patient:importEpidemic','keyPopulation:patient:sendNotice',
  'keyPopulation:patient:firstVisit','keyPopulation:patient:followUp','keyPopulation:patient:medication',
  'closeContact:patient:importEpidemic','closeContact:patient:sendNotice',
  'closeContact:patient:firstVisit','closeContact:patient:followUp','closeContact:patient:medication'
);

-- 五级(6)：接收确认 + 督导 + 随访操作权限
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT 6, p.id FROM `permission` p
WHERE p.code IN (
  'keyPopulation:latent:confirmNotice','keyPopulation:latent:supervision',
  'keyPopulation:latent:followUp','keyPopulation:latent:check',
  'closeContact:latent:confirmNotice','closeContact:latent:supervision',
  'closeContact:latent:confirmTreatment','closeContact:latent:check',
  'keyPopulation:patient:confirmNotice','keyPopulation:patient:firstVisit',
  'keyPopulation:patient:followUp','keyPopulation:patient:medication',
  'closeContact:patient:confirmNotice','closeContact:patient:firstVisit',
  'closeContact:patient:followUp','closeContact:patient:medication'
);

-- ==================== 部门表 ====================
CREATE TABLE IF NOT EXISTS `department` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(128) NOT NULL COMMENT '部门名称',
    `description` VARCHAR(256) DEFAULT NULL COMMENT '部门描述',
    `parent_id`   BIGINT       DEFAULT NULL COMMENT '上级部门ID，NULL表示市级顶级',
    `level`       TINYINT      NOT NULL DEFAULT 1 COMMENT '1市级 2区县 3社区',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 为已存在的部门表补充字段（若列已存在会报错，忽略即可）
ALTER TABLE `department` ADD COLUMN `parent_id` BIGINT DEFAULT NULL COMMENT '上级部门ID，NULL表示市级顶级';
ALTER TABLE `department` ADD COLUMN `level` TINYINT NOT NULL DEFAULT 1 COMMENT '1市级 2区县 3社区';

CREATE TABLE IF NOT EXISTS `user_permission` (
    `id`             BIGINT   NOT NULL AUTO_INCREMENT,
    `user_id`        BIGINT   NOT NULL COMMENT '用户ID',
    `permission_id`  BIGINT   NOT NULL COMMENT '权限ID',
    `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_perm` (`user_id`, `permission_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户额外权限（与角色权限合并）';

-- ==================== 五级(6)菜单页面权限补全 ====================
-- 五级管理员需要能进入潜伏感染和患者管理页面，才能执行督导/随访/确认通知单等操作
-- 赋予三条主线的父菜单 + 潜伏感染 + 患者管理 + 历史患者 菜单权限（仅页面访问，不含写操作按钮）
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT 6, p.id FROM `permission` p
WHERE p.code IN (
  'school', 'school:latent', 'school:patient', 'school:history',
  'keyPopulation', 'keyPopulation:latent', 'keyPopulation:patient', 'keyPopulation:history',
  'closeContact', 'closeContact:latent', 'closeContact:patient', 'closeContact:history'
);

-- ==================== V9：重点人群潜伏权限与分级诊疗在权限树中可见 ====================
-- 1）与学校人群一致：重点人群「潜伏感染」下补充追踪、胸片、转诊（诊断）按钮权限
INSERT IGNORE INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`) VALUES
(226, 'keyPopulation:latent:track', '追踪', 2, 21, 1),
(227, 'keyPopulation:latent:xray', '录入胸片诊断', 2, 21, 2),
(228, 'keyPopulation:latent:referral', '转诊', 2, 21, 3);
UPDATE `permission` SET `sort` = CASE `id`
  WHEN 220 THEN 4 WHEN 221 THEN 5 WHEN 222 THEN 6 WHEN 223 THEN 7 WHEN 224 THEN 8 WHEN 225 THEN 9
  END WHERE `id` IN (220, 221, 222, 223, 224, 225);
-- 2）统一 referral 记录：挂到「系统消息」下、类型为按钮（兼容旧库错误 type / parent）
UPDATE `permission` SET `parent_id` = 5, `sort` = 50, `type` = 2 WHERE `code` = 'referral';
-- 首批角色权限写入若早于 referral 权限行，超级管理员 role_permission 可能缺少 referral，此处补全
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT 1, `id` FROM `permission` WHERE `code` = 'referral';
-- 3）角色授权（与学校 latent:track / latent:xray / latent:referral 范围一致：一至四级）
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT 1, `id` FROM `permission` WHERE `code` IN ('keyPopulation:latent:track', 'keyPopulation:latent:xray', 'keyPopulation:latent:referral');
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id FROM (SELECT 2 AS role UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) r
CROSS JOIN `permission` p
WHERE p.code IN ('keyPopulation:latent:track', 'keyPopulation:latent:xray', 'keyPopulation:latent:referral');

-- ==================== V10：待诊断菜单权限 + 一二级用户权限管理访问 ====================
-- 新增三条主线"待诊断"菜单权限（挂在各自主线父节点下，sort=2，位于筛查管理之后）
INSERT IGNORE INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`) VALUES
(14, 'school:suspected',        '待诊断',   1, 1, 2),
(24, 'keyPopulation:suspected', '待诊断',   1, 2, 2),
(34, 'closeContact:followUp',   '监测随访', 1, 3, 6);

-- 调整后续子菜单排序（潜伏感染 3、患者管理 4、历史患者 5）
UPDATE `permission` SET `sort` = 3 WHERE `code` IN ('school:latent', 'keyPopulation:latent', 'closeContact:latent');
UPDATE `permission` SET `sort` = 4 WHERE `code` IN ('school:patient', 'keyPopulation:patient', 'closeContact:patient');
UPDATE `permission` SET `sort` = 5 WHERE `code` IN ('school:history', 'keyPopulation:history', 'closeContact:history');

-- 超级管理员获得所有新权限（幂等）
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT 1, `id` FROM `permission` WHERE `code` IN ('school:suspected', 'keyPopulation:suspected', 'closeContact:followUp');

-- 待诊断/监测随访权限默认不分配给其他角色，由超级管理员通过"权限管理"界面手动分配

-- 一级(role=2)、二级(role=3) 获得权限管理页面访问权：system 父菜单 + system:permissions + permission:assign
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id FROM (SELECT 2 AS role UNION SELECT 3) r
CROSS JOIN `permission` p
WHERE p.code IN ('system', 'system:permissions', 'permission:assign');

-- ==================== V12：新增数据清洗菜单权限 ====================
-- 菜单权限已在初始化权限数据中声明，此处补充角色授权。
-- 默认授予 1-4 级（监管与业务执行角色），5级按需在权限管理中分配
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id
FROM (SELECT 1 AS role UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) r
CROSS JOIN `permission` p
WHERE p.code = 'dataCleaning';

-- ==================== V11：密接人群-待诊断权限重命名为监测随访 ====================
-- closeContact:suspected（待诊断）已从密接人群菜单移除，
-- 对应页面改为监测随访（closeContact:followUp），排在历史患者之后
UPDATE `permission`
SET `code` = 'closeContact:followUp', `name` = '监测随访', `sort` = 6
WHERE `code` = 'closeContact:suspected';

-- 超级管理员补充获得更新后权限（幂等）
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT 1, `id` FROM `permission` WHERE `code` = 'closeContact:followUp';
