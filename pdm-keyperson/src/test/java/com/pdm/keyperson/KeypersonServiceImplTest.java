package com.pdm.keyperson;

import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;
import com.pdm.common.dto.PageResult;
import com.pdm.keyperson.entity.KeyPerson;
import com.pdm.keyperson.entity.PetitionRecord;
import com.pdm.keyperson.entity.VisitPlan;
import com.pdm.keyperson.mapper.KeyPersonMapper;
import com.pdm.keyperson.mapper.PetitionRecordMapper;
import com.pdm.keyperson.mapper.VisitPlanMapper;
import com.pdm.keyperson.service.impl.KeypersonServiceImpl;

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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("重点人员服务 — 单元测试")
class KeypersonServiceImplTest {

    @Mock
    private KeyPersonMapper keyPersonMapper;
    @Mock
    private VisitPlanMapper visitPlanMapper;
    @Mock
    private PetitionRecordMapper petitionRecordMapper;
    @InjectMocks
    private KeypersonServiceImpl keypersonService;

    private KeyPerson testKeyPerson;

    @BeforeEach
    void setUp() {
        testKeyPerson = new KeyPerson();
        testKeyPerson.setId(1L);
        testKeyPerson.setUuid("00000000-0000-0000-0000-000000000001");
        testKeyPerson.setControlLevel("一级");
        testKeyPerson.setControlType("刑事重点");
        testKeyPerson.setDesignatedAt(LocalDateTime.of(2026, 6, 1, 10, 0));
    }

    @Nested
    @DisplayName("列管重点人员")
    class DesignateKeyPerson {

        @Test
        @DisplayName("正常列管")
        void shouldDesignateKeyPerson() {
            KeyPerson kp = new KeyPerson();
            kp.setUuid("00000000-0000-0000-0000-000000000002");
            kp.setControlLevel("一级");
            kp.setControlType("刑事重点");

            when(keyPersonMapper.selectByUuid("00000000-0000-0000-0000-000000000002")).thenReturn(null);

            KeyPerson result = keypersonService.designateKeyPerson(kp);

            assertNotNull(result);
            assertNotNull(result.getDesignatedAt());
            verify(keyPersonMapper).insert(kp);
        }

        @Test
        @DisplayName("已列管人员不可重复列管")
        void shouldRejectDuplicateDesignation() {
            KeyPerson kp = new KeyPerson();
            kp.setUuid("00000000-0000-0000-0000-000000000001");

            when(keyPersonMapper.selectByUuid("00000000-0000-0000-0000-000000000001")).thenReturn(testKeyPerson);

            BusinessException ex = assertThrows(BusinessException.class, () -> keypersonService.designateKeyPerson(kp));
            assertEquals(ErrorCode.KEY_PERSON_ALREADY_EXISTS.getCode(), ex.getCode());
            verify(keyPersonMapper, never()).insert(Collections.singleton(any()));
        }
    }

    @Nested
    @DisplayName("撤销列管")
    class RevokeKeyPerson {

        @Test
        @DisplayName("正常撤销列管")
        void shouldRevokeKeyPerson() {
            when(keyPersonMapper.selectByUuid("00000000-0000-0000-0000-000000000001")).thenReturn(testKeyPerson);

            keypersonService.revokeKeyPerson("00000000-0000-0000-0000-000000000001");

            assertNotNull(testKeyPerson.getRevokedAt());
            verify(keyPersonMapper).updateById(testKeyPerson);
        }

