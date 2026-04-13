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

    private Long patientId;
    private String populationType;
    private String managementMethod;
    private String supervisor;
    private String sputumResult;
    /** 每日服药记录（JSON） */
    private String medicationRecords;
    private LocalDate stopDate;
}
