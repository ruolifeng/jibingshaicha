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

-- 初始密码均为 123456，已使用 BCrypt(strength=10) 加密（可重复执行，已存在则跳过）
INSERT IGNORE INTO `user` (`username`, `password`, `real_name`, `role`, `org_name`) VALUES
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
    `sputum_smear_result`   VARCHAR(64)  DEFAULT NULL COMMENT '痰涂片结果',
    `molecular_biology_result` VARCHAR(64) DEFAULT NULL COMMENT '分子生物学结果',
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
    `import_row_no`               INT          DEFAULT NULL COMMENT 'Excel导入行号（与模板行号一致，用于列表排序）',
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
    `phone_contact_relation`          VARCHAR(64)  DEFAULT NULL COMMENT '联系电话与接触者关系',
    `contact_type`                    VARCHAR(32)  DEFAULT NULL COMMENT '接触类型：家庭内/家庭外',
    `contact_place`                   VARCHAR(64)  DEFAULT NULL COMMENT '接触场所',
    `contact_place_other`             VARCHAR(128) DEFAULT NULL COMMENT '接触场所-其他（手工录入）',
    -- ===== 初次筛查（S-AE）=====
    `first_screen_date`               DATE         DEFAULT NULL COMMENT '首次筛查日期',
    `symptom1`                        VARCHAR(128) DEFAULT NULL COMMENT '结核症状1',
    `symptom2`                        VARCHAR(128) DEFAULT NULL COMMENT '结核症状2',
    `infection_check_date`            DATE         DEFAULT NULL COMMENT '感染检测日期',
    `infection_check_method`          VARCHAR(64)  DEFAULT NULL COMMENT '感染检测方法（EC/PPD/IGRA）',
    `infection_check_result`          VARCHAR(64)  DEFAULT NULL COMMENT '结果判定（阴性/阳性）',
    `imaging_date`                    DATE         DEFAULT NULL COMMENT '影像检查日期',
    `imaging_method`                  VARCHAR(64)  DEFAULT NULL COMMENT '影像方法（胸部X光片/胸部CT）',
    `imaging_method_other`            VARCHAR(128) DEFAULT NULL COMMENT '影像方法-其他（手工录入）',
    `imaging_result`                  VARCHAR(128) DEFAULT NULL COMMENT '影像结果',
    `imaging_result_other`            VARCHAR(128) DEFAULT NULL COMMENT '影像结果-其他（手工录入）',
    `sputum_check_date`               DATE         DEFAULT NULL COMMENT '痰检留标日期',
    `sputum_check_method`             VARCHAR(64)  DEFAULT NULL COMMENT '痰检方法',
    `sputum_check_method_other`       VARCHAR(128) DEFAULT NULL COMMENT '痰检方法-其他（手工录入）',
    `sputum_check_result`             VARCHAR(64)  DEFAULT NULL COMMENT '痰检结果',
    `sputum_check_result_other`       VARCHAR(128) DEFAULT NULL COMMENT '痰检结果-其他（手工录入）',
    `final_screening_result`          VARCHAR(32)  DEFAULT NULL COMMENT '最终筛查结果',
    `final_screening_result_other`    VARCHAR(128) DEFAULT NULL COMMENT '最终筛查结果-其他（手工录入）',
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

