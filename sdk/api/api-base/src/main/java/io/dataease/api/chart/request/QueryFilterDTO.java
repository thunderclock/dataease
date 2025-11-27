package io.dataease.api.chart.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 查询过滤器配置
 * 支持嵌套条件（并且、或者）和字段过滤
 */
@Data
public class QueryFilterDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 逻辑关系：and（并且）或 or（或者）
     */
    private String logic;

    /**
     * 过滤条件项列表
     * 可以是单个条件（FilterConditionItem）或嵌套条件（QueryFilterDTO）
     */
    private List<FilterConditionItem> items;

    /**
     * 单个过滤条件项
     */
    @Data
    public static class FilterConditionItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 条件类型：item（单个条件）或 tree（嵌套条件树）
         */
        private String type;

        /**
         * 字段名（当 type 为 item 时必需）
         * 可以是 originName、name 或 dbFieldName
         */
        private String fieldName;

        /**
         * 关系符（当 type 为 item 时必需）
         * 可选值：eq（等于）、not_eq（不等于）、lt（小于）、le（小于等于）、
         * gt（大于）、ge（大于等于）、in（包含）、not_in（不包含）、
         * like（模糊匹配）、not_like（不模糊匹配）、null（为空）、
         * not_null（不为空）、empty（为空字符串）、not_empty（不为空字符串）、
         * between（区间）
         */
        private String term;

        /**
         * 对比值类型：fixed（固定值）或 relative（相对值）
         */
        private String valueType;

        /**
         * 固定值（当 valueType 为 fixed 时使用）
         * 对于 in、not_in 等操作符，可以是多个值
         */
        private Object value;

        /**
         * 枚举值列表（用于 in、not_in 等操作符）
         */
        private List<Object> enumValue;

        /**
         * 相对值设置（当 valueType 为 relative 时使用）
         */
        private RelativeValueSetting relativeValue;

        /**
         * 嵌套条件树（当 type 为 tree 时使用）
         */
        private QueryFilterDTO subTree;
    }

    /**
     * 相对值设置
     */
    @Data
    public static class RelativeValueSetting implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 相对时间类型：如 today（今天）、yesterday（昨天）、thisWeek（本周）等
         */
        private String timeType;

        /**
         * 相对数量（如：-7 表示7天前，+7 表示7天后）
         */
        private Integer offset;

        /**
         * 时间单位：day（天）、week（周）、month（月）、year（年）
         */
        private String unit;
    }
}

