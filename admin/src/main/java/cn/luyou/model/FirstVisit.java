package cn.luyou.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("first_visit")
public class FirstVisit extends BaseEntity {

    private Long patientId;
    private String populationType;
    /** 随访时间 */
    private LocalDate visitDate;
    /** 随访方式：门诊/家庭 */
    private String visitMethod;
    /** 患者类型：初治/复治 */
    private String patientType;
    /** 痰菌情况：阳性/阴性/未查痰 */
    private String sputumStatus;
    /** 耐药情况：耐药/非耐药/未检测 */
    private String drugResistance;
    /** 症状及体征（逗号分隔编号） */
    private String symptoms;
    private String otherSymptoms;
    /** 化疗方案 */
    private String chemotherapy;
    /** 用法：每日/间歇 */
    private String medicationUsage;
    /** 药品剂型（逗号分隔） */
    private String drugForm;
    /** 督导人员 */
    private String supervisor;
    /** 单独的居室：有/无 */
    private String separateRoom;
    /** 通风情况：良好/一般/差 */
    private String ventilation;
    private String smokingAmount;
    private String drinkingAmount;
    private String medicationLocation;
    private String medicationPickTime;
    /** 健康教育培训各项掌握情况（JSON字符串） */
    private String educationItems;
    private LocalDate nextVisitDate;
    private String doctorSignature;
    private Long filledBy;
}