-- ==================== 密接个案表（独立电子表格模块） ====================
CREATE TABLE IF NOT EXISTS `close_contact_case` (
    `id`                              BIGINT       NOT NULL AUTO_INCREMENT,
    `city`                            VARCHAR(64)  DEFAULT NULL COMMENT '市/州',
    `district`                        VARCHAR(64)  DEFAULT NULL COMMENT '区/县',
    `source_patient_name`             VARCHAR(64)  DEFAULT NULL COMMENT '原患者姓名',
    `source_patient_case_no`          VARCHAR(64)  DEFAULT NULL COMMENT '原患者病案号',
    `source_patient_bacteriology_result` VARCHAR(64) DEFAULT NULL COMMENT '原患者病原学结果',
    `source_patient_phone`            VARCHAR(32)  DEFAULT NULL COMMENT '原患者电话',
    `source_patient_id_number`        VARCHAR(64)  DEFAULT NULL COMMENT '原患者身份证号',
    `report_date`                     DATE         DEFAULT NULL COMMENT '填表日期',
    `registration_date`               DATE         DEFAULT NULL COMMENT '密切接触者登记日期',
    `name`                            VARCHAR(64)  DEFAULT NULL COMMENT '接触者姓名',
    `id_number`                       VARCHAR(64)  DEFAULT NULL COMMENT '接触者身份证号',
    `age`                             INT          DEFAULT NULL COMMENT '年龄',
    `phone`                           VARCHAR(32)  DEFAULT NULL COMMENT '接触者电话',
    `contact_type`                    VARCHAR(32)  DEFAULT NULL COMMENT '接触类型',
    `contact_place`                   VARCHAR(64)  DEFAULT NULL COMMENT '接触场所',
    `first_screen_date`               DATE         DEFAULT NULL COMMENT '首次筛查日期',
    `symptom1`                        VARCHAR(128) DEFAULT NULL COMMENT '结核症状1',
    `symptom2`                        VARCHAR(128) DEFAULT NULL COMMENT '结核症状2',
    `infection_check_date`            DATE         DEFAULT NULL COMMENT '感染检测日期',
    `infection_check_method`          VARCHAR(64)  DEFAULT NULL COMMENT '感染检测方法',
    `infection_check_result`          VARCHAR(64)  DEFAULT NULL COMMENT '结果判定',
    `imaging_date`                    DATE         DEFAULT NULL COMMENT '影像检查日期',
    `imaging_method`                  VARCHAR(64)  DEFAULT NULL COMMENT '影像方法',
    `imaging_result`                  VARCHAR(128) DEFAULT NULL COMMENT '影像结果',
    `sputum_check_date`               DATE         DEFAULT NULL COMMENT '痰检留标日期',
    `sputum_check_method`             VARCHAR(64)  DEFAULT NULL COMMENT '痰检方法',
    `sputum_check_result`             VARCHAR(64)  DEFAULT NULL COMMENT '痰检结果',
    `final_screening_result`          VARCHAR(32)  DEFAULT NULL COMMENT '诊断结果：活动性肺结核/潜伏感染者/未做/未发现异常',
    `has_contraindication`            VARCHAR(32)  DEFAULT NULL COMMENT '有无禁忌症',
    `no_treatment_reason`             VARCHAR(128) DEFAULT NULL COMMENT '不接受预防治疗的原因',
    `contraindication_remark`         VARCHAR(256) DEFAULT NULL COMMENT '禁忌症备注',
    `has_preventive_treatment`        VARCHAR(10)  DEFAULT NULL COMMENT '是否开展预防治疗',
    `preventive_plan`                 VARCHAR(128) DEFAULT NULL COMMENT '预防性治疗方案',
    `preventive_plan_remark`          VARCHAR(256) DEFAULT NULL COMMENT '其他方案备注',
    `treatment_completed`             VARCHAR(10)  DEFAULT NULL COMMENT '是否完成治疗',
    `incomplete_reason`               VARCHAR(128) DEFAULT NULL COMMENT '未完成原因',
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
    `year`                            VARCHAR(10)  DEFAULT NULL COMMENT '年份',
    `gender`                          VARCHAR(10)  DEFAULT NULL COMMENT '性别',
    `ethnicity`                       VARCHAR(32)  DEFAULT NULL COMMENT '民族',
    `household_address`               VARCHAR(256) DEFAULT NULL COMMENT '户籍地址',
    `current_address`                 VARCHAR(256) DEFAULT NULL COMMENT '现住址',
    `upload_batch`                    VARCHAR(64)  DEFAULT NULL COMMENT '上传批次号',
    `department_id`                   BIGINT       DEFAULT NULL COMMENT '所属部门ID',
    `creator_username`                VARCHAR(64)  DEFAULT NULL COMMENT '录入用户名',
    `create_time`                     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`                     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_id_number` (`id_number`),
    KEY `idx_district` (`district`),
    KEY `idx_final_result` (`final_screening_result`),
    KEY `idx_creator_username` (`creator_username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密接个案表（电子表格，73列）';

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
    `tracking_history_json` TEXT       DEFAULT NULL COMMENT '追踪历史JSON（每次追踪的状态、时间、备注）',
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
    `medication_management_unit` VARCHAR(256) DEFAULT NULL COMMENT '服药管理单位（来自病案信息）',
    `remark`                 TEXT         DEFAULT NULL COMMENT '备注（手动填写）',
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
    `form_seq`               INT          DEFAULT NULL COMMENT '第几次督导表（status>=1时有效）',
    -- V5 新增基本信息
    `category`               VARCHAR(64)  DEFAULT NULL COMMENT '类别：密接/新生筛查/65岁以上老年人/糖尿病人/双感/其他',
    `gender`                 VARCHAR(10)  DEFAULT NULL COMMENT '性别',
    `age`                    INT          DEFAULT NULL COMMENT '年龄',
    `phone`                  VARCHAR(32)  DEFAULT NULL COMMENT '电话号码',
    `phone_remark`           VARCHAR(256) DEFAULT NULL COMMENT '电话备注（非本人电话时说明）',
    `current_address`        VARCHAR(256) DEFAULT NULL COMMENT '现住址',
    `household_address`      VARCHAR(255) DEFAULT NULL COMMENT '户籍地址',
    `id_number`              VARCHAR(50)  DEFAULT NULL COMMENT '身份证号',
    `birth_date`             VARCHAR(20)  DEFAULT NULL COMMENT '出生日期',
    `ethnicity`              VARCHAR(50)  DEFAULT NULL COMMENT '民族',
    `managing_unit`          VARCHAR(100) DEFAULT NULL COMMENT '管理单位',
    `has_preventive_treatment` VARCHAR(10) DEFAULT NULL COMMENT '是否进行预防性治疗：是/否',
    `supervising_doctor`     VARCHAR(100) DEFAULT NULL COMMENT '督导医生',
    `treatment_start_date`   DATE         DEFAULT NULL COMMENT '预防性治疗开始日期',
    `treatment_plan`         VARCHAR(256) DEFAULT NULL COMMENT '治疗方案（含新增"不服药"）',
    -- V4 旧字段兼容保留（实体字段 supervisionContent 映射至此列）
    `supervision_content`    TEXT         DEFAULT NULL COMMENT '督导内容（V4旧字段，兼容保留）',
    -- V5 改造：督导记录改为 JSON 数组（督导时间/内容/方式/备注）
    `supervision_records`    TEXT         DEFAULT NULL COMMENT '督导记录（JSON数组：time/content/method/remark）',
    -- V5 新增：全疗程规律治疗评价
    `treatment_completion_status` VARCHAR(32) DEFAULT NULL COMMENT '治疗完成情况：完成治疗/失败/死亡/失访/不良反应停药/未评估',
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
    `archive_remark`      VARCHAR(128) DEFAULT NULL COMMENT '归档备注（如：已转出）',
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
    `form_no`               VARCHAR(8)   DEFAULT NULL COMMENT '编号（8位数字，手动录入）',
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
    `start_treatment_date`    DATE         DEFAULT NULL COMMENT '开始治疗日期',
    `medication_records`      JSON         DEFAULT NULL COMMENT '每日服药记录（JSON：{日期:是否服药}）',
    `stop_date`               DATE         DEFAULT NULL COMMENT '停止完成时间',
    `create_time`             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                 TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_patient` (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服药管理表';

-- ==================== 领药记录表 ====================

CREATE TABLE IF NOT EXISTS `medication_pickup` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `patient_id`        BIGINT       NOT NULL COMMENT '关联患者ID',
    `population_type`   VARCHAR(32)  NOT NULL COMMENT '人群类型',
    `pickup_seq`        INT          DEFAULT NULL COMMENT '第几次领药',
    `drugs`             JSON         DEFAULT NULL COMMENT '药品及用量 [{name,dosage,quantity,quantityUnit}]',
    `quantity`          DECIMAL(10, 2) DEFAULT NULL COMMENT '领取数量',
    `quantity_unit`     VARCHAR(16)  DEFAULT NULL COMMENT '领取数量单位',
    `pickup_time`       DATE         DEFAULT NULL COMMENT '领取时间',
    `dispensing_unit`   VARCHAR(128) DEFAULT NULL COMMENT '发药单位',
    `remarks`           TEXT         DEFAULT NULL COMMENT '备注',
    `filled_by`         BIGINT       DEFAULT NULL COMMENT '填写人ID',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_patient` (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='领药记录表';

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
(30, 'closeContact:screening',  '密接筛查',     1, 3, 1),
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
-- 兼容 MySQL < 8.0.29：用存储过程忽略 1060 重复列错误（不支持 ADD COLUMN IF NOT EXISTS）
DROP PROCEDURE IF EXISTS _latent_treatment_migrate;
DELIMITER $$
CREATE PROCEDURE _latent_treatment_migrate()
BEGIN
    DECLARE CONTINUE HANDLER FOR 1060 BEGIN END;

    ALTER TABLE `latent_infection` ADD COLUMN `treatment_phase` TINYINT NOT NULL DEFAULT 0 COMMENT '治疗阶段：0未开始 1预防治疗中 2已结案' AFTER `diagnosis_result`;
    ALTER TABLE `latent_infection` ADD COLUMN `medication_status` TINYINT DEFAULT NULL COMMENT '服药状态：1按要求服药 2不服药' AFTER `treatment_phase`;
    ALTER TABLE `latent_infection` ADD COLUMN `archived_time` DATETIME DEFAULT NULL COMMENT '结案归档时间' AFTER `archived`;

    -- V4 扩展：潜伏感染表新增胸片/诊断/轮次字段
    ALTER TABLE `latent_infection` ADD COLUMN `has_chest_xray`    VARCHAR(10)  DEFAULT NULL COMMENT '是否进行胸片检查' AFTER `tracking_remark`;
    ALTER TABLE `latent_infection` ADD COLUMN `chest_xray_date`   DATE         DEFAULT NULL COMMENT '胸片检查日期' AFTER `has_chest_xray`;
    ALTER TABLE `latent_infection` ADD COLUMN `chest_xray_result` VARCHAR(128) DEFAULT NULL COMMENT '胸片检查结果：正常/异常/未查' AFTER `chest_xray_date`;
    ALTER TABLE `latent_infection` ADD COLUMN `diagnosis_first`   VARCHAR(64)  DEFAULT NULL COMMENT '首次诊断结果' AFTER `chest_xray_result`;
    ALTER TABLE `latent_infection` ADD COLUMN `active_round`      TINYINT      DEFAULT NULL COMMENT '密接阳性轮次：1首次 2半年后 3一年后' AFTER `diagnosis_first`;
END$$
DELIMITER ;
CALL _latent_treatment_migrate();
DROP PROCEDURE IF EXISTS _latent_treatment_migrate;
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

-- V5 迁移：notice 表补充 ethnicity 字段；重点人群筛查表补充乡镇/社区字段
DROP PROCEDURE IF EXISTS _v5_migrate_notice_key;
DELIMITER $$
CREATE PROCEDURE _v5_migrate_notice_key()
BEGIN
    DECLARE CONTINUE HANDLER FOR 1060 BEGIN END;

    ALTER TABLE `notice` ADD COLUMN `ethnicity` VARCHAR(32) DEFAULT NULL COMMENT '民族' AFTER `crowd_category`;
    ALTER TABLE `screening_key_population` ADD COLUMN `township_community` VARCHAR(128) DEFAULT NULL COMMENT '乡镇/社区' AFTER `household_address`;
END$$
DELIMITER ;
CALL _v5_migrate_notice_key();
DROP PROCEDURE IF EXISTS _v5_migrate_notice_key;

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
      'closeContact:screening','closeContact:followUp','closeContact:latent','closeContact:patient','closeContact:history','closeContact:case',
      'screening:upload','screening:create','screening:export','screening:edit','screening:delete',
      'keyPopulation:screening:upload','keyPopulation:screening:create','keyPopulation:screening:export','keyPopulation:screening:edit','keyPopulation:screening:delete',
      'closeContact:screening:upload','closeContact:screening:create','closeContact:screening:export','closeContact:screening:edit','closeContact:screening:delete',
      'closeContact:case:upload','closeContact:case:create','closeContact:case:export','closeContact:case:edit','closeContact:case:delete',
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
-- （V4 → V5 迁移的数据库该列已存在，重复列错误由 CONTINUE HANDLER 静默忽略）
DROP PROCEDURE IF EXISTS _v5_fix_supervision_content;
DELIMITER $$
CREATE PROCEDURE _v5_fix_supervision_content()
BEGIN
    DECLARE CONTINUE HANDLER FOR 1060 BEGIN END;
    ALTER TABLE `supervision_form` ADD COLUMN `supervision_content` TEXT DEFAULT NULL COMMENT '督导内容（V4旧字段，兼容保留）';
END$$
DELIMITER ;
CALL _v5_fix_supervision_content();
DROP PROCEDURE IF EXISTS _v5_fix_supervision_content;

-- ==================== 分级诊疗表 ====================
CREATE TABLE IF NOT EXISTS `referral` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `biz_id`          BIGINT       NOT NULL COMMENT '关联业务记录ID',
    `target_biz_id`   BIGINT       DEFAULT NULL COMMENT '接收确认后在接收方生成的业务记录ID',
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
    `referral_reason` VARCHAR(512) DEFAULT NULL COMMENT '转诊原因（发送方填写）',
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

-- ==================== V13：操作日志（P1 重构阶段） ====================

CREATE TABLE IF NOT EXISTS `operation_log` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`        BIGINT       DEFAULT NULL COMMENT '操作人ID',
    `user_name`      VARCHAR(64)  DEFAULT NULL COMMENT '操作人用户名',
    `real_name`      VARCHAR(64)  DEFAULT NULL COMMENT '操作人真实姓名',
    `department_id`  BIGINT       DEFAULT NULL COMMENT '所属部门ID',
    `role`           TINYINT      DEFAULT NULL COMMENT '角色：1-6',
    `op_type`        VARCHAR(16)  NOT NULL  COMMENT '操作类型：login=登录 import=导入 delete=删除 update=修改 export=导出 create=新增(扩展) logout=登出(扩展)',
    `op_module`      VARCHAR(64)  DEFAULT NULL COMMENT '业务模块：screening/latent/patient/referral/system/...',
    `op_action`      VARCHAR(256) DEFAULT NULL COMMENT '动作描述',
    `biz_id`         BIGINT       DEFAULT NULL COMMENT '关联业务ID',
    `biz_type`       VARCHAR(64)  DEFAULT NULL COMMENT '关联业务类型',
    `request_method` VARCHAR(8)   DEFAULT NULL COMMENT 'HTTP方法',
    `request_url`    VARCHAR(256) DEFAULT NULL COMMENT '请求URL',
    `request_params` TEXT         DEFAULT NULL COMMENT '请求参数（JSON，敏感字段已脱敏）',
    `ip`             VARCHAR(64)  DEFAULT NULL COMMENT '客户端IP',
    `user_agent`     VARCHAR(256) DEFAULT NULL COMMENT '客户端 UA',
    `result_status`  TINYINT      NOT NULL DEFAULT 1 COMMENT '1成功 0失败',
    `error_message`  TEXT         DEFAULT NULL COMMENT '失败错误信息',
    `cost_ms`        BIGINT       DEFAULT NULL COMMENT '耗时（毫秒）',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_op_type` (`op_type`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_module` (`op_module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表（V13）';

-- 操作日志菜单与按钮权限码（挂在系统管理=6 下）
INSERT IGNORE INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`) VALUES
(63,  'system:operationLog',   '操作日志', 1, 6,  4),
(150, 'operationLog:export',   '导出操作日志', 2, 63, 1),
(151, 'operationLog:filter',   '筛选操作日志', 2, 63, 2);

-- 超级管理员获得全部操作日志权限（幂等）
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT 1, `id` FROM `permission` WHERE `code` IN ('system:operationLog', 'operationLog:export', 'operationLog:filter');

-- 一级/二级用户默认可查看操作日志（不可导出，导出需权限管理单独分配）
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id
FROM (SELECT 2 AS role UNION SELECT 3) r
CROSS JOIN `permission` p
WHERE p.code IN ('system:operationLog', 'operationLog:filter');

-- ==================== V14：胸片与诊断按钮拆分（P2 重构阶段） ====================
-- 原 latent:xray 同时覆盖胸片+诊断两件事；V14 拆为两个独立操作：
--   latent:xray       —— 仅录入胸片结果
--   latent:diagnosis  —— 仅录入诊断结果（提交后自动驱动转诊）
-- 重点人群同步增加 keyPopulation:latent:diagnosis；密接的胸片/诊断流程不同，本阶段不动。

INSERT IGNORE INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`) VALUES
(152, 'latent:diagnosis',                '录入诊断结果', 2, 11, 10),
(153, 'keyPopulation:latent:diagnosis',  '录入诊断结果', 2, 21, 4);

-- 重命名旧权限（仅改名称，code 保持以保兼容）：latent:xray 显式表达"仅胸片"
UPDATE `permission` SET `name` = '录入胸片结果' WHERE `code` = 'latent:xray';
UPDATE `permission` SET `name` = '录入胸片结果' WHERE `code` = 'keyPopulation:latent:xray';

-- 超级管理员获得新权限（幂等）
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT 1, `id` FROM `permission` WHERE `code` IN ('latent:diagnosis', 'keyPopulation:latent:diagnosis');

-- 权限迁移：现有持有 *:latent:xray 权限的角色，自动获得对应 *:latent:diagnosis 权限
-- （按方案 v1.2 §10.3 ✅2 决策：旧权限保留，新权限自动赋予）
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT rp.role, p_new.id
FROM `role_permission` rp
JOIN `permission` p_old ON rp.permission_id = p_old.id AND p_old.code = 'latent:xray'
JOIN `permission` p_new ON p_new.code = 'latent:diagnosis';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT rp.role, p_new.id
FROM `role_permission` rp
JOIN `permission` p_old ON rp.permission_id = p_old.id AND p_old.code = 'keyPopulation:latent:xray'
JOIN `permission` p_new ON p_new.code = 'keyPopulation:latent:diagnosis';

-- 同步迁移用户级别权限（user_permission 表）
INSERT IGNORE INTO `user_permission` (`user_id`, `permission_id`)
SELECT up.user_id, p_new.id
FROM `user_permission` up
JOIN `permission` p_old ON up.permission_id = p_old.id AND p_old.code = 'latent:xray'
JOIN `permission` p_new ON p_new.code = 'latent:diagnosis';

INSERT IGNORE INTO `user_permission` (`user_id`, `permission_id`)
SELECT up.user_id, p_new.id
FROM `user_permission` up
JOIN `permission` p_old ON up.permission_id = p_old.id AND p_old.code = 'keyPopulation:latent:xray'
JOIN `permission` p_new ON p_new.code = 'keyPopulation:latent:diagnosis';

-- ==================== V15：首次/后续随访 备注+附件 & 后续随访按新模板重建 (P3 重构阶段) ====================
-- 用户要求："患者管理模块首次入户随访管理，后续随访管理需要增加备注，并可以上传2~6张照片作为附件。"
--              "后续随访表更改为现在的后续随访表，可多次填写。"（新模板：后续随访服务记录表.xlsx）
-- 设计原则：保留原字段不删除，向上兼容历史数据；新增字段按 v1.2 §3.3.5 落地。
-- 使用存储过程兼容重复执行（CONTINUE HANDLER FOR 1060 静默忽略 Duplicate column name）。

-- ---------- first_visit：补 remarks + attachment_urls ----------
DROP PROCEDURE IF EXISTS _v15_migrate_first_visit;
DELIMITER $$
CREATE PROCEDURE _v15_migrate_first_visit()
BEGIN
    DECLARE CONTINUE HANDLER FOR 1060 BEGIN END;
    ALTER TABLE `first_visit` ADD COLUMN `remarks`         TEXT DEFAULT NULL COMMENT 'V15 备注'                        AFTER `doctor_signature`;
    ALTER TABLE `first_visit` ADD COLUMN `attachment_urls` TEXT DEFAULT NULL COMMENT 'V15 附件图片URL JSON数组(2~6张)' AFTER `remarks`;
END$$
DELIMITER ;
CALL _v15_migrate_first_visit();
DROP PROCEDURE IF EXISTS _v15_migrate_first_visit;

-- ---------- follow_up_visit：按新 Excel 模板《后续随访服务记录表》扩展全部字段 ----------
-- 旧字段 visit_situation / remarks / attachment_url 保留兼容，新前端不再使用。
-- 字段命名严格对齐 v1.2 §3.3.5 表格。
DROP PROCEDURE IF EXISTS _v15_migrate_follow_up;
DELIMITER $$
CREATE PROCEDURE _v15_migrate_follow_up()
BEGIN
    DECLARE CONTINUE HANDLER FOR 1060 BEGIN END;
    ALTER TABLE `follow_up_visit` ADD COLUMN `treatment_month`           INT          DEFAULT NULL COMMENT 'V15 治疗月序（第X月）'                    AFTER `visit_date`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `supervisor`                VARCHAR(16)  DEFAULT NULL COMMENT 'V15 督导人员 1医生/2家属/3自服药/4其他'    AFTER `treatment_month`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `supervisor_other`          VARCHAR(64)  DEFAULT NULL COMMENT 'V15 督导人员-其他'                         AFTER `supervisor`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `symptoms`                  VARCHAR(64)  DEFAULT NULL COMMENT 'V15 症状及体征（多选0-11,逗号分隔）'        AFTER `visit_method`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `symptoms_other`            VARCHAR(256) DEFAULT NULL COMMENT 'V15 症状-其它'                             AFTER `symptoms`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `smoking_amount`            VARCHAR(16)  DEFAULT NULL COMMENT 'V15 吸烟（支/天）'                         AFTER `symptoms_other`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `drinking_amount`           VARCHAR(16)  DEFAULT NULL COMMENT 'V15 饮酒（两/天）'                         AFTER `smoking_amount`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `chemotherapy_plan`         VARCHAR(256) DEFAULT NULL COMMENT 'V15 化疗方案'                              AFTER `drinking_amount`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `medication_usage`          VARCHAR(16)  DEFAULT NULL COMMENT 'V15 用法 1每日/2间歇'                       AFTER `chemotherapy_plan`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `drug_form`                 VARCHAR(16)  DEFAULT NULL COMMENT 'V15 药品剂型 1固定剂量/2散装/3板式/4注射'   AFTER `medication_usage`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `missed_doses`              INT          DEFAULT NULL COMMENT 'V15 漏服药次数'                             AFTER `drug_form`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `adverse_reaction`          VARCHAR(16)  DEFAULT NULL COMMENT 'V15 药物不良反应 1无/2有'                   AFTER `missed_doses`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `adverse_reaction_detail`   VARCHAR(256) DEFAULT NULL COMMENT 'V15 不良反应详情'                          AFTER `adverse_reaction`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `complication`              VARCHAR(16)  DEFAULT NULL COMMENT 'V15 并发症或合并症 1无/2有'                 AFTER `adverse_reaction_detail`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `complication_detail`       VARCHAR(256) DEFAULT NULL COMMENT 'V15 并发症详情'                             AFTER `complication`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `referral_department`       VARCHAR(64)  DEFAULT NULL COMMENT 'V15 转诊-科别'                             AFTER `complication_detail`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `referral_reason`           VARCHAR(256) DEFAULT NULL COMMENT 'V15 转诊-原因'                             AFTER `referral_department`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `referral_two_week_result`  VARCHAR(256) DEFAULT NULL COMMENT 'V15 2周内随访结果'                         AFTER `referral_reason`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `handling_opinion`          TEXT         DEFAULT NULL COMMENT 'V15 处理意见'                              AFTER `referral_two_week_result`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `next_visit_date`           DATE         DEFAULT NULL COMMENT 'V15 下次随访时间'                          AFTER `handling_opinion`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `doctor_signature`          VARCHAR(64)  DEFAULT NULL COMMENT 'V15 随访医生签名'                          AFTER `next_visit_date`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `stop_treatment_date`       DATE         DEFAULT NULL COMMENT 'V15 停止治疗时间'                          AFTER `doctor_signature`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `stop_treatment_reason`     VARCHAR(32)  DEFAULT NULL COMMENT 'V15 停止治疗原因 完成疗程/死亡/丢失/转入耐多药' AFTER `stop_treatment_date`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `should_visit_count`        INT          DEFAULT NULL COMMENT 'V15 全程管理-应访视次数'                    AFTER `stop_treatment_reason`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `actual_visit_count`        INT          DEFAULT NULL COMMENT 'V15 全程管理-实际访视次数'                  AFTER `should_visit_count`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `should_dose_count`         INT          DEFAULT NULL COMMENT 'V15 全程管理-应服药次数'                    AFTER `actual_visit_count`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `actual_dose_count`         INT          DEFAULT NULL COMMENT 'V15 全程管理-实际服药次数'                  AFTER `should_dose_count`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `medication_rate`           VARCHAR(16)  DEFAULT NULL COMMENT 'V15 服药率（%）'                           AFTER `actual_dose_count`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `evaluator_signature`       VARCHAR(64)  DEFAULT NULL COMMENT 'V15 评估医生签名'                          AFTER `medication_rate`;
    ALTER TABLE `follow_up_visit` ADD COLUMN `attachment_urls`           TEXT         DEFAULT NULL COMMENT 'V15 附件图片URL JSON数组(2~6张)'            AFTER `attachment_url`;
END$$
DELIMITER ;
CALL _v15_migrate_follow_up();
DROP PROCEDURE IF EXISTS _v15_migrate_follow_up;

-- ==================== V16：P4 重构阶段 — 疫情筛查、聚合菜单、患者删除、新权限 ====================

-- ---------- 1. screening_key_population 增加 source_type 列（区分重点人群 vs 疫情筛查） ----------
-- DEFAULT 'keyPopulation' 保证存量数据不受影响。
ALTER TABLE `screening_key_population`
    ADD COLUMN `source_type` VARCHAR(32) NOT NULL DEFAULT 'keyPopulation'
        COMMENT 'V16 数据来源：keyPopulation=重点人群 / regular=疫情筛查'
        AFTER `upload_batch`;

-- ---------- 2. 新增权限码（V16 新增菜单对应的权限，ID 从 400 起） ----------
INSERT IGNORE INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`) VALUES
-- 筛查管理（一级菜单）
(400, 'screening',                      '筛查管理',             1, 0,   10),
-- 疫情筛查子菜单
(401, 'regular:screening',              '疫情筛查',             1, 400, 3),
(402, 'regular:screening:create',       '新增记录',             2, 401, 1),
(403, 'regular:screening:edit',         '编辑记录',             2, 401, 2),
(404, 'regular:screening:delete',       '删除记录',             2, 401, 3),
(405, 'regular:screening:upload',       '上传Excel',            2, 401, 4),
(406, 'regular:screening:export',       '导出',                 2, 401, 5),
-- 疫情筛查-待诊断子菜单
(407, 'regular:suspected',              '疫情筛查-待诊断',      1, 400, 4),
(408, 'regular:suspected:track',        '追踪操作',             2, 407, 1),
(409, 'regular:suspected:xray',         '录入胸片',             2, 407, 2),
(410, 'regular:suspected:diagnosis',    '录入诊断',             2, 407, 3),
-- 大疫情导入筛查
(411, 'epidemic:screening',             '大疫情导入筛查',       1, 400, 5),
-- 聚合潜伏感染者管理（一级菜单）
(412, 'latentManagement',               '潜伏感染者管理',       1, 0,   11),
(460, 'latentManagement:overview',      '在管总览',             1, 412, 0),
(461, 'latentManagement:edit',          '修改信息',             2, 460, 1),
(413, 'latentManagement:notice',        '通知单管理',           1, 412, 1),
(414, 'latentManagement:track',         '追踪',                 2, 413, 1),
(415, 'latentManagement:xray',          '录入胸片',             2, 413, 2),
(416, 'latentManagement:diagnosis',     '录入诊断',             2, 413, 3),
(417, 'latentManagement:referral',      '转出',                 2, 460, 2),
(418, 'latentManagement:close',         '归档',                 2, 413, 5),
(419, 'latentManagement:supervision',   '督导表管理',           1, 412, 2),
(472, 'latentManagement:supervision:fill','填写督导表',           2, 419, 1),
(469, 'latentManagement:supervision:edit','修改督导表',           2, 419, 2),
(464, 'latentManagement:history',     '历史患者',             1, 412, 3),
-- 聚合患者管理（一级菜单）
(420, 'patientManagement',              '患者管理',             1, 0,   12),
(462, 'patientManagement:overview',     '在管总览',             1, 420, 0),
(463, 'patientManagement:edit',         '修改信息',             2, 462, 1),
(421, 'patientManagement:notice',       '通知单管理',           1, 420, 1),
(422, 'patientManagement:firstVisit',   '首次随访',             1, 420, 2),
(473, 'patientManagement:firstVisit:fill','填写首次随访',         2, 422, 1),
(470, 'patientManagement:firstVisit:edit','修改首次随访',         2, 422, 2),
(423, 'patientManagement:followUp',     '后续随访',             1, 420, 3),
(474, 'patientManagement:followUp:fill',  '填写后续随访',         2, 423, 1),
(471, 'patientManagement:followUp:edit',  '修改随访记录',         2, 423, 2),
(424, 'patientManagement:medication',   '服药管理',             1, 420, 4),
(425, 'patientManagement:specialDisease','专病网导入',          1, 420, 5),
(426, 'patientManagement:history',      '历史患者',             1, 420, 6),
(427, 'patientManagement:referral',     '转出',                 2, 462, 2),
(475, 'patientManagement:notice:fill',  '填写通知单',           2, 421, 1),
(428, 'patientManagement:delete',       '删除患者',             2, 421, 2),
(468, 'patientManagement:pickup',       '填写领药',             2, 420, 7);

-- ---------- 3. 将 V16 新权限赋给角色 1（超级管理员）和 2（一级管理员） ----------
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id
FROM (SELECT 1 AS role UNION SELECT 2) r
         CROSS JOIN `permission` p
WHERE p.`code` IN (
    'screening',
    'regular:screening', 'regular:screening:create', 'regular:screening:edit',
    'regular:screening:delete', 'regular:screening:upload', 'regular:screening:export',
    'regular:suspected', 'regular:suspected:track', 'regular:suspected:xray', 'regular:suspected:diagnosis',
    'epidemic:screening',
    'latentManagement', 'latentManagement:overview', 'latentManagement:edit',
    'latentManagement:notice', 'latentManagement:track', 'latentManagement:xray',
    'latentManagement:diagnosis', 'latentManagement:referral', 'latentManagement:close', 'latentManagement:supervision',
    'latentManagement:supervision:fill', 'latentManagement:supervision:edit', 'latentManagement:history',
    'patientManagement', 'patientManagement:overview', 'patientManagement:edit',
    'patientManagement:notice', 'patientManagement:notice:fill', 'patientManagement:firstVisit', 'patientManagement:firstVisit:fill', 'patientManagement:firstVisit:edit',
    'patientManagement:followUp', 'patientManagement:followUp:fill', 'patientManagement:followUp:edit',
    'patientManagement:medication', 'patientManagement:pickup', 'patientManagement:specialDisease', 'patientManagement:history',
    'patientManagement:referral', 'patientManagement:delete'
);

-- 学校人群、重点人群归入「筛查管理」权限树
UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'screening'
SET child.`parent_id` = parent.id, child.`sort` = 1
WHERE child.`code` = 'school';
UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'screening'
SET child.`parent_id` = parent.id, child.`sort` = 2
WHERE child.`code` = 'keyPopulation';
UPDATE `permission` SET `sort` = 3 WHERE `code` = 'regular:screening';
UPDATE `permission` SET `sort` = 4 WHERE `code` = 'regular:suspected';
UPDATE `permission` SET `sort` = 5 WHERE `code` = 'epidemic:screening';

-- ==================== V17：P5 推介追踪模块 ====================

-- ---------- 1. 推介追踪记录表 ----------
CREATE TABLE IF NOT EXISTS `referral_tracking` (
    `id`                     BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `biz_mode`               VARCHAR(16)   NOT NULL                  COMMENT 'recommend=推介 / track=追踪',
    -- 基本信息（手动录入）
    `name`                   VARCHAR(64),
    `gender`                 VARCHAR(10),
    `birth_date`             DATE,
    `age`                    INT,
    `id_type`                VARCHAR(32),
    `id_number`              VARCHAR(64),
    `ethnicity`              VARCHAR(32),
    `phone`                  VARCHAR(32),
    `household_address`      VARCHAR(256),
    `current_address`        VARCHAR(256),
    `crowd_category`         VARCHAR(128)                            COMMENT '人群分类',
    `recommend_reason`       VARCHAR(512)                            COMMENT '推介原因（recommend模式）',
    `track_reason`           VARCHAR(512)                            COMMENT '追踪原因（track模式）',
    `source_type`            VARCHAR(16)   NOT NULL DEFAULT 'manual' COMMENT 'manual=手动 epidemic=大疫情导入',
    -- 大疫情导入字段
    `card_id`                VARCHAR(64)                             COMMENT '卡片ID',
    `parent_name`            VARCHAR(64)                             COMMENT '患儿家长姓名',
    `workplace`              VARCHAR(256)                            COMMENT '患者工作单位',
    `township`               VARCHAR(128)                            COMMENT '乡镇',
    `case_category`          VARCHAR(64)                             COMMENT '病例分类',
    `disease_name`           VARCHAR(128)                            COMMENT '疾病名称',
    `report_unit`            VARCHAR(256)                            COMMENT '报告单位',
    `report_card_time`       DATETIME                                COMMENT '报告卡录入时间',
    `epidemic_remark`        TEXT                                    COMMENT '大疫情备注',
    `upload_batch`           VARCHAR(64)                             COMMENT '导入批次号',
    -- 推介专用字段（biz_mode=recommend 时使用）
    `receiver_user_id`       BIGINT                                  COMMENT '接收推介的一至五级用户ID',
    `receiver_dept_id`       BIGINT                                  COMMENT '接收推介的用户所在部门ID（自动派生）',
    `recommend_status`       TINYINT                                 COMMENT '0未发送 1已发送 2已接受 3已拒绝',
    `rejected_reason`        VARCHAR(256),
    `recommend_sent_time`    DATETIME,
    `recommend_confirm_time` DATETIME,
    `joint_tracking`         TINYINT       NOT NULL DEFAULT 0        COMMENT '是否共同追踪：0否 1是',
    `joint_tracking_time`    DATETIME                                  COMMENT '开启共同追踪时间',
    -- 追踪状态
    `tracking_status`        TINYINT       NOT NULL DEFAULT 0        COMMENT '0待追踪 1到位 2未到位 3其他 4强制结束',
    `not_in_place_count`     INT           NOT NULL DEFAULT 0,
    `tracking_remark`        TEXT,
    `arrival_time`           DATETIME                                COMMENT '到位时间',
    `tracking_history_json`  TEXT                                    COMMENT '追踪过程记录JSON',
    -- 到位后补录
    `has_infection_screen`   VARCHAR(10),
    `screen_date`            DATE,
    `screen_method`          VARCHAR(64),
    `screen_result`          VARCHAR(128),
    `infection_result`       VARCHAR(128),
    `has_chest_xray`         VARCHAR(10),
    `chest_xray_date`        DATE,
    `chest_xray_result`      VARCHAR(128),
    `symptoms_json`          TEXT,
    -- 诊断
    `diagnosis_result`       VARCHAR(64)                             COMMENT '排除/确诊患者/潜伏感染者/其他',
    `diagnosis_remark`       TEXT                                    COMMENT '诊断结果选择其他时的备注',
    `diagnosis_time`         DATETIME,
    -- 归集去向
    `archived`               TINYINT       NOT NULL DEFAULT 0,
    `target_patient_id`      BIGINT,
    `target_latent_id`       BIGINT,
    -- 操作人/部门
    `department_id`          BIGINT,
    `creator_id`             BIGINT,
    `create_time`            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                TINYINT       NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推介追踪记录表（V17）';

-- 兼容已部署环境：在管总览菜单权限（可重复执行）
-- INSERT IGNORE INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`) VALUES
-- (460, 'latentManagement:overview', '在管总览', 1, 412, 0),
-- (461, 'latentManagement:edit', '修改信息', 2, 460, 1),
-- (462, 'patientManagement:overview', '在管总览', 1, 420, 0),
-- (463, 'patientManagement:edit', '修改信息', 2, 462, 1);
-- INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
-- SELECT r.role, p.id FROM (SELECT 1 AS role UNION SELECT 2) r
-- CROSS JOIN `permission` p WHERE p.`code` IN (
--   'latentManagement:overview', 'latentManagement:edit',
--   'patientManagement:overview', 'patientManagement:edit'
-- );

-- 兼容已部署环境：补充推介/追踪原因字段（列已存在时可忽略报错）
-- ALTER TABLE `referral_tracking` ADD COLUMN `recommend_reason` VARCHAR(512) COMMENT '推介原因（recommend模式）' AFTER `crowd_category`;
-- ALTER TABLE `referral_tracking` ADD COLUMN `track_reason` VARCHAR(512) COMMENT '追踪原因（track模式）' AFTER `recommend_reason`;
-- ALTER TABLE `referral_tracking` ADD COLUMN `arrival_time` DATETIME COMMENT '到位时间' AFTER `tracking_remark`;
-- ALTER TABLE `referral_tracking` ADD COLUMN `tracking_history_json` TEXT COMMENT '追踪过程记录JSON' AFTER `arrival_time`;
-- ALTER TABLE `referral_tracking` ADD COLUMN `source_type` VARCHAR(16) NOT NULL DEFAULT 'manual' COMMENT 'manual=手动 epidemic=大疫情导入' AFTER `track_reason`;
-- ALTER TABLE `referral_tracking` ADD COLUMN `card_id` VARCHAR(64) COMMENT '卡片ID' AFTER `source_type`;
-- ALTER TABLE `referral_tracking` ADD COLUMN `parent_name` VARCHAR(64) COMMENT '患儿家长姓名' AFTER `card_id`;
-- ALTER TABLE `referral_tracking` ADD COLUMN `workplace` VARCHAR(256) COMMENT '患者工作单位' AFTER `parent_name`;
-- ALTER TABLE `referral_tracking` ADD COLUMN `township` VARCHAR(128) COMMENT '乡镇' AFTER `workplace`;
-- ALTER TABLE `referral_tracking` ADD COLUMN `case_category` VARCHAR(64) COMMENT '病例分类' AFTER `township`;
-- ALTER TABLE `referral_tracking` ADD COLUMN `disease_name` VARCHAR(128) COMMENT '疾病名称' AFTER `case_category`;
-- ALTER TABLE `referral_tracking` ADD COLUMN `report_unit` VARCHAR(256) COMMENT '报告单位' AFTER `disease_name`;
-- ALTER TABLE `referral_tracking` ADD COLUMN `report_card_time` DATETIME COMMENT '报告卡录入时间' AFTER `report_unit`;
-- ALTER TABLE `referral_tracking` ADD COLUMN `epidemic_remark` TEXT COMMENT '大疫情备注' AFTER `report_card_time`;
-- ALTER TABLE `referral_tracking` ADD COLUMN `upload_batch` VARCHAR(64) COMMENT '导入批次号' AFTER `epidemic_remark`;

-- ---------- 2. 推介追踪权限码（ID 从 430 起） ----------
INSERT IGNORE INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`) VALUES
(430, 'referralManagement',              '推介追踪管理',         1, 0,   13),
(431, 'referralManagement:recommend',    '推介',                 1, 430, 1),
(432, 'referralManagement:create',       '新增推介/追踪记录',    2, 431, 1),
(433, 'referralManagement:send',         '发送推介通知',         2, 431, 2),
(434, 'referralManagement:confirm',      '确认/拒绝推介',        2, 431, 3),
(435, 'referralManagement:trackOperate', '操作追踪状态',         2, 431, 4),
(436, 'referralManagement:xray',         '录入胸片',             2, 431, 5),
(437, 'referralManagement:diagnosis',    '录入诊断',             2, 431, 6),
(438, 'referralManagement:delete',       '删除推介/追踪记录',    2, 431, 7),
(439, 'referralManagement:track',        '追踪',                 1, 430, 2),
(440, 'referralManagement:epidemicImport','大疫情导入',          2, 439, 1),
(441, 'referralManagement:export',       '导出推介/追踪记录',    2, 430, 3),
(442, 'referralManagement:edit',           '编辑追踪记录',         2, 439, 3);

-- 兼容：原拥有 epidemic:screening 权限的角色同步获得追踪模块大疫情导入权限
-- INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
-- SELECT rp.role, p.id FROM `role_permission` rp
-- JOIN `permission` old_p ON old_p.id = rp.permission_id AND old_p.`code` = 'epidemic:screening'
-- CROSS JOIN `permission` p WHERE p.`code` IN ('referralManagement:track', 'referralManagement:epidemicImport', 'referralManagement:export', 'referralManagement:edit');

-- ---------- 3. 将 V17 推介追踪权限赋给角色 1（超级管理员）和 2（一级管理员） ----------
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id
FROM (SELECT 1 AS role UNION SELECT 2) r
         CROSS JOIN `permission` p
WHERE p.`code` IN (
    'referralManagement', 'referralManagement:recommend', 'referralManagement:track',
    'referralManagement:create', 'referralManagement:send', 'referralManagement:confirm',
    'referralManagement:trackOperate', 'referralManagement:xray', 'referralManagement:diagnosis',
    'referralManagement:delete', 'referralManagement:epidemicImport', 'referralManagement:export',
    'referralManagement:edit'
);

-- ==================== V18：supervision_form 补充人员基本信息字段（P4 聚合潜伏督导表修复） ====================
DROP PROCEDURE IF EXISTS _v18_migrate_supervision_person;
DELIMITER $$
CREATE PROCEDURE _v18_migrate_supervision_person()
BEGIN
    DECLARE CONTINUE HANDLER FOR 1060 BEGIN END;

    ALTER TABLE `supervision_form` ADD COLUMN `household_address` VARCHAR(255) NULL COMMENT '户籍地址' AFTER `current_address`;
    ALTER TABLE `supervision_form` ADD COLUMN `id_number`         VARCHAR(50)  NULL COMMENT '身份证号' AFTER `household_address`;
    ALTER TABLE `supervision_form` ADD COLUMN `birth_date`         VARCHAR(20)  NULL COMMENT '出生日期' AFTER `id_number`;
    ALTER TABLE `supervision_form` ADD COLUMN `ethnicity`          VARCHAR(50)  NULL COMMENT '民族'    AFTER `birth_date`;
    ALTER TABLE `supervision_form` ADD COLUMN `managing_unit`      VARCHAR(100) NULL COMMENT '管理单位' AFTER `ethnicity`;
    ALTER TABLE `supervision_form` ADD COLUMN `supervising_doctor` VARCHAR(100) NULL COMMENT '督导医生' AFTER `managing_unit`;
END$$
DELIMITER ;
CALL _v18_migrate_supervision_person();
DROP PROCEDURE IF EXISTS _v18_migrate_supervision_person;

-- ==================== V19：旧 V1 权限码软删（§8 R5 决策：旧码保留 deleted=1，不立即物理删） ====================
-- 受影响的旧菜单权限：school / keyPopulation 的 patient、history、latent 子菜单
-- 以及对应三条主线的旧一级菜单权限码（V2 已用 screening / latentManagement / patientManagement 替代）
-- 注意：permission 表无 deleted 字段，用 UPDATE name 方式标记废弃；
--       同时将对应 role_permission 行 soft-delete（role_permission 表同样无 deleted，此处只做标注性注释保留）
-- 实际操作：仅重命名 V1 旧权限 name 加 [废弃] 前缀，权限码 code 保持不变以供历史数据兼容查询。

UPDATE `permission`
SET `name` = CONCAT('[废弃] ', `name`)
WHERE `code` IN (
    'school:latent',   'school:patient',  'school:history',
    'keyPopulation:latent', 'keyPopulation:patient', 'keyPopulation:history',
    'closeContact:patient', 'closeContact:history'
)
  AND `name` NOT LIKE '[废弃]%'; -- 幂等：避免重复执行时重复加前缀

-- ==================== V20：大疫情待诊断表（epidemic_import）====================
-- 文档 §5.2.1 要求新建 epidemic_import 表（含追踪、诊断字段），替代原仅存 raw_data JSON 的 epidemic_report 表。
-- epidemic_report 保留不删（历史数据兼容）；新功能使用 epidemic_import。

CREATE TABLE IF NOT EXISTS `epidemic_import` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    -- 从大疫情表提取的 10 个字段（文档§4.1）
    `name`               VARCHAR(64)  DEFAULT NULL COMMENT '患者姓名',
    `id_number`          VARCHAR(64)  DEFAULT NULL COMMENT '有效证件号',
    `gender`             VARCHAR(10)  DEFAULT NULL COMMENT '性别',
    `birth_date`         DATE         DEFAULT NULL COMMENT '出生日期',
    `age`                INT          DEFAULT NULL COMMENT '年龄',
    `phone`              VARCHAR(32)  DEFAULT NULL COMMENT '联系电话',
    `current_address`    VARCHAR(256) DEFAULT NULL COMMENT '现详细住址（来自：现住地址区现住详细）',
    `case_category`      VARCHAR(64)  DEFAULT NULL COMMENT '病例分类',
    `disease_name`       VARCHAR(128) DEFAULT NULL COMMENT '疾病名称',
    `report_unit`        VARCHAR(256) DEFAULT NULL COMMENT '报告单位',
    -- 追踪字段
    `tracking_status`    TINYINT      NOT NULL DEFAULT 0  COMMENT '0待追踪 1到位 2未到位 3其他 4强制结束',
    `not_in_place_count` INT          NOT NULL DEFAULT 0  COMMENT '未到位次数',
    `tracking_remark`    TEXT         DEFAULT NULL        COMMENT '追踪备注',
    -- 胸片字段
    `has_chest_xray`     VARCHAR(10)  DEFAULT NULL COMMENT '是否进行胸片检查',
    `chest_xray_date`    DATE         DEFAULT NULL COMMENT '胸片检查日期',
    `chest_xray_result`  VARCHAR(64)  DEFAULT NULL COMMENT '胸片结果：正常/异常/未查',
    -- 诊断字段（录入诊断后自动分流）
    `diagnosis_result`   VARCHAR(64)  DEFAULT NULL COMMENT '诊断结果：排除/疑似肺结核/潜伏感染者/确诊患者/其他',
    `diagnosis_time`     DATETIME     DEFAULT NULL,
    -- 归集去向
    `archived`           TINYINT      NOT NULL DEFAULT 0,
    `target_patient_id`  BIGINT       DEFAULT NULL COMMENT '分流到患者管理后的 patient.id',
    `target_latent_id`   BIGINT       DEFAULT NULL COMMENT '分流到潜伏感染后的 latent_infection.id',
    -- 批次与系统字段
    `upload_batch`       VARCHAR(64)  DEFAULT NULL COMMENT '上传批次号',
    `department_id`      BIGINT       DEFAULT NULL,
    `creator_id`         BIGINT       DEFAULT NULL,
    `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`            TINYINT      NOT NULL DEFAULT 0,
    KEY `idx_id_number`      (`id_number`),
    KEY `idx_tracking`       (`tracking_status`),
    KEY `idx_batch`          (`upload_batch`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大疫情待诊断记录表（V20，文档§4.1）';

-- 大疫情待诊断权限码补充（epidemic:screening 已在 V16 创建，此处补充操作按钮）
INSERT IGNORE INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`)
SELECT 'epidemic:screening:import', '上传大疫情表', 2, p.id, 1
FROM `permission` p
WHERE p.`code` = 'epidemic:screening';

INSERT IGNORE INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`)
SELECT 'epidemic:screening:track', '追踪', 2, p.id, 2
FROM `permission` p
WHERE p.`code` = 'epidemic:screening';

INSERT IGNORE INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`)
SELECT 'epidemic:screening:xray', '录入胸片', 2, p.id, 3
FROM `permission` p
WHERE p.`code` = 'epidemic:screening';

INSERT IGNORE INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`)
SELECT 'epidemic:screening:diagnosis', '录入诊断', 2, p.id, 4
FROM `permission` p
WHERE p.`code` = 'epidemic:screening';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id
FROM (SELECT 1 AS role UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) r
         CROSS JOIN `permission` p
WHERE p.`code` IN (
    'epidemic:screening:import', 'epidemic:screening:track',
    'epidemic:screening:xray', 'epidemic:screening:diagnosis'
);

-- ==================== V21：分级诊疗增加转诊原因字段 ====================
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral' AND COLUMN_NAME = 'referral_reason'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral` ADD COLUMN `referral_reason` VARCHAR(512) DEFAULT NULL COMMENT ''转诊原因（发送方填写）'' AFTER `reject_reason`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==================== V22：supervision_form 增加是否进行预防性治疗 ====================
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'supervision_form' AND COLUMN_NAME = 'has_preventive_treatment'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `supervision_form` ADD COLUMN `has_preventive_treatment` VARCHAR(10) DEFAULT NULL COMMENT ''是否进行预防性治疗：是/否'' AFTER `managing_unit`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==================== V23：筛查问卷配置 ====================
CREATE TABLE IF NOT EXISTS `questionnaire_config` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`            VARCHAR(64)  NOT NULL COMMENT '问卷编码',
    `title`           VARCHAR(256) NOT NULL COMMENT '问卷标题',
    `subtitle`        VARCHAR(512) DEFAULT NULL COMMENT '问卷说明',
    `enabled`         TINYINT      NOT NULL DEFAULT 1 COMMENT '是否开启：0否 1是',
    `population_type` VARCHAR(32)  NOT NULL DEFAULT 'school' COMMENT '关联人群类型',
    `fields_json`     LONGTEXT     NOT NULL COMMENT '字段分组 JSON',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='筛查问卷配置表';

INSERT IGNORE INTO `questionnaire_config` (`code`, `title`, `subtitle`, `enabled`, `population_type`, `fields_json`)
VALUES (
    'school',
    '学校人群结核病筛查调查问卷',
    '请如实填写以下信息，所有数据仅用于结核病防控统计分析，信息将严格保密。',
    1,
    'school',
    '[]'
);

-- ==================== V24：首次/后续随访草稿状态 ====================
DROP PROCEDURE IF EXISTS _v24_migrate_visit_draft_status;
DELIMITER //
CREATE PROCEDURE _v24_migrate_visit_draft_status()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'first_visit' AND COLUMN_NAME = 'status'
    ) THEN
        ALTER TABLE `first_visit`
            ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0草稿 1已完成' AFTER `attachment_urls`;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'follow_up_visit' AND COLUMN_NAME = 'status'
    ) THEN
        ALTER TABLE `follow_up_visit`
            ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0草稿 1已完成' AFTER `attachment_urls`;
    END IF;
END //
DELIMITER ;
CALL _v24_migrate_visit_draft_status();
DROP PROCEDURE IF EXISTS _v24_migrate_visit_draft_status;

-- ==================== V25：督导表增加电话备注 ====================
DROP PROCEDURE IF EXISTS _v25_migrate_supervision_phone_remark;
DELIMITER //
CREATE PROCEDURE _v25_migrate_supervision_phone_remark()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'supervision_form' AND COLUMN_NAME = 'phone_remark'
    ) THEN
        ALTER TABLE `supervision_form`
            ADD COLUMN `phone_remark` VARCHAR(256) DEFAULT NULL COMMENT '电话备注（非本人电话时说明）' AFTER `phone`;
    END IF;
END //
DELIMITER ;
CALL _v25_migrate_supervision_phone_remark();
DROP PROCEDURE IF EXISTS _v25_migrate_supervision_phone_remark;

-- ==================== V26：患者管理「转出」权限挂到在管总览下 ====================
UPDATE `permission`
SET `name` = '转出', `parent_id` = 462, `sort` = 2
WHERE `code` = 'patientManagement:referral'
  AND (`name` <> '转出' OR `parent_id` <> 462);

-- ==================== V27：患者表增加归档备注（转出归档用） ====================
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'patient' AND COLUMN_NAME = 'archive_remark'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `patient` ADD COLUMN `archive_remark` VARCHAR(128) DEFAULT NULL COMMENT ''归档备注（如：已转出）'' AFTER `archived_time`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==================== V28：通知单增加服药管理单位、备注 ====================
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notice' AND COLUMN_NAME = 'medication_management_unit'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `notice` ADD COLUMN `medication_management_unit` VARCHAR(256) DEFAULT NULL COMMENT ''服药管理单位（来自病案信息）'' AFTER `other_notes`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notice' AND COLUMN_NAME = 'remark'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `notice` ADD COLUMN `remark` TEXT DEFAULT NULL COMMENT ''备注（手动填写）'' AFTER `medication_management_unit`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==================== V29：后续随访增加是否停止治疗、停止治疗原因-其它 ====================
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'follow_up_visit' AND COLUMN_NAME = 'stop_treatment'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `follow_up_visit` ADD COLUMN `stop_treatment` VARCHAR(8) DEFAULT NULL COMMENT ''是否停止治疗（是/否）'' AFTER `doctor_signature`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'follow_up_visit' AND COLUMN_NAME = 'stop_treatment_reason_other'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `follow_up_visit` ADD COLUMN `stop_treatment_reason_other` VARCHAR(256) DEFAULT NULL COMMENT ''停止治疗原因-其它（手动录入）'' AFTER `stop_treatment_reason`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==================== V30：首次随访增加编号（8位数字） ====================
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'first_visit' AND COLUMN_NAME = 'form_no'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `first_visit` ADD COLUMN `form_no` VARCHAR(8) DEFAULT NULL COMMENT ''编号（8位数字，手动录入）'' AFTER `population_type`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==================== V31：推介追踪增加到位时间、追踪过程记录 ====================
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'arrival_time'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `arrival_time` DATETIME DEFAULT NULL COMMENT ''到位时间'' AFTER `tracking_remark`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'tracking_history_json'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `tracking_history_json` TEXT DEFAULT NULL COMMENT ''追踪过程记录JSON'' AFTER `arrival_time`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==================== V32：大疫情导入从筛查管理迁移至推介追踪-追踪 ====================
UPDATE `permission`
SET `name` = CONCAT('[废弃] ', `name`)
WHERE `code` IN (
    'epidemic:screening',
    'epidemic:screening:import',
    'epidemic:screening:track',
    'epidemic:screening:xray',
    'epidemic:screening:diagnosis'
)
  AND `name` NOT LIKE '[废弃]%';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         JOIN `permission` old_p ON old_p.id = rp.permission_id
    AND old_p.`code` IN (
        'epidemic:screening',
        'epidemic:screening:import',
        'epidemic:screening:track',
        'epidemic:screening:xray',
        'epidemic:screening:diagnosis'
    )
         CROSS JOIN `permission` p
WHERE p.`code` IN (
    'referralManagement', 'referralManagement:track', 'referralManagement:create',
    'referralManagement:trackOperate', 'referralManagement:xray', 'referralManagement:diagnosis',
    'referralManagement:delete', 'referralManagement:epidemicImport', 'referralManagement:export',
    'referralManagement:edit'
);

-- ==================== V33：密接个案表（独立电子表格模块） ====================
INSERT IGNORE INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`) VALUES
(35, 'closeContact:case',           '密接个案表',   1, 3, 1),
(350, 'closeContact:case:upload',    '导入个案',     2, 35, 1),
(351, 'closeContact:case:create',    '新增个案',     2, 35, 2),
(352, 'closeContact:case:export',    '导出个案',     2, 35, 3),
(353, 'closeContact:case:edit',      '编辑个案',     2, 35, 4),
(354, 'closeContact:case:delete',    '删除个案',     2, 35, 5);

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT 1, `id` FROM `permission` WHERE `code` LIKE 'closeContact:case%';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT 2, `id` FROM `permission` WHERE `code` LIKE 'closeContact:case%';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT 3, `id` FROM `permission` WHERE `code` LIKE 'closeContact:case%';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT 4, `id` FROM `permission` WHERE `code` LIKE 'closeContact:case%';

