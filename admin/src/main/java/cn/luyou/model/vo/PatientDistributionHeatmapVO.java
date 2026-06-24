package cn.luyou.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 患者分布热力图数据（区县 × 社区/街道）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientDistributionHeatmapVO {

    private Integer managementYear;
    private String statPeriodFrom;
    private String statPeriodTo;
    /** 患者总数 */
    private Integer total;
    /** 单格最大患者数（用于色阶） */
    private Integer maxCount;
    /** 行：区县 */
    private List<String> rowLabels;
    /** 列：社区序号（具体名称见 data 中 community 字段） */
    private List<String> colLabels;
    /** 热力点：[列索引, 行索引, 患者数, 区县名, 社区名] */
    private List<List<Object>> data;
}
