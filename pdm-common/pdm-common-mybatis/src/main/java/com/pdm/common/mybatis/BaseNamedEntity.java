package com.pdm.common.mybatis;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * Base entity for tables with named primary keys (non-{@code id} PK).
 * <p>
 * Unlike {@link BaseEntity}, this class does not declare an {@code id} field.
 * Subclasses must declare their own PK field annotated with
 * {@link com.baomidou.mybatisplus.annotation.TableId @TableId}.
 * </p>
 *
 * <p>Used by tables such as:
 * {@code resident_relation} (rid), {@code fp_register_record} (rid),
 * {@code visit_plan} (plan_id), {@code audit_log} (log_id), etc.</p>
 */
@Data
public class BaseNamedEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer isDeleted;
}
