/**
 * 密接潜伏感染管理 API
 * 注意：密接人群走独立流程，不使用学校/重点人群的 LatentInfection 通用接口
 * 数据直接从 screening_close_contact 表读取，以 finalScreeningResult + ccStatus 驱动流程
 */
import {
  getScreeningCloseContactListApi,
  getScreeningCloseContactDetailApi,
  setExpectedEndDateApi,
  confirmTreatmentApi,
  submitThreeMonthCheckApi,
  updateScreeningCloseContactApi
} from "@/pages/close-contact/screening/apis"

// 重新导出，让潜伏管理页面直接用
export {
  getScreeningCloseContactListApi as getLatentListApi,
  getScreeningCloseContactDetailApi as getDetailApi,
  setExpectedEndDateApi,
  confirmTreatmentApi,
  submitThreeMonthCheckApi,
  updateScreeningCloseContactApi as updateCcRecordApi
}

// 督导表相关（复用通用接口）
export {
  saveSupervisionApi,
  getSupervisionDetailApi,
  sendNoticeApi,
  confirmNoticeApi,
  getNoticeListByBizApi
} from "@/pages/school/latent/apis"
