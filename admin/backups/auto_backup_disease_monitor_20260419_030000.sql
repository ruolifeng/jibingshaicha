-- MySQL dump 10.13  Distrib 9.5.0, for macos26.2 (arm64)
--
-- Host: localhost    Database: disease_monitor
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

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ '353975fe-a03b-11ef-8a59-018bb6ee87d7:1-1424';

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
) ENGINE=InnoDB AUTO_INCREMENT=144 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `permission`
--

LOCK TABLES `permission` WRITE;
/*!40000 ALTER TABLE `permission` DISABLE KEYS */;
INSERT INTO `permission` VALUES (1,'school','学校人群',1,0,1),(2,'keyPopulation','重点人群',1,0,2),(3,'closeContact','密接人群',1,0,3),(4,'statistics','统计分析',1,0,4),(5,'message','系统消息',1,0,5),(6,'system','系统管理',1,0,6),(10,'school:screening','筛查管理',1,1,1),(11,'school:latent','潜伏感染',1,1,2),(12,'school:patient','患者管理',1,1,3),(13,'school:history','历史患者',1,1,4),(20,'keyPopulation:screening','筛查管理',1,2,1),(21,'keyPopulation:latent','潜伏感染',1,2,2),(22,'keyPopulation:patient','患者管理',1,2,3),(23,'keyPopulation:history','历史患者',1,2,4),(30,'closeContact:screening','筛查管理',1,3,1),(31,'closeContact:latent','潜伏感染',1,3,2),(32,'closeContact:patient','患者管理',1,3,3),(33,'closeContact:history','历史患者',1,3,4),(60,'system:users','用户管理',1,6,1),(61,'system:permissions','权限管理',1,6,2),(100,'screening:upload','上传筛查数据',2,10,1),(110,'latent:track','追踪',2,11,1),(111,'latent:referral','转诊',2,11,2),(112,'latent:sendNotice','发送潜伏者通知单',2,11,3),(113,'latent:confirmNotice','确认接收通知单',2,11,4),(114,'latent:supervision','填写督导表',2,11,5),(120,'patient:importEpidemic','导入大疫情表',2,12,1),(121,'patient:sendNotice','发送患者通知单',2,12,2),(122,'patient:confirmNotice','确认接收患者通知单',2,12,3),(123,'patient:firstVisit','首次随访',2,12,4),(124,'patient:followUp','后续随访',2,12,5),(125,'patient:medication','服药管理',2,12,6),(130,'statistics:export','导出统计',2,4,1),(140,'user:create','创建用户',2,60,1),(141,'user:edit','编辑用户',2,60,2),(142,'user:delete','删除用户',2,60,3),(143,'permission:assign','分配权限',2,61,1);
/*!40000 ALTER TABLE `permission` ENABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=219 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role_permission`
--

LOCK TABLES `role_permission` WRITE;
/*!40000 ALTER TABLE `role_permission` DISABLE KEYS */;
INSERT INTO `role_permission` VALUES (24,1,1),(6,1,2),(1,1,3),(30,1,4),(16,1,5),(32,1,6),(28,1,10),(26,1,11),(27,1,12),(25,1,13),(10,1,20),(8,1,21),(9,1,22),(7,1,23),(5,1,30),(3,1,31),(4,1,32),(2,1,33),(34,1,60),(33,1,61),(29,1,100),(15,1,110),(12,1,111),(13,1,112),(11,1,113),(14,1,114),(20,1,120),(22,1,121),(17,1,122),(18,1,123),(19,1,124),(21,1,125),(31,1,130),(35,1,140),(37,1,141),(36,1,142),(23,1,143),(86,2,1),(69,2,2),(64,2,3),(92,2,4),(79,2,5),(90,2,10),(88,2,11),(89,2,12),(87,2,13),(73,2,20),(71,2,21),(72,2,22),(70,2,23),(68,2,30),(66,2,31),(67,2,32),(65,2,33),(91,2,100),(78,2,110),(75,2,111),(76,2,112),(74,2,113),(77,2,114),(83,2,120),(85,2,121),(80,2,122),(81,2,123),(82,2,124),(84,2,125),(93,2,130),(117,3,1),(100,3,2),(95,3,3),(123,3,4),(110,3,5),(121,3,10),(119,3,11),(120,3,12),(118,3,13),(104,3,20),(102,3,21),(103,3,22),(101,3,23),(99,3,30),(97,3,31),(98,3,32),(96,3,33),(122,3,100),(109,3,110),(106,3,111),(107,3,112),(105,3,113),(108,3,114),(114,3,120),(116,3,121),(111,3,122),(112,3,123),(113,3,124),(115,3,125),(124,3,130),(148,4,1),(131,4,2),(126,4,3),(154,4,4),(141,4,5),(152,4,10),(150,4,11),(151,4,12),(149,4,13),(135,4,20),(133,4,21),(134,4,22),(132,4,23),(130,4,30),(128,4,31),(129,4,32),(127,4,33),(153,4,100),(140,4,110),(137,4,111),(138,4,112),(136,4,113),(139,4,114),(145,4,120),(147,4,121),(142,4,122),(143,4,123),(144,4,124),(146,4,125),(177,5,1),(162,5,2),(157,5,3),(183,5,4),(171,5,5),(181,5,10),(179,5,11),(180,5,12),(178,5,13),(166,5,20),(164,5,21),(165,5,22),(163,5,23),(161,5,30),(159,5,31),(160,5,32),(158,5,33),(182,5,100),(170,5,110),(167,5,111),(168,5,112),(169,5,114),(174,5,120),(176,5,121),(172,5,123),(173,5,124),(175,5,125),(205,6,1),(193,6,2),(188,6,3),(200,6,5),(209,6,10),(207,6,11),(208,6,12),(206,6,13),(197,6,20),(195,6,21),(196,6,22),(194,6,23),(192,6,30),(190,6,31),(191,6,32),(189,6,33),(198,6,113),(199,6,114),(201,6,122),(202,6,123),(203,6,124),(204,6,125);
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
  `occupation` varchar(64) DEFAULT NULL COMMENT '职业',
  `phone` varchar(32) DEFAULT NULL COMMENT '联系电话',
  `household_address` varchar(256) DEFAULT NULL COMMENT '户籍所在地',
  `current_address` varchar(256) DEFAULT NULL COMMENT '现住址',
  `contact_type` varchar(32) DEFAULT NULL COMMENT '接触类型：家庭内/家庭外',
  `source_patient_name` varchar(64) DEFAULT NULL COMMENT '原患者姓名',
  `source_patient_confirm_date` date DEFAULT NULL COMMENT '原患者确诊日期',
  `source_patient_id_number` varchar(64) DEFAULT NULL COMMENT '原患者身份证号',
  `first_screen_date` date DEFAULT NULL COMMENT '首次筛查日期',
  `first_symptom_result` varchar(128) DEFAULT NULL COMMENT '首次症状筛查结果',
  `first_infection_method` varchar(64) DEFAULT NULL COMMENT '首次感染检查方法（PPD/EC/IGRA）',
  `first_screen_result` varchar(128) DEFAULT NULL COMMENT '首次结果（mmXmm/EC阴性/EC阳性/IGRA阴性/IGRA阳性）',
  `first_infection_result` varchar(128) DEFAULT NULL COMMENT '首次感染筛查结果',
  `first_has_chest_xray` varchar(10) DEFAULT NULL COMMENT '首次是否进行胸片检查',
  `first_chest_xray_date` date DEFAULT NULL COMMENT '首次胸片检查日期',
  `first_chest_xray_result` varchar(128) DEFAULT NULL COMMENT '首次胸片检查结果',
  `first_diagnosis` varchar(64) DEFAULT NULL COMMENT '首次诊断结果：排除/疑似肺结核/潜伏感染者/确诊患者/其他',
  `half_year_screen_date` date DEFAULT NULL COMMENT '半年后筛查日期',
  `half_year_symptom_result` varchar(128) DEFAULT NULL COMMENT '半年后症状筛查结果',
  `half_year_infection_method` varchar(64) DEFAULT NULL COMMENT '半年后感染检查方法',
  `half_year_screen_result` varchar(128) DEFAULT NULL COMMENT '半年后结果',
  `half_year_infection_result` varchar(128) DEFAULT NULL COMMENT '半年后感染筛查结果',
  `half_year_has_chest_xray` varchar(10) DEFAULT NULL COMMENT '半年后是否进行胸片检查',
  `half_year_chest_xray_date` date DEFAULT NULL COMMENT '半年后胸片检查日期',
  `half_year_chest_xray_result` varchar(128) DEFAULT NULL COMMENT '半年后胸片检查结果',
  `half_year_diagnosis` varchar(64) DEFAULT NULL COMMENT '半年后诊断结果',
  `one_year_screen_date` date DEFAULT NULL COMMENT '一年后筛查日期',
  `one_year_symptom_result` varchar(128) DEFAULT NULL COMMENT '一年后症状筛查结果',
  `one_year_infection_method` varchar(64) DEFAULT NULL COMMENT '一年后感染筛查方法',
  `one_year_screen_result` varchar(128) DEFAULT NULL COMMENT '一年后结果',
  `one_year_infection_result` varchar(128) DEFAULT NULL COMMENT '一年后感染筛查结果',
  `one_year_has_chest_xray` varchar(10) DEFAULT NULL COMMENT '一年后是否进行胸片检查',
  `one_year_chest_xray_date` date DEFAULT NULL COMMENT '一年后胸片检查日期',
  `one_year_chest_xray_result` varchar(128) DEFAULT NULL COMMENT '一年后胸片检查结果',
  `one_year_diagnosis` varchar(64) DEFAULT NULL COMMENT '一年后诊断结果',
  `has_preventive_treatment` varchar(10) DEFAULT NULL COMMENT '是否进行预防性治疗',
  `preventive_plan` varchar(128) DEFAULT NULL COMMENT '预防性治疗方案',
  `preventive_start_date` date DEFAULT NULL COMMENT '预防性治疗开始时间',
  `preventive_end_date` date DEFAULT NULL COMMENT '预防性治疗完成时间',
  `preventive_result` varchar(64) DEFAULT NULL COMMENT '预防性治疗结果：规范完成/失访/自行中断治疗/确诊肺结核',
  `preventive_manager` varchar(256) DEFAULT NULL COMMENT '预防性治疗期间随访管理人员',
  `benefit_method` varchar(64) DEFAULT NULL COMMENT '惠民方式',
  `remark` text COMMENT '备注',
  `is_latent` tinyint NOT NULL DEFAULT '0' COMMENT '是否潜伏管理者：0否 1是',
  `active_round` tinyint DEFAULT NULL COMMENT '阳性轮次：1首次 2半年后 3一年后',
  `upload_batch` varchar(64) DEFAULT NULL COMMENT '上传批次号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_id_number` (`id_number`),
  KEY `idx_latent` (`is_latent`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='密接人群筛查数据表（V4三轮）';
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
  `treatment_start_date` date DEFAULT NULL COMMENT '预防性治疗开始日期',
  `treatment_end_date` date DEFAULT NULL COMMENT '预防性治疗完成时间（V4新增）',
  `treatment_plan` varchar(256) DEFAULT NULL COMMENT '治疗方案',
  `supervision_content` text COMMENT '督导内容（JSON格式存储表单数据）',
  `preventive_result` varchar(64) DEFAULT NULL COMMENT '预防性治疗结果：规范完成/失访/自行中断治疗/确诊肺结核（V4新增）',
  `preventive_manager` varchar(256) DEFAULT NULL COMMENT '预防性治疗期间随访管理人员（V4新增）',
  `filled_by` bigint DEFAULT NULL COMMENT '填写人ID',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0未填写 1已填写 2已归档',
  `archived_time` datetime DEFAULT NULL COMMENT '归档时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_latent` (`latent_infection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预防性治疗督导表（V4）';
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
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'admin','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi','超级管理员',1,'市疾控中心','2026-04-18 18:32:31','2026-04-18 18:32:31',0),(2,'level4user','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi','四级操作员',5,'区疾控中心','2026-04-18 18:32:31','2026-04-18 18:32:31',0),(3,'level5user','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi','五级操作员',6,'社区卫生服务中心','2026-04-18 18:32:31','2026-04-18 18:32:31',0);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
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

-- Dump completed on 2026-04-19  3:00:00
