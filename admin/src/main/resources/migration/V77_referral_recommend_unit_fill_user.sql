-- 推介记录：推介单位名称、填写用户名称（创建时快照）
ALTER TABLE referral_tracking
    ADD COLUMN recommend_unit_name VARCHAR(200) NULL COMMENT '推介单位名称' AFTER recommend_reason,
    ADD COLUMN fill_user_name VARCHAR(100) NULL COMMENT '填写用户名称' AFTER recommend_unit_name;
