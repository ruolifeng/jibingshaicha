package cn.luyou.service;

import cn.luyou.model.ImportResult;
import cn.luyou.model.LatentInfection;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface LatentInfectionService extends IService<LatentInfection> {

    /**
     * 分页查询
     * @param referralResult 转诊结果过滤：null 不过滤；"pending" 查询尚未转诊（referralResult IS NULL）；其他值精确匹配
     */
    IPage<LatentInfection> queryPage(int page, int size, String populationType,
                                      String name, String idNumber, Integer trackingStatus, Integer archived,
                                      String referralResult, String diagnosisFirst,
                                      String phone, String dateFrom, String dateTo,
                                      String dateFilterBy, String creatorName, String crowdCategory);

    /** 追踪操作 */
    void track(Long id, Integer status, String remark, LocalDate actualArrivalDate);

    /**
     * 录入胸片检查与首次诊断结果（V4 追踪到位后新增步骤）
     * diagnosisFirst 取值：排除/疑似肺结核/潜伏感染者/确诊患者/其他
     *
     * @deprecated V13 起拆分为 {@link #saveXrayOnly(Long, Map)} 与 {@link #saveDiagnosisOnly(Long, Map)}
     *             两个独立操作；本方法保留用于：1) 批量导入；2) 旧前端兼容（同时传胸片+诊断时）。
     */
    @Deprecated
    void saveXrayAndDiagnosis(Long id, Map<String, Object> data);

    /**
     * 仅录入胸片检查结果（V13 拆分新增）
     * 字段：hasChestXray / chestXrayDate / chestXrayResult
     * <p>不修改 diagnosisFirst，不触发转诊。
     */
    void saveXrayOnly(Long id, Map<String, Object> data);

    /**
     * 仅录入首次诊断结果（V13 拆分新增）
     * 字段：diagnosisFirst（必填）
     * <p>会按映射自动驱动后续转诊：排除/其他 → 归档；潜伏感染者 → 留在潜伏管理；
     *    疑似肺结核/确诊患者 → 创建患者档案并归档。
     */
    void saveDiagnosisOnly(Long id, Map<String, Object> data);

    /**
     * 批量导入胸片+诊断 Excel（含 Z-AE 列，按证件号匹配）
     */
    int importXrayBatch(MultipartFile file, String populationType);

    /** 转诊操作（V4 新增 suspected 疑似肺结核） */
    void referral(Long id, String result, String remark, LocalDate actualReferralDate);

    /** 设置服药状态（进入预防治疗管理） */
    void setMedicationStatus(Long id, Integer medicationStatus);

    /** 结案归档（按期检查通过后） */
    void closeCase(Long id);

    /** 历史患者（已归档潜伏感染者）列表 */
    IPage<LatentInfection> queryHistoryPage(int page, int size, String populationType,
                                            String name, String idNumber, String phone,
                                            String startTime, String endTime,
                                            String treatmentCompletionStatus);

    /** 按督导表优先记录的治疗完成情况筛选潜伏感染 ID */
    List<Long> findLatentIdsByPreferredTreatmentCompletionStatus(String treatmentCompletionStatus);

    /** 对导入时已包含首次诊断的潜伏感染记录进行自动分流处理。 */
    void autoReferralForDirectDiagnosis(List<LatentInfection> latents);

    /**
     * 筛查记录不再需进入待诊断时，归档误创建的待处理潜伏记录（重新导入时调用）。
     */
    void archivePendingLatentFromScreening(Long screeningId, String populationType, String diagnosisFirst);

    /** 查询潜伏感染详情（含筛查回填字段） */
    LatentInfection getDetail(Long id);

    /** 更新潜伏感染基本信息 */
    void updateBasicInfo(Long id, Map<String, Object> body);

    /** 手动新增潜伏感染记录（在管总览） */
    Long createManual(Map<String, Object> body);

    /** 批量导入潜伏感染记录（字段与手动新增一致） */
    ImportResult importManualBatch(MultipartFile file);

    /** 级联删除潜伏感染记录及其关联数据 */
    void deleteCascade(Long id);

    /** 批量级联删除潜伏感染记录 */
    void batchDeleteCascade(List<Long> ids);

    String ARCHIVE_REMARK_TRANSFERRED_OUT = "已转出";
    String ARCHIVE_REMARK_TRANSFER_PENDING = "转出待确认";

    static boolean isTransferLocked(LatentInfection latent) {
        if (latent == null || latent.getArchiveRemark() == null) {
            return false;
        }
        return ARCHIVE_REMARK_TRANSFER_PENDING.equals(latent.getArchiveRemark())
                || ARCHIVE_REMARK_TRANSFERRED_OUT.equals(latent.getArchiveRemark());
    }

    void markTransferPending(Long id);

    void markTransferredOut(Long id);

    void restoreTransferredLatent(Long id);

    Long copyLatentForTransferOut(Long sourceLatentId, Long receiverUserId);

    void assertLatentOperable(Long id);
}
