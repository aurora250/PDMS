package com.pdm.common.core.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("身份证号校验器 — GB 11643-1999")
class IdCardValidatorTest {

    // Valid test IDs computed with correct check digits
    private static final String VALID_MALE_1990 = "110101199003076632";
    private static final String VALID_FEMALE_1995 = "440305199512120016";
    private static final String VALID_FEMALE_1988 = "320102198807020029";
    private static final String VALID_MALE_X = "11010119900307221X";
    private static final String VALID_MALE_17TH_1 = "110101199003016613";
    private static final String VALID_FEM_17TH_2 = "110101199003026627";

    @Test
    @DisplayName("合法的18位身份证号应通过校验")
    void shouldPassValidIdCard() {
        assertTrue(IdCardValidator.isValid(VALID_MALE_1990));
        assertTrue(IdCardValidator.isValid(VALID_FEMALE_1995));
        assertTrue(IdCardValidator.isValid(VALID_FEMALE_1988));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "12345", "11010119900307663", "11010119900307663XA", "abcdefghijklmnopqr" })
    @DisplayName("非法身份证号应校验失败")
    void shouldFailInvalidIdCard(String idCardNo) {
        assertFalse(IdCardValidator.isValid(idCardNo));
    }

    @Test
    @DisplayName("校验码错误应返回false")
    void shouldFailWrongCheckCode() {
        // Correct check digit is '2', use wrong ones
        assertFalse(IdCardValidator.isValid("110101199003076634"));
        assertFalse(IdCardValidator.isValid("110101199003076635"));
    }

    @Test
    @DisplayName("自动提取出生日期")
    void shouldExtractBirthDate() {
        assertEquals(LocalDate.of(1990, 3, 7), IdCardValidator.extractBirthDate(VALID_MALE_1990));
        assertEquals(LocalDate.of(1995, 12, 12), IdCardValidator.extractBirthDate(VALID_FEMALE_1995));
        assertEquals(LocalDate.of(1988, 7, 2), IdCardValidator.extractBirthDate(VALID_FEMALE_1988));
    }

    @Test
    @DisplayName("非法号码提取出生日期应抛异常")
    void shouldThrowOnInvalidForBirthDate() {
        assertThrows(IllegalArgumentException.class, () -> IdCardValidator.extractBirthDate("12345"));
    }

    @Test
    @DisplayName("性别提取 — 第17位奇数→男，偶数→女")
    void shouldExtractGender() {
        assertEquals("男", IdCardValidator.extractGender(VALID_MALE_17TH_1));
        assertEquals("女", IdCardValidator.extractGender(VALID_FEM_17TH_2));
    }

    @Test
    @DisplayName("边界 — 末位X大小写兼容")
    void shouldHandleCaseInsensitiveX() {
        assertTrue(IdCardValidator.isValid(VALID_MALE_X));
        assertTrue(IdCardValidator.isValid(VALID_MALE_X.toLowerCase()));
    }
}
