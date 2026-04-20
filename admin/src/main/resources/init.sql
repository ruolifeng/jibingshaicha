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
    `year`                            VARCHAR(10)  DEFAULT NULL COMMENT '年份',
    `city`                            VARCHAR(64)  DEFAULT NULL COMMENT '市（州）',
    `district`                        VARCHAR(64)  DEFAULT NULL COMMENT '县（市、区）',
    `name`                            VARCHAR(64)  DEFAULT NULL COMMENT '姓名',
    `gender`                          VARCHAR(10)  DEFAULT NULL COMMENT '性别',
    `birth_date`                      DATE         DEFAULT NULL COMMENT '出生日期',
    `age`                             INT          DEFAULT NULL COMMENT '年龄',
    `id_type`                         VARCHAR(32)  DEFAULT NULL COMMENT '证件类型',
    `id_number`                       VARCHAR(64)  DEFAULT NULL COMMENT '证件号',
    `ethnicity`                       VARCHAR(32)  DEFAULT NULL COMMENT '民族',
    `occupation`                      VARCHAR(64)  DEFAULT NULL COMMENT '职业',
    `phone`                           VARCHAR(32)  DEFAULT NULL COMMENT '联系电话',
    `household_address`               VARCHAR(256) DEFAULT NULL COMMENT '户籍所在地',
    `current_address`                 VARCHAR(256) DEFAULT NULL COMMENT '现住址',
    `contact_type`                    VARCHAR(32)  DEFAULT NULL COMMENT '接触类型：家庭内/家庭外',
    `source_patient_name`             VARCHAR(64)  DEFAULT NULL COMMENT '原患者姓名',
    `source_patient_confirm_date`     DATE         DEFAULT NULL COMMENT '原患者确诊日期',
    `source_patient_id_number`        VARCHAR(64)  DEFAULT NULL COMMENT '原患者身份证号',
    -- 首次筛查（T-AB列）
    `first_screen_date`               DATE         DEFAULT NULL COMMENT '首次筛查日期',
    `first_symptom_result`            VARCHAR(128) DEFAULT NULL COMMENT '首次症状筛查结果',
    `first_infection_method`          VARCHAR(64)  DEFAULT NULL COMMENT '首次感染检查方法（PPD/EC/IGRA）',
    `first_screen_result`             VARCHAR(128) DEFAULT NULL COMMENT '首次结果（mmXmm/EC阴性/EC阳性/IGRA阴性/IGRA阳性）',
    `first_infection_result`          VARCHAR(128) DEFAULT NULL COMMENT '首次感染筛查结果',
    `first_has_chest_xray`            VARCHAR(10)  DEFAULT NULL COMMENT '首次是否进行胸片检查',
    `first_chest_xray_date`           DATE         DEFAULT NULL COMMENT '首次胸片检查日期',
    `first_chest_xray_result`         VARCHAR(128) DEFAULT NULL COMMENT '首次胸片检查结果',
    `first_diagnosis`                 VARCHAR(64)  DEFAULT NULL COMMENT '首次诊断结果：排除/疑似肺结核/潜伏感染者/确诊患者/其他',
    -- 半年后筛查（AC-AK列）
    `half_year_screen_date`           DATE         DEFAULT NULL COMMENT '半年后筛查日期',
    `half_year_symptom_result`        VARCHAR(128) DEFAULT NULL COMMENT '半年后症状筛查结果',
    `half_year_infection_method`      VARCHAR(64)  DEFAULT NULL COMMENT '半年后感染检查方法',
    `half_year_screen_result`         VARCHAR(128) DEFAULT NULL COMMENT '半年后结果',
    `half_year_infection_result`      VARCHAR(128) DEFAULT NULL COMMENT '半年后感染筛查结果',
    `half_year_has_chest_xray`        VARCHAR(10)  DEFAULT NULL COMMENT '半年后是否进行胸片检查',
    `half_year_chest_xray_date`       DATE         DEFAULT NULL COMMENT '半年后胸片检查日期',
    `half_year_chest_xray_result`     VARCHAR(128) DEFAULT NULL COMMENT '半年后胸片检查结果',
    `half_year_diagnosis`             VARCHAR(64)  DEFAULT NULL COMMENT '半年后诊断结果',
    -- 一年后筛查（AL-AT列）
    `one_year_screen_date`            DATE         DEFAULT NULL COMMENT '一年后筛查日期',
    `one_year_symptom_result`         VARCHAR(128) DEFAULT NULL COMMENT '一年后症状筛查结果',
    `one_year_infection_method`       VARCHAR(64)  DEFAULT NULL COMMENT '一年后感染筛查方法',
    `one_year_screen_result`          VARCHAR(128) DEFAULT NULL COMMENT '一年后结果',
    `one_year_infection_result`       VARCHAR(128) DEFAULT NULL COMMENT '一年后感染筛查结果',
    `one_year_has_chest_xray`         VARCHAR(10)  DEFAULT NULL COMMENT '一年后是否进行胸片检查',
    `one_year_chest_xray_date`        DATE         DEFAULT NULL COMMENT '一年后胸片检查日期',
    `one_year_chest_xray_result`      VARCHAR(128) DEFAULT NULL COMMENT '一年后胸片检查结果',
    `one_year_diagnosis`              VARCHAR(64)  DEFAULT NULL COMMENT '一年后诊断结果',
    -- 潜伏感染者管理情况（AU-AZ列，督导表归档后同步）
    `has_preventive_treatment`        VARCHAR(10)  DEFAULT NULL COMMENT '是否进行预防性治疗',
    `preventive_plan`                 VARCHAR(128) DEFAULT NULL COMMENT '预防性治疗方案',
    `preventive_start_date`           DATE         DEFAULT NULL COMMENT '预防性治疗开始时间',
    `preventive_end_date`             DATE         DEFAULT NULL COMMENT '预防性治疗完成时间',
    `preventive_result`               VARCHAR(64)  DEFAULT NULL COMMENT '预防性治疗结果：规范完成/失访/自行中断治疗/确诊肺结核',
    `preventive_manager`              VARCHAR(256) DEFAULT NULL COMMENT '预防性治疗期间随访管理人员',
    `benefit_method`                  VARCHAR(64)  DEFAULT NULL COMMENT '惠民方式',
    `remark`                          TEXT         DEFAULT NULL COMMENT '备注',
    `is_latent`                       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否潜伏管理者：0否 1是',
    `active_round`                    TINYINT      DEFAULT NULL COMMENT '阳性轮次：1首次 2半年后 3一年后',
    `upload_batch`                    VARCHAR(64)  DEFAULT NULL COMMENT '上传批次号',
    `create_time`                     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`                     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_id_number` (`id_number`),
    KEY `idx_latent` (`is_latent`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密接人群筛查数据表（V4三轮）';

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
-- V4 新增：治疗完成时间、预防性治疗结果、随访管理人员

CREATE TABLE IF NOT EXISTS `supervision_form` (
    `id`                     BIGINT       NOT NULL AUTO_INCREMENT,
    `latent_infection_id`    BIGINT       NOT NULL COMMENT '关联潜伏感染ID',
    `population_type`        VARCHAR(32)  NOT NULL COMMENT '人群类型',
    `patient_name`           VARCHAR(64)  DEFAULT NULL COMMENT '患者姓名',
    `treatment_start_date`   DATE         DEFAULT NULL COMMENT '预防性治疗开始日期',
    `treatment_end_date`     DATE         DEFAULT NULL COMMENT '预防性治疗完成时间（V4新增）',
    `treatment_plan`         VARCHAR(256) DEFAULT NULL COMMENT '治疗方案',
    `supervision_content`    TEXT         DEFAULT NULL COMMENT '督导内容（JSON格式存储表单数据）',
    `preventive_result`      VARCHAR(64)  DEFAULT NULL COMMENT '预防性治疗结果：规范完成/失访/自行中断治疗/确诊肺结核（V4新增）',
    `preventive_manager`     VARCHAR(256) DEFAULT NULL COMMENT '预防性治疗期间随访管理人员（V4新增）',
    `filled_by`              BIGINT       DEFAULT NULL COMMENT '填写人ID',
    `status`                 TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0未填写 1已填写 2已归档',
    `archived_time`          DATETIME     DEFAULT NULL COMMENT '归档时间',
    `create_time`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_latent` (`latent_infection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预防性治疗督导表（V4）';

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
  'latent:track','latent:referral','latent:sendNotice','latent:supervision',
  'patient:importEpidemic','patient:sendNotice','patient:firstVisit','patient:followUp','patient:medication'
);

-- 五级(6)：业务菜单 + 确认通知单/随访/督导
INSERT INTO `role_permission` (`role`, `permission_id`)
SELECT 6, `id` FROM `permission` WHERE `code` IN (
  'school','keyPopulation','closeContact','message',
  'school:screening','school:latent','school:patient','school:history',
  'keyPopulation:screening','keyPopulation:latent','keyPopulation:patient','keyPopulation:history',
  'closeContact:screening','closeContact:latent','closeContact:patient','closeContact:history',
  'latent:confirmNotice','latent:supervision','patient:confirmNotice','patient:firstVisit','patient:followUp','patient:medication'
);

-- ==================== 修复操作按钮 parent_id（数据库已存在时执行） ====================
-- 若数据库已初始化，运行以下语句将操作权限挂到正确的父菜单下
UPDATE `permission` SET `parent_id` = 10 WHERE `code` IN ('screening:upload','screening:create','screening:export','screening:edit','screening:delete');
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

-- V5 迁移：notice 表补充 ethnicity 字段
ALTER TABLE `notice` ADD COLUMN IF NOT EXISTS `ethnicity` VARCHAR(32) DEFAULT NULL COMMENT '民族' AFTER `crowd_category`;

-- V5 迁移：重点人群筛查表补充乡镇/社区字段
ALTER TABLE `screening_key_population` ADD COLUMN `township_community` VARCHAR(128) DEFAULT NULL COMMENT '乡镇/社区' AFTER `household_address`;
