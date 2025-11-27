package io.dataease.api.chart.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 表格数据响应
 * 二维表结构，包含字段名和数据行
 */
@Data
public class TableDataResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字段名列表（列名）
     */
    private List<String> columns;

    /**
     * 数据行列表
     * 每行是一个 Map，key 为字段名（dataeaseName），value 为字段值
     */
    private List<Map<String, Object>> rows;

    /**
     * 总记录数
     */
    private Long totalItems;

    /**
     * 总页数
     */
    private Long totalPage;

    /**
     * 当前页码
     */
    private Long currentPage;
}

