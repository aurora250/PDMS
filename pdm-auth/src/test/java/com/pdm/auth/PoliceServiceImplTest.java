package com.pdm.auth;

import com.pdm.auth.entity.Police;
import com.pdm.auth.mapper.PoliceMapper;
import com.pdm.auth.service.impl.PoliceServiceImpl;
import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;

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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("民警服务 — 单元测试")
class PoliceServiceImplTest {

    @Mock
    private PoliceMapper policeMapper;
    @InjectMocks
    private PoliceServiceImpl policeService;

    private Police testPolice;

    @BeforeEach
    void setUp() {
        testPolice = new Police();
        testPolice.setId(1L);
        testPolice.setPoliceNumber("P00000001");
        testPolice.setPoliceStation("朝阳分局");
        testPolice.setJurisdiction("朝阳区");
        testPolice.setDepartment("刑侦支队");
        testPolice.setPoliceRank("一级警司");
        testPolice.setDutyStatus("在岗");
    }

    @Nested
    @DisplayName("登记民警")
    class RegisterPolice {

        @Test
        @DisplayName("正常登记民警，自动生成警号")
        void shouldRegisterPoliceWithGeneratedNumber() {
            Police newPolice = new Police();
            newPolice.setPoliceStation("海淀分局");
            newPolice.setJurisdiction("海淀区");

            when(policeMapper.selectByPoliceNumber(anyString())).thenReturn(null);

            Police result = policeService.registerPolice(newPolice);

            assertNotNull(result);
            assertNotNull(result.getPoliceNumber());
            assertTrue(result.getPoliceNumber().startsWith("P"));
            verify(policeMapper).insert(any(Police.class));
        }

        @Test
        @DisplayName("警号已存在应拒绝")
        void shouldRejectDuplicatePoliceNumber() {
            Police newPolice = new Police();
            newPolice.setPoliceNumber("P00000001");
            newPolice.setPoliceStation("海淀分局");

            when(policeMapper.selectByPoliceNumber("P00000001")).thenReturn(testPolice);

            BusinessException ex = assertThrows(BusinessException.class, () -> policeService.registerPolice(newPolice));
            assertEquals(ErrorCode.DATA_DUPLICATE.getCode(), ex.getCode());
            verify(policeMapper, never()).insert(any(Police.class));
        }

        @Test
        @DisplayName("警号为空时自动生成")
        void shouldAutoGenerateWhenPoliceNumberIsNull() {
            Police newPolice = new Police();
            newPolice.setPoliceStation("丰台分局");

            when(policeMapper.selectByPoliceNumber(anyString())).thenReturn(null);

            Police result = policeService.registerPolice(newPolice);

            assertNotNull(result.getPoliceNumber());
            assertTrue(result.getPoliceNumber().length() > 1);
        }
    }

    @Nested
    @DisplayName("查询民警列表")
    class ListPolice {

        @Test
        @DisplayName("按关键字分页查询")
        void shouldListWithKeyword() {
            Page<Police> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(testPolice));
            mockPage.setTotal(1);

            when(policeMapper.selectPageWithResidentName(any(Page.class), eq("P00000001"), isNull())).thenReturn(mockPage);

            Page<Police> result = policeService.listPolice(1, 20, "P00000001", null);

            assertNotNull(result);
            assertEquals(1, result.getTotal());
            assertEquals(1, result.getRecords().size());
            assertEquals("P00000001", result.getRecords().get(0).getPoliceNumber());
        }

        @Test
        @DisplayName("无关键字时返回全部")
        void shouldListAllWhenNoKeyword() {
            Page<Police> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(testPolice));
            mockPage.setTotal(1);

            when(policeMapper.selectPageWithResidentName(any(Page.class), isNull(), isNull())).thenReturn(mockPage);

            Page<Police> result = policeService.listPolice(1, 20, null, null);

