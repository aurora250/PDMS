package com.pdm.common.core.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("学历代码常量 - GB/T 4658-2006")
class EducationConstantsTest {

    @Test
    @DisplayName("验证10个学历代码完整性")
    void shouldHave10EducationLevels() {
        assertEquals(10, EducationConstants.getAllEducations().size());
    }

    @Test
    @DisplayName("验证研究生代码10")
    void shouldValidateGraduateCode() {
        assertTrue(EducationConstants.isValidCode("10"));
        assertEquals("研究生", EducationConstants.getNameByCode("10"));
        assertEquals("10", EducationConstants.getCodeByName("研究生"));
    }

    @Test
    @DisplayName("验证大学本科代码20")
    void shouldValidateBachelorCode() {
        assertTrue(EducationConstants.isValidCode("20"));
        assertEquals("大学本科", EducationConstants.getNameByCode("20"));
    }

    @Test
    @DisplayName("验证小学代码80")
    void shouldValidatePrimarySchoolCode() {
        assertTrue(EducationConstants.isValidCode("80"));
        assertEquals("小学", EducationConstants.getNameByCode("80"));
    }

    @Test
    @DisplayName("验证文盲或半文盲代码90")
    void shouldValidateIlliterateCode() {
        assertTrue(EducationConstants.isValidCode("90"));
        assertEquals("文盲或半文盲", EducationConstants.getNameByCode("90"));
    }

    @Test
    @DisplayName("验证未知代码99")
    void shouldValidateUnknownCode() {
        assertTrue(EducationConstants.isValidCode("99"));
        assertEquals("未知", EducationConstants.getNameByCode("99"));
    }

    @Test
    @DisplayName("非法代码应返回false")
    void shouldRejectInvalidCode() {
        assertFalse(EducationConstants.isValidCode("00"));
        assertFalse(EducationConstants.isValidCode("15"));
        assertFalse(EducationConstants.isValidCode("100"));
        assertFalse(EducationConstants.isValidCode(null));
    }

    @Test
    @DisplayName("非法名称应返回false")
    void shouldRejectInvalidName() {
        assertFalse(EducationConstants.isValidName("不存在的学历"));
        assertFalse(EducationConstants.isValidName(null));
    }

    @Test
    @DisplayName("双向映射一致性")
    void shouldMaintainBidirectionalMapping() {
        for (String code : EducationConstants.getAllEducations().keySet()) {
            String name = EducationConstants.getNameByCode(code);
            assertEquals(code, EducationConstants.getCodeByName(name));
        }
    }
}