        @Test
        @DisplayName("重点人员不存在应抛异常")
        void shouldThrowWhenKeyPersonNotFound() {
            when(keyPersonMapper.selectByUuid("non-existent")).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> keypersonService.revokeKeyPerson("non-existent"));
            assertEquals(ErrorCode.KEY_PERSON_NOT_FOUND.getCode(), ex.getCode());
            verify(keyPersonMapper, never()).updateById(any(KeyPerson.class));
        }
    }

    @Nested
    @DisplayName("修改管控等级")
    class UpdateControlLevel {

        @Test
        @DisplayName("正常修改管控等级")
        void shouldUpdateControlLevel() {
            when(keyPersonMapper.selectByUuid("00000000-0000-0000-0000-000000000001")).thenReturn(testKeyPerson);

            KeyPerson result = keypersonService.updateControlLevel("00000000-0000-0000-0000-000000000001", "二级", null);

            assertEquals("二级", result.getControlLevel());
            verify(keyPersonMapper).updateById(testKeyPerson);
        }

        @Test
        @DisplayName("重点人员不存在应抛异常")
        void shouldThrowWhenKeyPersonNotFound() {
            when(keyPersonMapper.selectByUuid("non-existent")).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> keypersonService.updateControlLevel("non-existent", "二级", null));
            assertEquals(ErrorCode.KEY_PERSON_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("搜索重点人员")
    class SearchKeyPersons {

        @Test
        @DisplayName("按管控等级和类型搜索")
        void shouldSearchByLevelAndType() {
            Page<KeyPerson> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(testKeyPerson));
            mockPage.setTotal(1);

            when(keyPersonMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            PageResult<KeyPerson> result = keypersonService.searchKeyPersons("一级", "刑事重点", null, 1, 20);

            assertNotNull(result);
            assertEquals(1, result.getTotal());
        }

        @Test
        @DisplayName("按关键字搜索")
        void shouldSearchByKeyword() {
            Page<KeyPerson> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(testKeyPerson));
            mockPage.setTotal(1);

            when(keyPersonMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            PageResult<KeyPerson> result = keypersonService.searchKeyPersons(null, null,
                    "00000000-0000-0000-0000-000000000001", 1, 20);

            assertEquals(1, result.getTotal());
        }

        @Test
        @DisplayName("无匹配结果返回空")
        void shouldReturnEmptyOnNoMatch() {
            Page<KeyPerson> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of());
            mockPage.setTotal(0);

            when(keyPersonMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            PageResult<KeyPerson> result = keypersonService.searchKeyPersons("三级", null, null, 1, 20);

            assertEquals(0, result.getTotal());
            assertTrue(result.getRecords().isEmpty());
        }
    }

    @Nested
    @DisplayName("生成走访计划")
    class GenerateVisitPlan {

        @Test
        @DisplayName("一级管控人员每7天走访一次")
        void shouldSet7DaysIntervalForLevelOne() {
            VisitPlan plan = new VisitPlan();
            plan.setKeyPersonUuid("00000000-0000-0000-0000-000000000001");

            when(keyPersonMapper.selectByUuid("00000000-0000-0000-0000-000000000001")).thenReturn(testKeyPerson);

            VisitPlan result = keypersonService.generateVisitPlan(plan);

            assertNotNull(result);
            assertEquals("待走访", result.getStatus());
            assertNotNull(result.getPlannedDate());
            assertEquals(0, result.getIsAlerted());
            verify(visitPlanMapper).insert(plan);
        }

        @Test
        @DisplayName("二级管控人员每30天走访一次")
        void shouldSet30DaysIntervalForLevelTwo() {
            testKeyPerson.setControlLevel("二级");
            VisitPlan plan = new VisitPlan();
            plan.setKeyPersonUuid("00000000-0000-0000-0000-000000000001");

            when(keyPersonMapper.selectByUuid("00000000-0000-0000-0000-000000000001")).thenReturn(testKeyPerson);

            VisitPlan result = keypersonService.generateVisitPlan(plan);

            assertNotNull(result);
            assertTrue(result.getPlannedDate().isAfter(LocalDate.now().plusDays(25)));
        }

        @Test
        @DisplayName("重点人员不存在应抛异常")
        void shouldThrowWhenKeyPersonNotFound() {
            VisitPlan plan = new VisitPlan();
            plan.setKeyPersonUuid("non-existent");

            when(keyPersonMapper.selectByUuid("non-existent")).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> keypersonService.generateVisitPlan(plan));
            assertEquals(ErrorCode.KEY_PERSON_NOT_FOUND.getCode(), ex.getCode());
            verify(visitPlanMapper, never()).insert(any(VisitPlan.class));
        }
    }

    @Nested
    @DisplayName("执行走访")
    class CompleteVisit {

        @Test
        @DisplayName("正常完成走访（无信访记录）")
        void shouldCompleteVisitWithoutPetition() {
            VisitPlan plan = new VisitPlan();
            plan.setPlanId(1L);
            plan.setStatus("待走访");
            plan.setKeyPersonUuid("00000000-0000-0000-0000-000000000001");

            when(visitPlanMapper.selectById(1L)).thenReturn(plan);

            VisitPlan result = keypersonService.completeVisit(1L, LocalDate.of(2026, 7, 15), null);

            assertEquals("已完成", result.getStatus());
            assertEquals(LocalDate.of(2026, 7, 15), result.getActualDate());
            verify(visitPlanMapper).updateById(plan);
            verifyNoInteractions(petitionRecordMapper);
        }

        @Test
        @DisplayName("完成走访并记录信访")
        void shouldCompleteVisitWithPetition() {
            VisitPlan plan = new VisitPlan();
            plan.setPlanId(1L);
            plan.setStatus("待走访");
            plan.setKeyPersonUuid("00000000-0000-0000-0000-000000000001");

            PetitionRecord petition = new PetitionRecord();
            petition.setRemark("上访记录内容");
            petition.setAddress("某某街道");

            when(visitPlanMapper.selectById(1L)).thenReturn(plan);

            VisitPlan result = keypersonService.completeVisit(1L, LocalDate.of(2026, 7, 15), petition);

            assertEquals("已完成", result.getStatus());
            verify(petitionRecordMapper).insert(petition);
            assertEquals("00000000-0000-0000-0000-000000000001", petition.getKeyPersonUuid());
        }

        @Test
        @DisplayName("走访计划不存在应抛异常")
        void shouldThrowWhenVisitPlanNotFound() {
            when(visitPlanMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> keypersonService.completeVisit(999L, LocalDate.now(), null));
            assertEquals(ErrorCode.VISIT_PLAN_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("记录信访")
    class RecordPetition {

        @Test
        @DisplayName("单独记录信访事件")
        void shouldRecordPetition() {
            PetitionRecord petition = new PetitionRecord();
            petition.setKeyPersonUuid("00000000-0000-0000-0000-000000000001");
            petition.setRemark("信访内容");

            PetitionRecord result = keypersonService.recordPetition(petition);

            assertNotNull(result);
            assertNotNull(result.getPetitionTime());
            verify(petitionRecordMapper).insert(petition);
        }
    }

    @Nested
    @DisplayName("获取GIS数据")
    class GetGisData {

        @Test
        @DisplayName("获取GIS简化数据")
        void shouldGetGisData() {
            var person2 = new KeyPerson();
            person2.setUuid("uuid-002");
            person2.setControlLevel("二级");
            person2.setControlType("治安重点");

            when(keyPersonMapper.selectList(null)).thenReturn(List.of(testKeyPerson, person2));

            List<Map<String, Object>> result = keypersonService.getGisData();

            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.get(0).containsKey("uuid"));
            assertTrue(result.get(0).containsKey("controlLevel"));
            assertTrue(result.get(0).containsKey("controlType"));
        }

        @Test
        @DisplayName("无重点人员时返回空列表")
        void shouldReturnEmptyList() {
            when(keyPersonMapper.selectList(null)).thenReturn(List.of());

            List<Map<String, Object>> result = keypersonService.getGisData();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }
}
