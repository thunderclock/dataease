package io.dataease.datasource.utils;

import io.dataease.extensions.datasource.dto.DatasourceRequest;
import io.dataease.extensions.datasource.dto.DatasourceSchemaDTO;
import io.dataease.extensions.datasource.dto.TableField;
import io.dataease.datasource.provider.CalciteProvider;
import io.dataease.extensions.datasource.dto.ConnectionObj;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

/**
 * 字段注释获取工具类
 */
public class FieldCommentUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(FieldCommentUtils.class);
    
    /**
     * 为字段列表添加注释信息
     */
    public static void enrichFieldComments(List<TableField> fields, DatasourceRequest request) {
        logger.error("enrichFieldComments called with fields: {}, request: {}", fields.size(), request);
        
        if (fields == null || fields.isEmpty() || request == null) {
            logger.error("enrichFieldComments early return: fields={}, request={}", fields == null ? "null" : "empty", request == null ? "null" : "not null");
            return;
        }
        
        String dbType = request.getDatasource().getType();
        String tableName = request.getTable();
        
        logger.error("enrichFieldComments dbType: {}, tableName: {}", dbType, tableName);
        
        if (StringUtils.isEmpty(tableName)) {
            logger.error("enrichFieldComments early return: tableName is empty");
            // 对于没有表名的情况，无法获取字段注释
            return;
        }
        
        try {
            switch (dbType.toLowerCase()) {
                case "doris":
                    enrichDorisComments(fields, request, tableName);
                    break;
                case "mysql":
                case "mariadb":
                    enrichMySQLComments(fields, request, tableName);
                    break;
                case "oracle":
                    enrichOracleComments(fields, request, tableName);
                    break;
                case "pg":
                case "postgresql":
                    enrichPostgreSQLComments(fields, request, tableName);
                    break;
                case "sqlserver":
                    enrichSQLServerComments(fields, request, tableName);
                    break;
                default:
                    // 其他数据库类型暂不支持
                    break;
            }
        } catch (Exception e) {
            logger.warn("Failed to enrich field comments for table {}: {}", tableName, e.getMessage());
        }
    }
    
    /**
     * 为Doris字段添加注释
     */
    private static void enrichDorisComments(List<TableField> fields, DatasourceRequest request, String tableName) {
        try (Connection connection = getConnection(request);
             Statement statement = connection.createStatement()) {
            
            String sql = "SELECT COLUMN_NAME, COLUMN_COMMENT FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
            
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, tableName);
                
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String columnName = rs.getString("COLUMN_NAME");
                        String comment = rs.getString("COLUMN_COMMENT");
                        
                        // 找到对应的字段并设置注释
                        for (TableField field : fields) {
                            if (columnName.equals(field.getOriginName())) {
                                if (StringUtils.isNotEmpty(comment)) {
                                    field.setDescription(comment);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to query Doris comments: {}", e.getMessage());
        }
    }
    
    /**
     * 为MySQL字段添加注释
     */
    private static void enrichMySQLComments(List<TableField> fields, DatasourceRequest request, String tableName) {
        try (Connection connection = getConnection(request);
             Statement statement = connection.createStatement()) {
            
            String sql = "SELECT COLUMN_NAME, COLUMN_COMMENT FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
            
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, tableName);
                
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String columnName = rs.getString("COLUMN_NAME");
                        String comment = rs.getString("COLUMN_COMMENT");
                        
                        // 找到对应的字段并设置注释
                        for (TableField field : fields) {
                            if (columnName.equals(field.getOriginName())) {
                                if (StringUtils.isNotEmpty(comment)) {
                                    field.setDescription(comment);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to query MySQL comments: {}", e.getMessage());
        }
    }
    
    /**
     * 为Oracle字段添加注释
     */
    private static void enrichOracleComments(List<TableField> fields, DatasourceRequest request, String tableName) {
        try (Connection connection = getConnection(request);
             Statement statement = connection.createStatement()) {
            
            String sql = "SELECT COLUMN_NAME, COMMENTS FROM USER_COL_COMMENTS " +
                        "WHERE TABLE_NAME = ?";
            
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, tableName.toUpperCase());
                
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String columnName = rs.getString("COLUMN_NAME");
                        String comment = rs.getString("COMMENTS");
                        
                        // 找到对应的字段并设置注释
                        for (TableField field : fields) {
                            if (columnName.equalsIgnoreCase(field.getName()) || columnName.equalsIgnoreCase(field.getOriginName())) {
                                if (StringUtils.isNotEmpty(comment)) {
                                    field.setDescription(comment);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to query Oracle comments: {}", e.getMessage());
        }
    }
    
    /**
     * 为PostgreSQL字段添加注释
     */
    private static void enrichPostgreSQLComments(List<TableField> fields, DatasourceRequest request, String tableName) {
        try (Connection connection = getConnection(request);
             Statement statement = connection.createStatement()) {
            
            String sql = "SELECT a.attname as column_name, obj_description(a.attrelid, 'pg_class') as comment " +
                        "FROM pg_class c " +
                        "JOIN pg_namespace n ON n.oid = c.relnamespace " +
                        "JOIN pg_attribute a ON a.attrelid = c.oid " +
                        "WHERE c.relname = ? AND n.nspname = current_schema() AND a.attnum > 0";
            
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, tableName);
                
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String columnName = rs.getString("column_name");
                        String comment = rs.getString("comment");
                        
                        // 找到对应的字段并设置注释
                        for (TableField field : fields) {
                            if (columnName.equals(field.getOriginName())) {
                                if (StringUtils.isNotEmpty(comment)) {
                                    field.setDescription(comment);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to query PostgreSQL comments: {}", e.getMessage());
        }
    }
    
    /**
     * 为SQL Server字段添加注释
     */
    private static void enrichSQLServerComments(List<TableField> fields, DatasourceRequest request, String tableName) {
        try (Connection connection = getConnection(request);
             Statement statement = connection.createStatement()) {
            
            String sql = "SELECT c.name as column_name, ep.value as comment " +
                        "FROM sys.tables t " +
                        "JOIN sys.columns c ON c.object_id = t.object_id " +
                        "LEFT JOIN sys.extended_properties ep ON ep.major_id = t.object_id " +
                        "AND ep.minor_id = c.column_id AND ep.name = 'MS_Description' " +
                        "WHERE t.name = ?";
            
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, tableName);
                
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String columnName = rs.getString("column_name");
                        String comment = rs.getString("comment");
                        
                        // 找到对应的字段并设置注释
                        for (TableField field : fields) {
                            if (columnName.equals(field.getOriginName())) {
                                if (StringUtils.isNotEmpty(comment)) {
                                    field.setDescription(comment);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to query SQL Server comments: {}", e.getMessage());
        }
    }
    
    /**
     * 获取数据库连接
     */
    private static Connection getConnection(DatasourceRequest request) throws Exception {
        // 使用CalciteProvider的getConnection方法获取连接
        CalciteProvider provider = new CalciteProvider();
        ConnectionObj connectionObj = provider.getConnection(request.getDatasource());
        return connectionObj.getConnection();
    }
}
