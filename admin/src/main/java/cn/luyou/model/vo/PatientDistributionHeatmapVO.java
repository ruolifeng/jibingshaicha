package cn.luyou.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 患者分布地图热力图（自贡市：市级区县 / 区县级乡镇）。
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
    /** 单区域最大患者数（用于色阶） */
    private Integer maxCount;
    /** city=自贡各区县，district=区县下乡镇 */
    private String mapLevel;
    /** 下钻区县名称（mapLevel=district 时有值） */
    private String districtName;
    /** 下钻区县 adcode（mapLevel=district 时有值） */
    private String districtAdcode;
    /** 地图区域统计 */
    private List<MapRegion> regions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MapRegion {
        /** 区域名称（区县名或乡镇/社区名） */
        private String name;
        /** 患者数 */
        private Integer value;
        /** 行政区划 adcode（市级区县时有值） */
        private String adcode;
    }
}
