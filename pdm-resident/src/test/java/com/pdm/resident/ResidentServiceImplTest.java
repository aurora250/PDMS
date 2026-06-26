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

/**
 * 常住人口业务实现类单元测试
 * 覆盖创建、更新、亲属关系、变更申请等核心业务场景
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("常住人口服务 — 单元测试")
class ResidentServiceImplTest {

    /**
     * 常住人口Mapper模拟对象
     */
    @Mock
    private ResidentMapper residentMapper;

    /**
     * 亲属关系Mapper模拟对象
     */
    @Mock
    private ResidentRelationMapper relationMapper;

    /**
     * 变更申请Mapper模拟对象
     */
    @Mock
    private ResidentChangeRequestMapper changeRequestMapper;

    /**
     * ES仓储模拟对象
     */
    @Mock
    private ResidentEsRepository residentEsRepository;

    /**
     * JSON序列化工具模拟对象
     */
    @Mock
    private ObjectMapper objectMapper;

    /**
     * 待测试的业务实现类
     */
    @InjectMocks
    private ResidentServiceImpl residentService;

    /**
     * 测试用常住人口对象
     */
    private Resident testResident;

    /**
     * 测试前置初始化
     * 初始化测试用常住人口数据
     */
    @BeforeEach
    void setUp() {
        testResident = new Resident();
        testResident.setId(1L);
        testResident.setUuid("00000000-0000-0000-0000-000000000001");
        testResident.setName("张三");
        testResident.setIdCardNo("110101199003076632");
        testResident.setGender("男");
        testResident.setNation("汉族");
        testResident.setHouseholdStatus("正常");
        testResident.setPhone("13800001111");
        testResident.setMaritalStatus("未婚");
        testResident.setHouseholdAddress("北京市东城区某某街道1号");
    }

    /**
     * 创建常住人口测试用例集
     */
    @Nested
    @DisplayName("创建常住人口")
    class CreateResident {

        /**
         * 合法身份证号创建成功场景
         * 验证：自动提取出生日期和性别，数据入库成功
         */
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

        /**
         * 重复身份证号创建失败场景
         * 验证：抛出身份证重复异常
         */
        @Test
        @DisplayName("重复身份证号拒绝创建")
        void shouldRejectDuplicateIdCard() {
            when(residentMapper.selectByIdCardNo("110101199003076632")).thenReturn(testResident);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> residentService.createResident(testResident));
            assertEquals(ErrorCode.ID_CARD_DUPLICATE.getCode(), ex.getCode());
        }

        /**
         * 非法身份证号创建失败场景
         * 验证：抛出身份证无效异常
         */
        @Test
        @DisplayName("非法身份证号拒绝创建")
        void shouldRejectInvalidIdCard() {
            testResident.setIdCardNo("12345");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> residentService.createResident(testResident));
            assertEquals(ErrorCode.ID_CARD_INVALID.getCode(), ex.getCode());
        }
    }

    /**
     * 人员关系测试用例集
     */
    @Nested
    @DisplayName("人员关系")
    class Relations {

        /**
         * 设置父亲关系成功场景
         * 验证：亲属关系数据入库成功
         */
        @Test
        @DisplayName("设置父亲关系成功")
        void shouldSetFatherRelation() {
            ResidentRelation relation = new ResidentRelation();
            relation.setRelationPersonUuid("00000000-0000-0000-0000-000000000001");
            relation.setFatherUuid("00000000-0000-0000-0000-000000000002");
            relation.setMotherUuid("00000000-0000-0000-0000-000000000003");

            when(relationMapper.selectByPersonUuid("00000000-0000-0000-0000-000000000001")).thenReturn(null, relation);
            when(relationMapper.selectByPersonUuid("00000000-0000-0000-0000-000000000002")).thenReturn(null);
            when(relationMapper.selectByPersonUuid("00000000-0000-0000-0000-000000000003")).thenReturn(null);

            ResidentRelation result = residentService.setRelations(relation);
            assertNotNull(result);
            verify(relationMapper).insert(any(ResidentRelation.class));
        }

        /**
         * 循环亲属关系拦截场景
         * 验证：A的父亲是B，B的父亲是A时抛出循环关系异常
         */
        @Test
        @DisplayName("循环亲属关系应被拦截 — A的父亲是B, B的父亲是A")
        void shouldRejectCircularRelation() {
            // A's father is B
            ResidentRelation relation = new ResidentRelation();
            relation.setRelationPersonUuid("00000000-0000-0000-0000-00000000000a");
            relation.setFatherUuid("00000000-0000-0000-0000-00000000000b");

            // B's father is already set to A (circular)
            ResidentRelation fatherRel = new ResidentRelation();
            fatherRel.setFatherUuid("00000000-0000-0000-0000-00000000000a");
            when(relationMapper.selectByPersonUuid("00000000-0000-0000-0000-00000000000a")).thenReturn(null);
            when(relationMapper.selectByPersonUuid("00000000-0000-0000-0000-00000000000b")).thenReturn(fatherRel);

            BusinessException ex = assertThrows(BusinessException.class, () -> residentService.setRelations(relation));
            assertEquals(ErrorCode.RELATION_CIRCULAR.getCode(), ex.getCode());
        }

        /**
         * 配偶关系自动双向设置场景
         * 验证：设置A的配偶为B时，自动设置B的配偶为A
         */
        @Test
        @DisplayName("配偶关系自动双向设置")
        void shouldAutoBidirectionalSpouse() {
            ResidentRelation relation = new ResidentRelation();
            relation.setRelationPersonUuid("00000000-0000-0000-0000-00000000000a");
            relation.setSpouseUuid("00000000-0000-0000-0000-00000000000b");

            when(relationMapper.selectByPersonUuid("00000000-0000-0000-0000-00000000000a")).thenReturn(null);
            when(relationMapper.selectByPersonUuid("00000000-0000-0000-0000-00000000000b")).thenReturn(null);
            when(relationMapper.selectByPersonUuid("00000000-0000-0000-0000-00000000000a")).thenReturn(relation);

            residentService.setRelations(relation);

            // Verify spouse's relation is also created/updated
            verify(relationMapper, atLeastOnce()).insert(any(ResidentRelation.class));
        }
    }

    /**
     * 信息修改测试用例集
     */
    @Nested
    @DisplayName("信息修改")
    class UpdateResident {

        /**
         * 正常人员信息修改成功场景
         * 验证：可修改姓名、电话、职业等字段
         */
        @Test
        @DisplayName("正常人员信息可修改")
        void shouldUpdateNormalResident() {
            when(residentMapper.selectByUuid("00000000-0000-0000-0000-000000000001")).thenReturn(testResident);

            Resident updates = new Resident();
            updates.setName("张三丰");
            updates.setPhone("13900001111");
            updates.setOccupation("工程师");

            Resident result = residentService.updateResident("00000000-0000-0000-0000-000000000001", updates);
            assertEquals("张三丰", result.getName());
            assertEquals("13900001111", result.getPhone());
            assertEquals("工程师", result.getOccupation());
        }

        /**
         * 死亡注销状态修改拦截场景
         * 验证：死亡注销状态不允许修改信息
         */
        @Test
        @DisplayName("死亡注销状态不允许修改")
        void shouldRejectUpdateOnDeceasedResident() {
            testResident.setHouseholdStatus("死亡注销");
            when(residentMapper.selectByUuid("00000000-0000-0000-0000-000000000001")).thenReturn(testResident);

            Resident updates = new Resident();
            updates.setName("张三丰");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> residentService.updateResident("00000000-0000-0000-0000-000000000001", updates));
            assertEquals(ErrorCode.RESIDENT_STATUS_INVALID.getCode(), ex.getCode());
        }

        /**
         * 迁出注销状态修改拦截场景
         * 验证：迁出注销状态不允许修改信息
         */
        @Test
        @DisplayName("迁出注销状态不允许修改")
        void shouldRejectUpdateOnMigratedResident() {
            testResident.setHouseholdStatus("迁出注销");
            when(residentMapper.selectByUuid("00000000-0000-0000-0000-000000000001")).thenReturn(testResident);

            Resident updates = new Resident();
            updates.setOccupation("新职业");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> residentService.updateResident("00000000-0000-0000-0000-000000000001", updates));
            assertEquals(ErrorCode.RESIDENT_STATUS_INVALID.getCode(), ex.getCode());
        }

        /**
         * 不存在人员修改失败场景
         * 验证：修改不存在的人员抛出人员不存在异常
         */
        @Test
        @DisplayName("不存在的人员更新返回错误")
        void shouldFailOnNotFound() {
            when(residentMapper.selectByUuid("00000000-0000-0000-0000-000000000099")).thenReturn(null);

            Resident updates = new Resident();
            updates.setName("test");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> residentService.updateResident("00000000-0000-0000-0000-000000000099", updates));
            assertEquals(ErrorCode.RESIDENT_NOT_FOUND.getCode(), ex.getCode());
        }
    }
}