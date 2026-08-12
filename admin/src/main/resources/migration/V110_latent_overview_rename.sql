-- V110：潜伏感染者管理「在管总览」改名为「潜伏感染者在管总览」
UPDATE `permission`
SET `name` = '潜伏感染者在管总览'
WHERE `code` = 'latentManagement:overview';
