package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 自贡市各区县下辖乡镇/街道名录（用于热力图补全 0 值区域） */
public final class ZigongTownshipCatalog {

    private static final Map<String, List<String>> TOWNSHIPS_BY_DISTRICT = load();

    private ZigongTownshipCatalog() {
    }

    public static List<String> getTownships(String districtName) {
        if (StrUtil.isBlank(districtName)) {
            return List.of();
        }
        for (Map.Entry<String, List<String>> entry : TOWNSHIPS_BY_DISTRICT.entrySet()) {
            if (districtNamesMatch(entry.getKey(), districtName)) {
                return entry.getValue();
            }
        }
        return List.of();
    }

    private static Map<String, List<String>> load() {
        try {
            ClassPathResource resource = new ClassPathResource("geo/zigong-townships.json");
            String json = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JSONObject obj = JSONUtil.parseObj(json);
            Map<String, List<String>> map = new LinkedHashMap<>();
            for (String key : obj.keySet()) {
                map.put(key, obj.getBeanList(key, String.class));
            }
            return Collections.unmodifiableMap(map);
        } catch (IOException e) {
            return Map.of();
        }
    }

    private static boolean districtNamesMatch(String a, String b) {
        if (StrUtil.isBlank(a) || StrUtil.isBlank(b)) {
            return false;
        }
        String na = normalizeDistrictLabel(a);
        String nb = normalizeDistrictLabel(b);
        if (na.equals(nb)) {
            return true;
        }
        return stripDistrictAdminSuffix(na).equals(stripDistrictAdminSuffix(nb));
    }

    private static String normalizeDistrictLabel(String name) {
        return name.trim().replace("自贡市", "").replaceAll("\\s+", "");
    }

    private static String stripDistrictAdminSuffix(String name) {
        if (name.endsWith("区") || name.endsWith("县")) {
            return name.substring(0, name.length() - 1);
        }
        return name;
    }
}
