package com.pdm.floatingpopulation;

import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;
import com.pdm.floatingpopulation.entity.FpRegisterRecord;
import com.pdm.floatingpopulation.entity.ResidentPermit;
import com.pdm.floatingpopulation.entity.ResidentPermitRenewal;
import com.pdm.floatingpopulation.entity.ResidentRegistration;
import com.pdm.floatingpopulation.mapper.FpRegisterRecordMapper;
import com.pdm.floatingpopulation.mapper.ResidentPermitMapper;
import com.pdm.floatingpopulation.mapper.ResidentPermitRenewalMapper;
import com.pdm.floatingpopulation.mapper.ResidentRegistrationMapper;
import com.pdm.floatingpopulation.service.impl.FloatingPopulationServiceImpl;

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
@DisplayName("流动人口服务 — 单元测试")
class FloatingPopulationServiceImplTest {

    @Mock
    private FpRegisterRecordMapper fpRegisterRecordMapper;
    @Mock
    private ResidentPermitMapper residentPermitMapper;
    @Mock
    private ResidentPermitRenewalMapper residentPermitRenewalMapper;
    @Mock
    private ResidentRegistrationMapper residentRegistrationMapper;
    @InjectMocks
    private FloatingPopulationServiceImpl fpService;

    private FpRegisterRecord testFpRecord;
    private ResidentPermit testPermit;
    private ResidentRegistration testResidence;

    @BeforeEach
    void setUp() {
        testFpRecord = new FpRegisterRecord();
        testFpRecord.setRid(101L);
        testFpRecord.setUuid("fp-uuid-001");
        testFpRecord.setRegisterDate(LocalDate.of(2026, 6, 1));

        testPermit = new ResidentPermit();
        testPermit.setId(1L);
        testPermit.setPermitNo("RP202606000001");
        testPermit.setUuid("permit-uuid-001");
        testPermit.setStatus("有效");
        testPermit.setIssueDate(LocalDate.of(2026, 6, 1));
        testPermit.setExpiryDate(LocalDate.of(2027, 5, 31));

        testResidence = new ResidentRegistration();
        testResidence.setRid(201L);
        testResidence.setCurrentAddress("北京市朝阳区XX路1号");
        testResidence.setAddressType("租赁");
        testResidence.setRegisterDate(LocalDate.of(2026, 6, 1));
    }

    @Nested
    @DisplayName("登记流动人口")
    class RegisterFp {

        @Test
        @DisplayName("正常登记流动人口，自动设置登记日期")
        void shouldRegisterFloatingPopulation() {
            FpRegisterRecord record = new FpRegisterRecord();
            record.setUuid("fp-uuid-new");

            FpRegisterRecord result = fpService.registerFp(record);

            assertNotNull(result);
            assertNotNull(result.getRegisterDate());
            verify(fpRegisterRecordMapper).insert(record);
        }
    }

    @Nested
    @DisplayName("修改流动人口登记")
    class UpdateFp {

        @Test
        @DisplayName("正常修改登记信息")
        void shouldUpdateFpRecord() {
            FpRegisterRecord updates = new FpRegisterRecord();
            updates.setRejectReason("信息变更");

            when(fpRegisterRecordMapper.selectById(101L)).thenReturn(testFpRecord);

            FpRegisterRecord result = fpService.updateFp(101L, updates);

            assertNotNull(result);
            assertEquals(Long.valueOf(101L), result.getRid());
            verify(fpRegisterRecordMapper).updateById(updates);
        }

        @Test
        @DisplayName("登记记录不存在应抛异常")
        void shouldThrowWhenRecordNotFound() {
            FpRegisterRecord updates = new FpRegisterRecord();

            when(fpRegisterRecordMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> fpService.updateFp(999L, updates));
            assertEquals(ErrorCode.FP_RECORD_NOT_FOUND.getCode(), ex.getCode());
            verify(fpRegisterRecordMapper, never()).updateById(any(FpRegisterRecord.class));
        }
    }

    @Nested
    @DisplayName("注销流动人口登记")
    class CancelFp {