-- ==================== V34：常规筛查改名为疫情筛查 ====================
UPDATE `permission`
SET `name` = '疫情筛查'
WHERE `code` = 'regular:screening';

UPDATE `permission`
SET `name` = '疫情筛查-待诊断'
WHERE `code` = 'regular:suspected';

-- ==================== V35：潜伏感染者管理「转出」权限挂到在管总览下 ====================
UPDATE `permission`
SET `name` = '转出', `parent_id` = 460, `sort` = 2
WHERE `code` = 'latentManagement:referral'
  AND (`name` <> '转出' OR `parent_id` <> 460);

-- ==================== V36：待诊断阶段清除误写入 latent 的首次诊断 ====================
-- 筛查导入时 diagnosisFirst 应仅保存在筛查表，待「确认诊断」后再写入 latent 并分流
UPDATE `latent_infection` li
    INNER JOIN `screening_school` ss ON li.`screening_id` = ss.`id`
SET li.`diagnosis_first` = NULL
WHERE li.`population_type` = 'school'
  AND li.`referral_result` IS NULL
  AND li.`archived` = 0
  AND li.`diagnosis_first` IS NOT NULL;

UPDATE `latent_infection` li
    INNER JOIN `screening_key_population` sk ON li.`screening_id` = sk.`id`
