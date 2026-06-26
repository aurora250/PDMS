package com.pdm.common.mybatis;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * 实体基类（带自增主键）。
 *
 * <p>
 * 提供所有实体共用的基础字段：
 * <ul>
 * <li>{@code id} —— 自增主键</li>
 * <li>{@code createTime} —— 创建时间（插入时自动填充）</li>
 * <li>{@code updateTime} —— 更新时间（插入和更新时自动填充）</li>
 * <li>{@code isDeleted} —— 逻辑删除标记（0=未删除, 1=已删除）</li>
 * </ul>
 *
 * <p>
 * 适用于以自增 ID 为主键的表，如 {@code police}、{@code sys_user}。
 * </p>
 */
@Data
public class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 自增主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

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