            assertNotNull(result);
            assertEquals(1, result.getRecords().size());
        }

        @Test
        @DisplayName("空结果列表")
        void shouldReturnEmptyOnNoMatch() {
            Page<Police> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of());
            mockPage.setTotal(0);

            when(policeMapper.selectPageWithResidentName(any(Page.class), eq("不存在的警号"), isNull())).thenReturn(mockPage);

            Page<Police> result = policeService.listPolice(1, 20, "不存在的警号", null);

            assertNotNull(result);
            assertEquals(0, result.getTotal());
            assertTrue(result.getRecords().isEmpty());
        }
    }

    @Nested
    @DisplayName("查询民警详情")
    class GetPoliceByNumber {

        @Test
        @DisplayName("按警号查询成功")
        void shouldFindByPoliceNumber() {
            when(policeMapper.selectByPoliceNumber("P00000001")).thenReturn(testPolice);

            Police result = policeService.getPoliceByNumber("P00000001");

            assertNotNull(result);
            assertEquals("P00000001", result.getPoliceNumber());
            assertEquals("朝阳分局", result.getPoliceStation());
        }

        @Test
        @DisplayName("警号不存在应抛异常")
        void shouldThrowOnNonExistentPoliceNumber() {
            when(policeMapper.selectByPoliceNumber("P99999999")).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> policeService.getPoliceByNumber("P99999999"));
            assertEquals(ErrorCode.DATA_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("修改民警信息")
    class UpdatePolice {

        @Test
        @DisplayName("正常修改民警信息")
        void shouldUpdatePoliceSuccessfully() {
            Police updates = new Police();
            updates.setPoliceStation("海淀分局");
            updates.setDepartment("经侦支队");
            updates.setPoliceRank("二级警司");
            updates.setJurisdiction("海淀区");

            when(policeMapper.selectByPoliceNumber("P00000001")).thenReturn(testPolice);

            Police result = policeService.updatePolice("P00000001", updates);

            assertNotNull(result);
            assertEquals("海淀分局", result.getPoliceStation());
            assertEquals("经侦支队", result.getDepartment());
            assertEquals("二级警司", result.getPoliceRank());
            assertEquals("海淀区", result.getJurisdiction());
            verify(policeMapper).updateById(any(Police.class));
        }

        @Test
        @DisplayName("部分字段更新")
        void shouldPartialUpdate() {
            Police updates = new Police();
            updates.setPoliceStation("丰台分局");

            when(policeMapper.selectByPoliceNumber("P00000001")).thenReturn(testPolice);

            Police result = policeService.updatePolice("P00000001", updates);

            assertEquals("丰台分局", result.getPoliceStation());
            assertEquals("刑侦支队", result.getDepartment());
        }

        @Test
        @DisplayName("民警不存在应抛异常")
        void shouldThrowWhenPoliceNotFound() {
            Police updates = new Police();
            updates.setPoliceStation("丰台分局");

            when(policeMapper.selectByPoliceNumber("P99999999")).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> policeService.updatePolice("P99999999", updates));
            assertEquals(ErrorCode.DATA_NOT_FOUND.getCode(), ex.getCode());
            verify(policeMapper, never()).updateById(any(Police.class));
        }
    }

    @Nested
    @DisplayName("更新民警执勤状态")
    class UpdatePoliceStatus {

        @Test
        @DisplayName("正常更新为在岗")
        void shouldSetStatusToOnDuty() {
            when(policeMapper.selectByPoliceNumber("P00000001")).thenReturn(testPolice);

            policeService.updatePoliceStatus("P00000001", "在岗");

            assertEquals("在岗", testPolice.getDutyStatus());
            verify(policeMapper).updateById(testPolice);
        }

        @Test
        @DisplayName("正常更新为调岗")
        void shouldSetStatusToTransferred() {
            when(policeMapper.selectByPoliceNumber("P00000001")).thenReturn(testPolice);

            policeService.updatePoliceStatus("P00000001", "调岗");

            assertEquals("调岗", testPolice.getDutyStatus());
        }

        @Test
        @DisplayName("正常更新为离职")
        void shouldSetStatusToResigned() {
            when(policeMapper.selectByPoliceNumber("P00000001")).thenReturn(testPolice);

            policeService.updatePoliceStatus("P00000001", "离职");

            assertEquals("离职", testPolice.getDutyStatus());
        }

        @Test
        @DisplayName("非法执勤状态应抛异常")
        void shouldRejectInvalidStatus() {
            when(policeMapper.selectByPoliceNumber("P00000001")).thenReturn(testPolice);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> policeService.updatePoliceStatus("P00000001", "病假"));
            assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
            verify(policeMapper, never()).updateById(any(Police.class));
        }

        @Test
        @DisplayName("民警不存在时更新状态应抛异常")
        void shouldThrowWhenPoliceNotFound() {
            when(policeMapper.selectByPoliceNumber("P99999999")).thenReturn(null);

            assertThrows(BusinessException.class, () -> policeService.updatePoliceStatus("P99999999", "在岗"));
        }
    }
}