SET li.`diagnosis_first` = NULL
WHERE li.`population_type` IN ('keyPopulation', 'regular')
  AND li.`referral_result` IS NULL
  AND li.`archived` = 0
  AND li.`diagnosis_first` IS NOT NULL;

-- ==================== V37：督导表支持一对多记录（form_seq） ====================
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'supervision_form' AND COLUMN_NAME = 'form_seq'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `supervision_form` ADD COLUMN `form_seq` INT DEFAULT NULL COMMENT ''第几次督导表（status>=1时有效）'' AFTER `patient_name`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `supervision_form` sf
    INNER JOIN (
        SELECT id,
               ROW_NUMBER() OVER (PARTITION BY latent_infection_id ORDER BY create_time ASC) AS seq
        FROM `supervision_form`
        WHERE `status` >= 1
    ) ranked ON sf.id = ranked.id
SET sf.`form_seq` = ranked.seq
WHERE sf.`status` >= 1;

-- ==================== V38：推介追踪表补充大疫情导入字段 ====================
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'source_type'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `source_type` VARCHAR(16) NOT NULL DEFAULT ''manual'' COMMENT ''manual=手动 epidemic=大疫情导入'' AFTER `track_reason`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'card_id'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `card_id` VARCHAR(64) DEFAULT NULL COMMENT ''卡片ID'' AFTER `source_type`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'parent_name'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `parent_name` VARCHAR(64) DEFAULT NULL COMMENT ''患儿家长姓名'' AFTER `card_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'workplace'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `workplace` VARCHAR(256) DEFAULT NULL COMMENT ''患者工作单位'' AFTER `parent_name`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'township'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `township` VARCHAR(128) DEFAULT NULL COMMENT ''乡镇'' AFTER `workplace`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'case_category'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `case_category` VARCHAR(64) DEFAULT NULL COMMENT ''病例分类'' AFTER `township`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'disease_name'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `disease_name` VARCHAR(128) DEFAULT NULL COMMENT ''疾病名称'' AFTER `case_category`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'report_unit'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `report_unit` VARCHAR(256) DEFAULT NULL COMMENT ''报告单位'' AFTER `disease_name`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'report_card_time'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `report_card_time` DATETIME DEFAULT NULL COMMENT ''报告卡录入时间'' AFTER `report_unit`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'epidemic_remark'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `epidemic_remark` TEXT DEFAULT NULL COMMENT ''大疫情备注'' AFTER `report_card_time`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'upload_batch'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `upload_batch` VARCHAR(64) DEFAULT NULL COMMENT ''导入批次号'' AFTER `epidemic_remark`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==================== V39：权限树清理废弃项 + 统计分析问卷权限 ====================
-- 1）统计分析 — 筛查问卷（挂在 statistics=4 下，与 statistics:export 并列）
INSERT IGNORE INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`) VALUES
(131, 'statistics:questionnaire', '筛查问卷', 2, 4, 2);

