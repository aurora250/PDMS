package com.pdm.common.mybatis;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * PostgreSQL UUID 类型与 Java String 之间的类型转换器。
 *
 * <p>用于 MyBatis 在读写数据库 {@code uuid} 类型字段时进行自动转换：
 *
 * <ul>
 *   <li>写入时将 {@code String} 转为 {@code UUID} 对象存入 PostgreSQL
 *   <li>读取时将 PostgreSQL 返回的 {@code UUID} 对象转为 {@code String}
 *   <li>空值或空字符串写入时设为 SQL NULL
 * </ul>
 */
public class UuidTypeHandler extends BaseTypeHandler<String> {

    /**
     * 设置 PreparedStatement 中的非空参数。
     *
     * @param ps 预编译语句
     * @param i 参数索引
     * @param parameter UUID 字符串或空字符串
     * @param jdbcType JDBC 类型
     * @throws SQLException SQL 异常
     */
    @Override
    public void setNonNullParameter(
            PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null || parameter.isEmpty()) {
            ps.setNull(i, java.sql.Types.OTHER);
        } else {
            ps.setObject(i, UUID.fromString(parameter));
        }
    }

    /**
     * 从 ResultSet 的列名获取 UUID 并转为 String。
     *
     * @param rs 结果集
     * @param columnName 列名
     * @return UUID 字符串，为 null 时返回 {@code null}
     * @throws SQLException SQL 异常
     */
    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        UUID uuid = rs.getObject(columnName, UUID.class);
        return uuid != null ? uuid.toString() : null;
    }

    /**
     * 从 ResultSet 的列索引获取 UUID 并转为 String。
     *
     * @param rs 结果集
     * @param columnIndex 列索引
     * @return UUID 字符串，为 null 时返回 {@code null}
     * @throws SQLException SQL 异常
     */
    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        UUID uuid = rs.getObject(columnIndex, UUID.class);
        return uuid != null ? uuid.toString() : null;
    }

    /**
     * 从 CallableStatement 获取 UUID 并转为 String。
     *
     * @param cs 可调用语句
     * @param columnIndex 列索引
     * @return UUID 字符串，为 null 时返回 {@code null}
     * @throws SQLException SQL 异常
     */
    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        UUID uuid = cs.getObject(columnIndex, UUID.class);
        return uuid != null ? uuid.toString() : null;
    }
}
