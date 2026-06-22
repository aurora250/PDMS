package com.pdm.household.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pdm.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Data @EqualsAndHashCode(callSuper = true)
@TableName("household_register")
public class HouseholdRegister extends BaseEntity {
    @TableField("household_book_no") private String householdBookNo;
    @TableField("householder_uuid") private String householderUuid;
    @TableField("establish_date") private LocalDate establishDate;
    @TableField("hukou_address") private String hukouAddress;
    @TableField("hukou_area_id") private Long hukouAreaId;
    @TableField("status") private String status;
    @TableField("member_uuid_list") private String memberUuidList;
}