UPDATE `permission`
SET `parent_id` = 4, `sort` = 2, `name` = '筛查问卷', `type` = 2
WHERE `code` = 'statistics:questionnaire';

-- 2）默认授予超级管理员、一至三级（与 statistics:export 范围一致）
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id
FROM (SELECT 1 AS role UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) r
         CROSS JOIN `permission` p
WHERE p.`code` = 'statistics:questionnaire';

-- 3）清理角色权限表中已废弃权限的关联（含其全部子权限）
DELETE rp FROM `role_permission` rp
    INNER JOIN `permission` p ON p.id = rp.permission_id
WHERE p.`name` LIKE '[废弃]%'
   OR p.`parent_id` IN (SELECT id FROM (SELECT id FROM `permission` WHERE `name` LIKE '[废弃]%') AS deprecated_parents)
   OR p.`parent_id` IN (
       SELECT id FROM (
           SELECT c.id
           FROM `permission` c
                    INNER JOIN `permission` parent ON parent.id = c.parent_id
           WHERE parent.`name` LIKE '[废弃]%'
       ) AS deprecated_children
   );

-- ==================== V40：潜伏感染者管理 — 历史患者（归档信息） ====================
INSERT IGNORE INTO `permission` (`id`, `code`, `name`, `type`, `parent_id`, `sort`) VALUES
(464, 'latentManagement:history', '历史患者', 1, 412, 3);

