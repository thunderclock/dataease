package io.dataease.api.chart.request;

import io.dataease.extensions.view.dto.ChartFieldCompareDTO;
import io.dataease.extensions.view.dto.ChartViewFieldFilterDTO;
import io.dataease.extensions.view.dto.FormatterCfgDTO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 字段查询配置
 * 包含完整的字段信息和查询相关的配置
 */
@Data
public class FieldQueryConfig implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字段名（必需）
     * 可以是 originName、name 或 dbFieldName，系统会根据此字段名从数据集中解析出 dataeaseName
     */
    private String fieldName;

    /**
     * 字段类型（必需）
     * dataease字段类型：0-文本，1-时间，2-整型数值，3-浮点数值，4-布尔，5-地理位置，6-二进制, 7-URL
     */
    private Integer deType;

    /**
     * 维度/指标标识（可选，不需要传递）
     * 系统会根据字段在 dimensions 还是 measures 中自动设置：
     * - dimensions 中的字段自动设置为 "d"（维度）
     * - measures 中的字段自动设置为 "q"（指标）
     * 如果传递了此字段，也会被忽略
     */
    private String groupType;

    /**
     * 字段名用于展示（可选，如果未提供，将从数据集字段中获取）
     */
    private String name;

    /**
     * 聚合函数（如 sum, count, avg 等）
     */
    private String summary;

    /**
     * 排序方式（如 asc, desc, none）
     */
    private String sort;

    /**
     * 过滤条件
     */
    private List<ChartViewFieldFilterDTO> filter;

    /**
     * 自定义排序
     */
    private List<String> customSort;

    /**
     * 日期解析格式
     */
    private String dateStyle;

    /**
     * 日期分隔符
     */
    private String datePattern;

    /**
     * 日期显示格式
     */
    private String dateShowFormat;

    /**
     * 格式化配置（可选，如果不传递则使用默认值）
     */
    private FormatterCfgDTO formatterCfg;

    /**
     * 比较计算配置（可选，如果不传递则使用默认值）
     */
    private ChartFieldCompareDTO compareCalc;
}

