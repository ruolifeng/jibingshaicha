package cn.luyou.utils;

import java.time.LocalDate;

/**
 * 统计年度周期：上年度 12 月 1 日 — 本年度 11 月 30 日。
 * statYear 取周期结束日所在自然年（如 2026 年度 = 2025-12-01 ~ 2026-11-30）。
 */
public record StatYearPeriod(LocalDate start, LocalDate end, int statYear) {

    public static StatYearPeriod of(int statYear) {
        return new StatYearPeriod(
                LocalDate.of(statYear - 1, 12, 1),
                LocalDate.of(statYear, 11, 30),
                statYear
        );
    }

    /** 按当前日期推断所处统计年度（12 月 1 日起进入下一统计年度） */
    public static StatYearPeriod current() {
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        LocalDate nov30 = LocalDate.of(year, 11, 30);
        if (!today.isAfter(nov30)) {
            return of(year);
        }
        return of(year + 1);
    }
}
