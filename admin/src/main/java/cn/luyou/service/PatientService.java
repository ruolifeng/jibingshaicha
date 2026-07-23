package cn.luyou.service;

import cn.luyou.model.ImportResult;
import cn.luyou.model.Patient;
import cn.luyou.model.vo.PatientDistributionHeatmapVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface PatientService extends IService<Patient> {

    String ARCHIVE_REMARK_TRANSFERRED_OUT = "已转出";

    /** 转出待接收方确认 */
    String ARCHIVE_REMARK_TRANSFER_PENDING = "转出待确认";

    /** 是否处于转出锁定（待确认或已转出，不可编辑/再次转出） */
    static boolean isTransferLocked(Patient patient) {
        if (patient == null || patient.getArchiveRemark() == null) {
            return false;
        }
        return ARCHIVE_REMARK_TRANSFER_PENDING.equals(patient.getArchiveRemark())
                || ARCHIVE_REMARK_TRANSFERRED_OUT.equals(patient.getArchiveRemark());
    }

    /** 停止治疗归档备注前缀（后接具体原因） */
    String ARCHIVE_REMARK_STOP_TREATMENT_PREFIX = "停止治疗：";

    /** 停止治疗原因：转入耐多药治疗（不归档，可继续随访） */
    String STOP_TREATMENT_REASON_MDR = "转入耐多药治疗";

    /** 停止治疗是否应归档（完成疗程/死亡/丢失/其它） */
    static boolean shouldArchiveOnStopTreatment(String stopTreatment, String reason) {
        return "是".equals(stopTreatment)
                && reason != null
                && !reason.isBlank()
                && !STOP_TREATMENT_REASON_MDR.equals(reason);
    }

    /** 是否为停止治疗导致的归档 */
    static boolean isStopTreatmentArchiveRemark(String archiveRemark) {
        return archiveRemark != null && archiveRemark.startsWith(ARCHIVE_REMARK_STOP_TREATMENT_PREFIX);
    }

    IPage<Patient> queryPage(int page, int size, String populationType,
                              String name, String idNumber, String phone, String currentAddress,
                              String diagnosisResult, Integer archived, String dateFrom, String dateTo,
                              String dateFilterBy, String medicationManagementUnit, String crowdCategory,
                              String creatorUsername, String columnFilters, String sortField, String sortOrder);

    default IPage<Patient> queryPage(int page, int size, String populationType,
                                     String name, String idNumber, String phone, String currentAddress,
                                     String diagnosisResult, Integer archived, String dateFrom, String dateTo,
                                     String dateFilterBy, String medicationManagementUnit, String crowdCategory,
                                     String creatorUsername, String columnFilters) {
        return queryPage(page, size, populationType, name, idNumber, phone, currentAddress,
                diagnosisResult, archived, dateFrom, dateTo, dateFilterBy, medicationManagementUnit, crowdCategory,
                creatorUsername, columnFilters, null, null);
    }

    default IPage<Patient> queryPage(int page, int size, String populationType,
                                     String name, String idNumber, String phone, String currentAddress,
                                     String diagnosisResult, Integer archived, String dateFrom, String dateTo,
                                     String dateFilterBy, String medicationManagementUnit, String crowdCategory) {
        return queryPage(page, size, populationType, name, idNumber, phone, currentAddress,
                diagnosisResult, archived, dateFrom, dateTo, dateFilterBy, medicationManagementUnit, crowdCategory,
                null, null, null, null);
    }

    /** 手动新增在管患者（在管总览） */
    Long createManual(Map<String, Object> body);

    /** 批量导入在管患者（字段与手动新增一致） */
    ImportResult importManualBatch(MultipartFile file, boolean confirmSkipInvalid, boolean confirmSkipDuplicateInFile);

    default ImportResult importManualBatch(MultipartFile file, boolean confirmSkipInvalid) {
        return importManualBatch(file, confirmSkipInvalid, false);
    }

    /** 批量删除患者（级联删除） */
    void batchDeletePatients(List<Long> ids);

    /** 导出用患者列表（与列表查询使用相同的数据范围过滤） */
    List<Patient> listForExport(String populationType, String name, String idNumber,
                                 String phone, String currentAddress, String diagnosisResult,
                                 Integer archived, String dateFrom, String dateTo,
                                 String startTime, String endTime,
                                 String dateFilterBy, String medicationManagementUnit,
                                 String crowdCategory);

    /** 导入大疫情表并模糊匹配合并 */
    int importEpidemic(MultipartFile file, String populationType);

    /** 归档患者（停止完成时间） */
    void archivePatient(Long id);

    /** 归档患者并写入备注 */
    void archivePatient(Long id, String archiveRemark);

    /** 转出被拒绝后恢复为在管（仅 archiveRemark=转出待确认 时生效） */
    void restoreTransferredPatient(Long id);

    /** 发起转出：标记为转出待确认（保留在在管列表） */
    void markTransferPending(Long id);

    /** 转出确认后：标记原记录为已转出并归档退出在管（全系统在管仅保留接收方一条） */
    void markTransferredOut(Long id);

    /**
     * 接收方确认转出后，复制患者及关联子记录至接收方部门/用户。
     * @return 新患者 ID
     */
    Long copyPatientForTransferOut(Long sourcePatientId, Long receiverUserId);

    /** 停止治疗归档解锁（管理员操作，仅停止治疗归档可解锁） */
    void unarchivePatientFromStopTreatment(Long id);

    /** 历史患者列表 */
    IPage<Patient> queryHistoryPage(int page, int size, String populationType,
                                     String name, String idNumber, String phone,
                                     String diagnosisResult, String startTime, String endTime,
                                     String stopTreatmentReason);

    /** 按后续随访优先记录的停止治疗原因筛选患者 ID */
    List<Long> findPatientIdsByPreferredStopTreatmentReason(String stopTreatmentReason);

    /** 删除患者（级联软删首次随访、后续随访、服药记录、通知单） */
    void deletePatient(Long id);

    /**
     * 导入专病网/病案信息表（populationType='specialDisease'）。
     * 提取字段：患者姓名、身份证号、性别、出生日期、年龄、联系电话、人群分类、
     *          现详细住址、户籍地址、现管单位、诊断结果（病原学阴/阳性）
     */
    int importSpecialDisease(MultipartFile file);

    /** 查询患者详情（含关联状态） */
    Patient getDetail(Long id);

    /** 更新患者基本信息 */
    void updateBasicInfo(Long id, Map<String, Object> body);

    /** 校验患者可编辑（非转出锁定） */
    void assertPatientOperable(Long id);

    /**
     * 首页统计：在管总览 + 历史患者总数（与列表查询一致的数据权限）。
     *
     * @param statYear 统计年度（自然年 1/1—12/31）；为 null 时不限年度
     */
    long countManagedPatientsForDashboard(Integer statYear, List<Long> filterDeptIds);

    /**
     * 首页统计：在管总览 + 历史患者中「病原学结果阳性」人数（与列表筛选项「病原学结果阳性」口径一致）。
     */
    long countPathogenPositivePatientsForDashboard(Integer statYear, List<Long> filterDeptIds);

    /**
     * 首页统计：治疗成功人数。分母为 statYear 年度管理患者；分子为其中任意时间完成疗程者（可跨年）。
     */
    long countTreatmentSuccessForDashboard(Integer statYear, List<Long> filterDeptIds);

    /**
     * 患者分布热力图（三级及以上用户，自贡地图：市级区县 / 区县级乡镇）。
     *
     * @param districtName 下钻区县名称，为空时返回自贡各区县汇总
     */
    PatientDistributionHeatmapVO buildPatientDistributionHeatmap(Integer statYear, String districtName,
                                                               List<Long> filterDeptIds);

    /**
     * 表头 Excel 式筛选：在权限范围内返回某列实际出现过的去重值。
     *
     * @param field    白名单字段（如 diagnosisResult、gender）
     * @param archived 0=在管，1=历史；null 不限
     */
    List<String> listDistinctColumnValues(String field, Integer archived);

}
