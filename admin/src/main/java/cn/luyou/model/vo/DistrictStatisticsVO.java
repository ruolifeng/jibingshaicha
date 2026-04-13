package cn.luyou.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistrictStatisticsVO {
    private String district;
    private Long actualScreenCount;
    private Long closeContactCount;
    private Long suspiciousSymptomCount;
    private Long chestXrayCount;
    private Long chestXrayAbnormalCount;
    private Long ppdTestCount;
    private Long ppdPositive1;
    private Long ppdPositive2;
    private Long ppdPositive3;
    private Long ppdPositiveTotal;
    private Long ecNegative;
    private Long ecPositive;
    private Long igraPositive;
    private Long igraNegative;
    private Long tbPatientCount;
    private String remark;
}
