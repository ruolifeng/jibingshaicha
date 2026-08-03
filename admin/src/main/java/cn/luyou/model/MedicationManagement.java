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
@TableName("medication_management")
public class MedicationManagement extends BaseEntity {

    /** 关联患者ID（潜伏感染服药记录为空） */
    private Long patientId;
    /** 关联潜伏感染者ID（患者服药记录为空） */
    private Long latentInfectionId;
    private String populationType;
    private String managementMethod;
    private String supervisor;
    private String sputumResult;
    /** 开始治疗日期（治疗记录卡，首次标记服药日自动生成，可手改） */
    private LocalDate startTreatmentDate;
    /** 每日服药记录（JSON） */
    private String medicationRecords;
    private LocalDate stopDate;
}
