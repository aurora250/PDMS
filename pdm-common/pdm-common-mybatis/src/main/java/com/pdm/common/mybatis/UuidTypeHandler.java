package com.pdm.common.mybatis;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class UuidTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        if (parameter == null || parameter.isEmpty()) {
            ps.setNull(i, java.sql.Types.OTHER);
        } else {
            ps.setObject(i, UUID.fromString(parameter));
        }
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        UUID uuid = rs.getObject(columnName, UUID.class);
        return uuid != null ? uuid.toString() : null;
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        UUID uuid = rs.getObject(columnIndex, UUID.class);
        return uuid != null ? uuid.toString() : null;
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        UUID uuid = cs.getObject(columnIndex, UUID.class);
        return uuid != null ? uuid.toString() : null;
    }
}
