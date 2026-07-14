package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import cn.luyou.model.CloseContactCase;
import cn.luyou.model.ScreeningCloseContact;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 密接 Excel 自动生成列：报表填报季度、随访期限提示、年龄组；并补全 6/12/24 月随访到期日（仅导出/展示，不写库）。
 */
public final class CloseContactCaseExcelDerivedSupport {

    private static final Pattern REPORT_QUARTER_PATTERN = Pattern.compile("^(\\d{4})年Q([1-4])$");

    private CloseContactCaseExcelDerivedSupport() {
    }

    public static void applyAll(List<CloseContactCase> rows) {
        if (rows == null) {
            return;
        }
        rows.forEach(CloseContactCaseExcelDerivedSupport::apply);
    }

    public static void applyAllScreening(List<ScreeningCloseContact> rows) {
        if (rows == null) {
            return;
        }
        rows.forEach(CloseContactCaseExcelDerivedSupport::apply);
    }

    public static void apply(CloseContactCase row) {
        if (row == null) {
            return;
        }
        ensureFollowupDueDates(row);
        fillDerivedFields(
                row.getRegistrationDate(),
                row.getAge(),
                row.getFollowup6DueDate(),
                row.getFollowup6ScreenDate(),
                row.getFollowup12DueDate(),
                row.getFollowup12ScreenDate(),
                row.getFollowup24DueDate(),
                row.getFollowup24ScreenDate(),
                row::setReportQuarter,
                row::setRegistrationIntervalHint,
                row::setAgeGroup
        );
    }

    public static void apply(ScreeningCloseContact row) {
        if (row == null) {
            return;
        }
        ensureFollowupDueDates(row);
        fillDerivedFields(
                row.getRegistrationDate(),
                row.getAge(),
                row.getFollowup6DueDate(),
                row.getFollowup6ScreenDate(),
                row.getFollowup12DueDate(),
                row.getFollowup12ScreenDate(),
                row.getFollowup24DueDate(),
                row.getFollowup24ScreenDate(),
                row::setReportQuarter,
                row::setRegistrationIntervalHint,
                row::setAgeGroup
        );
    }

    /** 报表填报季度，如 2026年Q2；无登记日期时为「登记日期为空」。 */
    public static String resolveReportQuarter(LocalDate registrationDate) {
        if (registrationDate == null) {
            return "登记日期为空";
        }
        int quarter = (registrationDate.getMonthValue() - 1) / 3 + 1;
        return registrationDate.getYear() + "年Q" + quarter;
    }

    /**
     * 将「2026年Q2」解析为登记日期季度起止 [start, end]；无法解析时返回 null。
     */
    public static LocalDate[] resolveReportQuarterDateRange(String reportQuarter) {
        if (StrUtil.isBlank(reportQuarter) || isEmptyRegistrationQuarter(reportQuarter)) {
            return null;
        }
        Matcher matcher = REPORT_QUARTER_PATTERN.matcher(reportQuarter.trim());
        if (!matcher.matches()) {
            return null;
        }
        int year = Integer.parseInt(matcher.group(1));
        int quarter = Integer.parseInt(matcher.group(2));
        LocalDate start = LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
        LocalDate end = start.plusMonths(3).minusDays(1);
        return new LocalDate[]{start, end};
    }

    /** 是否筛选登记日期为空（报表填报季度=登记日期为空）。 */
    public static boolean isEmptyRegistrationQuarter(String reportQuarter) {
        return "登记日期为空".equals(StrUtil.trim(reportQuarter));
    }

    /** 年龄组：密接筛查按 15 岁分界；无有效年龄时为「无有效年龄」。 */
    public static String resolveAgeGroup(Integer age) {
        if (age == null || age < 0) {
            return "无有效年龄";
        }
        return age < 15 ? "<15岁" : "≥15岁";
    }

