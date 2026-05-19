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
@TableName("epidemic_import")
public class EpidemicImport extends BaseEntity {

    private String name;
    private String idNumber;
    private String gender;
    private LocalDate birthDate;
    private Integer age;
    private String phone;
    private String currentAddress;
    private String caseCategory;
    private String diseaseName;
    private String reportUnit;

    /** 0待追踪 1到位 2未到位 3其他 4强制结束 */
    private Integer trackingStatus;
    private Integer notInPlaceCount;
    private String trackingRemark;

    private String hasChestXray;
    private LocalDate chestXrayDate;
    private String chestXrayResult;

    /** 排除/疑似肺结核/潜伏感染者/确诊患者/其他 */
    private String diagnosisResult;
    private LocalDateTime diagnosisTime;

    private Integer archived;
    private Long targetPatientId;
    private Long targetLatentId;

    private String uploadBatch;
    private Long departmentId;
    private Long creatorId;
}

