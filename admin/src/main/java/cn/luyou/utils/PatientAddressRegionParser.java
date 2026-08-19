package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从患者现住址解析县级、乡级行政区划。
 * <p>
 * 按「省 → 地级市 → 区县 → 乡镇」从左到右切分，避免把「狮市镇」里的「市」
 * 误当成地级市前缀，导致只解析到县、解析不到镇。
 */
public final class PatientAddressRegionParser {

    /**
     * group1=县级，group2=乡级（可空）。<br>
     * 地级市前缀不允许跨越「县/区/旗」，因此「富顺县狮市镇」不会把「狮市」剥掉。
     */
    private static final Pattern REGION_PATTERN = Pattern.compile(
            "^(?:.+?省)?"
                    + "(?:[^县区旗]+?市)?"
                    + "(.+?(?:自治县|县级市|县|区|旗))"
                    + "(?:(.+?(?:街道办事处|民族乡|街道|镇|乡)))?");

    private PatientAddressRegionParser() {
    }

    public record ParsedRegion(String county, String township) {
        public boolean hasCounty() {
            return StrUtil.isNotBlank(county);
        }

        public boolean hasTownship() {
            return StrUtil.isNotBlank(township);
        }
    }

    public static ParsedRegion parse(String rawAddress) {
        String address = normalize(rawAddress);
        if (StrUtil.isBlank(address)) {
            return new ParsedRegion(null, null);
        }

        Matcher matcher = REGION_PATTERN.matcher(address);
        if (!matcher.find()) {
            return new ParsedRegion(null, null);
        }
        String county = blankToNull(matcher.group(1));
        String township = blankToNull(matcher.group(2));
        return new ParsedRegion(county, township);
    }

    /** 仅解析乡级，解析不到时返回 null（不再回退成县名） */
    public static String extractTownship(String rawAddress) {
        return parse(rawAddress).township();
    }

    /** 去掉空白，便于按省市区划切分 */
    public static String normalize(String rawAddress) {
        if (StrUtil.isBlank(rawAddress)) {
            return "";
        }
        return rawAddress.replace('\u00A0', ' ').replaceAll("\\s+", "").trim();
    }

    private static String blankToNull(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