UPDATE `permission`
SET `parent_id` = 412, `sort` = 3, `name` = '历史患者', `type` = 1
WHERE `code` = 'latentManagement:history';

-- 与 latentManagement 其它子菜单一致：授予已拥有「潜伏感染者管理」父权限的角色
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` parent ON parent.id = rp.permission_id AND parent.`code` = 'latentManagement'
         CROSS JOIN `permission` p
WHERE p.`code` = 'latentManagement:history';

-- ==================== V41：密接人群 — 菜单顺序（个案表在筛查管理前）+ 补偿建表/字段 ====================
UPDATE `permission` SET `sort` = 1 WHERE `code` = 'closeContact:case';
UPDATE `permission` SET `sort` = 2 WHERE `code` = 'closeContact:screening';
UPDATE `permission` SET `sort` = 3 WHERE `code` = 'closeContact:latent';
UPDATE `permission` SET `sort` = 4 WHERE `code` = 'closeContact:followUp';

-- 补偿建表：生产库若未执行过含 close_contact_case 的 init 段，此处确保表存在
CREATE TABLE IF NOT EXISTS `close_contact_case` (
    `id`                              BIGINT       NOT NULL AUTO_INCREMENT,
    `city`                            VARCHAR(64)  DEFAULT NULL COMMENT '市/州',
    `district`                        VARCHAR(64)  DEFAULT NULL COMMENT '区/县',
    `source_patient_name`             VARCHAR(64)  DEFAULT NULL COMMENT '原患者姓名',
    `source_patient_case_no`          VARCHAR(64)  DEFAULT NULL COMMENT '原患者病案号',
    `source_patient_bacteriology_result` VARCHAR(64) DEFAULT NULL COMMENT '原患者病原学结果',
    `source_patient_phone`            VARCHAR(32)  DEFAULT NULL COMMENT '原患者电话',
    `source_patient_id_number`        VARCHAR(64)  DEFAULT NULL COMMENT '原患者身份证号',
    `report_date`                     DATE         DEFAULT NULL COMMENT '填表日期',
    `registration_date`               DATE         DEFAULT NULL COMMENT '密切接触者登记日期',
    `name`                            VARCHAR(64)  DEFAULT NULL COMMENT '接触者姓名',
    `id_number`                       VARCHAR(64)  DEFAULT NULL COMMENT '接触者身份证号',
    `age`                             INT          DEFAULT NULL COMMENT '年龄',
    `phone`                           VARCHAR(32)  DEFAULT NULL COMMENT '接触者电话',
    `contact_type`                    VARCHAR(32)  DEFAULT NULL COMMENT '接触类型',
    `contact_place`                   VARCHAR(64)  DEFAULT NULL COMMENT '接触场所',
    `first_screen_date`               DATE         DEFAULT NULL COMMENT '首次筛查日期',
    `symptom1`                        VARCHAR(128) DEFAULT NULL COMMENT '结核症状1',
    `symptom2`                        VARCHAR(128) DEFAULT NULL COMMENT '结核症状2',
    `infection_check_date`            DATE         DEFAULT NULL COMMENT '感染检测日期',
    `infection_check_method`          VARCHAR(64)  DEFAULT NULL COMMENT '感染检测方法',
    `infection_check_result`          VARCHAR(64)  DEFAULT NULL COMMENT '结果判定',
    `imaging_date`                    DATE         DEFAULT NULL COMMENT '影像检查日期',
    `imaging_method`                  VARCHAR(64)  DEFAULT NULL COMMENT '影像方法',
    `imaging_result`                  VARCHAR(128) DEFAULT NULL COMMENT '影像结果',
    `sputum_check_date`               DATE         DEFAULT NULL COMMENT '痰检留标日期',
    `sputum_check_method`             VARCHAR(64)  DEFAULT NULL COMMENT '痰检方法',
    `sputum_check_result`             VARCHAR(64)  DEFAULT NULL COMMENT '痰检结果',
    `final_screening_result`          VARCHAR(32)  DEFAULT NULL COMMENT '诊断结果',
    `has_contraindication`            VARCHAR(32)  DEFAULT NULL COMMENT '有无禁忌症',
    `no_treatment_reason`             VARCHAR(128) DEFAULT NULL COMMENT '不接受预防治疗的原因',
    `contraindication_remark`         VARCHAR(256) DEFAULT NULL COMMENT '禁忌症备注',
    `has_preventive_treatment`        VARCHAR(10)  DEFAULT NULL COMMENT '是否开展预防性治疗',
    `preventive_plan`                 VARCHAR(128) DEFAULT NULL COMMENT '预防性治疗方案',
    `preventive_plan_remark`          VARCHAR(256) DEFAULT NULL COMMENT '其他方案备注',
    `treatment_completed`             VARCHAR(10)  DEFAULT NULL COMMENT '是否完成治疗',
    `incomplete_reason`               VARCHAR(128) DEFAULT NULL COMMENT '未完成原因',
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
    `year`                            VARCHAR(10)  DEFAULT NULL COMMENT '年份',
    `gender`                          VARCHAR(10)  DEFAULT NULL COMMENT '性别',
    `ethnicity`                       VARCHAR(32)  DEFAULT NULL COMMENT '民族',
    `household_address`               VARCHAR(256) DEFAULT NULL COMMENT '户籍地址',
    `current_address`                 VARCHAR(256) DEFAULT NULL COMMENT '现住址',
    `upload_batch`                    VARCHAR(64)  DEFAULT NULL COMMENT '上传批次号',
    `department_id`                   BIGINT       DEFAULT NULL COMMENT '所属部门ID',
    `creator_username`                VARCHAR(64)  DEFAULT NULL COMMENT '录入用户名',
    `create_time`                     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`                     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_id_number` (`id_number`),
    KEY `idx_district` (`district`),
    KEY `idx_final_result` (`final_screening_result`),
    KEY `idx_creator_username` (`creator_username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密接个案表（电子表格，73列）';

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_close_contact' AND COLUMN_NAME = 'department_id'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `screening_close_contact` ADD COLUMN `department_id` BIGINT DEFAULT NULL COMMENT ''所属部门ID''',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==================== V42：推介追踪 — 大疫情导入/导出授予一至五级 ====================
UPDATE `permission`
SET `name` = '导出推介/追踪记录', `parent_id` = 430, `sort` = 3
WHERE `code` = 'referralManagement:export';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id
FROM (SELECT 2 AS role UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) r
         CROSS JOIN `permission` p
WHERE p.`code` IN (
    'referralManagement',
    'referralManagement:recommend',
    'referralManagement:track',
    'referralManagement:epidemicImport',
    'referralManagement:export',
    'referralManagement:edit',
    'referralManagement:create',
    'referralManagement:trackOperate'
);

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` parent ON parent.id = rp.permission_id AND parent.`code` = 'referralManagement'
         CROSS JOIN `permission` p
WHERE p.`code` IN (
    'referralManagement:recommend',
    'referralManagement:track',
    'referralManagement:epidemicImport',
    'referralManagement:export',
    'referralManagement:edit',
    'referralManagement:create',
    'referralManagement:trackOperate'
);

-- ==================== V43：五级用户 — 患者管理服药权限（不含填写领药） ====================
INSERT IGNORE INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`) VALUES
('patientManagement:pickup', '填写领药', 2, 420, 7);

UPDATE `permission`
SET `parent_id` = 420, `sort` = 7, `name` = '填写领药', `type` = 2
WHERE `code` = 'patientManagement:pickup';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT 6, p.id
FROM `permission` p
WHERE p.`code` IN (
    'patientManagement',
    'patientManagement:medication',
    'patientManagement:firstVisit',
    'patientManagement:followUp',
    'patientManagement:notice'
);

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` old_p ON old_p.id = rp.permission_id
    AND old_p.`code` = 'patient:medication'
         CROSS JOIN `permission` p
WHERE p.`code` IN ('patientManagement', 'patientManagement:medication')
  AND rp.`role` != 6;

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` old_p ON old_p.id = rp.permission_id
    AND old_p.`code` = 'patient:medication'
         CROSS JOIN `permission` p
WHERE p.`code` = 'patientManagement:pickup'
  AND rp.`role` != 6;

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` old_p ON old_p.id = rp.permission_id
    AND old_p.`code` = 'patient:firstVisit'
         CROSS JOIN `permission` p
WHERE p.`code` = 'patientManagement:firstVisit';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` old_p ON old_p.id = rp.permission_id
    AND old_p.`code` = 'patient:followUp'
         CROSS JOIN `permission` p
WHERE p.`code` = 'patientManagement:followUp';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` old_p ON old_p.id = rp.permission_id
    AND old_p.`code` = 'patient:confirmNotice'
         CROSS JOIN `permission` p
WHERE p.`code` = 'patientManagement:notice';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` parent ON parent.id = rp.permission_id AND parent.`code` = 'patientManagement'
         CROSS JOIN `permission` p
