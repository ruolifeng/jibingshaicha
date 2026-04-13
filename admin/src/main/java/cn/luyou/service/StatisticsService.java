package cn.luyou.service;

import cn.luyou.model.vo.DistrictStatisticsVO;
import cn.luyou.model.vo.SchoolStatisticsVO;

import java.util.List;

public interface StatisticsService {

    List<SchoolStatisticsVO> getSchoolStatistics(String year, String district);

    List<DistrictStatisticsVO> getDistrictStatistics(String year, String district);
}
