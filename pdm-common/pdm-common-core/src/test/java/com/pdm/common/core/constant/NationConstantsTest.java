package com.pdm.common.core.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("民族代码常量 - GB 3304-1991")
class NationConstantsTest {

    @Test
    @DisplayName("验证56个民族代码完整性")
    void shouldHave56Nations() {
        // 56个民族 + 2个特殊代码（97其他, 98外籍）
        assertEquals(58, NationConstants.getAllNations().size());
    }

    @Test
    @DisplayName("验证汉族代码01")
    void shouldValidateHanNation() {
        assertTrue(NationConstants.isValidCode("01"));
        assertEquals("汉族", NationConstants.getNameByCode("01"));
        assertEquals("01", NationConstants.getCodeByName("汉族"));
    }

    @Test
    @DisplayName("验证最后一个民族-基诺族代码56")
    void shouldValidateJinuoNation() {
        assertTrue(NationConstants.isValidCode("56"));
        assertEquals("基诺族", NationConstants.getNameByCode("56"));
        assertEquals("56", NationConstants.getCodeByName("基诺族"));
    }

    @Test
    @DisplayName("验证特殊代码-其他97")
    void shouldValidateOtherCode() {
        assertTrue(NationConstants.isValidCode("97"));
        assertEquals("其他", NationConstants.getNameByCode("97"));
    }

    @Test
    @DisplayName("验证特殊代码-外籍98")
    void shouldValidateForeignCode() {
        assertTrue(NationConstants.isValidCode("98"));
        assertEquals("外国血统中国籍人士", NationConstants.getNameByCode("98"));
    }

    @Test
    @DisplayName("非法代码应返回false")
    void shouldRejectInvalidCode() {
        assertFalse(NationConstants.isValidCode("00"));
        assertFalse(NationConstants.isValidCode("57"));
        assertFalse(NationConstants.isValidCode("99"));
        assertFalse(NationConstants.isValidCode(null));
    }

    @Test
    @DisplayName("非法名称应返回false")
    void shouldRejectInvalidName() {
        assertFalse(NationConstants.isValidName("不存在的民族"));
        assertFalse(NationConstants.isValidName(null));
    }

    @Test
    @DisplayName("双向映射一致性")
    void shouldMaintainBidirectionalMapping() {
        for (String code : NationConstants.getAllNations().keySet()) {
            String name = NationConstants.getNameByCode(code);
            assertEquals(code, NationConstants.getCodeByName(name));
        }
    }
}
