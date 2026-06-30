package com.pdm.log;

import com.pdm.common.dto.PageResult;
import com.pdm.log.entity.AuditLog;
import com.pdm.log.entity.LoginLog;
import com.pdm.log.mapper.AuditLogMapper;
import com.pdm.log.mapper.LoginLogMapper;
import com.pdm.log.service.impl.LogServiceImpl;

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

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("日志服务 — 单元测试")
class LogServiceImplTest {

    @Mock
    private AuditLogMapper auditLogMapper;
    @Mock
    private LoginLogMapper loginLogMapper;
    @Mock
    private HttpServletResponse httpServletResponse;
    @InjectMocks
    private LogServiceImpl logService;

    private AuditLog testAuditLog;
    private LoginLog testLoginLog;

    @BeforeEach
    void setUp() {
        testAuditLog = new AuditLog();
        testAuditLog.setLogId(1L);
        testAuditLog.setOperatorUuid("00000000-0000-0000-0000-000000000001");
        testAuditLog.setOperationType("LOGIN");
        testAuditLog.setOperationTime(LocalDateTime.of(2026, 6, 29, 10, 0));
        testAuditLog.setIpAddress("127.0.0.1");

        testLoginLog = new LoginLog();
        testLoginLog.setLogId(1L);
        testLoginLog.setUserUuid("00000000-0000-0000-0000-000000000001");
        testLoginLog.setLoginTime(LocalDateTime.of(2026, 6, 29, 10, 0));
        testLoginLog.setIpAddress("127.0.0.1");
        testLoginLog.setIsSuccess(1);
    }

    @Nested
    @DisplayName("记录审计日志")
    class RecordAuditLog {

        @Test
        @DisplayName("正常记录审计日志，自动设置操作时间")
        void shouldRecordAuditLog() {
            AuditLog log = new AuditLog();
            log.setOperatorUuid("uuid-001");
            log.setOperationType("CREATE");

            logService.recordAuditLog(log);

            assertNotNull(log.getOperationTime());
            verify(auditLogMapper).insert(log);
        }

        @Test
        @DisplayName("已有操作时间则不覆盖")
        void shouldNotOverrideExistingOperationTime() {
            AuditLog log = new AuditLog();
            LocalDateTime existingTime = LocalDateTime.of(2026, 1, 1, 0, 0);
            log.setOperationTime(existingTime);

            logService.recordAuditLog(log);

            assertEquals(existingTime, log.getOperationTime());
            verify(auditLogMapper).insert(log);
        }
    }

    @Nested
    @DisplayName("记录登录日志")
    class RecordLoginLog {

        @Test
        @DisplayName("正常记录登录日志，自动设置登录时间")
        void shouldRecordLoginLog() {
            LoginLog log = new LoginLog();

            logService.recordLoginLog(log);

            assertNotNull(log.getLoginTime());
            verify(loginLogMapper).insert(log);
        }

        @Test
        @DisplayName("已有登录时间则不覆盖")
        void shouldNotOverrideExistingLoginTime() {
            LoginLog log = new LoginLog();
            LocalDateTime existingTime = LocalDateTime.of(2026, 1, 1, 0, 0);
            log.setLoginTime(existingTime);

            logService.recordLoginLog(log);

            assertEquals(existingTime, log.getLoginTime());
            verify(loginLogMapper).insert(log);
        }
    }

    @Nested
    @DisplayName("搜索审计日志")
    class SearchAuditLogs {

        @Test
        @DisplayName("按时间范围和操作类型搜索")
        void shouldSearchByTimeRangeAndType() {
            Page<AuditLog> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(testAuditLog));
            mockPage.setTotal(1);

            when(auditLogMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            PageResult<AuditLog> result = logService.searchAuditLogs(LocalDateTime.of(2026, 6, 1, 0, 0),
                    LocalDateTime.of(2026, 6, 30, 23, 59), null, "LOGIN", 1, 20);

            assertNotNull(result);
            assertEquals(1, result.getTotal());
        }