        @Test
        @DisplayName("正常注销登记")
        void shouldCancelFpRecord() {
            when(fpRegisterRecordMapper.selectById(101L)).thenReturn(testFpRecord);

            fpService.cancelFp(101L);

            verify(fpRegisterRecordMapper).deleteById(101L);
        }

        @Test
        @DisplayName("登记不存在应抛异常")
        void shouldThrowWhenRecordNotFound() {
            when(fpRegisterRecordMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> fpService.cancelFp(999L));
            assertEquals(ErrorCode.FP_RECORD_NOT_FOUND.getCode(), ex.getCode());
            verify(fpRegisterRecordMapper, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("申领居住证")
    class ApplyPermit {

        @Test
        @DisplayName("正常申领居住证，自动生成证号和有效期")
        void shouldApplyPermit() {
            ResidentPermit permit = new ResidentPermit();

            ResidentPermit result = fpService.applyPermit(permit);

            assertNotNull(result);
            assertNotNull(result.getPermitNo());
            assertEquals(18, result.getPermitNo().length());
            assertEquals("有效", result.getStatus());
            assertNotNull(result.getIssueDate());
            assertNotNull(result.getExpiryDate());
            assertEquals(result.getIssueDate().plusYears(1), result.getExpiryDate());
            verify(residentPermitMapper).insert(permit);
        }
    }

    @Nested
    @DisplayName("审批居住证")
    class ApprovePermit {

        @Test
        @DisplayName("审批通过居住证")
        void shouldApprovePermit() {
            testPermit.setStatus("审批中");
            when(residentPermitMapper.selectById(1L)).thenReturn(testPermit);

            ResidentPermit result = fpService.approvePermit(1L, "admin-uuid");

            assertNotNull(result);
            assertEquals("有效", result.getStatus());
            verify(residentPermitMapper).updateById(testPermit);
        }

        @Test
        @DisplayName("居住证不存在应抛异常")
        void shouldThrowWhenPermitNotFound() {
            when(residentPermitMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> fpService.approvePermit(999L, "admin-uuid"));
            assertEquals(ErrorCode.RESIDENT_PERMIT_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("签发居住证")
    class IssuePermit {

        @Test
        @DisplayName("正常签发居住证并更新登记记录")
        void shouldIssuePermitAndUpdateFpRecord() {
            when(residentPermitMapper.selectById(1L)).thenReturn(testPermit);
            when(fpRegisterRecordMapper.selectOne(any())).thenReturn(testFpRecord);

            ResidentPermit result = fpService.issuePermit(1L);

            assertNotNull(result);
            verify(residentPermitMapper).updateById(testPermit);
            verify(fpRegisterRecordMapper).updateById(testFpRecord);
        }

        @Test
        @DisplayName("签发时无关联登记记录仍正常签发")
        void shouldIssuePermitEvenWithoutFpRecord() {
            when(residentPermitMapper.selectById(1L)).thenReturn(testPermit);
            when(fpRegisterRecordMapper.selectOne(any())).thenReturn(null);

            ResidentPermit result = fpService.issuePermit(1L);

            assertNotNull(result);
            verify(residentPermitMapper).updateById(testPermit);
            verify(fpRegisterRecordMapper, never()).updateById(any(FpRegisterRecord.class));
        }
    }

    @Nested
    @DisplayName("续期居住证")
    class RenewPermit {

        @Test
        @DisplayName("正常续期居住证")
        void shouldRenewPermit() {
            ResidentPermitRenewal renewal = new ResidentPermitRenewal();

            when(residentPermitMapper.selectById(1L)).thenReturn(testPermit);

            ResidentPermitRenewal result = fpService.renewPermit(1L, renewal);

            assertNotNull(result);
            verify(residentPermitRenewalMapper).insert(renewal);
            verify(residentPermitMapper).updateById(testPermit);
            assertEquals(LocalDate.of(2028, 5, 31), testPermit.getExpiryDate());
        }

        @Test
        @DisplayName("居住证不存在应抛异常")
        void shouldThrowWhenPermitNotFound() {
            ResidentPermitRenewal renewal = new ResidentPermitRenewal();

            when(residentPermitMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> fpService.renewPermit(999L, renewal));
            assertEquals(ErrorCode.RESIDENT_PERMIT_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("已过期居住证不可续期")
        void shouldRejectExpiredPermitRenewal() {
            testPermit.setStatus("已过期");
            ResidentPermitRenewal renewal = new ResidentPermitRenewal();

            when(residentPermitMapper.selectById(1L)).thenReturn(testPermit);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> fpService.renewPermit(1L, renewal));
            assertEquals(ErrorCode.RESIDENT_PERMIT_EXPIRED.getCode(), ex.getCode());
            verify(residentPermitRenewalMapper, never()).insert(any(ResidentPermitRenewal.class));
        }
    }

    @Nested
    @DisplayName("居住登记")
    class RegisterResidence {

        @Test
        @DisplayName("正常登记居住地址")
        void shouldRegisterResidence() {
            ResidentRegistration registration = new ResidentRegistration();
            registration.setCurrentAddress("北京市海淀区XX路2号");

            ResidentRegistration result = fpService.registerResidence(registration);

            assertNotNull(result);
            assertNotNull(result.getRegisterDate());
            verify(residentRegistrationMapper).insert(registration);
        }
    }

    @Nested
    @DisplayName("变更居住信息")
    class ChangeResidence {

        @Test
        @DisplayName("正常变更居住地址")
        void shouldChangeResidence() {
            ResidentRegistration updates = new ResidentRegistration();
            updates.setCurrentAddress("北京市丰台区YY路3号");
            updates.setAddressType("自购");

            when(residentRegistrationMapper.selectById(201L)).thenReturn(testResidence);

            ResidentRegistration result = fpService.changeResidence(201L, updates);

            assertNotNull(result);
            assertEquals("北京市丰台区YY路3号", result.getCurrentAddress());
            assertEquals("自购", result.getAddressType());
            verify(residentRegistrationMapper).updateById(testResidence);
        }

        @Test
        @DisplayName("居住记录不存在应抛异常")
        void shouldThrowWhenResidenceNotFound() {
            ResidentRegistration updates = new ResidentRegistration();
            updates.setCurrentAddress("新地址");

            when(residentRegistrationMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> fpService.changeResidence(999L, updates));
            assertEquals(ErrorCode.DATA_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("注销居住登记")
    class CancelResidence {

        @Test
        @DisplayName("正常注销居住登记")
        void shouldCancelResidence() {
            when(residentRegistrationMapper.selectById(201L)).thenReturn(testResidence);

            fpService.cancelResidence(201L);

            verify(residentRegistrationMapper).deleteById(201L);
        }

        @Test
        @DisplayName("居住记录不存在应抛异常")
        void shouldThrowWhenResidenceNotFound() {
            when(residentRegistrationMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> fpService.cancelResidence(999L));
            assertEquals(ErrorCode.DATA_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("统计分析")
    class Statistics {

        @Test
        @DisplayName("获取热力图数据")
        void shouldGetHeatmapData() {
            when(residentRegistrationMapper.selectList(any())).thenReturn(List.of(testResidence));

            List<Map<String, Object>> result = fpService.getHeatmapData();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertTrue(result.get(0).containsKey("areaId"));
            assertTrue(result.get(0).containsKey("addressType"));
            assertTrue(result.get(0).containsKey("uuid"));
        }

        @Test
        @DisplayName("热力图无数据时返回空列表")
        void shouldReturnEmptyHeatmap() {
            when(residentRegistrationMapper.selectList(any())).thenReturn(List.of());

            List<Map<String, Object>> result = fpService.getHeatmapData();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("获取趋势统计数据")
        void shouldGetTrendData() {
            when(fpRegisterRecordMapper.selectList(any())).thenReturn(List.of(testFpRecord));

            List<Map<String, Object>> result = fpService.getTrendData();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertTrue(result.get(0).containsKey("registerDate"));
            assertTrue(result.get(0).containsKey("rid"));
        }

        @Test
        @DisplayName("趋势无数据时返回空列表")
        void shouldReturnEmptyTrend() {
            when(fpRegisterRecordMapper.selectList(any())).thenReturn(List.of());

            List<Map<String, Object>> result = fpService.getTrendData();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }
}
