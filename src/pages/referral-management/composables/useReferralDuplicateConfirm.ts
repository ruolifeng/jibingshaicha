import { ElMessageBox } from "element-plus"
import { checkReferralDuplicateApi, createReferralTrackingApi } from "../apis/index"

/** 用户取消重复确认弹窗 */
export function isReferralDuplicateCancel(err: unknown) {
  return err === "cancel" || (err instanceof Error && err.message === "cancel")
}

/** 手动新增推介/追踪：重复患者（证件号+姓名）时弹窗确认是否新增记录 */
export async function createReferralWithDuplicateConfirm(data: Record<string, any>) {
  const { data: check } = await checkReferralDuplicateApi({
    bizMode: data.bizMode,
    idNumber: data.idNumber,
    name: data.name
  })

  if (check?.exists) {
    try {
      await ElMessageBox.confirm(
        `患者「${data.name}」（${data.idNumber}）信息已经导入过，是否需要增加记录？\n选择「是」将新增一条记录，原有记录将保留。`,
        "重复患者确认",
        {
          confirmButtonText: "是，新增记录",
          cancelButtonText: "否，取消录入",
          type: "warning"
        }
      )
    } catch {
      throw new Error("cancel")
    }
    return createReferralTrackingApi({ ...data, confirmDuplicate: true })
  }

  return createReferralTrackingApi(data)
}
