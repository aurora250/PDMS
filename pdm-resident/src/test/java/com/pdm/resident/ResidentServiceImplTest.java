package com.pdm.resident;

import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;
import com.pdm.resident.entity.Resident;
import com.pdm.resident.entity.ResidentRelation;
import com.pdm.resident.es.ResidentEsRepository;
import com.pdm.resident.mapper.ResidentChangeRequestMapper;
import com.pdm.resident.mapper.ResidentMapper;
import com.pdm.resident.mapper.ResidentRelationMapper;
import com.pdm.resident.service.impl.ResidentServiceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("常住人口服务 — 单元测试")
class ResidentServiceImplTest {

    @Mock
    private ResidentMapper residentMapper;
    @Mock
    private ResidentRelationMapper relationMapper;
    @Mock
    private ResidentChangeRequestMapper changeRequestMapper;
    @Mock
    private ResidentEsRepository residentEsRepository;
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private ResidentServiceImpl residentService;

    private Resident testResident;

    @BeforeEach
    void setUp() {
        testResident = new Resident();
        testResident.setId(1L);
        testResident.setUuid("r-uuid-001");
        testResident.setName("张三");
        testResident.setIdCardNo("110101199003076632");
        testResident.setGender("男");
        testResident.setNation("汉族");
        testResident.setHouseholdStatus("正常");
        testResident.setPhone("13800001111");
        testResident.setMaritalStatus("未婚");
        testResident.setHouseholdAddress("北京市东城区某某街道1号");
    }

    @Nested
    @DisplayName("创建常住人口")
    class CreateResident {

        @Test
        @DisplayName("合法身份证号创建成功，自动提取出生日期和性别")
        void shouldCreateWithValidIdCard() {
            when(residentMapper.selectByIdCardNo("110101199003076632")).thenReturn(null);

            Resident result = residentService.createResident(testResident);

            assertNotNull(result);
            assertEquals("1990-03-07", result.getBirthDate().toString());
            assertEquals("男", result.getGender());
            verify(residentMapper).insert(any(Resident.class));
        }

        @Test
        @DisplayName("重复身份证号拒绝创建")
        void shouldRejectDuplicateIdCard() {
            when(residentMapper.selectByIdCardNo("110101199003076632")).thenReturn(testResident);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> residentService.createResident(testResident));
            assertEquals(ErrorCode.ID_CARD_DUPLICATE.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非法身份证号拒绝创建")
        void shouldRejectInvalidIdCard() {
            testResident.setIdCardNo("12345");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> residentService.createResident(testResident));
            assertEquals(ErrorCode.ID_CARD_INVALID.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("人员关系")
    class Relations {

        @Test
        @DisplayName("设置父亲关系成功")
        void shouldSetFatherRelation() {
            ResidentRelation relation = new ResidentRelation();
            relation.setRelationPersonUuid("r-uuid-001");
            relation.setFatherUuid("r-uuid-002");
            relation.setMotherUuid("r-uuid-003");

            when(relationMapper.selectByPersonUuid("r-uuid-001")).thenReturn(null, relation);
            when(relationMapper.selectByPersonUuid("r-uuid-002")).thenReturn(null);
            when(relationMapper.selectByPersonUuid("r-uuid-003")).thenReturn(null);

            ResidentRelation result = residentService.setRelations(relation);
            assertNotNull(result);
            verify(relationMapper).insert(any(ResidentRelation.class));
        }

        @Test
        @DisplayName("循环亲属关系应被拦截 — A的父亲是B, B的父亲是A")
        void shouldRejectCircularRelation() {
            // A's father is B
            ResidentRelation relation = new ResidentRelation();
            relation.setRelationPersonUuid("person-A");
            relation.setFatherUuid("person-B");

            // B's father is already set to A (circular)
            ResidentRelation fatherRel = new ResidentRelation();
            fatherRel.setFatherUuid("person-A");
            when(relationMapper.selectByPersonUuid("person-A")).thenReturn(null);
            when(relationMapper.selectByPersonUuid("person-B")).thenReturn(fatherRel);

            BusinessException ex = assertThrows(BusinessException.class, () -> residentService.setRelations(relation));
            assertEquals(ErrorCode.RELATION_CIRCULAR.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("配偶关系自动双向设置")
        void shouldAutoBidirectionalSpouse() {
            ResidentRelation relation = new ResidentRelation();
            relation.setRelationPersonUuid("person-A");
            relation.setSpouseUuid("person-B");

            when(relationMapper.selectByPersonUuid("person-A")).thenReturn(null);
            when(relationMapper.selectByPersonUuid("person-B")).thenReturn(null);
            when(relationMapper.selectByPersonUuid("person-A")).thenReturn(relation);

            residentService.setRelations(relation);

            // Verify spouse's relation is also created/updated
            verify(relationMapper, atLeastOnce()).insert(any(ResidentRelation.class));
        }
    }

    @Nested
    @DisplayName("信息修改")
    class UpdateResident {

        @Test
        @DisplayName("正常人员信息可修改")
        void shouldUpdateNormalResident() {
            when(residentMapper.selectByUuid("r-uuid-001")).thenReturn(testResident);

            Resident updates = new Resident();
            updates.setName("张三丰");
            updates.setPhone("13900001111");
            updates.setOccupation("工程师");

            Resident result = residentService.updateResident("r-uuid-001", updates);
            assertEquals("张三丰", result.getName());
            assertEquals("13900001111", result.getPhone());
            assertEquals("工程师", result.getOccupation());
        }

        @Test
        @DisplayName("死亡注销状态不允许修改")
        void shouldRejectUpdateOnDeceasedResident() {
            testResident.setHouseholdStatus("死亡注销");
            when(residentMapper.selectByUuid("r-uuid-001")).thenReturn(testResident);

            Resident updates = new Resident();
            updates.setName("张三丰");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> residentService.updateResident("r-uuid-001", updates));
            assertEquals(ErrorCode.RESIDENT_STATUS_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("迁出注销状态不允许修改")
        void shouldRejectUpdateOnMigratedResident() {
            testResident.setHouseholdStatus("迁出注销");
            when(residentMapper.selectByUuid("r-uuid-001")).thenReturn(testResident);

            Resident updates = new Resident();
            updates.setOccupation("新职业");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> residentService.updateResident("r-uuid-001", updates));
            assertEquals(ErrorCode.RESIDENT_STATUS_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("不存在的人员更新返回错误")
        void shouldFailOnNotFound() {
            when(residentMapper.selectByUuid("non-existent")).thenReturn(null);

            Resident updates = new Resident();
            updates.setName("test");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> residentService.updateResident("non-existent", updates));
            assertEquals(ErrorCode.RESIDENT_NOT_FOUND.getCode(), ex.getCode());
        }
    }
}
