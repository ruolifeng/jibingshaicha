-- V130：潜伏感染者通知单服药管理单位去掉「富顺县」前缀；督导表管理单位同步

UPDATE `notice`
SET `medication_management_unit` = '代寺镇中心卫生院'
WHERE `notice_type` = 'latent'
  AND `medication_management_unit` = '富顺县代寺镇中心卫生院';

UPDATE `notice`
SET `medication_management_unit` = '童寺镇中心卫生院'
WHERE `notice_type` = 'latent'
  AND `medication_management_unit` = '富顺县童寺镇中心卫生院';

UPDATE `notice`
SET `medication_management_unit` = '邓关街道社区卫生服务中心'
WHERE `notice_type` = 'latent'
  AND `medication_management_unit` = '富顺县邓关街道社区卫生服务中心';

UPDATE `supervision_form`
SET `managing_unit` = '代寺镇中心卫生院'
WHERE `managing_unit` = '富顺县代寺镇中心卫生院';

UPDATE `supervision_form`
SET `managing_unit` = '童寺镇中心卫生院'
WHERE `managing_unit` = '富顺县童寺镇中心卫生院';

UPDATE `supervision_form`
SET `managing_unit` = '邓关街道社区卫生服务中心'
WHERE `managing_unit` = '富顺县邓关街道社区卫生服务中心';