WHERE p.`code` = 'patientManagement:medication';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` parent ON parent.id = rp.permission_id AND parent.`code` = 'patientManagement'
         CROSS JOIN `permission` p
WHERE p.`code` = 'patientManagement:pickup'
  AND rp.`role` != 6;

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id
FROM (SELECT 1 AS role UNION SELECT 2) r
         CROSS JOIN `permission` p
WHERE p.`code` = 'patientManagement:pickup';

-- ==================== V44：市/县级用户 — 查看并管理辖区内五级用户工作 ====================
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id
FROM (SELECT 3 AS role UNION SELECT 4 UNION SELECT 5) r
         CROSS JOIN `permission` p
WHERE p.`code` IN (
    'latentManagement', 'latentManagement:overview', 'latentManagement:edit',
    'latentManagement:notice', 'latentManagement:track', 'latentManagement:xray',
    'latentManagement:diagnosis', 'latentManagement:referral', 'latentManagement:close',
    'latentManagement:supervision', 'latentManagement:supervision:fill', 'latentManagement:supervision:edit', 'latentManagement:history',
    'patientManagement', 'patientManagement:overview', 'patientManagement:edit',
    'patientManagement:notice', 'patientManagement:notice:fill', 'patientManagement:firstVisit', 'patientManagement:firstVisit:fill', 'patientManagement:firstVisit:edit',
    'patientManagement:followUp', 'patientManagement:followUp:fill', 'patientManagement:followUp:edit',
    'patientManagement:medication', 'patientManagement:pickup', 'patientManagement:specialDisease',
    'patientManagement:history', 'patientManagement:referral', 'patientManagement:delete'
);

-- ==================== V45：随访方式「其他」手工录入字段 ====================
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'first_visit' AND COLUMN_NAME = 'visit_method_other'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `first_visit` ADD COLUMN `visit_method_other` VARCHAR(64) DEFAULT NULL COMMENT ''随访方式-其他（手工录入）'' AFTER `visit_method`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'follow_up_visit' AND COLUMN_NAME = 'visit_method_other'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `follow_up_visit` ADD COLUMN `visit_method_other` VARCHAR(64) DEFAULT NULL COMMENT ''随访方式-其他（手工录入）'' AFTER `visit_method`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- V46：在管总览手动新增/导入潜伏感染者 — screening_id 可空 + 扩展字段
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'latent_infection' AND COLUMN_NAME = 'household_address'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `latent_infection`
        MODIFY COLUMN `screening_id` BIGINT DEFAULT NULL COMMENT ''关联筛查数据ID（手动新增可为空）'',
        ADD COLUMN `household_address` VARCHAR(256) DEFAULT NULL COMMENT ''户籍地址'' AFTER `phone`,
        ADD COLUMN `current_address` VARCHAR(256) DEFAULT NULL COMMENT ''现住地址'' AFTER `household_address`,
        ADD COLUMN `phone_contact_relation` VARCHAR(64) DEFAULT NULL COMMENT ''联系电话与联系人关系'' AFTER `current_address`,
        ADD COLUMN `infection_screen_date` DATE DEFAULT NULL COMMENT ''感染筛查日期'' AFTER `phone_contact_relation`,
        ADD COLUMN `remark` TEXT DEFAULT NULL COMMENT ''备注'' AFTER `tracking_remark`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- V47：密接筛查 — 联系电话与接触者关系、接触场所-其他
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_close_contact' AND COLUMN_NAME = 'phone_contact_relation'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `screening_close_contact`
        ADD COLUMN `phone_contact_relation` VARCHAR(64) DEFAULT NULL COMMENT ''联系电话与接触者关系'' AFTER `phone`,
        ADD COLUMN `contact_place_other` VARCHAR(128) DEFAULT NULL COMMENT ''接触场所-其他（手工录入）'' AFTER `contact_place`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- V48：密接筛查初次筛查 — 影像/痰检/最终筛查结果「其他」手工录入
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_close_contact' AND COLUMN_NAME = 'imaging_method_other'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `screening_close_contact`
        ADD COLUMN `imaging_method_other` VARCHAR(128) DEFAULT NULL COMMENT ''影像方法-其他（手工录入）'' AFTER `imaging_method`,
        ADD COLUMN `imaging_result_other` VARCHAR(128) DEFAULT NULL COMMENT ''影像结果-其他（手工录入）'' AFTER `imaging_result`,
        ADD COLUMN `sputum_check_method_other` VARCHAR(128) DEFAULT NULL COMMENT ''痰检方法-其他（手工录入）'' AFTER `sputum_check_method`,
        ADD COLUMN `sputum_check_result_other` VARCHAR(128) DEFAULT NULL COMMENT ''痰检结果-其他（手工录入）'' AFTER `sputum_check_result`,
        ADD COLUMN `final_screening_result_other` VARCHAR(128) DEFAULT NULL COMMENT ''最终筛查结果-其他（手工录入）'' AFTER `final_screening_result`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- V49：患者/潜伏感染者 — 录入人（五级「谁录入谁可见」）
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'patient' AND COLUMN_NAME = 'creator_id'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `patient` ADD COLUMN `creator_id` BIGINT DEFAULT NULL COMMENT ''录入人用户ID'' AFTER `department_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'latent_infection' AND COLUMN_NAME = 'creator_id'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `latent_infection` ADD COLUMN `creator_id` BIGINT DEFAULT NULL COMMENT ''录入人用户ID'' AFTER `department_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==================== V50：推介追踪 — 一至五级补全大疫情导入/导出权限 ====================
UPDATE `permission`
SET `name` = '导出推介/追踪记录', `parent_id` = 430, `sort` = 3
WHERE `code` = 'referralManagement:export';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id
FROM (SELECT 2 AS role UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) r
         CROSS JOIN `permission` p
WHERE p.`code` IN ('referralManagement:epidemicImport', 'referralManagement:export');

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` existing ON existing.id = rp.permission_id
         CROSS JOIN `permission` p
WHERE existing.`code` LIKE 'referralManagement%'
  AND p.`code` IN ('referralManagement:epidemicImport', 'referralManagement:export');

-- ==================== V51：修复 patientManagement:pickup 权限 ID 冲突 ====================
INSERT IGNORE INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`) VALUES
('patientManagement:pickup', '填写领药', 2, 420, 7);

UPDATE `permission`
SET `parent_id` = 420, `sort` = 7, `name` = '填写领药', `type` = 2
WHERE `code` = 'patientManagement:pickup';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id
FROM (SELECT 1 AS role UNION SELECT 2) r
         CROSS JOIN `permission` p
WHERE p.`code` = 'patientManagement:pickup';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` existing ON existing.id = rp.permission_id
         CROSS JOIN `permission` p
WHERE existing.`code` IN (
    'patient:medication', 'patientManagement:medication',
    'keyPopulation:patient:medication', 'closeContact:patient:medication'
)
  AND p.`code` = 'patientManagement:pickup'
  AND rp.`role` != 6;

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` parent ON parent.id = rp.permission_id AND parent.`code` = 'patientManagement'
         CROSS JOIN `permission` p
WHERE p.`code` = 'patientManagement:pickup'
  AND rp.`role` != 6;

-- ==================== V54：五级用户 — 服药管理与填写领药分离 ====================
DELETE rp FROM `role_permission` rp
         INNER JOIN `permission` p ON p.id = rp.permission_id
WHERE rp.`role` = 6
  AND p.`code` = 'patientManagement:pickup';

DELETE up FROM `user_permission` up
         INNER JOIN `permission` p ON p.id = up.permission_id
         INNER JOIN `user` u ON u.id = up.user_id
WHERE u.role = 6
  AND u.deleted = 0
  AND p.`code` = 'patientManagement:pickup';

-- ==================== V53：推介追踪 — 一至五级补全操作按钮权限 ====================
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id
FROM (SELECT 2 AS role UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) r
         CROSS JOIN `permission` p
WHERE p.`code` IN (
    'referralManagement:edit',
    'referralManagement:trackOperate',
    'referralManagement:xray',
    'referralManagement:diagnosis',
    'referralManagement:delete'
);

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` parent ON parent.id = rp.permission_id AND parent.`code` = 'referralManagement'
         CROSS JOIN `permission` p
WHERE p.`code` IN (
    'referralManagement:edit',
    'referralManagement:trackOperate',
    'referralManagement:xray',
    'referralManagement:diagnosis',
    'referralManagement:delete'
);

-- ==================== V55：专病网导入补全 creator_id，修复五级用户看不到自己导入的患者 ====================
UPDATE `patient` p
    INNER JOIN (
        SELECT u.department_id, MIN(u.id) AS user_id, COUNT(*) AS cnt
        FROM `user` u
        WHERE u.role = 6 AND u.deleted = 0 AND u.department_id IS NOT NULL
        GROUP BY u.department_id
        HAVING cnt = 1
    ) solo ON solo.department_id = p.department_id
SET p.creator_id = solo.user_id
WHERE p.population_type = 'specialDisease'
  AND p.creator_id IS NULL
  AND p.deleted = 0;

-- ==================== V56：填写领药与服药管理权限树分离 ====================
UPDATE `permission`
SET `parent_id` = 420, `sort` = 7, `name` = '填写领药', `type` = 2
WHERE `code` = 'patientManagement:pickup';

DELETE rp FROM `role_permission` rp
         INNER JOIN `permission` p ON p.id = rp.permission_id
WHERE rp.`role` = 6
  AND p.`code` = 'patientManagement:pickup';

DELETE up FROM `user_permission` up
         INNER JOIN `permission` p ON p.id = up.permission_id
         INNER JOIN `user` u ON u.id = up.user_id
WHERE u.role = 6
  AND u.deleted = 0
  AND p.`code` = 'patientManagement:pickup';

-- ==================== V57：转出确认后同步复制患者至接收方部门 ====================
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral' AND COLUMN_NAME = 'target_biz_id'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral` ADD COLUMN `target_biz_id` BIGINT DEFAULT NULL COMMENT ''接收确认后在接收方生成的业务记录ID'' AFTER `biz_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'patient' AND COLUMN_NAME = 'source_patient_id'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `patient` ADD COLUMN `source_patient_id` BIGINT DEFAULT NULL COMMENT ''转出复制来源患者ID'' AFTER `creator_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `patient`
SET `archived` = 0,
    `archived_time` = NULL
WHERE `archive_remark` = '已转出'
  AND `archived` = 1;

-- ==================== V58：潜伏感染者转出同步（archive_remark、source_latent_id） ====================
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'latent_infection' AND COLUMN_NAME = 'archive_remark'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `latent_infection` ADD COLUMN `archive_remark` VARCHAR(128) DEFAULT NULL COMMENT ''归档备注（如：已转出）'' AFTER `archived_time`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'latent_infection' AND COLUMN_NAME = 'source_latent_id'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `latent_infection` ADD COLUMN `source_latent_id` BIGINT DEFAULT NULL COMMENT ''转出复制来源潜伏感染ID'' AFTER `creator_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `latent_infection`
SET `archived` = 0,
    `archived_time` = NULL
WHERE `archive_remark` = '已转出'
  AND `archived` = 1;

-- ==================== V59–V62：填写完成后「修改」独立按钮权限（按 code 写入，避免 id 冲突） ====================
INSERT INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`)
SELECT 'latentManagement:supervision:edit', '修改督导表', 2, parent.id, 1
FROM `permission` parent
WHERE parent.`code` = 'latentManagement:supervision'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'latentManagement:supervision:edit');

UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'latentManagement:supervision'
SET child.`parent_id` = parent.id, child.`sort` = 1, child.`name` = '修改督导表', child.`type` = 2
WHERE child.`code` = 'latentManagement:supervision:edit';

INSERT INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`)
SELECT 'patientManagement:firstVisit:edit', '编辑首次随访', 2, parent.id, 1
FROM `permission` parent
WHERE parent.`code` = 'patientManagement:firstVisit'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'patientManagement:firstVisit:edit');

UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'patientManagement:firstVisit'
SET child.`parent_id` = parent.id, child.`sort` = 1, child.`name` = '编辑首次随访', child.`type` = 2
WHERE child.`code` = 'patientManagement:firstVisit:edit';

INSERT INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`)
SELECT 'patientManagement:followUp:edit', '修改随访记录', 2, parent.id, 1
FROM `permission` parent
WHERE parent.`code` = 'patientManagement:followUp'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'patientManagement:followUp:edit');

UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'patientManagement:followUp'
SET child.`parent_id` = parent.id, child.`sort` = 1, child.`name` = '修改随访记录', child.`type` = 2
WHERE child.`code` = 'patientManagement:followUp:edit';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` parent ON parent.id = rp.permission_id
         CROSS JOIN `permission` p
WHERE (parent.`code` = 'latentManagement:supervision' AND p.`code` = 'latentManagement:supervision:edit')
   OR (parent.`code` = 'patientManagement:firstVisit' AND p.`code` = 'patientManagement:firstVisit:edit')
   OR (parent.`code` = 'patientManagement:followUp' AND p.`code` = 'patientManagement:followUp:edit');

-- ==================== V63：填写 / 修改 拆分为独立按钮权限（见 migration/V63） ====================
INSERT INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`)
SELECT 'latentManagement:supervision:fill', '填写督导表', 2, parent.id, 1
FROM `permission` parent
WHERE parent.`code` = 'latentManagement:supervision'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'latentManagement:supervision:fill');

INSERT INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`)
SELECT 'patientManagement:firstVisit:fill', '填写首次随访', 2, parent.id, 1
FROM `permission` parent
WHERE parent.`code` = 'patientManagement:firstVisit'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'patientManagement:firstVisit:fill');

INSERT INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`)
SELECT 'patientManagement:followUp:fill', '填写后续随访', 2, parent.id, 1
FROM `permission` parent
WHERE parent.`code` = 'patientManagement:followUp'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'patientManagement:followUp:fill');

