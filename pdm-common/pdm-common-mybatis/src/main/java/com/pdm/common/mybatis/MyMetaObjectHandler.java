package com.pdm.common.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;

import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充处理器。
 *
 * <p>
 * 在插入和更新操作时自动为标记了 {@link com.baomidou.mybatisplus.annotation.FieldFill} 的字段赋值：
 * <ul>
 * <li><b>插入</b> —— 自动填充 {@code createTime}、{@code updateTime}
 * 为当前时间，{@code isDeleted} 为 0</li>
 * <li><b>更新</b> —— 自动填充 {@code updateTime} 为当前时间</li>
 * </ul>
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入时的字段填充策略。
     *
     * @param metaObject
     *            元对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "isDeleted", Integer.class, 0);
    }

    /**
     * 更新时的字段填充策略。
     *
     * @param metaObject
     *            元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
