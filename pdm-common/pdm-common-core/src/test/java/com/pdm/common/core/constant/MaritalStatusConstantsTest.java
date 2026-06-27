package com.pdm.common.core.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("婚姻状况代码常量 - GB/T 2261.2-2003")
class MaritalStatusConstantsTest {

    @Test
    @DisplayName("验证8个婚姻状况代码完整性")
    void shouldHave8MaritalStatuses() {
        assertEquals(8, MaritalStatusConstants.getAllStatuses().size());
    }

    @Test
    @DisplayName("验证未婚代码10")
    void shouldValidateUnmarriedCode() {
        assertTrue(MaritalStatusConstants.isValidCode("10"));
        assertEquals("未婚", MaritalStatusConstants.getNameByCode("10"));
        assertEquals("10", MaritalStatusConstants.getCodeByName("未婚"));
    }

    @Test
    @DisplayName("验证已婚代码20")
    void shouldValidateMarriedCode() {
        assertTrue(MaritalStatusConstants.isValidCode("20"));
        assertEquals("已婚", MaritalStatusConstants.getNameByCode("20"));
    }

    @Test
    @DisplayName("验证初婚代码21")
    void shouldValidateFirstMarriageCode() {
        assertTrue(MaritalStatusConstants.isValidCode("21"));
        assertEquals("初婚", MaritalStatusConstants.getNameByCode("21"));
    }

    @Test
    @DisplayName("验证再婚代码22")
    void shouldValidateRemarriageCode() {
        assertTrue(MaritalStatusConstants.isValidCode("22"));
        assertEquals("再婚", MaritalStatusConstants.getNameByCode("22"));
    }

    @Test
    @DisplayName("验证复婚代码23")
    void shouldValidateReunionCode() {
        assertTrue(MaritalStatusConstants.isValidCode("23"));
        assertEquals("复婚", MaritalStatusConstants.getNameByCode("23"));
    }

    @Test
    @DisplayName("验证丧偶代码30")
    void shouldValidateWidowedCode() {
        assertTrue(MaritalStatusConstants.isValidCode("30"));
        assertEquals("丧偶", MaritalStatusConstants.getNameByCode("30"));
    }

    @Test
    @DisplayName("验证离婚代码40")
    void shouldValidateDivorcedCode() {
        assertTrue(MaritalStatusConstants.isValidCode("40"));
        assertEquals("离婚", MaritalStatusConstants.getNameByCode("40"));
    }

    @Test
    @DisplayName("验证未说明代码90")
    void shouldValidateUnspecifiedCode() {
        assertTrue(MaritalStatusConstants.isValidCode("90"));
        assertEquals("未说明的婚姻状况", MaritalStatusConstants.getNameByCode("90"));
    }

    @Test
    @DisplayName("非法代码应返回false")
    void shouldRejectInvalidCode() {
        assertFalse(MaritalStatusConstants.isValidCode("00"));
        assertFalse(MaritalStatusConstants.isValidCode("50"));
        assertFalse(MaritalStatusConstants.isValidCode("99"));
        assertFalse(MaritalStatusConstants.isValidCode(null));
    }

    @Test
    @DisplayName("非法名称应返回false")
    void shouldRejectInvalidName() {
        assertFalse(MaritalStatusConstants.isValidName("不存在的状况"));
        assertFalse(MaritalStatusConstants.isValidName(null));
    }

    @Test
    @DisplayName("双向映射一致性")
    void shouldMaintainBidirectionalMapping() {
        for (String code : MaritalStatusConstants.getAllStatuses().keySet()) {
            String name = MaritalStatusConstants.getNameByCode(code);
            assertEquals(code, MaritalStatusConstants.getCodeByName(name));
        }
    }
}
