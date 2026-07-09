package cn.luyou.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("patient")
public class Patient extends BaseEntity {

    private Long screeningId;
    private Long latentInfectionId;
    private String populationType;
    private String name;
    private String gender;
    private LocalDate birthDate;
    private Integer age;
    private String idType;
    private String idNumber;
    private String ethnicity;
    private String phone;
    private String householdAddress;
    private String currentAddress;
    private String diagnosisResult;
    /** 来源：confirmed=转诊确诊 epidemic=大疫情导入 */
    private String source;
    private Integer archived;
    private LocalDateTime archivedTime;
    /** 归档备注（如：已转出） */
    private String archiveRemark;
    /** 大疫情表额外字段（JSON） */
    private String epidemicData;
    /** 所属部门ID */
    private Long departmentId;
    /** 录入人用户ID（手动新增/导入时写入，五级数据权限） */
    private Long creatorId;

    /** Excel 导入行号（与模板物理行号一致，用于列表按原 Excel 顺序展示） */
    private Integer importRowNo;

    /** 录入用户名（非数据库字段，查询时按 creatorId 填充） */
    @TableField(exist = false)
    private String creatorUsername;

    /** 转出复制来源患者ID（接收方患者记录） */
    private Long sourcePatientId;

    /** 患者通知单状态（非数据库字段，查询时填充）：0草稿 1已发送 2已确认 null无通知单 */
    @TableField(exist = false)
    private Integer noticeStatus;

    /** 患者通知单ID（非数据库字段，查询时填充） */
    @TableField(exist = false)
    private Long noticeId;

    /** 是否已填写首次随访（非数据库字段，查询时填充） */
    @TableField(exist = false)
    private Boolean hasFirstVisit;

    /** 首次随访状态（非数据库字段）：0草稿 1已完成 null无记录 */
    @TableField(exist = false)
    private Integer firstVisitStatus;

    /** 当前用户是否可编辑已完成首次随访（非数据库字段，五级用户 10 天限制） */
    @TableField(exist = false)
    private Boolean firstVisitEditable;

    /** 胸片检查日期（非数据库字段，从筛查表关联填充） */
    @TableField(exist = false)
    private LocalDate chestXrayDate;

    /** 胸片检查结果（非数据库字段，从筛查表关联填充） */
    @TableField(exist = false)
    private String chestXrayResult;

    /** 感染筛查日期（非数据库字段，从筛查表关联填充） */
    @TableField(exist = false)
    private LocalDate screenDate;

    /** 感染检查方法（非数据库字段，从筛查表关联填充） */
    @TableField(exist = false)
    private String screenMethod;

    /** 感染筛查结果（非数据库字段，从筛查表关联填充） */
    @TableField(exist = false)
    private String infectionResult;

    /** 人群分类（非数据库字段，专病网导入时从 epidemicData 解析） */
    @TableField(exist = false)
    private String crowdCategory;

    /** 现管单位（非数据库字段，专病网导入时从 epidemicData 解析） */
    @TableField(exist = false)
    private String currentManagementUnit;

    /** 治疗分类（非数据库字段，专病网导入时从 epidemicData 解析） */
    @TableField(exist = false)
    private String treatmentClass;

    /** 导入 Excel 全部字段（非数据库字段，从 epidemicData 解析） */
    @TableField(exist = false)
    private Map<String, String> importFields;

    /** 通知单发送时间（非数据库字段，列表查询时填充） */
    @TableField(exist = false)
    private LocalDateTime noticeSentTime;

    /** 通知单接收时间（非数据库字段，列表查询时填充） */
    @TableField(exist = false)
    private LocalDateTime noticeConfirmedTime;

    /** 通知单服药管理单位（非数据库字段，列表查询时填充） */
    @TableField(exist = false)
    private String noticeMedicationUnit;

    /** 通知单备注（非数据库字段，列表查询时填充） */
    @TableField(exist = false)
    private String noticeRemark;

    /** 登记号（非数据库字段，专病网/大疫情导入时从 epidemicData 解析） */
    @TableField(exist = false)
    private String registrationNo;

    /** 领药次数（非数据库字段，列表查询时填充） */
    @TableField(exist = false)
    private Integer medicationPickupCount;

    /** 最近领药时间（非数据库字段，列表查询时填充） */
    @TableField(exist = false)
    private String medicationPickTime;

    /** 最近领药-药品摘要（非数据库字段，列表查询时填充） */
    @TableField(exist = false)
    private String medicationChemotherapy;

    /** 最近领药-数量摘要（非数据库字段，列表查询时填充） */
    @TableField(exist = false)
    private String medicationDrugForm;

    /** 停止治疗原因（来自后续随访，非数据库字段） */
    @TableField(exist = false)
    private String stopTreatmentReason;

    /** 停止治疗原因-其它（来自后续随访，非数据库字段） */
    @TableField(exist = false)
    private String stopTreatmentReasonOther;
}
