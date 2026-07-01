-- V82：服药管理增加开始治疗日期（治疗记录卡）
ALTER TABLE `medication_management`
    ADD COLUMN `start_treatment_date` DATE DEFAULT NULL COMMENT '开始治疗日期' AFTER `sputum_result`;
