-- V136：重点人群「老年人」「糖尿病患者」历史勾选符号「√/✓/✔」统一归并为「是」

UPDATE `screening_key_population`
SET `crowd_category_elder` = '是'
WHERE TRIM(`crowd_category_elder`) IN ('√', '✓', '✔');

UPDATE `screening_key_population`
SET `crowd_category_diabetes` = '是'
WHERE TRIM(`crowd_category_diabetes`) IN ('√', '✓', '✔');
