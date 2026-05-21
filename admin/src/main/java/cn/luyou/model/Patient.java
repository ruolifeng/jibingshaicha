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
    /** 大疫情表额外字段（JSON） */
    private String epidemicData;
    /** 所属部门ID */
    private Long departmentId;

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
}
