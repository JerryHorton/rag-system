package cn.cug.sxy.ai.infrastructure.dao.mybatis.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @version 1.0
 * @Date 2025/9/9 17:27
 * @Description
 * @Author jerryhotton
 */

public class PgVectorFloatArrayTypeHandler extends BaseTypeHandler<float[]> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, float[] parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null || parameter.length == 0) {
            ps.setObject(i, null);
            return;
        }
        PGobject vector = new PGobject();
        vector.setType("vector");
        vector.setValue(toVectorLiteral(parameter));
        ps.setObject(i, vector);
    }

    @Override
    public float[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Object obj = rs.getObject(columnName);
        return parseVectorObject(obj);
    }

    @Override
    public float[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Object obj = rs.getObject(columnIndex);
        return parseVectorObject(obj);
    }

    @Override
    public float[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Object obj = cs.getObject(columnIndex);
        return parseVectorObject(obj);
    }

    private float[] parseVectorObject(Object obj) throws SQLException {
        if (obj == null) return null;
        String text;
        if (obj instanceof PGobject) {
            text = ((PGobject) obj).getValue();
        } else {
            text = obj.toString();
        }
        if (text == null || text.isEmpty()) return null;
        // 兼容形如 "[0.1,0.2]" 或 "0.1,0.2"
        String s = text.trim();
        if (s.startsWith("[") && s.endsWith("]")) {
            s = s.substring(1, s.length() - 1);
        }
        s = s.trim();
        if (s.isEmpty()) return new float[0];
        String[] parts = s.split(",");
        float[] arr = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            arr[i] = Float.parseFloat(parts[i].trim());
        }
        return arr;
    }

    private String toVectorLiteral(float[] f) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < f.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(Float.toString(f[i]));
        }
        sb.append(']');
        return sb.toString();
    }

}
