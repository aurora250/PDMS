package com.pdm.household;

import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;
import com.pdm.household.entity.ApprovalPermit;
import com.pdm.household.entity.Area;
import com.pdm.household.entity.HouseholdBusinessRequest;
import com.pdm.household.entity.HouseholdMigrationRequest;
import com.pdm.household.entity.HouseholdRegister;
import com.pdm.household.entity.MigrationPermit;
import com.pdm.household.mapper.ApprovalPermitMapper;
import com.pdm.household.mapper.AreaMapper;
import com.pdm.household.mapper.HouseholdBusinessRequestMapper;
import com.pdm.household.mapper.HouseholdMigrationRequestMapper;
import com.pdm.household.mapper.HouseholdRegisterMapper;
import com.pdm.household.mapper.MigrationPermitMapper;
import com.pdm.household.service.impl.HouseholdServiceImpl;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("户籍管理服务 — 单元测试")
class HouseholdServiceImplTest {

    @Mock
    private HouseholdRegisterMapper bookMapper;
    @Mock
    private HouseholdBusinessRequestMapper businessMapper;
    @Mock
    private HouseholdMigrationRequestMapper migrationMapper;
    @Mock
    private ApprovalPermitMapper approvalPermitMapper;
    @Mock
    private MigrationPermitMapper migrationPermitMapper;
    @Mock
    private AreaMapper areaMapper;
    @InjectMocks
    private HouseholdServiceImpl householdService;

    private HouseholdRegister testBook;
    private HouseholdBusinessRequest testBusiness;
    private HouseholdMigrationRequest testMigration;

    @BeforeEach
    void setUp() {
        testBook = new HouseholdRegister();
        testBook.setId(1L);
        testBook.setHouseholdBookNo("HB202606000001");
        testBook.setHouseholderUuid("00000000-0000-0000-0000-000000000001");
        testBook.setHukouAddress("北京市朝阳区XX街道1号");
        testBook.setStatus("有效");
        testBook.setEstablishDate(LocalDate.of(2026, 6, 1));

        testBusiness = new HouseholdBusinessRequest();
        testBusiness.setRid(301L);
        testBusiness.setApplicantUuid("00000000-0000-0000-0000-000000000001");
        testBusiness.setBusinessType("登记");
        testBusiness.setStatus("审批中");

        testMigration = new HouseholdMigrationRequest();
        testMigration.setRid(401L);
        testMigration.setApplicantUuid("00000000-0000-0000-0000-000000000001");
        testMigration.setBusinessType("迁入");
        testMigration.setStatus("准迁证审批中");
    }

    @Nested
    @DisplayName("户口簿管理")
    class BookManagement {

        @Test
        @DisplayName("申领户口簿，自动生成编号和日期")
        void shouldApplyBook() {
            HouseholdRegister book = new HouseholdRegister();
            book.setHouseholderUuid("uuid-new");
            book.setHukouAddress("北京市朝阳区XX路1号");

            HouseholdRegister result = householdService.applyBook(book);

            assertNotNull(result);
            assertNotNull(result.getHouseholdBookNo());
            assertTrue(result.getHouseholdBookNo().startsWith("HB"));
            assertEquals("审批中", result.getStatus());
            assertNotNull(result.getEstablishDate());
            verify(bookMapper).insert(book);
        }

        @Test
        @DisplayName("补办户口簿")
        void shouldReissueBook() {
            when(bookMapper.selectByBookNo("HB202606000001")).thenReturn(testBook);

            HouseholdRegister result = householdService.reissueBook("HB202606000001");

            assertNotNull(result);
            assertEquals("HB202606000001", result.getHouseholdBookNo());
        }

