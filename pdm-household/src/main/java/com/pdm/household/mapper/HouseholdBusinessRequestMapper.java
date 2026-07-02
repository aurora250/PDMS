package com.pdm.household.mapper;

import com.pdm.household.entity.HouseholdBusinessRequest;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HouseholdBusinessRequestMapper extends BaseMapper<HouseholdBusinessRequest> {

    /** 带 JSONB 强制转换的插入，绕过 MyBatis-Plus 的类型映射缺陷 */
    @Insert("INSERT INTO household_business_request (applicant_uuid, attachment, business_type, handle_date,"
            + " handle_basis, fee, status, reject_reason, remark, detail_json, is_deleted)"
            + " VALUES (#{applicantUuid}, #{attachment}, #{businessType}, #{handleDate},"
            + " #{handleBasis}, #{fee}, #{status}, #{rejectReason}, #{remark},"
            + " CAST(#{detailJson} AS jsonb), 0)")
    @Options(useGeneratedKeys = true, keyProperty = "rid")
    int insertBusiness(HouseholdBusinessRequest req);

    /** 带 JSONB 强制转换的更新，绕过 MyBatis-Plus 的类型映射缺陷 */
    @Update("UPDATE household_business_request SET handler_uuid=#{handlerUuid},"
            + " applicant_uuid=#{applicantUuid}, attachment=#{attachment},"
            + " business_type=#{businessType}, handle_date=#{handleDate},"
            + " handle_basis=#{handleBasis}, fee=#{fee}, status=#{status},"
            + " reject_reason=#{rejectReason}, remark=#{remark},"
            + " detail_json=CAST(#{detailJson} AS jsonb), update_time=CURRENT_TIMESTAMP"
            + " WHERE rid=#{rid} AND is_deleted=0")
    int updateBusiness(HouseholdBusinessRequest req);
}
