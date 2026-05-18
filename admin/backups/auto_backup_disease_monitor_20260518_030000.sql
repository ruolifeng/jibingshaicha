-- MySQL dump 10.13  Distrib 9.5.0, for macos26.2 (arm64)
--
-- Host: 127.0.0.1    Database: disease_monitor
-- ------------------------------------------------------
-- Server version	9.5.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ '353975fe-a03b-11ef-8a59-018bb6ee87d7:1-86475';

--
-- Table structure for table `department`
--

DROP TABLE IF EXISTS `department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `department` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(128) NOT NULL COMMENT '部门名称',
  `description` varchar(256) DEFAULT NULL COMMENT '部门描述',
  `parent_id` bigint DEFAULT NULL COMMENT '上级部门ID，NULL 表示市级顶级',
  `level` tinyint NOT NULL DEFAULT '1' COMMENT '1市级 2区县 3社区',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `department`
--

LOCK TABLES `department` WRITE;
/*!40000 ALTER TABLE `department` DISABLE KEYS */;
/*!40000 ALTER TABLE `department` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `epidemic_report`
--

DROP TABLE IF EXISTS `epidemic_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `epidemic_report` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `population_type` varchar(32) NOT NULL COMMENT '人群类型',
  `patient_id` bigint DEFAULT NULL COMMENT '匹配到的患者ID',
  `raw_data` json NOT NULL COMMENT '原始导入数据（JSON）',
  `matched` tinyint NOT NULL DEFAULT '0' COMMENT '是否已匹配',
  `upload_batch` varchar(64) DEFAULT NULL COMMENT '上传批次号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_patient` (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='大疫情导入数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `epidemic_report`
--

