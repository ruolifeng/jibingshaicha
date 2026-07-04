package cn.luyou.utils;

import java.time.LocalDate;

/**
 * 统计年度周期：自然年 1 月 1 日 — 12 月 31 日。
 * statYear 即自然年（如 2026 年度 = 2026-01-01 ~ 2026-12-31）。
 * <p>
 * 年度内纳入统计的患者（按登记/创建时间归属该年），其后续指标（如治疗成功）
 * 可在次年完成，仍计入该年度分母/分子。
 */
public record StatYearPeriod(LocalDate start, LocalDate end, int statYear) {

    public static StatYearPeriod of(int statYear) {
        return new StatYearPeriod(
                LocalDate.of(statYear, 1, 1),
                LocalDate.of(statYear, 12, 31),
                statYear
        );
    }

    /** 按当前日期推断所处统计年度（自然年） */
    public static StatYearPeriod current() {
        return of(LocalDate.now().getYear());
    }
}
