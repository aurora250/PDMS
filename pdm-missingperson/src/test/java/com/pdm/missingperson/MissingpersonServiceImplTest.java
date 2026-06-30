package com.pdm.missingperson;

import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;
import com.pdm.common.dto.PageRequest;
import com.pdm.missingperson.entity.MissingPerson;
import com.pdm.missingperson.entity.MissingPersonRecovery;
import com.pdm.missingperson.mapper.MissingPersonMapper;
import com.pdm.missingperson.mapper.MissingPersonRecoveryMapper;
import com.pdm.missingperson.service.impl.MissingpersonServiceImpl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("失踪人口服务 — 单元测试")
class MissingpersonServiceImplTest {

    @Mock
    private MissingPersonMapper missingPersonMapper;
    @Mock
    private MissingPersonRecoveryMapper recoveryMapper;
    @InjectMocks
    private MissingpersonServiceImpl missingpersonService;

    private MissingPerson testMissing;

    @BeforeEach
    void setUp() {
        testMissing = new MissingPerson();
        testMissing.setRid(101L);
        testMissing.setResidentUuid("00000000-0000-0000-0000-000000000001");
        testMissing.setMissingDate(LocalDate.of(2026, 6, 1));
        testMissing.setMissingPlace("北京市朝阳区");
        testMissing.setStatus("失踪中");
    }

    @Nested
    @DisplayName("登记失踪人员")
    class RegisterMissing {

        @Test
        @DisplayName("正常登记失踪人员，默认状态为失踪中")
        void shouldRegisterMissingPerson() {
            MissingPerson mp = new MissingPerson();
            mp.setResidentUuid("00000000-0000-0000-0000-000000000002");
            mp.setMissingDate(LocalDate.of(2026, 6, 15));
            mp.setMissingPlace("上海市浦东新区");

            MissingPerson result = missingpersonService.register(mp);

            assertNotNull(result);
            assertEquals("失踪中", result.getStatus());
            verify(missingPersonMapper).insert(mp);
        }
    }

    @Nested
    @DisplayName("撤销失踪登记")
    class CancelMissing {

        @Test
        @DisplayName("正常撤销失踪记录")
        void shouldCancelMissingRecord() {
            when(missingPersonMapper.selectById(101L)).thenReturn(testMissing);

            missingpersonService.cancel(101L);

            verify(missingPersonMapper).deleteById(101L);
        }

        @Test
        @DisplayName("失踪记录不存在应抛异常")
        void shouldThrowWhenRecordNotFound() {
            when(missingPersonMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class, () -> missingpersonService.cancel(999L));
            assertEquals(ErrorCode.MISSING_PERSON_NOT_FOUND.getCode(), ex.getCode());
            verify(missingPersonMapper, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("记录寻回")
    class RecordRecovery {

        @Test
        @DisplayName("正常记录寻回")
        void shouldRecordRecovery() {
            MissingPersonRecovery recovery = new MissingPersonRecovery();
            recovery.setMissingRecordRid(101L);
            recovery.setRecoveryDate(LocalDate.of(2026, 7, 1));
            recovery.setSummary("公安机关在廊坊市解救");

            when(missingPersonMapper.selectById(101L)).thenReturn(testMissing);

            MissingPersonRecovery result = missingpersonService.recordRecovery(recovery);

            assertNotNull(result);
            verify(recoveryMapper).insert(recovery);
            assertEquals("已经寻回", testMissing.getStatus());
            verify(missingPersonMapper).updateById(testMissing);
        }

        @Test
        @DisplayName("失踪记录不存在应抛异常")
        void shouldThrowWhenRecordNotFound() {
            MissingPersonRecovery recovery = new MissingPersonRecovery();
            recovery.setMissingRecordRid(999L);

            when(missingPersonMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> missingpersonService.recordRecovery(recovery));
            assertEquals(ErrorCode.MISSING_PERSON_NOT_FOUND.getCode(), ex.getCode());
            verify(recoveryMapper, never()).insert(any(MissingPersonRecovery.class));
        }

        @Test
        @DisplayName("已寻回的人员不可重复寻回")
        void shouldRejectDoubleRecovery() {
            testMissing.setStatus("已经寻回");
            MissingPersonRecovery recovery = new MissingPersonRecovery();
            recovery.setMissingRecordRid(101L);

            when(missingPersonMapper.selectById(101L)).thenReturn(testMissing);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> missingpersonService.recordRecovery(recovery));
            assertEquals(ErrorCode.MISSING_PERSON_ALREADY_RECOVERED.getCode(), ex.getCode());
            verify(recoveryMapper, never()).insert(any(MissingPersonRecovery.class));
        }
    }

    @Nested
    @DisplayName("搜索失踪人员")
    class SearchMissing {

        @Test
        @DisplayName("按residentUuid搜索")
        void shouldSearchByResidentUuid() {
            Page<MissingPerson> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(testMissing));
            mockPage.setTotal(1);

            when(missingPersonMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            var result = missingpersonService.search("00000000-0000-0000-0000-000000000001", null, null,
                    buildPageRequest(1, 20));

            assertNotNull(result);
            assertEquals(1, result.getTotal());
        }

        @Test
        @DisplayName("按状态筛选")
        void shouldFilterByStatus() {
            Page<MissingPerson> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(testMissing));
            mockPage.setTotal(1);

            when(missingPersonMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            var result = missingpersonService.search(null, "失踪中", null, buildPageRequest(1, 20));

            assertNotNull(result);
            assertEquals(1, result.getTotal());
        }

        @Test
        @DisplayName("无匹配结果返回空")
        void shouldReturnEmptyOnNoMatch() {
            Page<MissingPerson> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of());
            mockPage.setTotal(0);

            when(missingPersonMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            var result = missingpersonService.search("nonexistent", null, null, buildPageRequest(1, 20));

            assertEquals(0, result.getTotal());
            assertTrue(result.getRecords().isEmpty());
        }
    }

    @Nested
    @DisplayName("失踪人口统计")
    class GetStatistics {

        @Test
        @DisplayName("获取统计概览")
        void shouldGetStatistics() {
            Map<String, Object> result = missingpersonService.getStatistics();

            assertNotNull(result);
            assertTrue(result.containsKey("totalCount"));
            assertTrue(result.containsKey("missingCount"));
            assertTrue(result.containsKey("recoveredCount"));
            assertTrue(result.containsKey("byGender"));
            assertTrue(result.containsKey("byAgeGroup"));
        }
    }

    /** Helper to create PageRequest (no 2-arg constructor exists). */
    private static PageRequest buildPageRequest(int page, int size) {
        return new PageRequest(page, size, null, null);
    }
}
