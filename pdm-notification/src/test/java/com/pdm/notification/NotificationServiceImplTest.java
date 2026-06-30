package com.pdm.notification;

import com.pdm.notification.entity.Alert;
import com.pdm.notification.mapper.AlertMapper;
import com.pdm.notification.service.impl.NotificationServiceImpl;

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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("通知/预警服务 — 单元测试")
class NotificationServiceImplTest {

    @Mock
    private AlertMapper alertMapper;
    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Alert testAlert;

    @BeforeEach
    void setUp() {
        testAlert = new Alert();
        testAlert.setAlertId(1L);
        testAlert.setAlertType("居住证到期");
        testAlert.setSeverity("高");
        testAlert.setAlertContent("居住证编号XX即将到期");
        testAlert.setIsHandled(0);
    }

    @Nested
    @DisplayName("创建预警")
    class CreateAlert {

        @Test
        @DisplayName("正常创建预警，isHandled默认为0")
        void shouldCreateAlert() {
            Alert alert = new Alert();
            alert.setAlertType("居住证到期");
            alert.setSeverity("高");
            alert.setAlertContent("test message");

            Alert result = notificationService.createAlert(alert);

            assertNotNull(result);
            assertEquals(0, result.getIsHandled());
            verify(alertMapper).insert(alert);
        }

        @Test
        @DisplayName("未显式设置isHandled时自动设为0")
        void shouldDefaultIsHandledToZero() {
            Alert alert = new Alert();
            alert.setAlertType("test");

            Alert result = notificationService.createAlert(alert);

            assertEquals(0, result.getIsHandled());
        }
    }

    @Nested
    @DisplayName("处理预警")
    class MarkHandled {

        @Test
        @DisplayName("正常标记为已处理")
        void shouldMarkAlertAsHandled() {
            when(alertMapper.selectById(1L)).thenReturn(testAlert);

            Alert result = notificationService.markHandled(1L, "admin-uuid");

            assertNotNull(result);
            assertEquals(1, result.getIsHandled());
            assertEquals("admin-uuid", result.getHandledBy());
            assertNotNull(result.getHandledAt());
            verify(alertMapper).updateById(testAlert);
        }

        @Test
        @DisplayName("预警不存在应抛异常")
        void shouldThrowWhenAlertNotFound() {
            when(alertMapper.selectById(999L)).thenReturn(null);

            assertThrows(RuntimeException.class, () -> notificationService.markHandled(999L, "admin-uuid"));
            verify(alertMapper, never()).updateById(any(Alert.class));
        }
    }

    @Nested
    @DisplayName("获取待处理预警")
    class GetPendingAlerts {

        @Test
        @DisplayName("获取所有未处理预警")
        void shouldGetAllPendingAlerts() {
            Alert alert2 = new Alert();
            alert2.setAlertId(2L);
            alert2.setAlertType("走访逾期");
            alert2.setSeverity("中");
            alert2.setIsHandled(0);

            when(alertMapper.selectList(any())).thenReturn(List.of(testAlert, alert2));

            List<Alert> result = notificationService.getPendingAlerts();

            assertNotNull(result);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("无待处理预警时返回空列表")
        void shouldReturnEmptyList() {
            when(alertMapper.selectList(any())).thenReturn(List.of());

            List<Alert> result = notificationService.getPendingAlerts();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("搜索预警")
    class SearchAlerts {

        @Test
        @DisplayName("按类型和严重程度搜索")
        void shouldSearchByTypeAndSeverity() {
            Page<Alert> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(testAlert));
            mockPage.setTotal(1);

            when(alertMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            var result = notificationService.search("居住证到期", "高", 0, 1, 20);

            assertNotNull(result);
            assertEquals(1, result.getTotal());
            assertEquals("居住证到期", result.getRecords().get(0).getAlertType());
        }

        @Test
        @DisplayName("按已处理状态搜索")
        void shouldSearchByHandledStatus() {
            testAlert.setIsHandled(1);
            Page<Alert> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(testAlert));
            mockPage.setTotal(1);

            when(alertMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            var result = notificationService.search(null, null, 1, 1, 20);

            assertEquals(1, result.getRecords().get(0).getIsHandled());
        }

        @Test
        @DisplayName("无条件全量搜索")
        void shouldSearchAll() {
            Page<Alert> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(testAlert));
            mockPage.setTotal(1);

            when(alertMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            var result = notificationService.search(null, null, null, 1, 20);

            assertEquals(1, result.getTotal());
        }
    }

    @Nested
    @DisplayName("定时扫描任务")
    class ScheduledScans {

        @Test
        @DisplayName("居住证到期扫描（当前为stub，不抛异常即可）")
        void shouldExecutePermitScan() {
            assertDoesNotThrow(() -> notificationService.scanPermitExpiry());
        }

        @Test
        @DisplayName("走访逾期扫描（当前为stub，不抛异常即可）")
        void shouldExecuteVisitScan() {
            assertDoesNotThrow(() -> notificationService.scanVisitOverdue());
        }
    }
}
