package cn.luyou.model;

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
}
