package io.dataease.api.chart;

import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.dataease.api.chart.dto.TableDataResponse;
import io.dataease.api.chart.request.QueryDataRequest;
import io.dataease.api.chart.request.QueryChartDataRequest;
import io.dataease.auth.DeApiPath;
import io.dataease.auth.DeLinkPermit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static io.dataease.constant.AuthResourceEnum.PANEL;

/**
 * @Author Junjun
 */
@Tag(name = "通用数据查询")
@ApiSupport(order = 990)
@DeApiPath(value = "/commonData", rt = PANEL)
public interface CommonDataApi {
    @Operation(summary = "查询数据（简化版）")
    @PostMapping("/queryChartData")
    @DeLinkPermit("#p0.sceneId")
    TableDataResponse queryChartData(@RequestBody QueryChartDataRequest request) throws Exception;

    @Operation(summary = "根据字段ID查询数据")
    @PostMapping("/queryData")
    TableDataResponse queryData(@RequestBody QueryDataRequest request) throws Exception;
}

