package cn.luyou.service;

import cn.luyou.model.vo.DistrictStatisticsVO;
import cn.luyou.model.vo.KeyPopulationTbSymptomReferralStatisticsVO;
import cn.luyou.model.vo.SchoolStatisticsVO;
import cn.luyou.model.vo.StudentReportStatisticsVO;

import java.util.List;

public interface StatisticsService {

    List<SchoolStatisticsVO> getSchoolStatistics(String year, String district, List<Long> filterDeptIds);

    List<DistrictStatisticsVO> getDistrictStatistics(String year, String district, List<Long> filterDeptIds);

    /**
     * 新生入学体检结核病检查情况（学生报表）。
     *
     * @param schoolCategories 学校分类标签（可多选）；空则返回全部五类
     */
    List<StudentReportStatisticsVO> getStudentReportStatistics(
            String year, String district, List<String> schoolCategories, List<Long> filterDeptIds);

    /** 获取筛查数据中所有存在的区县列表（去重排序），用于前端筛选下拉框 */
    List<String> getDistrictOptions(List<Long> filterDeptIds);

    /**
     * 重点人群症状筛查推介报表：地区筛选项（区县 + 乡镇/社区，不含学校筛查区县）。
     */
    List<String> getKeyPopulationRegionOptions(List<Long> filterDeptIds);

    /**
     * 重点人群肺结核可疑症状筛查和推介情况报表（按区县汇总）。
     */
    List<KeyPopulationTbSymptomReferralStatisticsVO> getKeyPopulationTbSymptomReferralStatistics(
            String year, String district, List<Long> filterDeptIds, List<Long> selectedDeptIds);
}
