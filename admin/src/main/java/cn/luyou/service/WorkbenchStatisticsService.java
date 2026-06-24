package cn.luyou.service;

import java.util.Map;

public interface WorkbenchStatisticsService {

    /**
     * 我的工作台年度统计（统计周期：上年度 12/1—本年度 11/30）。
     *
     * @param statYear 统计年度，null 时取当前所处统计年度
     */
    Map<String, Object> buildSummary(Integer statYear);
}
