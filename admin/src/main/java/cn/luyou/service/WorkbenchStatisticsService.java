package cn.luyou.service;

import java.util.Map;

public interface WorkbenchStatisticsService {

    /**
     * 我的工作台年度统计（统计周期：自然年 1/1—12/31）。
     *
     * @param statYear 统计年度，null 时取当前所处统计年度
     */
    Map<String, Object> buildSummary(Integer statYear);
}
