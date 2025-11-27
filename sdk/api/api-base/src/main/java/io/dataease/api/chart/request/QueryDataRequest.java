package io.dataease.api.chart.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 根据字段配置查询数据的请求
 * 传递字段ID和查询相关的配置信息
 */
@Data
public class QueryDataRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 数据集表ID
     */
    private Long tableId;

    /**
     * 维度字段配置列表（包含字段ID和查询配置）
     */
    private List<FieldQueryConfig> dimensions;

    /**
     * 度量字段配置列表（包含字段ID和查询配置）
     */
    private List<FieldQueryConfig> measures;

    /**
     * 过滤条件 (FILTER)
     * 支持嵌套条件（并且、或者）和字段过滤
     */
    private QueryFilterDTO filters;

    /**
     * 分页信息
     */
    private PageInfo pageInfo;

    /**
     * 分页信息内部类
     */
    @Data
    public static class PageInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 当前页码（从1开始）
         */
        private Long pageNum;

        /**
         * 每页大小
         */
        private Long pageSize;
    }
}

