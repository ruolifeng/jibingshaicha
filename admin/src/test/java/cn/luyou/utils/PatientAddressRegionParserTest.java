package cn.luyou.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PatientAddressRegionParserTest {

    @Test
    void parseTownNameContainingShi() {
        PatientAddressRegionParser.ParsedRegion region = PatientAddressRegionParser.parse(
                "四川省自贡市富顺县狮市镇何家村一组14号");
        assertEquals("富顺县", region.county());
        assertEquals("狮市镇", region.township());
    }

    @Test
    void parseWithoutProvincePrefix() {
        PatientAddressRegionParser.ParsedRegion region = PatientAddressRegionParser.parse(
                "富顺县狮市镇何家村一组14号");
        assertEquals("富顺县", region.county());
        assertEquals("狮市镇", region.township());
    }

    @Test
    void parseQilongTownAndDistrictStreet() {
        assertEquals("骑龙镇", PatientAddressRegionParser.extractTownship("自贡市富顺县骑龙镇某某村"));
        PatientAddressRegionParser.ParsedRegion street = PatientAddressRegionParser.parse(
                "自贡市大安区凤凰街道某某社区");
        assertEquals("大安区", street.county());
        assertEquals("凤凰街道", street.township());
    }

    @Test
    void countyOnlyDoesNotFallbackToCountyAsTownship() {
        PatientAddressRegionParser.ParsedRegion region = PatientAddressRegionParser.parse("四川省自贡市富顺县");
        assertEquals("富顺县", region.county());
        assertNull(region.township());
        assertNull(PatientAddressRegionParser.extractTownship("四川省自贡市富顺县"));
    }
}