LOCK TABLES `epidemic_report` WRITE;
/*!40000 ALTER TABLE `epidemic_report` DISABLE KEYS */;
/*!40000 ALTER TABLE `epidemic_report` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `first_visit`
--

DROP TABLE IF EXISTS `first_visit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `first_visit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `patient_id` bigint NOT NULL COMMENT '关联患者ID',
  `population_type` varchar(32) NOT NULL COMMENT '人群类型',
  `visit_date` date DEFAULT NULL COMMENT '随访时间',
  `visit_method` varchar(16) DEFAULT NULL COMMENT '随访方式：门诊/家庭',
  `patient_type` varchar(16) DEFAULT NULL COMMENT '患者类型：初治/复治',
  `sputum_status` varchar(16) DEFAULT NULL COMMENT '痰菌情况：阳性/阴性/未查痰',
  `drug_resistance` varchar(16) DEFAULT NULL COMMENT '耐药情况：耐药/非耐药/未检测',
  `symptoms` varchar(256) DEFAULT NULL COMMENT '症状及体征（多选，逗号分隔编号）',
  `other_symptoms` varchar(256) DEFAULT NULL COMMENT '其他症状',
  `chemotherapy` varchar(256) DEFAULT NULL COMMENT '化疗方案',
  `medication_usage` varchar(16) DEFAULT NULL COMMENT '用法：每日/间歇',
  `drug_form` varchar(64) DEFAULT NULL COMMENT '药品剂型',
  `supervisor` varchar(32) DEFAULT NULL COMMENT '督导人员：医生/家属/自服药/其他',
  `separate_room` varchar(8) DEFAULT NULL COMMENT '单独的居室：有/无',
  `ventilation` varchar(8) DEFAULT NULL COMMENT '通风情况：良好/一般/差',
  `smoking_amount` varchar(32) DEFAULT NULL COMMENT '吸烟量（支/天）',
  `drinking_amount` varchar(32) DEFAULT NULL COMMENT '饮酒量（两/天）',
  `medication_location` varchar(256) DEFAULT NULL COMMENT '取药地点',
  `medication_pick_time` varchar(64) DEFAULT NULL COMMENT '取药时间',
  `education_items` json DEFAULT NULL COMMENT '健康教育及培训各项掌握情况',
  `next_visit_date` date DEFAULT NULL COMMENT '下次随访时间',
  `doctor_signature` varchar(64) DEFAULT NULL COMMENT '评估医生签名',
  `filled_by` bigint DEFAULT NULL COMMENT '填写人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_patient` (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='首次入户随访记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `first_visit`
--

LOCK TABLES `first_visit` WRITE;
/*!40000 ALTER TABLE `first_visit` DISABLE KEYS */;
/*!40000 ALTER TABLE `first_visit` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `follow_up_visit`
--

DROP TABLE IF EXISTS `follow_up_visit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `follow_up_visit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `patient_id` bigint NOT NULL COMMENT '关联患者ID',
  `population_type` varchar(32) NOT NULL COMMENT '人群类型',
  `visit_seq` int DEFAULT NULL COMMENT '随访次数（第几次）',
  `visit_date` date DEFAULT NULL COMMENT '随访时间',
  `visit_method` varchar(16) DEFAULT NULL COMMENT '随访方式：门诊/家庭',
  `visit_situation` text COMMENT '随访情况',
  `remarks` text COMMENT '备注',
  `attachment_url` varchar(512) DEFAULT NULL COMMENT '附件图片URL',
  `filled_by` bigint DEFAULT NULL COMMENT '填写人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_patient` (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='后续随访记录表（患者随访汇总表）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `follow_up_visit`
--

LOCK TABLES `follow_up_visit` WRITE;
/*!40000 ALTER TABLE `follow_up_visit` DISABLE KEYS */;
/*!40000 ALTER TABLE `follow_up_visit` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `latent_check`
--

DROP TABLE IF EXISTS `latent_check`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `latent_check` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `latent_infection_id` bigint NOT NULL COMMENT '关联潜伏感染ID',
  `check_date` date NOT NULL COMMENT '检查日期',
  `check_period` varchar(32) NOT NULL COMMENT '检查周期：3个月/6个月/12个月',
  `check_result` varchar(128) NOT NULL COMMENT '检查结果：未发病/发病/其他',
  `content` text COMMENT '检查详情',
  `operator` varchar(64) DEFAULT NULL COMMENT '操作人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_latent` (`latent_infection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='潜伏感染者按期检查记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `latent_check`
--

LOCK TABLES `latent_check` WRITE;
/*!40000 ALTER TABLE `latent_check` DISABLE KEYS */;
/*!40000 ALTER TABLE `latent_check` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `latent_follow_up`
--

DROP TABLE IF EXISTS `latent_follow_up`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `latent_follow_up` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `latent_infection_id` bigint NOT NULL COMMENT '关联潜伏感染ID',
  `follow_up_date` date NOT NULL COMMENT '随访日期',
  `follow_up_type` varchar(32) NOT NULL DEFAULT '电话随访' COMMENT '随访方式',
  `content` text COMMENT '随访内容',
  `result` varchar(256) DEFAULT NULL COMMENT '随访结果',
  `operator` varchar(64) DEFAULT NULL COMMENT '操作人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_latent` (`latent_infection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='潜伏感染者电话随访记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `latent_follow_up`
--

LOCK TABLES `latent_follow_up` WRITE;
/*!40000 ALTER TABLE `latent_follow_up` DISABLE KEYS */;
/*!40000 ALTER TABLE `latent_follow_up` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `latent_infection`
--

DROP TABLE IF EXISTS `latent_infection`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `latent_infection` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `screening_id` bigint NOT NULL COMMENT '关联筛查数据ID',
  `population_type` varchar(32) NOT NULL COMMENT '人群类型：school/keyPopulation/closeContact',
  `name` varchar(64) DEFAULT NULL COMMENT '姓名',
  `id_number` varchar(64) DEFAULT NULL COMMENT '证件号',
  `gender` varchar(10) DEFAULT NULL COMMENT '性别',
  `age` int DEFAULT NULL COMMENT '年龄',
  `phone` varchar(32) DEFAULT NULL COMMENT '联系电话',
  `infection_result` varchar(128) DEFAULT NULL COMMENT '感染筛查结果',
  `tracking_status` tinyint NOT NULL DEFAULT '0' COMMENT '追踪状态：0待追踪 1到位 2未到位 3其他 4强制结束',
  `not_in_place_count` int NOT NULL DEFAULT '0' COMMENT '未到位次数',
  `tracking_remark` text COMMENT '追踪备注原因',
  `has_chest_xray` varchar(10) DEFAULT NULL COMMENT '是否进行胸片检查（是/否）',
  `chest_xray_date` date DEFAULT NULL COMMENT '胸片检查日期',
  `chest_xray_result` varchar(128) DEFAULT NULL COMMENT '胸片检查结果：正常/异常/未查',
  `diagnosis_first` varchar(64) DEFAULT NULL COMMENT '首次诊断：排除/疑似肺结核/潜伏感染者/确诊患者/其他',
  `active_round` tinyint DEFAULT NULL COMMENT '密接阳性轮次：1首次 2半年后 3一年后',
  `referral_result` varchar(32) DEFAULT NULL COMMENT '转诊结果：excluded/other/confirmed/suspected/latent',
  `referral_remark` text COMMENT '转诊备注',
  `diagnosis_result` varchar(64) DEFAULT NULL COMMENT '诊断结果展示列值',
  `treatment_phase` tinyint NOT NULL DEFAULT '0' COMMENT '治疗阶段：0未开始 1预防治疗中 2已结案',
  `medication_status` tinyint DEFAULT NULL COMMENT '服药状态：1按要求服药 2不服药',
  `archived` tinyint NOT NULL DEFAULT '0' COMMENT '是否已归档：0否 1是',
  `archived_time` datetime DEFAULT NULL COMMENT '结案归档时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `department_id` bigint DEFAULT NULL COMMENT '所属部门ID',
  PRIMARY KEY (`id`),
  KEY `idx_screening` (`screening_id`),
  KEY `idx_population` (`population_type`),
  KEY `idx_tracking` (`tracking_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='潜伏感染管理表（V4）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `latent_infection`
--

LOCK TABLES `latent_infection` WRITE;
/*!40000 ALTER TABLE `latent_infection` DISABLE KEYS */;
/*!40000 ALTER TABLE `latent_infection` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `medication_management`
--

DROP TABLE IF EXISTS `medication_management`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `medication_management` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `patient_id` bigint NOT NULL COMMENT '关联患者ID',
  `population_type` varchar(32) NOT NULL COMMENT '人群类型',
  `management_method` varchar(32) DEFAULT NULL COMMENT '管理方式',
  `supervisor` varchar(32) DEFAULT NULL COMMENT '督导人员',
  `sputum_result` varchar(32) DEFAULT NULL COMMENT '治疗前痰菌检查结果',
  `medication_records` json DEFAULT NULL COMMENT '每日服药记录（JSON：{日期:是否服药}）',
  `stop_date` date DEFAULT NULL COMMENT '停止完成时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_patient` (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服药管理表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `medication_management`
--

LOCK TABLES `medication_management` WRITE;
/*!40000 ALTER TABLE `medication_management` DISABLE KEYS */;
/*!40000 ALTER TABLE `medication_management` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notice`
--

DROP TABLE IF EXISTS `notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notice` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `notice_type` varchar(16) NOT NULL COMMENT '通知单类型：latent=潜伏者通知单 patient=患者通知单',
  `population_type` varchar(32) NOT NULL COMMENT '人群类型',
  `biz_id` bigint NOT NULL COMMENT '关联业务ID（latent_infection.id 或 patient.id）',
  `patient_name` varchar(64) DEFAULT NULL COMMENT '患者/潜伏者姓名',
  `id_number` varchar(64) DEFAULT NULL COMMENT '身份证',
  `gender` varchar(10) DEFAULT NULL COMMENT '性别',
  `birth_date` date DEFAULT NULL COMMENT '出生日期',
  `age` int DEFAULT NULL COMMENT '年龄',
  `phone` varchar(32) DEFAULT NULL COMMENT '联系方式',
  `crowd_category` varchar(128) DEFAULT NULL COMMENT '人群分类',
  `ethnicity` varchar(32) DEFAULT NULL COMMENT '民族',
  `current_address` varchar(256) DEFAULT NULL COMMENT '现居住地址',
  `household_address` varchar(256) DEFAULT NULL COMMENT '户籍地址',
  `chest_xray_date` date DEFAULT NULL COMMENT '胸片检查时间',
  `chest_xray_result` varchar(32) DEFAULT NULL COMMENT '胸片检查结果：正常/异常/未查',
  `treatment_institution` varchar(256) DEFAULT NULL COMMENT '治疗机构',
  `issued_time` date DEFAULT NULL COMMENT '下发时间',
  `infection_date` date DEFAULT NULL COMMENT '感染检测时间',
  `infection_method` varchar(64) DEFAULT NULL COMMENT '感染检查方法：PPD/EC/IGRA',
  `infection_result_value` varchar(128) DEFAULT NULL COMMENT '感染检查结果',
  `latent_treatment_option` varchar(64) DEFAULT NULL COMMENT '废弃字段，治疗方案已统一使用 treatment_plan',
  `patient_type` varchar(32) DEFAULT NULL COMMENT '患者类型：初治/复治',
  `management_method` varchar(64) DEFAULT NULL COMMENT '管理方式：全程督导/强化督导/全程管理/未管理',
  `treatment_plan` varchar(256) DEFAULT NULL COMMENT '治疗方案（患者，FDC等7个方案）',
  `custom_plan_detail` text COMMENT '个体化方案详情',
  `sputum_smear` varchar(32) DEFAULT NULL COMMENT '痰涂片：未出结果/阴性/阳性/未做/未知',
  `sputum_culture` varchar(32) DEFAULT NULL COMMENT '痰培养',
  `molecular_test` varchar(32) DEFAULT NULL COMMENT '分子检查',
  `pathology_test` varchar(32) DEFAULT NULL COMMENT '病理学检查',
  `other_notes` text COMMENT '其他注意事项',
  `sender_id` bigint NOT NULL COMMENT '发送人ID（4级）',
  `receiver_org_id` bigint DEFAULT NULL COMMENT '接收单位ID（5级）',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1已发送 2已确认',
  `sent_time` datetime DEFAULT NULL COMMENT '发送时间',
  `confirmed_time` datetime DEFAULT NULL COMMENT '确认接收时间',
  `timeout_notified` tinyint NOT NULL DEFAULT '0' COMMENT '是否已发送通知单48h超时提醒',
  `supervision_timeout_notified` tinyint NOT NULL DEFAULT '0' COMMENT '是否已发送督导表72h超时提醒',
  `visit_timeout_notified` tinyint NOT NULL DEFAULT '0' COMMENT '是否已发送首次随访72h超时提醒',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_biz` (`biz_id`,`notice_type`),
  KEY `idx_status` (`status`,`sent_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notice`
--

LOCK TABLES `notice` WRITE;
/*!40000 ALTER TABLE `notice` DISABLE KEYS */;
/*!40000 ALTER TABLE `notice` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `patient`
--

DROP TABLE IF EXISTS `patient`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `patient` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `screening_id` bigint DEFAULT NULL COMMENT '关联筛查数据ID',
  `latent_infection_id` bigint DEFAULT NULL COMMENT '关联潜伏感染ID（确诊来源）',
  `population_type` varchar(32) NOT NULL COMMENT '人群类型',
  `name` varchar(64) DEFAULT NULL COMMENT '姓名',
  `gender` varchar(10) DEFAULT NULL COMMENT '性别',
  `birth_date` date DEFAULT NULL COMMENT '出生日期',
  `age` int DEFAULT NULL COMMENT '年龄',
  `id_type` varchar(32) DEFAULT NULL COMMENT '证件类型',
  `id_number` varchar(64) DEFAULT NULL COMMENT '证件号',
  `ethnicity` varchar(32) DEFAULT NULL COMMENT '民族',
  `phone` varchar(32) DEFAULT NULL COMMENT '联系电话',
  `household_address` varchar(256) DEFAULT NULL COMMENT '户籍所在地',
  `current_address` varchar(256) DEFAULT NULL COMMENT '现住址',
  `diagnosis_result` varchar(128) DEFAULT NULL COMMENT '诊断结果',
  `source` varchar(32) NOT NULL DEFAULT 'confirmed' COMMENT '来源：confirmed=转诊确诊 epidemic=大疫情导入',
  `archived` tinyint NOT NULL DEFAULT '0' COMMENT '是否已归档（历史患者）',
  `archived_time` datetime DEFAULT NULL COMMENT '归档时间',
  `epidemic_data` json DEFAULT NULL COMMENT '大疫情表额外字段（JSON）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `department_id` bigint DEFAULT NULL COMMENT '所属部门ID',
  PRIMARY KEY (`id`),
  KEY `idx_id_number` (`id_number`),
  KEY `idx_population` (`population_type`,`archived`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='患者管理表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `patient`
--

LOCK TABLES `patient` WRITE;
/*!40000 ALTER TABLE `patient` DISABLE KEYS */;
/*!40000 ALTER TABLE `patient` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `permission`
--

DROP TABLE IF EXISTS `permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(128) NOT NULL COMMENT '权限编码',
  `name` varchar(128) NOT NULL COMMENT '权限名称',
  `type` tinyint NOT NULL COMMENT '类型：1=菜单 2=按钮/操作',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父权限ID，0为顶级',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=336 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `permission`
--

LOCK TABLES `permission` WRITE;
/*!40000 ALTER TABLE `permission` DISABLE KEYS */;
INSERT INTO `permission` VALUES (1,'school','学校人群',1,0,1),(2,'keyPopulation','重点人群',1,0,2),(3,'closeContact','密接人群',1,0,3),(4,'statistics','统计分析',1,0,4),(5,'message','系统消息',1,0,5),(6,'system','系统管理',1,0,6),(7,'dataClean','数据清洗',1,0,7),(10,'school:screening','筛查管理',1,1,1),(11,'school:latent','潜伏感染',1,1,3),(12,'school:patient','患者管理',1,1,4),(13,'school:history','历史患者',1,1,5),(14,'school:suspected','待诊断',1,1,2),(15,'school:questionnaire','学生问卷',1,1,6),(20,'keyPopulation:screening','筛查管理',1,2,1),(21,'keyPopulation:latent','潜伏感染',1,2,3),(22,'keyPopulation:patient','患者管理',1,2,4),(23,'keyPopulation:history','历史患者',1,2,5),(24,'keyPopulation:suspected','待诊断',1,2,2),(30,'closeContact:screening','筛查管理',1,3,1),(31,'closeContact:latent','潜伏感染',1,3,3),(32,'closeContact:patient','患者管理',1,3,4),(33,'closeContact:history','历史患者',1,3,5),(34,'closeContact:followUp','监测随访',1,3,6),(60,'system:users','用户管理',1,6,1),(61,'system:permissions','权限管理',1,6,2),(62,'system:department','部门管理',1,6,3),(100,'screening:upload','上传筛查数据',2,10,1),(101,'screening:create','新增筛查数据',2,10,2),(102,'screening:export','导出筛查数据',2,10,3),(103,'screening:edit','编辑筛查数据',2,10,4),(104,'screening:delete','删除筛查数据',2,10,5),(110,'latent:track','追踪',2,11,1),(111,'latent:referral','转诊',2,11,2),(112,'latent:sendNotice','发送潜伏者通知单',2,11,3),(113,'latent:confirmNotice','确认接收通知单',2,11,4),(114,'latent:supervision','填写督导表',2,11,5),(115,'latent:followUp','潜伏电话随访',2,11,6),(116,'latent:check','潜伏按期检查',2,11,7),(117,'latent:closeCase','潜伏结案归档',2,11,8),(118,'latent:xray','录入胸片诊断',2,11,9),(119,'system:backup','数据备份',2,6,10),(120,'patient:importEpidemic','导入大疫情表',2,12,1),(121,'patient:sendNotice','发送患者通知单',2,12,2),(122,'patient:confirmNotice','确认接收患者通知单',2,12,3),(123,'patient:firstVisit','首次随访',2,12,4),(124,'patient:followUp','后续随访',2,12,5),(125,'patient:medication','服药管理',2,12,6),(130,'statistics:export','导出统计',2,4,1),(140,'user:create','创建用户',2,60,1),(141,'user:edit','编辑用户',2,60,2),(142,'user:delete','删除用户',2,60,3),(143,'permission:assign','分配权限',2,61,1),(210,'keyPopulation:screening:upload','上传筛查数据',2,20,1),(211,'keyPopulation:screening:create','新增筛查数据',2,20,2),(212,'keyPopulation:screening:export','导出筛查数据',2,20,3),(213,'keyPopulation:screening:edit','编辑筛查数据',2,20,4),(214,'keyPopulation:screening:delete','删除筛查数据',2,20,5),(220,'keyPopulation:latent:sendNotice','发送潜伏者通知单',2,21,4),(221,'keyPopulation:latent:confirmNotice','确认接收通知单',2,21,5),(222,'keyPopulation:latent:supervision','填写督导表',2,21,6),(223,'keyPopulation:latent:followUp','潜伏电话随访',2,21,7),(224,'keyPopulation:latent:check','潜伏按期检查',2,21,8),(225,'keyPopulation:latent:closeCase','潜伏结案归档',2,21,9),(226,'keyPopulation:latent:track','追踪',2,21,1),(227,'keyPopulation:latent:xray','录入胸片诊断',2,21,2),(228,'keyPopulation:latent:referral','转诊',2,21,3),(230,'keyPopulation:patient:importEpidemic','导入大疫情表',2,22,1),(231,'keyPopulation:patient:sendNotice','发送患者通知单',2,22,2),(232,'keyPopulation:patient:confirmNotice','确认接收患者通知单',2,22,3),(233,'keyPopulation:patient:firstVisit','首次随访',2,22,4),(234,'keyPopulation:patient:followUp','后续随访',2,22,5),(235,'keyPopulation:patient:medication','服药管理',2,22,6),(310,'closeContact:screening:upload','上传筛查数据',2,30,1),(311,'closeContact:screening:create','新增筛查数据',2,30,2),(312,'closeContact:screening:export','导出筛查数据',2,30,3),(313,'closeContact:screening:edit','编辑筛查数据',2,30,4),(314,'closeContact:screening:delete','删除筛查数据',2,30,5),(315,'referral','分级诊疗',2,5,50),(320,'closeContact:latent:treatmentDecision','确认预防治疗',2,31,1),(321,'closeContact:latent:sendNotice','发送通知单',2,31,2),(322,'closeContact:latent:confirmNotice','确认接收通知单',2,31,3),(323,'closeContact:latent:supervision','填写督导表',2,31,4),(324,'closeContact:latent:setExpectedDate','设置预计完成时间',2,31,5),(325,'closeContact:latent:confirmTreatment','确认治疗完成',2,31,6),(326,'closeContact:latent:check','录入随访复查',2,31,7),(330,'closeContact:patient:importEpidemic','导入大疫情表',2,32,1),(331,'closeContact:patient:sendNotice','发送患者通知单',2,32,2),(332,'closeContact:patient:confirmNotice','确认接收患者通知单',2,32,3),(333,'closeContact:patient:firstVisit','首次随访',2,32,4),(334,'closeContact:patient:followUp','后续随访',2,32,5),(335,'closeContact:patient:medication','服药管理',2,32,6);
/*!40000 ALTER TABLE `permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `referral`
--

DROP TABLE IF EXISTS `referral`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `referral` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `biz_id` bigint NOT NULL COMMENT '关联业务记录ID',
  `biz_type` varchar(64) NOT NULL COMMENT '业务类型：screening_school/screening_key/screening_close/suspected_school/suspected_key/suspected_close/latent_school/latent_key/latent_close/patient_school/patient_key/patient_close',
  `population_type` varchar(32) NOT NULL COMMENT '人群类型：school/key/close',
  `module_type` varchar(32) NOT NULL COMMENT '模块类型：screening/suspected/latent/patient',
  `subject_name` varchar(64) DEFAULT NULL COMMENT '对象姓名（用于展示）',
  `summary` text COMMENT '推送的业务摘要（JSON格式）',
  `sender_id` bigint NOT NULL COMMENT '发送方用户ID',
  `receiver_org_id` bigint DEFAULT NULL COMMENT '接收方用户/部门ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1=待确认 2=已接收 3=已拒绝',
  `sent_time` datetime DEFAULT NULL COMMENT '发送时间',
  `confirmed_time` datetime DEFAULT NULL COMMENT '接收时间',
  `rejected_time` datetime DEFAULT NULL COMMENT '拒绝时间',
  `reject_reason` varchar(256) DEFAULT NULL COMMENT '拒绝原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_biz` (`biz_id`,`biz_type`),
  KEY `idx_sender` (`sender_id`),
  KEY `idx_receiver` (`receiver_org_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分级诊疗推送记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `referral`
--

LOCK TABLES `referral` WRITE;
/*!40000 ALTER TABLE `referral` DISABLE KEYS */;
/*!40000 ALTER TABLE `referral` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role_permission`
--

DROP TABLE IF EXISTS `role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role` tinyint NOT NULL COMMENT '角色编号：1-6',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_perm` (`role`,`permission_id`)
) ENGINE=InnoDB AUTO_INCREMENT=633 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role_permission`
--

LOCK TABLES `role_permission` WRITE;
/*!40000 ALTER TABLE `role_permission` DISABLE KEYS */;
INSERT INTO `role_permission` VALUES (34,1,1),(11,1,2),(1,1,3),(44,1,4),(26,1,5),(46,1,6),(620,1,7),(38,1,10),(36,1,11),(37,1,12),(35,1,13),(611,1,14),(621,1,15),(15,1,20),(13,1,21),(14,1,22),(12,1,23),(610,1,24),(5,1,30),(3,1,31),(4,1,32),(2,1,33),(609,1,34),(48,1,60),(47,1,61),(377,1,62),(43,1,100),(39,1,101),(42,1,102),(41,1,103),(40,1,104),(25,1,110),(22,1,111),(23,1,112),(21,1,113),(24,1,114),(331,1,115),(333,1,116),(332,1,117),(330,1,118),(361,1,119),(30,1,120),(32,1,121),(27,1,122),(28,1,123),(29,1,124),(31,1,125),(45,1,130),(49,1,140),(51,1,141),(50,1,142),(33,1,143),(20,1,210),(16,1,211),(19,1,212),(18,1,213),(17,1,214),(457,1,220),(449,1,221),(461,1,222),(453,1,223),(441,1,224),(445,1,225),(592,1,226),(593,1,227),(591,1,228),(477,1,230),(485,1,231),(465,1,232),(469,1,233),(473,1,234),(481,1,235),(10,1,310),(6,1,311),(9,1,312),(8,1,313),(7,1,314),(590,1,315),(413,1,320),(401,1,321),(393,1,322),(409,1,323),(405,1,324),(397,1,325),(389,1,326),(429,1,330),(437,1,331),(417,1,332),(421,1,333),(425,1,334),(433,1,335),(96,2,1),(74,2,2),(64,2,3),(106,2,4),(89,2,5),(613,2,6),(623,2,7),(100,2,10),(98,2,11),(99,2,12),(97,2,13),(78,2,20),(76,2,21),(77,2,22),(75,2,23),(68,2,30),(66,2,31),(67,2,32),(65,2,33),(612,2,61),(105,2,100),(101,2,101),(104,2,102),(103,2,103),(102,2,104),(88,2,110),(85,2,111),(86,2,112),(84,2,113),(87,2,114),(335,2,115),(337,2,116),(336,2,117),(334,2,118),(93,2,120),(95,2,121),(90,2,122),(91,2,123),(92,2,124),(94,2,125),(107,2,130),(614,2,143),(83,2,210),(79,2,211),(82,2,212),(81,2,213),(80,2,214),(456,2,220),(448,2,221),(460,2,222),(452,2,223),(440,2,224),(444,2,225),(595,2,226),(594,2,227),(596,2,228),(476,2,230),(484,2,231),(464,2,232),(468,2,233),(472,2,234),(480,2,235),(73,2,310),(69,2,311),(72,2,312),(71,2,313),(70,2,314),(379,2,315),(412,2,320),(400,2,321),(392,2,322),(408,2,323),(404,2,324),(396,2,325),(388,2,326),(428,2,330),(436,2,331),(416,2,332),(420,2,333),(424,2,334),(432,2,335),(159,3,1),(137,3,2),(127,3,3),(169,3,4),(152,3,5),(616,3,6),(624,3,7),(163,3,10),(161,3,11),(162,3,12),(160,3,13),(141,3,20),(139,3,21),(140,3,22),(138,3,23),(131,3,30),(129,3,31),(130,3,32),(128,3,33),(615,3,61),(168,3,100),(164,3,101),(167,3,102),(166,3,103),(165,3,104),(151,3,110),(148,3,111),(149,3,112),(147,3,113),(150,3,114),(339,3,115),(341,3,116),(340,3,117),(338,3,118),(156,3,120),(158,3,121),(153,3,122),(154,3,123),(155,3,124),(157,3,125),(170,3,130),(617,3,143),(146,3,210),(142,3,211),(145,3,212),(144,3,213),(143,3,214),(455,3,220),(447,3,221),(459,3,222),(451,3,223),(439,3,224),(443,3,225),(598,3,226),(597,3,227),(599,3,228),(475,3,230),(483,3,231),(463,3,232),(467,3,233),(471,3,234),(479,3,235),(136,3,310),(132,3,311),(135,3,312),(134,3,313),(133,3,314),(380,3,315),(411,3,320),(399,3,321),(391,3,322),(407,3,323),(403,3,324),(395,3,325),(387,3,326),(427,3,330),(435,3,331),(415,3,332),(419,3,333),(423,3,334),(431,3,335),(222,4,1),(200,4,2),(190,4,3),(232,4,4),(215,4,5),(625,4,7),(226,4,10),(224,4,11),(225,4,12),(223,4,13),(630,4,15),(204,4,20),(202,4,21),(203,4,22),(201,4,23),(194,4,30),(192,4,31),(193,4,32),(191,4,33),(231,4,100),(227,4,101),(230,4,102),(229,4,103),(228,4,104),(214,4,110),(211,4,111),(212,4,112),(210,4,113),(213,4,114),(343,4,115),(345,4,116),(344,4,117),(342,4,118),(219,4,120),(221,4,121),(216,4,122),(217,4,123),(218,4,124),(220,4,125),(209,4,210),(205,4,211),(208,4,212),(207,4,213),(206,4,214),(454,4,220),(446,4,221),(458,4,222),(450,4,223),(438,4,224),(442,4,225),(601,4,226),(600,4,227),(602,4,228),(474,4,230),(482,4,231),(462,4,232),(466,4,233),(470,4,234),(478,4,235),(199,4,310),(195,4,311),(198,4,312),(197,4,313),(196,4,314),(381,4,315),(410,4,320),(398,4,321),(390,4,322),(406,4,323),(402,4,324),(394,4,325),(386,4,326),(426,4,330),(434,4,331),(414,4,332),(418,4,333),(422,4,334),(430,4,335),(283,5,1),(263,5,2),(253,5,3),(293,5,4),(277,5,5),(626,5,7),(287,5,10),(285,5,11),(286,5,12),(284,5,13),(631,5,15),(267,5,20),(265,5,21),(266,5,22),(264,5,23),(257,5,30),(255,5,31),(256,5,32),(254,5,33),(292,5,100),(288,5,101),(291,5,102),(290,5,103),(289,5,104),(276,5,110),(273,5,111),(274,5,112),(275,5,114),(323,5,115),(324,5,116),(325,5,117),(326,5,118),(280,5,120),(282,5,121),(278,5,123),(279,5,124),(281,5,125),(272,5,210),(268,5,211),(271,5,212),(270,5,213),(269,5,214),(526,5,220),(527,5,222),(525,5,223),(523,5,224),(524,5,225),(604,5,226),(603,5,227),(605,5,228),(530,5,230),(532,5,231),(528,5,233),(529,5,234),(531,5,235),(262,5,310),(258,5,311),(261,5,312),(260,5,313),(259,5,314),(382,5,315),(517,5,320),(514,5,321),(516,5,323),(515,5,324),(513,5,326),(520,5,330),(522,5,331),(518,5,333),(519,5,334),(521,5,335),(583,6,1),(579,6,2),(575,6,3),(318,6,5),(585,6,11),(586,6,12),(584,6,13),(581,6,21),(582,6,22),(580,6,23),(577,6,31),(578,6,32),(576,6,33),(316,6,113),(317,6,114),(327,6,115),(328,6,116),(319,6,122),(320,6,123),(321,6,124),(322,6,125),(553,6,221),(555,6,222),(554,6,223),(552,6,224),(556,6,232),(557,6,233),(558,6,234),(559,6,235),(383,6,315),(545,6,322),(547,6,323),(546,6,325),(544,6,326),(548,6,332),(549,6,333),(550,6,334),(551,6,335);
/*!40000 ALTER TABLE `role_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `screening_close_contact`
--

DROP TABLE IF EXISTS `screening_close_contact`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `screening_close_contact` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `city` varchar(64) DEFAULT NULL COMMENT '市/州',
  `district` varchar(64) DEFAULT NULL COMMENT '区/县',
  `source_patient_name` varchar(64) DEFAULT NULL COMMENT '原患者姓名',
  `source_patient_case_no` varchar(64) DEFAULT NULL COMMENT '原患者病案号',
  `source_patient_bacteriology_result` varchar(64) DEFAULT NULL COMMENT '原患者病原学结果',
  `source_patient_phone` varchar(32) DEFAULT NULL COMMENT '原患者电话',
  `source_patient_id_number` varchar(64) DEFAULT NULL COMMENT '原患者身份证号',
  `report_date` date DEFAULT NULL COMMENT '填表日期',
  `registration_date` date DEFAULT NULL COMMENT '密切接触者登记日期（6/12/24月随访基准）',
  `name` varchar(64) DEFAULT NULL COMMENT '接触者姓名',
  `id_number` varchar(64) DEFAULT NULL COMMENT '接触者身份证号',
  `age` int DEFAULT NULL COMMENT '年龄',
  `phone` varchar(32) DEFAULT NULL COMMENT '接触者电话',
  `contact_type` varchar(32) DEFAULT NULL COMMENT '接触类型：家庭内/家庭外',
  `contact_place` varchar(64) DEFAULT NULL COMMENT '接触场所',
  `first_screen_date` date DEFAULT NULL COMMENT '首次筛查日期',
  `symptom1` varchar(128) DEFAULT NULL COMMENT '结核症状1',
  `symptom2` varchar(128) DEFAULT NULL COMMENT '结核症状2',
  `infection_check_date` date DEFAULT NULL COMMENT '感染检测日期',
  `infection_check_method` varchar(64) DEFAULT NULL COMMENT '感染检测方法（EC/PPD/IGRA）',
  `infection_check_result` varchar(64) DEFAULT NULL COMMENT '结果判定（阴性/阳性）',
  `imaging_date` date DEFAULT NULL COMMENT '影像检查日期',
  `imaging_method` varchar(64) DEFAULT NULL COMMENT '影像方法（胸部X光片/胸部CT）',
  `imaging_result` varchar(128) DEFAULT NULL COMMENT '影像结果',
  `sputum_check_date` date DEFAULT NULL COMMENT '痰检留标日期',
  `sputum_check_method` varchar(64) DEFAULT NULL COMMENT '痰检方法',
  `sputum_check_result` varchar(64) DEFAULT NULL COMMENT '痰检结果',
  `final_screening_result` varchar(32) DEFAULT NULL COMMENT '最终筛查结果：活动性肺结核/潜伏感染者/未做/未发现异常',
  `has_contraindication` varchar(32) DEFAULT NULL COMMENT '有无禁忌症',
  `no_treatment_reason` varchar(128) DEFAULT NULL COMMENT '不接受预防治疗的原因',
  `contraindication_remark` varchar(256) DEFAULT NULL COMMENT '禁忌症备注',
  `has_preventive_treatment` varchar(10) DEFAULT NULL COMMENT '是否开展预防治疗：开展/未开展',
  `preventive_plan` varchar(128) DEFAULT NULL COMMENT '预防性治疗方案',
  `preventive_plan_remark` varchar(256) DEFAULT NULL COMMENT '其他方案备注',
  `treatment_completed` varchar(10) DEFAULT NULL COMMENT '是否完成治疗：是/否',
  `incomplete_reason` varchar(128) DEFAULT NULL COMMENT '未完成原因',
  `followup6_due_date` date DEFAULT NULL COMMENT '6月随访到期日期',
  `followup6_screen_date` date DEFAULT NULL COMMENT '6月-症状筛查日期',
  `followup6_symptom1` varchar(128) DEFAULT NULL COMMENT '6月-症状1',
  `followup6_symptom2` varchar(128) DEFAULT NULL COMMENT '6月-症状2',
  `followup6_imaging_date` date DEFAULT NULL COMMENT '6月-影像检查日期',
  `followup6_imaging_method` varchar(64) DEFAULT NULL COMMENT '6月-影像方法',
  `followup6_imaging_result` varchar(128) DEFAULT NULL COMMENT '6月-影像结果',
  `followup6_sputum_date` date DEFAULT NULL COMMENT '6月-痰检日期',
  `followup6_sputum_method` varchar(64) DEFAULT NULL COMMENT '6月-病原学方法',
  `followup6_sputum_result` varchar(64) DEFAULT NULL COMMENT '6月-病原学结果',
  `followup6_result` varchar(32) DEFAULT NULL COMMENT '6月随访筛查结果',
  `followup12_due_date` date DEFAULT NULL COMMENT '12月随访到期日期',
  `followup12_screen_date` date DEFAULT NULL COMMENT '12月-症状筛查日期',
  `followup12_symptom1` varchar(128) DEFAULT NULL COMMENT '12月-症状1',
  `followup12_symptom2` varchar(128) DEFAULT NULL COMMENT '12月-症状2',
  `followup12_imaging_date` date DEFAULT NULL COMMENT '12月-影像检查日期',
  `followup12_imaging_method` varchar(64) DEFAULT NULL COMMENT '12月-影像方法',
  `followup12_imaging_result` varchar(128) DEFAULT NULL COMMENT '12月-影像结果',
  `followup12_sputum_date` date DEFAULT NULL COMMENT '12月-痰检日期',
  `followup12_sputum_method` varchar(64) DEFAULT NULL COMMENT '12月-病原学方法',
  `followup12_sputum_result` varchar(64) DEFAULT NULL COMMENT '12月-病原学结果',
  `followup12_result` varchar(32) DEFAULT NULL COMMENT '12月随访筛查结果',
  `followup24_due_date` date DEFAULT NULL COMMENT '24月随访到期日期',
  `followup24_screen_date` date DEFAULT NULL COMMENT '24月-症状筛查日期',
  `followup24_symptom1` varchar(128) DEFAULT NULL COMMENT '24月-症状1',
  `followup24_symptom2` varchar(128) DEFAULT NULL COMMENT '24月-症状2',
  `followup24_imaging_date` date DEFAULT NULL COMMENT '24月-影像检查日期',
  `followup24_imaging_method` varchar(64) DEFAULT NULL COMMENT '24月-影像方法',
  `followup24_imaging_result` varchar(128) DEFAULT NULL COMMENT '24月-影像结果',
  `followup24_sputum_date` date DEFAULT NULL COMMENT '24月-痰检日期',
  `followup24_sputum_method` varchar(64) DEFAULT NULL COMMENT '24月-病原学方法',
  `followup24_sputum_result` varchar(64) DEFAULT NULL COMMENT '24月-病原学结果',
  `followup24_result` varchar(32) DEFAULT NULL COMMENT '24月随访筛查结果',
  `remark` text COMMENT '备注',
  `year` varchar(10) DEFAULT NULL COMMENT '年份（从登记日期提取）',
  `gender` varchar(10) DEFAULT NULL COMMENT '性别',
  `ethnicity` varchar(32) DEFAULT NULL COMMENT '民族',
  `household_address` varchar(256) DEFAULT NULL COMMENT '户籍地址',
  `current_address` varchar(256) DEFAULT NULL COMMENT '现住址',
  `cc_status` tinyint NOT NULL DEFAULT '0' COMMENT '密接流程状态：0待处理 1活动性肺结核-患者管理 2潜伏感染者-管理中 3潜伏感染者-归档 4随访监测中 5随访监测归档 6未发现异常-待3月复查 7-3月复查阴性结束 8-3月复查阳性转潜伏流程',
  `expected_treatment_end_date` date DEFAULT NULL COMMENT '系统设定的预计完成治疗时间（用于到期提醒）',
  `three_month_check_date` date DEFAULT NULL COMMENT '3月复查感染检测日期（未发现异常流程）',
  `three_month_check_result` varchar(64) DEFAULT NULL COMMENT '3月复查感染检测结果',
  `three_month_final_result` varchar(16) DEFAULT NULL COMMENT '3月复查最终判定：阴性/阳性',
  `upload_batch` varchar(64) DEFAULT NULL COMMENT '上传批次号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `department_id` bigint DEFAULT NULL COMMENT '所属部门ID',
  PRIMARY KEY (`id`),
  KEY `idx_id_number` (`id_number`),
  KEY `idx_cc_status` (`cc_status`),
  KEY `idx_final_result` (`final_screening_result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='密接人群筛查数据表（新模板73列）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `screening_close_contact`
--

LOCK TABLES `screening_close_contact` WRITE;
/*!40000 ALTER TABLE `screening_close_contact` DISABLE KEYS */;
/*!40000 ALTER TABLE `screening_close_contact` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `screening_key_population`
--

DROP TABLE IF EXISTS `screening_key_population`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `screening_key_population` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `year` varchar(10) DEFAULT NULL COMMENT '年份',
  `city` varchar(64) DEFAULT NULL COMMENT '市（州）',
  `district` varchar(64) DEFAULT NULL COMMENT '县（市、区）',
  `name` varchar(64) DEFAULT NULL COMMENT '姓名',
  `gender` varchar(10) DEFAULT NULL COMMENT '性别',
  `birth_date` date DEFAULT NULL COMMENT '出生日期',
  `age` int DEFAULT NULL COMMENT '年龄',
  `id_type` varchar(32) DEFAULT NULL COMMENT '证件类型',
  `id_number` varchar(64) DEFAULT NULL COMMENT '证件号',
  `ethnicity` varchar(32) DEFAULT NULL COMMENT '民族',
  `phone` varchar(32) DEFAULT NULL COMMENT '联系电话',
  `household_address` varchar(256) DEFAULT NULL COMMENT '户籍所在地',
  `township_community` varchar(128) DEFAULT NULL COMMENT '乡镇/社区',
  `current_address` varchar(256) DEFAULT NULL COMMENT '现住址',
  `crowd_category_close` varchar(10) DEFAULT NULL COMMENT '人群分类-密接（是/否）',
  `crowd_category_student` varchar(10) DEFAULT NULL COMMENT '人群分类-学生（是/否）',
  `crowd_category_teacher` varchar(10) DEFAULT NULL COMMENT '人群分类-教职工（是/否）',
  `crowd_category_elder` varchar(10) DEFAULT NULL COMMENT '人群分类-老年人（是/否）',
  `crowd_category_diabetes` varchar(10) DEFAULT NULL COMMENT '人群分类-糖尿病（是/否）',
  `crowd_category_dual` varchar(10) DEFAULT NULL COMMENT '人群分类-双感（是/否）',
  `crowd_category_tb_hist` varchar(10) DEFAULT NULL COMMENT '人群分类-既往结核史（是/否）',
  `crowd_category_normal` varchar(10) DEFAULT NULL COMMENT '人群分类-非重点人群（是/否）',
  `has_suspicious_symptoms` varchar(10) DEFAULT NULL COMMENT '是否有可疑症状',
  `cough` varchar(10) DEFAULT NULL COMMENT '咳嗽咳痰',
  `hemoptysis` varchar(10) DEFAULT NULL COMMENT '咯血或血痰',
  `fever` varchar(10) DEFAULT NULL COMMENT '发热',
  `chest_pain` varchar(10) DEFAULT NULL COMMENT '胸痛',
  `night_sweats` varchar(10) DEFAULT NULL COMMENT '夜间盗汗',
  `appetite_loss` varchar(10) DEFAULT NULL COMMENT '食欲不振',
  `fatigue` varchar(10) DEFAULT NULL COMMENT '乏力',
  `weight_loss` varchar(10) DEFAULT NULL COMMENT '体重减轻',
  `has_infection_screen` varchar(10) DEFAULT NULL COMMENT '是否进行感染筛',
  `screen_date` date DEFAULT NULL COMMENT '感染筛查日期',
  `screen_method` varchar(64) DEFAULT NULL COMMENT '感染筛查方法（PPD/EC/IGRA）',
  `screen_result` varchar(128) DEFAULT NULL COMMENT '结果（mmXmm/EC阴性/EC阳性/IGRA阴性/IGRA阳性）',
  `infection_result` varchar(128) DEFAULT NULL COMMENT '感染筛查结果',
  `has_chest_xray` varchar(10) DEFAULT NULL COMMENT '是否进行胸片检查',
  `chest_xray_date` date DEFAULT NULL COMMENT '胸片检查日期',
  `chest_xray_result` varchar(128) DEFAULT NULL COMMENT '胸片结果',
  `diagnosis_first` varchar(128) DEFAULT NULL COMMENT '诊断结果',
  `diagnosis_half_year` varchar(128) DEFAULT NULL COMMENT '诊断结果（半年后）',
  `diagnosis_one_year` varchar(128) DEFAULT NULL COMMENT '诊断结果（一年后）',
  `has_preventive_treatment` varchar(10) DEFAULT NULL COMMENT '是否进行预防性治疗',
  `preventive_plan` varchar(128) DEFAULT NULL COMMENT '预防性治疗方案',
  `preventive_start_date` date DEFAULT NULL COMMENT '预防性治疗开始时间',
  `preventive_end_date` date DEFAULT NULL COMMENT '预防性治疗完成时间',
  `preventive_result` varchar(64) DEFAULT NULL COMMENT '预防性治疗结果：规范完成/失访/自行中断治疗/确诊肺结核',
  `preventive_manager` varchar(256) DEFAULT NULL COMMENT '预防性治疗期间随访管理人员',
  `remark` text COMMENT '备注',
  `is_latent` tinyint NOT NULL DEFAULT '0' COMMENT '是否潜伏管理者：0否 1是',
  `upload_batch` varchar(64) DEFAULT NULL COMMENT '上传批次号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `department_id` bigint DEFAULT NULL COMMENT '所属部门ID',
  PRIMARY KEY (`id`),
  KEY `idx_id_number` (`id_number`),
  KEY `idx_latent` (`is_latent`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='重点人群筛查数据表（V4）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `screening_key_population`
--

LOCK TABLES `screening_key_population` WRITE;
/*!40000 ALTER TABLE `screening_key_population` DISABLE KEYS */;
/*!40000 ALTER TABLE `screening_key_population` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `screening_school`
--

DROP TABLE IF EXISTS `screening_school`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `screening_school` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `year` varchar(10) DEFAULT NULL COMMENT '年份',
  `city` varchar(64) DEFAULT NULL COMMENT '市（州）',
  `district` varchar(64) DEFAULT NULL COMMENT '县（市、区）',
  `name` varchar(64) DEFAULT NULL COMMENT '姓名',
  `gender` varchar(10) DEFAULT NULL COMMENT '性别',
  `birth_date` date DEFAULT NULL COMMENT '出生日期',
  `age` int DEFAULT NULL COMMENT '年龄',
  `id_type` varchar(32) DEFAULT NULL COMMENT '证件类型',
  `id_number` varchar(64) DEFAULT NULL COMMENT '证件号',
  `ethnicity` varchar(32) DEFAULT NULL COMMENT '民族',
  `phone` varchar(32) DEFAULT NULL COMMENT '联系电话',
  `household_address` varchar(256) DEFAULT NULL COMMENT '户籍所在地',
  `current_address` varchar(256) DEFAULT NULL COMMENT '现地址',
  `school_type` varchar(64) DEFAULT NULL COMMENT '学校类型',
  `school_name` varchar(128) DEFAULT NULL COMMENT '学校名称',
  `class_name` varchar(128) DEFAULT NULL COMMENT '班级（院系）',
  `tb_history` varchar(64) DEFAULT NULL COMMENT '既往结核病史',
  `close_contact_history` varchar(64) DEFAULT NULL COMMENT '密切接触史',
  `suspicious_symptoms` varchar(128) DEFAULT NULL COMMENT '结核病可疑症状',
  `has_infection_screen` varchar(10) DEFAULT NULL COMMENT '是否进行感染筛',
  `screen_date` date DEFAULT NULL COMMENT '感染筛查日期',
  `screen_method` varchar(64) DEFAULT NULL COMMENT '方法（PPD/EC/IGRA）',
  `screen_result` varchar(128) DEFAULT NULL COMMENT '结果（mmXmm/EC阴性/EC阳性/IGRA阴性/IGRA阳性）',
  `infection_result` varchar(128) DEFAULT NULL COMMENT '感染筛查结果（V4：PPD阴性/PPD+/PPD++/PPD+++/EC阴性/EC阳性/IGRA阴性/IGRA阳性）',
  `has_chest_xray` varchar(10) DEFAULT NULL COMMENT '是否进行胸片检查',
  `chest_xray_date` date DEFAULT NULL COMMENT '胸片检查日期',
  `chest_xray_result` varchar(128) DEFAULT NULL COMMENT '胸片结果',
  `diagnosis_first` varchar(128) DEFAULT NULL COMMENT '诊断结果',
  `diagnosis_half_year` varchar(128) DEFAULT NULL COMMENT '诊断结果（半年后）',
  `diagnosis_one_year` varchar(128) DEFAULT NULL COMMENT '诊断结果（一年后）',
  `has_preventive_treatment` varchar(10) DEFAULT NULL COMMENT '是否进行预防性治疗',
  `preventive_plan` varchar(128) DEFAULT NULL COMMENT '预防性治疗方案',
  `preventive_start_date` date DEFAULT NULL COMMENT '预防性治疗开始时间',
  `preventive_end_date` date DEFAULT NULL COMMENT '预防性治疗完成时间',
  `preventive_result` varchar(64) DEFAULT NULL COMMENT '预防性治疗结果：规范完成/失访/自行中断治疗/确诊肺结核',
  `preventive_manager` varchar(256) DEFAULT NULL COMMENT '预防性治疗期间随访管理人员',
  `remark` text COMMENT '备注',
  `is_latent` tinyint NOT NULL DEFAULT '0' COMMENT '是否潜伏管理者：0否 1是',
  `upload_batch` varchar(64) DEFAULT NULL COMMENT '上传批次号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `department_id` bigint DEFAULT NULL COMMENT '所属部门ID',
  PRIMARY KEY (`id`),
  KEY `idx_id_number` (`id_number`),
  KEY `idx_school` (`school_name`,`district`),
  KEY `idx_latent` (`is_latent`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学校人群筛查数据表（V4）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `screening_school`
--

LOCK TABLES `screening_school` WRITE;
/*!40000 ALTER TABLE `screening_school` DISABLE KEYS */;
/*!40000 ALTER TABLE `screening_school` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `supervision_form`
--

DROP TABLE IF EXISTS `supervision_form`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `supervision_form` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `latent_infection_id` bigint NOT NULL COMMENT '关联潜伏感染ID',
  `population_type` varchar(32) NOT NULL COMMENT '人群类型',
  `patient_name` varchar(64) DEFAULT NULL COMMENT '患者姓名',
  `category` varchar(64) DEFAULT NULL COMMENT '类别：密接/新生筛查/65岁以上老年人/糖尿病人/双感/其他',
  `gender` varchar(10) DEFAULT NULL COMMENT '性别',
  `age` int DEFAULT NULL COMMENT '年龄',
  `phone` varchar(32) DEFAULT NULL COMMENT '电话号码',
  `current_address` varchar(256) DEFAULT NULL COMMENT '现住址',
  `treatment_start_date` date DEFAULT NULL COMMENT '预防性治疗开始日期',
  `treatment_plan` varchar(256) DEFAULT NULL COMMENT '治疗方案（含新增"不服药"）',
  `supervision_content` text COMMENT '督导内容（V4旧字段，兼容保留）',
  `supervision_records` text COMMENT '督导记录（JSON数组：time/content/method/remark）',
  `interrupt_medication` varchar(16) DEFAULT NULL COMMENT '中断用药：有/无',
  `interrupt_count` int DEFAULT NULL COMMENT '中断次数',
  `total_doses` int DEFAULT NULL COMMENT '全程应用药次数',
  `actual_doses` int DEFAULT NULL COMMENT '实际用药次数',
  `medication_rate` varchar(16) DEFAULT NULL COMMENT '用药率（%）',
  `treatment_end_date` date DEFAULT NULL COMMENT '预防性治疗完成（结束疗程）时间',
  `preventive_result` varchar(64) DEFAULT NULL COMMENT '预防性治疗结果：规范完成/失访/自行中断治疗/确诊肺结核（V4旧字段）',
  `preventive_manager` varchar(256) DEFAULT NULL COMMENT '预防性治疗期间随访管理人员（V4旧字段）',
  `manager_type` varchar(64) DEFAULT NULL COMMENT '督导管理人员类型',
  `manager_name` varchar(64) DEFAULT NULL COMMENT '督导管理人员姓名',
  `remark` text COMMENT '备注',
  `attachment_urls` text COMMENT '附件（JSON数组，存储图片/文件URL）',
  `filled_by` bigint DEFAULT NULL COMMENT '填写人ID',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0未填写 1已填写 2已归档',
  `archived_time` datetime DEFAULT NULL COMMENT '归档时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_latent` (`latent_infection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预防性治疗督导表（V5）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `supervision_form`
--

LOCK TABLES `supervision_form` WRITE;
/*!40000 ALTER TABLE `supervision_form` DISABLE KEYS */;
/*!40000 ALTER TABLE `supervision_form` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_message`
--

DROP TABLE IF EXISTS `sys_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sender_id` bigint DEFAULT NULL COMMENT '发送人ID（系统消息为空）',
  `receiver_id` bigint NOT NULL COMMENT '接收人ID',
  `title` varchar(256) NOT NULL COMMENT '消息标题',
  `content` text COMMENT '消息内容',
  `type` varchar(32) NOT NULL COMMENT '消息类型：notice_timeout/supervision_timeout/visit_timeout',
  `biz_id` bigint DEFAULT NULL COMMENT '关联业务ID',
  `is_read` tinyint NOT NULL DEFAULT '0' COMMENT '是否已读：0未读 1已读',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_receiver` (`receiver_id`,`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_message`
--

LOCK TABLES `sys_message` WRITE;
/*!40000 ALTER TABLE `sys_message` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(64) NOT NULL COMMENT '用户名',
  `password` varchar(128) NOT NULL COMMENT '密码',
  `real_name` varchar(64) DEFAULT NULL COMMENT '真实姓名',
  `role` tinyint NOT NULL DEFAULT '6' COMMENT '角色：1=超级管理员 2=一级 3=二级 4=三级 5=四级 6=五级',
  `org_name` varchar(128) DEFAULT NULL COMMENT '所属机构名称',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `department_id` bigint DEFAULT NULL COMMENT '所属部门ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'admin','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi','超级管理员',1,'市疾控中心','2026-05-17 17:13:11','2026-05-17 17:13:11',0,NULL),(2,'level4user','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi','四级操作员',5,'区疾控中心','2026-05-17 17:13:11','2026-05-17 17:13:11',0,NULL),(3,'level5user','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi','五级操作员',6,'社区卫生服务中心','2026-05-17 17:13:11','2026-05-17 17:13:11',0,NULL);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_permission`
--

DROP TABLE IF EXISTS `user_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_perm` (`user_id`,`permission_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户额外权限（与角色权限合并）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_permission`
--

LOCK TABLES `user_permission` WRITE;
/*!40000 ALTER TABLE `user_permission` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_permission` ENABLE KEYS */;
UNLOCK TABLES;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-18  3:00:00