    /** 登记日至今日的间隔与随访期限提示。 */
    public static String resolveRegistrationIntervalHint(
            LocalDate registrationDate,
            LocalDate followup6DueDate,
            LocalDate followup6ScreenDate,
            LocalDate followup12DueDate,
            LocalDate followup12ScreenDate,
            LocalDate followup24DueDate,
            LocalDate followup24ScreenDate) {
        return resolveRegistrationIntervalHint(registrationDate,
                followup6DueDate, followup6ScreenDate,
                followup12DueDate, followup12ScreenDate,
                followup24DueDate, followup24ScreenDate,
                LocalDate.now());
    }

    static String resolveRegistrationIntervalHint(
            LocalDate registrationDate,
            LocalDate followup6DueDate,
            LocalDate followup6ScreenDate,
            LocalDate followup12DueDate,
            LocalDate followup12ScreenDate,
            LocalDate followup24DueDate,
            LocalDate followup24ScreenDate,
            LocalDate today) {
        if (registrationDate == null) {
            return "登记日期为空";
        }
        long days = ChronoUnit.DAYS.between(registrationDate, today);
        LocalDate due6 = followup6DueDate != null ? followup6DueDate : registrationDate.plusMonths(6);
        LocalDate due12 = followup12DueDate != null ? followup12DueDate : registrationDate.plusMonths(12);
        LocalDate due24 = followup24DueDate != null ? followup24DueDate : registrationDate.plusMonths(24);

        if (followup6ScreenDate == null && !today.isBefore(due6)) {
            return "距登记" + days + "天，6月随访到期";
        }
        if (followup12ScreenDate == null && !today.isBefore(due12)) {
            return "距登记" + days + "天，12月随访到期";
        }
        if (followup24ScreenDate == null && !today.isBefore(due24)) {
            return "距登记" + days + "天，24月随访到期";
        }
        return "距登记" + days + "天，未到随访时间";
    }

    private static void fillDerivedFields(
            LocalDate registrationDate,
            Integer age,
            LocalDate followup6DueDate,
            LocalDate followup6ScreenDate,
            LocalDate followup12DueDate,
            LocalDate followup12ScreenDate,
            LocalDate followup24DueDate,
            LocalDate followup24ScreenDate,
            java.util.function.Consumer<String> reportQuarterSetter,
            java.util.function.Consumer<String> intervalHintSetter,
            java.util.function.Consumer<String> ageGroupSetter) {
        reportQuarterSetter.accept(resolveReportQuarter(registrationDate));
        intervalHintSetter.accept(resolveRegistrationIntervalHint(
                registrationDate,
                followup6DueDate, followup6ScreenDate,
                followup12DueDate, followup12ScreenDate,
                followup24DueDate, followup24ScreenDate));
        ageGroupSetter.accept(resolveAgeGroup(age));
    }

    private static void ensureFollowupDueDates(CloseContactCase row) {
        if (row.getRegistrationDate() == null) {
            return;
        }
        if (row.getFollowup6DueDate() == null) {
            row.setFollowup6DueDate(row.getRegistrationDate().plusMonths(6));
        }
        if (row.getFollowup12DueDate() == null) {
            row.setFollowup12DueDate(row.getRegistrationDate().plusMonths(12));
        }
        if (row.getFollowup24DueDate() == null) {
            row.setFollowup24DueDate(row.getRegistrationDate().plusMonths(24));
        }
    }

    private static void ensureFollowupDueDates(ScreeningCloseContact row) {
        if (row.getRegistrationDate() == null) {
            return;
        }
        if (row.getFollowup6DueDate() == null) {
            row.setFollowup6DueDate(row.getRegistrationDate().plusMonths(6));
        }
        if (row.getFollowup12DueDate() == null) {
            row.setFollowup12DueDate(row.getRegistrationDate().plusMonths(12));
        }
        if (row.getFollowup24DueDate() == null) {
            row.setFollowup24DueDate(row.getRegistrationDate().plusMonths(24));
        }
    }
}