UPDATE `permission` SET `sort` = 1, `name` = '填写督导表' WHERE `code` = 'latentManagement:supervision:fill';
UPDATE `permission` SET `sort` = 2, `name` = '修改督导表' WHERE `code` = 'latentManagement:supervision:edit';
UPDATE `permission` SET `sort` = 1, `name` = '填写首次随访' WHERE `code` = 'patientManagement:firstVisit:fill';
UPDATE `permission` SET `sort` = 2, `name` = '修改首次随访' WHERE `code` = 'patientManagement:firstVisit:edit';
UPDATE `permission` SET `sort` = 1, `name` = '填写后续随访' WHERE `code` = 'patientManagement:followUp:fill';
UPDATE `permission` SET `sort` = 2, `name` = '修改随访记录' WHERE `code` = 'patientManagement:followUp:edit';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` parent ON parent.id = rp.permission_id
         CROSS JOIN `permission` p
WHERE (parent.`code` = 'latentManagement:supervision'
       AND p.`code` IN ('latentManagement:supervision:fill', 'latentManagement:supervision:edit'))
   OR (parent.`code` = 'patientManagement:firstVisit'
       AND p.`code` IN ('patientManagement:firstVisit:fill', 'patientManagement:firstVisit:edit'))
   OR (parent.`code` = 'patientManagement:followUp'
       AND p.`code` IN ('patientManagement:followUp:fill', 'patientManagement:followUp:edit'));

-- ==================== V64：推介确认/拒绝后同步接收方系统消息（见 migration/V64） ====================
UPDATE `sys_message` sm
    INNER JOIN `referral_tracking` rt ON sm.biz_id = rt.id AND rt.deleted = 0
SET sm.type     = 'referral_tracking_confirmed',
    sm.title    = '推介已接收',
    sm.content  = CONCAT('「', IFNULL(NULLIF(TRIM(rt.name), ''), '（未知姓名）'),
                         '」的推介通知单您已确认接收，已进入追踪环节。'),
    sm.is_read  = 1
WHERE sm.type = 'referral_tracking_receive'
  AND sm.deleted = 0
  AND rt.recommend_status = 2;

UPDATE `sys_message` sm
    INNER JOIN `referral_tracking` rt ON sm.biz_id = rt.id AND rt.deleted = 0
SET sm.type     = 'referral_tracking_rejected',
    sm.title    = '推介已被拒绝',
    sm.content  = CONCAT('「', IFNULL(NULLIF(TRIM(rt.name), ''), '（未知姓名）'),
                         '」的推介通知单您已拒绝，原因：',
                         IFNULL(NULLIF(TRIM(rt.rejected_reason), ''), '（未填写）')),
    sm.is_read  = 1
WHERE sm.type = 'referral_tracking_receive'
  AND sm.deleted = 0
  AND rt.recommend_status = 3;

-- ==================== V65：已确认推介保留在推介模块（见 migration/V65） ====================
UPDATE `referral_tracking`
SET `biz_mode` = 'recommend'
WHERE `recommend_status` = 2
  AND `biz_mode` = 'track'
  AND `recommend_sent_time` IS NOT NULL
  AND `deleted` = 0;

-- ==================== V66：已确认推介消息与 biz_mode 历史数据修复（见 migration/V66） ====================
UPDATE `sys_message` sm
    INNER JOIN `referral_tracking` rt ON sm.biz_id = rt.id AND rt.deleted = 0
SET sm.type     = 'referral_tracking_confirmed',
    sm.title    = '推介已接收',
    sm.content  = CONCAT('「', IFNULL(NULLIF(TRIM(rt.name), ''), '（未知姓名）'),
                         '」的推介通知单您已确认接收，已进入追踪环节。'),
    sm.is_read  = 1
WHERE sm.type = 'referral_tracking_receive'
  AND sm.deleted = 0
  AND rt.recommend_status = 2;

UPDATE `sys_message` sm
    INNER JOIN `referral_tracking` rt ON sm.biz_id = rt.id AND rt.deleted = 0
SET sm.type     = 'referral_tracking_rejected',
    sm.title    = '推介已被拒绝',
    sm.content  = CONCAT('「', IFNULL(NULLIF(TRIM(rt.name), ''), '（未知姓名）'),
                         '」的推介通知单您已拒绝，原因：',
                         IFNULL(NULLIF(TRIM(rt.rejected_reason), ''), '（未填写）')),
    sm.is_read  = 1
WHERE sm.type = 'referral_tracking_receive'
  AND sm.deleted = 0
  AND rt.recommend_status = 3;

UPDATE `referral_tracking`
SET `biz_mode` = 'recommend'
WHERE `recommend_sent_time` IS NOT NULL
  AND `biz_mode` = 'track'
  AND `recommend_status` IN (1, 2)
  AND `deleted` = 0;

-- ==================== V67：通知单填写权限 + 筛查管理权限树归并（见 migration/V67） ====================
INSERT INTO `permission` (`code`, `name`, `type`, `parent_id`, `sort`)
SELECT 'patientManagement:notice:fill', '填写通知单', 2, parent.id, 1
FROM `permission` parent
WHERE parent.`code` = 'patientManagement:notice'
  AND NOT EXISTS (SELECT 1 FROM `permission` x WHERE x.`code` = 'patientManagement:notice:fill');

UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'patientManagement:notice'
SET child.`parent_id` = parent.id, child.`type` = 2, child.`sort` = 1, child.`name` = '填写通知单'
WHERE child.`code` = 'patientManagement:notice:fill';

UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'patientManagement:notice'
SET child.`parent_id` = parent.id, child.`type` = 2, child.`sort` = 2, child.`name` = '删除患者'
WHERE child.`code` = 'patientManagement:delete';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` parent ON parent.id = rp.permission_id
         CROSS JOIN `permission` p
WHERE parent.`code` = 'patientManagement:notice'
  AND p.`code` = 'patientManagement:notice:fill'
  AND rp.role != 6;

DELETE rp FROM `role_permission` rp
         INNER JOIN `permission` p ON p.id = rp.permission_id
WHERE rp.role = 6 AND p.`code` = 'patientManagement:notice:fill';

UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'screening'
SET child.`parent_id` = parent.id, child.`sort` = 1
WHERE child.`code` = 'school';

UPDATE `permission` child
         INNER JOIN `permission` parent ON parent.`code` = 'screening'
SET child.`parent_id` = parent.id, child.`sort` = 2
WHERE child.`code` = 'keyPopulation';

UPDATE `permission` SET `sort` = 3 WHERE `code` = 'regular:screening';
UPDATE `permission` SET `sort` = 4 WHERE `code` = 'regular:suspected';
UPDATE `permission` SET `sort` = 5 WHERE `code` = 'epidemic:screening';

INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT DISTINCT rp.role, p.id
FROM `role_permission` rp
         INNER JOIN `permission` old ON old.id = rp.permission_id
         CROSS JOIN `permission` p
WHERE p.`code` = 'screening'
  AND old.`code` IN (
      'school', 'keyPopulation',
      'school:screening', 'school:suspected',
      'keyPopulation:screening', 'keyPopulation:suspected'
  );

-- ==================== V68：推介追踪共同追踪（见 migration/V68_referral_joint_tracking.sql） ====================
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'joint_tracking'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `joint_tracking` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否共同追踪：0否 1是'' AFTER `recommend_confirm_time`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'joint_tracking_time'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `joint_tracking_time` DATETIME DEFAULT NULL COMMENT ''开启共同追踪时间'' AFTER `joint_tracking`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==================== V70：密接筛查菜单改名（见 migration/V70_close_contact_screening_rename.sql） ====================
UPDATE `permission`
SET `name` = '密接筛查'
WHERE `code` = 'closeContact:screening';

-- ==================== V73：待诊断操作权限补齐（见 migration/V73_latent_suspected_role_permissions.sql） ====================
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.`role`, p.`id`
FROM (
    SELECT 2 AS `role`
    UNION SELECT 3
    UNION SELECT 4
    UNION SELECT 5
    UNION SELECT 6
) r
CROSS JOIN `permission` p
WHERE p.`code` IN ('latent:track', 'latent:xray', 'latent:diagnosis');

-- ==================== V74：学生筛查痰涂片/分子生物学字段（见 migration/V74_screening_school_pathogen_results.sql） ====================
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_school' AND COLUMN_NAME = 'sputum_smear_result'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `screening_school`
        ADD COLUMN `sputum_smear_result` VARCHAR(64) DEFAULT NULL COMMENT ''痰涂片结果'' AFTER `chest_xray_result`,
        ADD COLUMN `molecular_biology_result` VARCHAR(64) DEFAULT NULL COMMENT ''分子生物学结果'' AFTER `sputum_smear_result`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `latent_infection`
SET `diagnosis_result` = NULL,
    `referral_result` = NULL,
    `archived` = 0,
    `archived_time` = NULL
WHERE `diagnosis_result` = '疑似肺结核';

-- ==================== V75：一至四级用户默认可访问部门管理（见 migration/V75_system_department_roles.sql） ====================
-- 一至四级（role=2~5）获得部门管理菜单权限（父菜单 anyPermission 已含 system:department，无需额外授予 system）
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id
FROM (SELECT 2 AS role UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) r
CROSS JOIN `permission` p
WHERE p.code = 'system:department';

-- ==================== V76：一至五级用户部门管理权限补全（见 migration/V76_system_department_all_levels.sql） ====================
INSERT IGNORE INTO `role_permission` (`role`, `permission_id`)
SELECT r.role, p.id
FROM (SELECT 2 AS role UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) r
CROSS JOIN `permission` p
WHERE p.code = 'system:department';

-- ==================== V80：督导表增加治疗完成情况（见 migration/V80_supervision_treatment_completion_status.sql） ====================
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'supervision_form' AND COLUMN_NAME = 'treatment_completion_status'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `supervision_form` ADD COLUMN `treatment_completion_status` VARCHAR(32) DEFAULT NULL COMMENT ''治疗完成情况：完成治疗/失败/死亡/失访/不良反应停药/未评估'' AFTER `supervision_records`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==================== V81：潜伏感染者人群分类（见 migration/V81_latent_crowd_category.sql） ====================
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'latent_infection' AND COLUMN_NAME = 'crowd_category'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `latent_infection` ADD COLUMN `crowd_category` VARCHAR(128) DEFAULT NULL COMMENT ''人群分类（重点人群：老年人/糖尿病/双感；密接：家庭内/家庭外）'' AFTER `population_type`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==================== V83：真实到位/转诊时间（见 migration/V83_actual_arrival_referral_date.sql） ====================
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'actual_arrival_date'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `actual_arrival_date` DATE DEFAULT NULL COMMENT ''真实到位时间（手动录入）'' AFTER `arrival_time`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'latent_infection' AND COLUMN_NAME = 'actual_arrival_date'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `latent_infection` ADD COLUMN `actual_arrival_date` DATE DEFAULT NULL COMMENT ''真实到位时间（手动录入）'' AFTER `tracking_history_json`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'latent_infection' AND COLUMN_NAME = 'actual_referral_date'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `latent_infection` ADD COLUMN `actual_referral_date` DATE DEFAULT NULL COMMENT ''真实转诊时间（手动录入）'' AFTER `referral_remark`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral' AND COLUMN_NAME = 'actual_referral_date'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral` ADD COLUMN `actual_referral_date` DATE DEFAULT NULL COMMENT ''真实转诊时间（手动录入）'' AFTER `confirmed_time`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- V87：导入行号（按原 Excel 顺序展示）+ 筛查表录入用户

-- ---------- screening_school ----------
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_school' AND COLUMN_NAME = 'import_row_no'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `screening_school` ADD COLUMN `import_row_no` INT DEFAULT NULL COMMENT ''Excel导入行号（与模板行号一致，用于列表排序）'' AFTER `upload_batch`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_school' AND COLUMN_NAME = 'creator_id'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `screening_school` ADD COLUMN `creator_id` BIGINT DEFAULT NULL COMMENT ''录入人用户ID'' AFTER `department_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_school' AND COLUMN_NAME = 'creator_username'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `screening_school` ADD COLUMN `creator_username` VARCHAR(64) DEFAULT NULL COMMENT ''录入用户名'' AFTER `creator_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- screening_key_population ----------
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_key_population' AND COLUMN_NAME = 'creator_id'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `screening_key_population` ADD COLUMN `creator_id` BIGINT DEFAULT NULL COMMENT ''录入人用户ID'' AFTER `department_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_key_population' AND COLUMN_NAME = 'creator_username'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `screening_key_population` ADD COLUMN `creator_username` VARCHAR(64) DEFAULT NULL COMMENT ''录入用户名'' AFTER `creator_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- screening_close_contact ----------
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_close_contact' AND COLUMN_NAME = 'import_row_no'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `screening_close_contact` ADD COLUMN `import_row_no` INT DEFAULT NULL COMMENT ''Excel导入行号（与模板行号一致，用于列表排序）'' AFTER `upload_batch`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_close_contact' AND COLUMN_NAME = 'creator_id'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `screening_close_contact` ADD COLUMN `creator_id` BIGINT DEFAULT NULL COMMENT ''录入人用户ID'' AFTER `department_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_close_contact' AND COLUMN_NAME = 'creator_username'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `screening_close_contact` ADD COLUMN `creator_username` VARCHAR(64) DEFAULT NULL COMMENT ''录入用户名'' AFTER `creator_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- close_contact_case ----------
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'close_contact_case' AND COLUMN_NAME = 'import_row_no'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `close_contact_case` ADD COLUMN `import_row_no` INT DEFAULT NULL COMMENT ''Excel导入行号（与模板行号一致，用于列表排序）'' AFTER `upload_batch`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- referral_tracking ----------
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_tracking' AND COLUMN_NAME = 'import_row_no'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `referral_tracking` ADD COLUMN `import_row_no` INT DEFAULT NULL COMMENT ''Excel导入行号（与模板行号一致，用于列表排序）'' AFTER `upload_batch`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- patient ----------
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'patient' AND COLUMN_NAME = 'import_row_no'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `patient` ADD COLUMN `import_row_no` INT DEFAULT NULL COMMENT ''Excel导入行号（与模板行号一致，用于列表排序）'' AFTER `creator_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- latent_infection ----------
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'latent_infection' AND COLUMN_NAME = 'import_row_no'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `latent_infection` ADD COLUMN `import_row_no` INT DEFAULT NULL COMMENT ''Excel导入行号（与模板行号一致，用于列表排序）'' AFTER `creator_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- V87 appended