        @Test
        @DisplayName("按操作人UUID搜索")
        void shouldSearchByOperatorUuid() {
            Page<AuditLog> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(testAuditLog));
            mockPage.setTotal(1);

            when(auditLogMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            PageResult<AuditLog> result = logService.searchAuditLogs(null, null, "00000000-0000-0000-0000-000000000001",
                    null, 1, 20);

            assertNotNull(result);
            assertEquals(1, result.getTotal());
        }

        @Test
        @DisplayName("无匹配结果返回空")
        void shouldReturnEmptyOnNoMatch() {
            Page<AuditLog> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of());
            mockPage.setTotal(0);

            when(auditLogMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            PageResult<AuditLog> result = logService.searchAuditLogs(null, null, null, null, 1, 20);

            assertEquals(0, result.getTotal());
            assertTrue(result.getRecords().isEmpty());
        }
    }

    @Nested
    @DisplayName("搜索登录日志")
    class SearchLoginLogs {

        @Test
        @DisplayName("按用户和登录状态搜索")
        void shouldSearchByUserAndSuccessStatus() {
            Page<LoginLog> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(testLoginLog));
            mockPage.setTotal(1);

            when(loginLogMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            PageResult<LoginLog> result = logService.searchLoginLogs("00000000-0000-0000-0000-000000000001",
                    LocalDateTime.of(2026, 6, 1, 0, 0), LocalDateTime.of(2026, 6, 30, 23, 59), 1, 1, 20);

            assertNotNull(result);
            assertEquals(1, result.getTotal());
            assertEquals(1, result.getRecords().get(0).getIsSuccess());
        }

        @Test
        @DisplayName("按失败登录筛选")
        void shouldFilterByFailedLogin() {
            testLoginLog.setIsSuccess(0);
            Page<LoginLog> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(testLoginLog));
            mockPage.setTotal(1);

            when(loginLogMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            PageResult<LoginLog> result = logService.searchLoginLogs(null, null, null, 0, 1, 20);

            assertEquals(0, result.getRecords().get(0).getIsSuccess());
        }

        @Test
        @DisplayName("无条件全量搜索")
        void shouldSearchAllLoginLogs() {
            Page<LoginLog> mockPage = new Page<>(1, 20);
            mockPage.setRecords(List.of(testLoginLog));
            mockPage.setTotal(1);

            when(loginLogMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            PageResult<LoginLog> result = logService.searchLoginLogs(null, null, null, null, 1, 20);

            assertEquals(1, result.getTotal());
        }
    }

    @Nested
    @DisplayName("导出审计日志")
    class ExportAuditLogs {

        @Test
        @DisplayName("正常导出为Excel（CSV文本格式）")
        void shouldExportAuditLogs() throws IOException {
            List<AuditLog> mockList = List.of(testAuditLog);
            when(auditLogMapper.selectList(any())).thenReturn(mockList);

            PrintWriter mockWriter = mock(PrintWriter.class);
            when(httpServletResponse.getWriter()).thenReturn(mockWriter);

            logService.exportAuditLogs(LocalDateTime.of(2026, 6, 1, 0, 0), LocalDateTime.of(2026, 6, 30, 23, 59), null,
                    null, httpServletResponse);

            verify(httpServletResponse)
                    .setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            verify(httpServletResponse).setHeader(eq("Content-Disposition"), contains(".xlsx"));
            verify(mockWriter).flush();
        }

        @Test
        @DisplayName("无数据时导出空CSV（仅表头）")
        void shouldExportEmptyFileWhenNoData() throws IOException {
            when(auditLogMapper.selectList(any())).thenReturn(List.of());

            PrintWriter mockWriter = mock(PrintWriter.class);
            when(httpServletResponse.getWriter()).thenReturn(mockWriter);

            logService.exportAuditLogs(null, null, null, null, httpServletResponse);

            verify(httpServletResponse)
                    .setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            verify(mockWriter).flush();
        }
    }
}
