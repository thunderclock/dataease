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
     * 数据集参数列表（用于替换 SQL 中的参数变量）
     * 参数 ID 会自动从 datasetTableId 和 variableName 构造（格式：{datasetTableId}|DE|{variableName}）
     */
    private List<DatasetParam> params;

    /**
     * 分页信息
     */
    private PageInfo pageInfo;

    /**
     * 数据集参数内部类
     */
    @Data
    public static class DatasetParam implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 参数变量名（SQL中定义的变量名）
         */
        private String variableName;

        /**
         * 数据集表ID
         */
        private Long datasetTableId;

        /**
         * 数据集组ID
         */
        private Long datasetGroupId;

        /**
         * 操作符：eq（等于）、in（包含）、between（区间）等
         * 默认为 "eq"，如果未提供则使用默认值
         */
        private String operator;

        /**
         * 参数值列表
         */
        private List<String> value;
    }

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

