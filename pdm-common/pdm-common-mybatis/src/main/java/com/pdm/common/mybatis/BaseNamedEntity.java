package com.pdm.common.mybatis;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * 实体基类（无自增主键，适用于以业务键为主键的表）。
 *
 * <p>与 {@link BaseEntity} 的区别是不包含 {@code id} 字段， 适用于以自定义业务键（如 {@code group_id}）为主键的表，如 {@code
 * permission_group}。 提供创建时间、更新时间、逻辑删除标记三个公共字段。
 */
@Data
public class BaseNamedEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 创建时间（插入时自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（插入和更新时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记（0=未删除, 1=已删除） */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer isDeleted;
}
