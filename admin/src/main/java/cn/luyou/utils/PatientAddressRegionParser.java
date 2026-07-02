package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从患者现住址解析县级、乡级行政区划。
 * <p>
 * 县级：市辖区、县、县级市、自治县、旗（以「区/县/旗」等后缀识别）<br>
 * 乡级：镇、乡、民族乡、街道办事处（简写「街道」）
 */
public final class PatientAddressRegionParser {

    /** 县级，优先匹配较长后缀 */
    private static final Pattern COUNTY_PATTERN = Pattern.compile(
            "^(.+?(?:自治县|县级市|县|区|旗))");

    /** 乡级，优先匹配较长后缀 */
    private static final Pattern TOWNSHIP_PATTERN = Pattern.compile(
            "^(.+?(?:街道办事处|民族乡|街道|镇|乡))");

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

        Matcher countyMatcher = COUNTY_PATTERN.matcher(address);
        if (!countyMatcher.find()) {
            return new ParsedRegion(null, null);
        }

        String county = countyMatcher.group(1).trim();
        String remainder = address.substring(countyMatcher.end()).trim();
        if (StrUtil.isBlank(remainder)) {
            return new ParsedRegion(county, null);
        }

        Matcher townshipMatcher = TOWNSHIP_PATTERN.matcher(remainder);
        if (!townshipMatcher.find()) {
            return new ParsedRegion(county, null);
        }

        return new ParsedRegion(county, townshipMatcher.group(1).trim());
    }

    /** 去掉省、市前缀，便于从「区县+乡镇」起始解析 */
    public static String normalize(String rawAddress) {
        if (StrUtil.isBlank(rawAddress)) {
            return "";
        }
        String normalized = rawAddress.trim()
                .replaceAll("\\s+", "");
        normalized = normalized.replaceFirst("^[^省\\s]+省", "");
        normalized = normalized.replaceFirst("^[^市\\s]+市", "");
        normalized = normalized.replaceFirst("^自贡市", "");
        return normalized.trim();
    }
}
