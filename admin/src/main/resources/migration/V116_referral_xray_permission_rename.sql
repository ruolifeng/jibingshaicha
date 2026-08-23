-- V116：推介追踪 — 「录入胸片」改名为「录入感染检测结果及胸片结果」
UPDATE `permission`
SET `name` = '录入感染检测结果及胸片结果'
WHERE `code` = 'referralManagement:xray';
