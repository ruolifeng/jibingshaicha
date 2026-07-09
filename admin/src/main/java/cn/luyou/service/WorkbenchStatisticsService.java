package cn.luyou.service;

import java.util.List;
import java.util.Map;

public interface WorkbenchStatisticsService {

    /**
     * 我的工作台年度统计（统计周期：自然年 1/1—12/31）。
     *
     * @param statYear 统计年度，null 时取当前所处统计年度
     * @param filterDeptIds 可选部门筛选（已展开并与辖区取交集），null 表示不限
     */
    Map<String, Object> buildSummary(Integer statYear, List<Long> filterDeptIds);
}
