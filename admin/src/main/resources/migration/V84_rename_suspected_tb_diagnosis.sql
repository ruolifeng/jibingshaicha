-- 筛查诊断结果：「疑似肺结核」统一更名为「疑似结核」
UPDATE `screening_school`
SET `diagnosis_first` = '疑似结核'
WHERE `diagnosis_first` = '疑似肺结核';

UPDATE `screening_key_population`
SET `diagnosis_first` = '疑似结核'
WHERE `diagnosis_first` = '疑似肺结核';

UPDATE `latent_infection`
SET `diagnosis_first` = '疑似结核'
WHERE `diagnosis_first` = '疑似肺结核';

UPDATE `latent_infection`
SET `diagnosis_result` = '疑似结核'
WHERE `diagnosis_result` = '疑似肺结核';

UPDATE `epidemic_import`
SET `diagnosis_result` = '疑似结核'
WHERE `diagnosis_result` = '疑似肺结核';

UPDATE `screening_close_contact`
SET `final_screening_result` = '疑似结核'
WHERE `final_screening_result` = '疑似肺结核';

UPDATE `screening_close_contact`
SET `followup6_result` = '疑似结核'
WHERE `followup6_result` = '疑似肺结核';

UPDATE `screening_close_contact`
SET `followup12_result` = '疑似结核'
WHERE `followup12_result` = '疑似肺结核';

UPDATE `screening_close_contact`
SET `followup24_result` = '疑似结核'
WHERE `followup24_result` = '疑似肺结核';

UPDATE `close_contact_case`
SET `final_screening_result` = '疑似结核'
WHERE `final_screening_result` = '疑似肺结核';
