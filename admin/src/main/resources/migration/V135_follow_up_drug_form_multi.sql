-- V135：后续随访「药品剂型」支持多选（逗号分隔存储）
-- 原 VARCHAR(16) 仅够单选编码，扩展为 VARCHAR(64) 与首次随访一致

ALTER TABLE `follow_up_visit`
    MODIFY COLUMN `drug_form` VARCHAR(64) DEFAULT NULL COMMENT 'V15 药品剂型 1固定剂量/2散装/3板式/4注射，多选逗号分隔';
