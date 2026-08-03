package cn.luyou.service;

import cn.luyou.model.vo.DistrictStatisticsVO;
import cn.luyou.model.vo.KeyPopulationTbSymptomReferralStatisticsVO;
import cn.luyou.model.vo.SchoolStatisticsVO;

import java.util.List;

public interface StatisticsService {

    List<SchoolStatisticsVO> getSchoolStatistics(String year, String district, List<Long> filterDeptIds);

    List<DistrictStatisticsVO> getDistrictStatistics(String year, String district, List<Long> filterDeptIds);

    /** 获取筛查数据中所有存在的区县列表（去重排序），用于前端筛选下拉框 */
    List<String> getDistrictOptions(List<Long> filterDeptIds);

    /**
     * 重点人群肺结核可疑症状筛查和推介情况报表（按区县汇总）。
     */
    List<KeyPopulationTbSymptomReferralStatisticsVO> getKeyPopulationTbSymptomReferralStatistics(
            String year, String district, List<Long> filterDeptIds);
}