        @Test
        @DisplayName("补办不存在的户口簿应抛异常")
        void shouldThrowOnReissueNonExistentBook() {
            when(bookMapper.selectByBookNo("HB999")).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class, () -> householdService.reissueBook("HB999"));
            assertEquals(ErrorCode.HOUSEHOLD_BOOK_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("换发户口簿")
        void shouldRenewBook() {
            when(bookMapper.selectByBookNo("HB202606000001")).thenReturn(testBook);

            HouseholdRegister result = householdService.renewBook("HB202606000001");

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("户籍业务")
    class BusinessRequest {

        @Test
        @DisplayName("提交户籍业务申请")
        void shouldSubmitBusinessRequest() {
            HouseholdBusinessRequest request = new HouseholdBusinessRequest();
            request.setBusinessType("登记");
            request.setApplicantUuid("uuid-001");

            HouseholdBusinessRequest result = householdService.submitBusiness(request);

            assertNotNull(result);
            assertEquals("审批中", result.getStatus());
            assertNotNull(result.getHandleDate());
            verify(businessMapper).insert(request);
        }

        @Test
        @DisplayName("审批通过户籍业务")
        void shouldApproveBusiness() {
            when(businessMapper.selectById(301L)).thenReturn(testBusiness);

            HouseholdBusinessRequest result = householdService.approveBusiness(301L, "通过", "admin-uuid", null);

            assertNotNull(result);
            assertEquals("已批准", result.getStatus());
            verify(businessMapper).updateById(testBusiness);
        }

        @Test
        @DisplayName("驳回户籍业务")
        void shouldRejectBusiness() {
            when(businessMapper.selectById(301L)).thenReturn(testBusiness);

            HouseholdBusinessRequest result = householdService.approveBusiness(301L, "驳回", "admin-uuid", "材料不齐全");

            assertNotNull(result);
            assertEquals("已驳回", result.getStatus());
            assertEquals("材料不齐全", result.getRejectReason());
            verify(businessMapper).updateById(testBusiness);
        }

        @Test
        @DisplayName("业务不存在应抛异常")
        void shouldThrowWhenBusinessNotFound() {
            when(businessMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> householdService.approveBusiness(999L, "通过", "admin-uuid", null));
            assertEquals(ErrorCode.DATA_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("迁移管理")
    class MigrationManagement {

        @Test
        @DisplayName("提交迁移申请")
        void shouldSubmitMigration() {
            HouseholdMigrationRequest request = new HouseholdMigrationRequest();
            request.setBusinessType("迁出");
            request.setApplicantUuid("uuid-001");

            HouseholdMigrationRequest result = householdService.submitMigration(request);

            assertNotNull(result);
            assertEquals("准迁证审批中", result.getStatus());
            verify(migrationMapper).insert(request);
        }

        @Test
        @DisplayName("审批通过迁移申请")
        void shouldApproveMigration() {
            when(migrationMapper.selectById(401L)).thenReturn(testMigration);

            HouseholdMigrationRequest result = householdService.approveMigration(401L, "通过", "admin-uuid", null);

            assertNotNull(result);
            assertEquals("准迁证已批准", result.getStatus());
            verify(migrationMapper).updateById(testMigration);
        }

        @Test
        @DisplayName("驳回迁移申请")
        void shouldRejectMigration() {
            when(migrationMapper.selectById(401L)).thenReturn(testMigration);

            HouseholdMigrationRequest result = householdService.approveMigration(401L, "驳回", "admin-uuid", "不符合迁移条件");

            assertNotNull(result);
            assertEquals("准迁证审批驳回", result.getStatus());
            assertEquals("不符合迁移条件", result.getRejectReason());
        }

        @Test
        @DisplayName("迁移申请不存在应抛异常")
        void shouldThrowWhenMigrationNotFound() {
            when(migrationMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> householdService.approveMigration(999L, "通过", "admin-uuid", null));
            assertEquals(ErrorCode.MIGRATION_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("查看迁移轨迹")
        void shouldGetMigrationTrace() {
            HouseholdMigrationRequest m2 = new HouseholdMigrationRequest();
            m2.setRid(402L);
            m2.setBusinessType("迁出");

            when(migrationMapper.selectList(any())).thenReturn(List.of(testMigration, m2));

            List<HouseholdMigrationRequest> result = householdService
                    .getMigrationTrace("00000000-0000-0000-0000-000000000001");

            assertNotNull(result);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("无迁移记录时返回空列表")
        void shouldReturnEmptyTrace() {
            when(migrationMapper.selectList(any())).thenReturn(List.of());

            List<HouseholdMigrationRequest> result = householdService.getMigrationTrace("uuid-no-migration");

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("证件管理")
    class PermitManagement {

        @Test
        @DisplayName("签发准迁证")
        void shouldIssueApprovalPermit() {
            ApprovalPermit permit = new ApprovalPermit();

            ApprovalPermit result = householdService.issueApprovalPermit(permit);

            assertNotNull(result);
            assertNotNull(result.getPermitNo());
            assertNotNull(result.getPermitNo());
            assertEquals(16, result.getPermitNo().length());
            assertEquals("有效", result.getStatus());
            assertNotNull(result.getIssueDate());
            verify(approvalPermitMapper).insert(permit);
        }

        @Test
        @DisplayName("签发迁移证")
        void shouldIssueMigrationPermit() {
            MigrationPermit permit = new MigrationPermit();

            MigrationPermit result = householdService.issueMigrationPermit(permit);

            assertNotNull(result);
            assertNotNull(result.getPermitNo());
            assertNotNull(result.getPermitNo());
            assertEquals(16, result.getPermitNo().length());
            assertEquals("有效", result.getStatus());
            assertNotNull(result.getIssueDate());
            verify(migrationPermitMapper).insert(permit);
        }
    }

    @Nested
    @DisplayName("行政区划查询")
    class AreaQueries {

        @Test
        @DisplayName("获取省级区域树")
        void shouldGetProvinceAreaTree() {
            Area guangdong = new Area();
            guangdong.setAreaId(440000L);
            guangdong.setAreaName("广东省");
            guangdong.setAreaLevel("省");

            when(areaMapper.selectByLevel("省")).thenReturn(List.of(guangdong));

            List<Area> result = householdService.getAreaTree();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("广东省", result.get(0).getAreaName());
        }

        @Test
        @DisplayName("按父级ID获取子区域")
        void shouldGetAreasByParentId() {
            Area shenzhen = new Area();
            shenzhen.setAreaId(440300L);
            shenzhen.setAreaName("深圳市");
            shenzhen.setAreaLevel("市");

            when(areaMapper.selectByParentId(440000L)).thenReturn(List.of(shenzhen));

            List<Area> result = householdService.getAreasByParent(440000L);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("深圳市", result.get(0).getAreaName());
        }

        @Test
        @DisplayName("获取区域祖先链")
        void shouldGetAreaAncestors() {
            Area province = new Area();
            province.setAreaId(440000L);
            province.setAreaName("广东省");
            Area city = new Area();
            city.setAreaId(440300L);
            city.setAreaName("深圳市");

            when(areaMapper.selectAncestors(440305L)).thenReturn(List.of(province, city));

            List<Area> result = householdService.getAreaAncestors(440305L);

            assertNotNull(result);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("获取区域路径字符串")
        void shouldGetAreaPath() {
            when(areaMapper.selectAreaPath(440305L)).thenReturn("广东省/深圳市/南山区");

            String result = householdService.getAreaPath(440305L);

            assertEquals("广东省/深圳市/南山区", result);
        }

        @Test
        @DisplayName("获取全部区域（级联选择器）")
        void shouldGetAllAreas() {
            Area province = new Area();
            province.setAreaId(440000L);
            province.setAreaName("广东省");

            when(areaMapper.selectAll()).thenReturn(List.of(province));

            List<Area> result = householdService.getAllAreas();

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }
}
