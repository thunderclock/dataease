package io.dataease.api.chart.request;

import io.dataease.extensions.view.dto.ChartExtFilterDTO;
import io.dataease.extensions.view.dto.ChartViewFieldDTO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 简化的查询数据请求
 * 从 ChartViewDTO 中抽取核心查询参数
 */
@Data
public class QueryChartDataRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 数据集表ID
     */
    private Long tableId;

    /**
     * 维度字段列表 (DIM)
     */
    private List<ChartViewFieldDTO> dimensions;

    /**
     * 度量字段列表 (MEASURE)
     */
    private List<ChartViewFieldDTO> measures;

    /**
     * 过滤条件 (FILTER)
     */
    private List<ChartExtFilterDTO> filters;

    /**
     * 分页信息
     */
    private PageInfo pageInfo;

    /**
     * 场景ID（用于权限校验）
     */
    private Long sceneId;

    /**
     * 图表ID（用于模板数据获取）
     */
    private Long id;

    /**
     * 数据来源 template 模板数据 dataset 数据集数据
     */
    private String dataFrom;

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
        private Long goPage;

        /**
         * 每页大小
         */
        private Long pageSize;
    }
}

