package io.dataease.chart.server;

import io.dataease.api.chart.CommonDataApi;
import io.dataease.api.chart.dto.TableDataResponse;
import io.dataease.api.chart.request.FieldQueryConfig;
import io.dataease.api.chart.request.QueryChartDataRequest;
import io.dataease.api.chart.request.QueryDataRequest;
import io.dataease.api.chart.request.QueryFilterDTO;
import io.dataease.api.permissions.auth.dto.BusiPerCheckDTO;
import io.dataease.auth.DeLinkPermit;
import io.dataease.chart.constant.ChartConstants;
import io.dataease.chart.manage.ChartDataManage;
import io.dataease.chart.manage.ChartViewManege;
import io.dataease.constant.AuthEnum;
import io.dataease.constant.CommonConstants;
import io.dataease.constant.DeTypeConstants;
import io.dataease.dataset.manage.DatasetTableFieldManage;
import io.dataease.dataset.utils.DatasetUtils;
import io.dataease.engine.constant.ExtFieldConstant;
import io.dataease.exception.DEException;
import io.dataease.extensions.datasource.dto.DatasetTableFieldDTO;
import io.dataease.extensions.view.dto.ChartExtFilterDTO;
import io.dataease.extensions.view.dto.ChartExtRequest;
import io.dataease.extensions.view.dto.ChartFieldCompareDTO;
import io.dataease.extensions.view.dto.ChartViewDTO;
import io.dataease.extensions.view.dto.ChartViewFieldDTO;
import io.dataease.extensions.view.dto.FormatterCfgDTO;
import io.dataease.extensions.view.dto.SqlVariableDetails;
import io.dataease.extensions.view.filter.DynamicTimeSetting;
import io.dataease.extensions.view.filter.FilterTreeItem;
import io.dataease.extensions.view.filter.FilterTreeObj;
import io.dataease.i18n.Lang;
import io.dataease.i18n.Translator;
import io.dataease.result.ResultCode;
import io.dataease.system.manage.CorePermissionManage;
import io.dataease.utils.JsonUtil;
import io.dataease.utils.LogUtil;
import io.dataease.visualization.manage.VisualizationTemplateExtendDataManage;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通用数据查询服务
 * 
 * 注意：此服务的所有 POST 接口都需要 AccessKey 签名验证
 * 请求头必须包含：
 * - X-ACCESS-KEY: AccessKey
 * - X-TIMESTAMP: 时间戳（毫秒）
 * - X-SIGNATURE: 签名（HMAC-SHA256，Base64编码）
 * 
 * 签名算法：signature = Base64(HMAC-SHA256(accessSecret + timestamp + requestBody, accessSecret))
 * 
 * @Author Junjun
 */
@RestController
@RequestMapping("/commonData")
public class CommonDataServer implements CommonDataApi {
    @Resource
    private ChartDataManage chartDataManage;

    @Resource
    private VisualizationTemplateExtendDataManage extendDataManage;

    @Resource
    private DatasetTableFieldManage datasetTableFieldManage;

    @Resource
    private ChartViewManege chartViewManege;

    @Resource
    private CorePermissionManage corePermissionManage;

    @Override
    @DeLinkPermit("#p0.sceneId")
    @PostMapping("/queryChartData")
    public TableDataResponse queryChartData(QueryChartDataRequest request) throws Exception {
        try {
            // 打印请求参数
            LogUtil.info("QueryDataRequest params: " + JsonUtil.toJSONString(request));
            
            ChartViewDTO resultDTO;
            
            // 从模板数据获取（与 getData 方法保持一致）
            if (CommonConstants.VIEW_DATA_FROM.TEMPLATE.equalsIgnoreCase(request.getDataFrom())) {
                if (request.getId() == null) {
                    DEException.throwException(ResultCode.DATA_IS_WRONG.code(), "模板数据获取时图表ID不能为空");
                }
                // 构建 ChartViewDTO 用于模板数据获取
                ChartViewDTO chartViewDTO = new ChartViewDTO();
                chartViewDTO.setId(request.getId());
                chartViewDTO.setSceneId(request.getSceneId());
                resultDTO = extendDataManage.getChartDataInfo(request.getId(), chartViewDTO);
            } else {
                // 从数据集获取数据（与 getData 方法保持一致）
                // 参数校验
                if (request.getTableId() == null) {
                    DEException.throwException(ResultCode.DATA_IS_WRONG.code(), "数据集ID不能为空");
                }
                
                // 检查数据集权限
                BusiPerCheckDTO dto = new BusiPerCheckDTO();
                dto.setId(request.getTableId());
                dto.setAuthEnum(AuthEnum.READ);
                boolean checked = corePermissionManage.checkAuth(dto);
                if (!checked) {
                    DEException.throwException(ResultCode.DATA_IS_WRONG.code(), Translator.get("i18n_no_dataset_permission"));
                }

                // 构建简化的 ChartViewDTO
                ChartViewDTO chartViewDTO = new ChartViewDTO();
                chartViewDTO.setTableId(request.getTableId());
                chartViewDTO.setSceneId(request.getSceneId());
                
                // 设置维度字段（xAxis）和度量字段（yAxis）
                if (CollectionUtils.isNotEmpty(request.getDimensions())) {
                    chartViewDTO.setXAxis(request.getDimensions());
                } else {
                    chartViewDTO.setXAxis(new ArrayList<>());
                }
                
                if (CollectionUtils.isNotEmpty(request.getMeasures())) {
                    chartViewDTO.setYAxis(request.getMeasures());
                } else {
                    chartViewDTO.setYAxis(new ArrayList<>());
                }

                // 设置为表格类型，便于获取表格数据
                chartViewDTO.setType("table-info");
                chartViewDTO.setRender("antv");
                chartViewDTO.setResultMode(ChartConstants.VIEW_RESULT_MODE.ALL);

                // 设置分页信息
                ChartExtRequest chartExtRequest = new ChartExtRequest();
                if (request.getPageInfo() != null) {
                    chartExtRequest.setGoPage(request.getPageInfo().getGoPage());
                    chartExtRequest.setPageSize(request.getPageInfo().getPageSize());
                }
                
                // 设置 customAttr 以启用分页模式，这样才能计算 totalItems 和 totalPage
                Map<String, Object> customAttr = new HashMap<>();
                Map<String, Object> basicStyle = new HashMap<>();
                basicStyle.put("tablePageMode", "page");
                if (request.getPageInfo() != null && request.getPageInfo().getPageSize() != null) {
                    basicStyle.put("tablePageSize", request.getPageInfo().getPageSize().intValue());
                } else {
                    basicStyle.put("tablePageSize", 10); // 默认每页10条
                }
                customAttr.put("basicStyle", basicStyle);
                chartViewDTO.setCustomAttr(customAttr);
                
                // 设置过滤条件（QueryChartDataRequest 使用 List<ChartExtFilterDTO>）
                if (CollectionUtils.isNotEmpty(request.getFilters())) {
                    chartExtRequest.setFilter(request.getFilters());
                }
                
                chartViewDTO.setChartExtRequest(chartExtRequest);

                // 计算数据（与 getData 方法保持一致的处理流程）
                DatasetUtils.viewDecode(chartViewDTO);
                resultDTO = chartDataManage.calcData(chartViewDTO);
                DatasetUtils.viewEncode(resultDTO);
                chartDataManage.encodeData(resultDTO);
            }

            // 构建响应对象
            TableDataResponse response = new TableDataResponse();
            response.setTotalItems(resultDTO.getTotalItems());
            response.setTotalPage(resultDTO.getTotalPage());
            if (request.getPageInfo() != null && request.getPageInfo().getGoPage() != null) {
                response.setCurrentPage(request.getPageInfo().getGoPage());
            } else {
                response.setCurrentPage(1L);
            }

            // 从 data 中提取表格数据
            Map<String, Object> dataMap = resultDTO.getData();
            if (dataMap == null) {
                response.setColumns(new ArrayList<>());
                response.setRows(new ArrayList<>());
                return response;
            }

            // 获取字段列表
            List<ChartViewFieldDTO> fields = (List<ChartViewFieldDTO>) dataMap.get("fields");
            List<String> columns = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(fields)) {
                columns = fields.stream()
                        .map(field -> StringUtils.isNotBlank(field.getChartShowName()) 
                                ? field.getChartShowName() 
                                : (StringUtils.isNotBlank(field.getName()) ? field.getName() : field.getDataeaseName()))
                        .collect(Collectors.toList());
            }

            // 获取表格行数据
            List<Map<String, Object>> tableRow = null;
            Object tableRowObj = dataMap.get("tableRow");
            if (tableRowObj != null) {
                // 如果 tableRow 已存在，需要将 dataeaseName 转换为 columnName
                List<Map<String, Object>> originalTableRow = (List<Map<String, Object>>) tableRowObj;
                tableRow = new ArrayList<>();
                // 构建 dataeaseName 到 columnName 的映射
                Map<String, String> nameMapping = new HashMap<>();
                if (CollectionUtils.isNotEmpty(fields)) {
                    for (ChartViewFieldDTO field : fields) {
                        String columnName = StringUtils.isNotBlank(field.getChartShowName()) 
                                ? field.getChartShowName() 
                                : (StringUtils.isNotBlank(field.getName()) ? field.getName() : field.getDataeaseName());
                        nameMapping.put(field.getDataeaseName(), columnName);
                    }
                }
                // 转换 tableRow 中的 key
                for (Map<String, Object> row : originalTableRow) {
                    Map<String, Object> newRow = new HashMap<>();
                    for (Map.Entry<String, Object> entry : row.entrySet()) {
                        String key = entry.getKey();
                        String newKey = nameMapping.getOrDefault(key, key);
                        newRow.put(newKey, entry.getValue());
                    }
                    tableRow.add(newRow);
                }
            } else {
                // 如果没有 tableRow，尝试从 sourceData 获取
                Object sourceDataObj = dataMap.get("sourceData");
                if (sourceDataObj != null) {
                    List<Object[]> sourceData = (List<Object[]>) sourceDataObj;
                    tableRow = new ArrayList<>();
                    if (CollectionUtils.isNotEmpty(fields) && CollectionUtils.isNotEmpty(sourceData)) {
                        for (Object[] row : sourceData) {
                            Map<String, Object> rowMap = new HashMap<>();
                            for (int i = 0; i < Math.min(fields.size(), row.length); i++) {
                                ChartViewFieldDTO field = fields.get(i);
                                Object value = row[i];
                                // 使用 columnName（优先使用 chartShowName，其次使用 name，最后使用 dataeaseName）
                                String columnName = StringUtils.isNotBlank(field.getChartShowName()) 
                                        ? field.getChartShowName() 
                                        : (StringUtils.isNotBlank(field.getName()) ? field.getName() : field.getDataeaseName());
                                // 根据字段类型处理值
                                if (field.getDeType() == DeTypeConstants.DE_INT 
                                        || field.getDeType() == DeTypeConstants.DE_FLOAT) {
                                    if (value != null && StringUtils.isNotEmpty(value.toString())) {
                                        try {
                                            rowMap.put(columnName, new BigDecimal(value.toString()).setScale(8, java.math.RoundingMode.HALF_UP));
                                        } catch (Exception e) {
                                            rowMap.put(columnName, value);
                                        }
                                    } else {
                                        rowMap.put(columnName, null);
                                    }
                                } else {
                                    rowMap.put(columnName, value != null ? value.toString() : "");
                                }
                            }
                            tableRow.add(rowMap);
                        }
                    }
                }
            }

            if (tableRow == null) {
                tableRow = new ArrayList<>();
            }

            response.setColumns(columns);
            response.setRows(tableRow);

            return response;
        } catch (Exception e) {
            DEException.throwException(ResultCode.DATA_IS_WRONG.code(), e.getMessage() + "\n\n" + ExceptionUtils.getStackTrace(e));
        }
        return null;
    }

    @Override
    @PostMapping("/queryData")
    public TableDataResponse queryData(QueryDataRequest request) throws Exception {
        try {
            // 打印请求参数
            LogUtil.info("QueryDataRequest params: " + JsonUtil.toJSONString(request));
            
            // 参数校验
            if (request.getTableId() == null) {
                DEException.throwException(ResultCode.DATA_IS_WRONG.code(), "数据集ID不能为空");
            }

            // 检查数据集权限
            BusiPerCheckDTO dto = new BusiPerCheckDTO();
            dto.setId(request.getTableId());
            dto.setAuthEnum(AuthEnum.READ);
            boolean checked = corePermissionManage.checkAuth(dto);
            if (!checked) {
                DEException.throwException(ResultCode.DATA_IS_WRONG.code(), Translator.get("i18n_no_dataset_permission"));
            }

            // 校验维度字段和度量字段
            if ((CollectionUtils.isEmpty(request.getDimensions()) || request.getDimensions().stream().allMatch(c -> c == null))
                    && (CollectionUtils.isEmpty(request.getMeasures()) || request.getMeasures().stream().allMatch(c -> c == null))) {
                DEException.throwException(ResultCode.DATA_IS_WRONG.code(), "维度字段和度量字段不能同时为空");
            }

            // 直接从配置构建字段对象
            List<ChartViewFieldDTO> dimensions = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(request.getDimensions())) {
                for (int i = 0; i < request.getDimensions().size(); i++) {
                    FieldQueryConfig config = request.getDimensions().get(i);
                    if (config != null) {
                        // dimensions 中的字段自动设置为维度（groupType = "d"）
                        // 根据在数组中的位置自动设置 index
                        ChartViewFieldDTO field = buildFieldFromConfig(config, request.getTableId(), true, i);
                        if (field != null) {
                            dimensions.add(field);
                        }
                    }
                }
            }

            List<ChartViewFieldDTO> measures = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(request.getMeasures())) {
                for (int i = 0; i < request.getMeasures().size(); i++) {
                    FieldQueryConfig config = request.getMeasures().get(i);
                    if (config != null) {
                        // measures 中的字段自动设置为指标（groupType = "q"）
                        // 根据在数组中的位置自动设置 index（从 dimensions 的长度开始）
                        ChartViewFieldDTO field = buildFieldFromConfig(config, request.getTableId(), false, dimensions.size() + i);
                        if (field != null) {
                            measures.add(field);
                        }
                    }
                }
            }

            // 构建简化的 ChartViewDTO
            ChartViewDTO chartViewDTO = new ChartViewDTO();
            chartViewDTO.setTableId(request.getTableId());
            chartViewDTO.setXAxis(dimensions);
            chartViewDTO.setYAxis(measures);

            // 设置为表格类型，便于获取表格数据
            chartViewDTO.setType("table-info");
            chartViewDTO.setRender("antv");
            chartViewDTO.setResultMode(ChartConstants.VIEW_RESULT_MODE.ALL);

            // 设置分页信息
            ChartExtRequest chartExtRequest = new ChartExtRequest();
            if (request.getPageInfo() != null) {
                chartExtRequest.setGoPage(request.getPageInfo().getPageNum());
                chartExtRequest.setPageSize(request.getPageInfo().getPageSize());
            }
            
            // 设置 customAttr 以启用分页模式，这样才能计算 totalItems 和 totalPage
            Map<String, Object> customAttr = new HashMap<>();
            Map<String, Object> basicStyle = new HashMap<>();
            basicStyle.put("tablePageMode", "page");
            if (request.getPageInfo() != null && request.getPageInfo().getPageSize() != null) {
                basicStyle.put("tablePageSize", request.getPageInfo().getPageSize().intValue());
            } else {
                basicStyle.put("tablePageSize", 10); // 默认每页10条
            }
            customAttr.put("basicStyle", basicStyle);
            chartViewDTO.setCustomAttr(customAttr);
            
            // 设置过滤条件（QueryDataRequest 使用 QueryFilterDTO，需要转换为 FilterTreeObj）
            if (request.getFilters() != null) {
                LogUtil.info("Converting filters: {}", JsonUtil.toJSONString(request.getFilters()));
                FilterTreeObj filterTree = convertQueryFilterToFilterTree(request.getFilters(), request.getTableId());
                if (filterTree != null && CollectionUtils.isNotEmpty(filterTree.getItems())) {
                    LogUtil.info("Filter tree converted: logic={}, items count={}", filterTree.getLogic(), filterTree.getItems().size());
                    ChartExtFilterDTO filterDTO = new ChartExtFilterDTO();
                    filterDTO.setIsTree(true);
                    filterDTO.setCustomFilter(filterTree);
                    chartExtRequest.setFilter(List.of(filterDTO));
                    LogUtil.info("Filter set to ChartExtRequest: {}", JsonUtil.toJSONString(filterDTO));
                } else {
                    LogUtil.warn("Filter tree is null or empty after conversion");
                }
            } else {
                LogUtil.info("No filters in request");
            }

            // 处理数据集参数（如果提供了 params）
            if (CollectionUtils.isNotEmpty(request.getParams())) {
                LogUtil.info("Processing dataset params: {}", JsonUtil.toJSONString(request.getParams()));
                List<ChartExtFilterDTO> paramFilters = new ArrayList<>();
                for (QueryDataRequest.DatasetParam param : request.getParams()) {
                    // 自动构造参数 ID（格式：{datasetTableId}|DE|{variableName}）
                    String paramId = param.getDatasetTableId() + "|DE|" + param.getVariableName();
                    
                    ChartExtFilterDTO paramFilter = new ChartExtFilterDTO();
                    // 设置 fieldId 为参数 ID（包含 "DE" 标识），这样才能被识别为数据集参数
                    paramFilter.setFieldId(paramId);
                    // 如果未提供 operator，默认使用 "eq"
                    String operator = StringUtils.isNotEmpty(param.getOperator()) ? param.getOperator() : "eq";
                    paramFilter.setOperator(operator);
                    paramFilter.setValue(param.getValue());
                    
                    // 构建 SqlVariableDetails 对象
                    SqlVariableDetails sqlVariable = new SqlVariableDetails();
                    sqlVariable.setId(paramId);
                    sqlVariable.setVariableName(param.getVariableName());
                    sqlVariable.setDatasetTableId(param.getDatasetTableId());
                    sqlVariable.setDatasetGroupId(param.getDatasetGroupId());
                    sqlVariable.setOperator(operator);
                    sqlVariable.setValue(param.getValue());
                    // deType 使用默认值 0，如果需要可以从数据集定义中获取
                    
                    paramFilter.setParameters(List.of(sqlVariable));
                    paramFilters.add(paramFilter);
                }
                
                // 将参数 filter 添加到 filters 列表中
                if (CollectionUtils.isEmpty(chartExtRequest.getFilter())) {
                    chartExtRequest.setFilter(new ArrayList<>());
                }
                chartExtRequest.getFilter().addAll(paramFilters);
                LogUtil.info("Added {} param filters to ChartExtRequest", paramFilters.size());
            }
            
            chartViewDTO.setChartExtRequest(chartExtRequest);

            // 计算数据（与 queryChartData 方法保持一致的处理流程）
            DatasetUtils.viewDecode(chartViewDTO);
            ChartViewDTO resultDTO = chartDataManage.calcData(chartViewDTO);
            DatasetUtils.viewEncode(resultDTO);
            chartDataManage.encodeData(resultDTO);

            // 构建响应对象
            TableDataResponse response = new TableDataResponse();
            response.setTotalItems(resultDTO.getTotalItems());
            response.setTotalPage(resultDTO.getTotalPage());
            if (request.getPageInfo() != null && request.getPageInfo().getPageNum() != null) {
                response.setCurrentPage(request.getPageInfo().getPageNum());
            } else {
                response.setCurrentPage(1L);
            }

            // 从 data 中提取表格数据
            Map<String, Object> dataMap = resultDTO.getData();
            if (dataMap == null) {
                response.setColumns(new ArrayList<>());
                response.setRows(new ArrayList<>());
                return response;
            }

            // 获取字段列表
            List<ChartViewFieldDTO> fields = (List<ChartViewFieldDTO>) dataMap.get("fields");
            List<String> columns = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(fields)) {
                columns = fields.stream()
                        .map(field -> StringUtils.isNotBlank(field.getChartShowName()) 
                                ? field.getChartShowName() 
                                : (StringUtils.isNotBlank(field.getName()) ? field.getName() : field.getDataeaseName()))
                        .collect(Collectors.toList());
            }

            // 获取表格行数据
            List<Map<String, Object>> tableRow = null;
            Object tableRowObj = dataMap.get("tableRow");
            if (tableRowObj != null) {
                // 如果 tableRow 已存在，需要将 dataeaseName 转换为 columnName
                List<Map<String, Object>> originalTableRow = (List<Map<String, Object>>) tableRowObj;
                tableRow = new ArrayList<>();
                // 构建 dataeaseName 到 columnName 的映射
                Map<String, String> nameMapping = new HashMap<>();
                if (CollectionUtils.isNotEmpty(fields)) {
                    for (ChartViewFieldDTO field : fields) {
                        String columnName = StringUtils.isNotBlank(field.getChartShowName()) 
                                ? field.getChartShowName() 
                                : (StringUtils.isNotBlank(field.getName()) ? field.getName() : field.getDataeaseName());
                        nameMapping.put(field.getDataeaseName(), columnName);
                    }
                }
                // 转换 tableRow 中的 key
                for (Map<String, Object> row : originalTableRow) {
                    Map<String, Object> newRow = new HashMap<>();
                    for (Map.Entry<String, Object> entry : row.entrySet()) {
                        String key = entry.getKey();
                        String newKey = nameMapping.getOrDefault(key, key);
                        newRow.put(newKey, entry.getValue());
                    }
                    tableRow.add(newRow);
                }
            } else {
                // 如果没有 tableRow，尝试从 sourceData 获取
                Object sourceDataObj = dataMap.get("sourceData");
                if (sourceDataObj != null) {
                    List<Object[]> sourceData = (List<Object[]>) sourceDataObj;
                    tableRow = new ArrayList<>();
                    if (CollectionUtils.isNotEmpty(fields) && CollectionUtils.isNotEmpty(sourceData)) {
                        for (Object[] row : sourceData) {
                            Map<String, Object> rowMap = new HashMap<>();
                            for (int i = 0; i < Math.min(fields.size(), row.length); i++) {
                                ChartViewFieldDTO field = fields.get(i);
                                Object value = row[i];
                                // 使用 columnName（优先使用 chartShowName，其次使用 name，最后使用 dataeaseName）
                                String columnName = StringUtils.isNotBlank(field.getChartShowName()) 
                                        ? field.getChartShowName() 
                                        : (StringUtils.isNotBlank(field.getName()) ? field.getName() : field.getDataeaseName());
                                // 根据字段类型处理值
                                if (field.getDeType() == DeTypeConstants.DE_INT 
                                        || field.getDeType() == DeTypeConstants.DE_FLOAT) {
                                    if (value != null && StringUtils.isNotEmpty(value.toString())) {
                                        try {
                                            rowMap.put(columnName, new BigDecimal(value.toString()).setScale(8, java.math.RoundingMode.HALF_UP));
                                        } catch (Exception e) {
                                            rowMap.put(columnName, value);
                                        }
                                    } else {
                                        rowMap.put(columnName, null);
                                    }
                                } else {
                                    rowMap.put(columnName, value != null ? value.toString() : "");
                                }
                            }
                            tableRow.add(rowMap);
                        }
                    }
                }
            }

            if (tableRow == null) {
                tableRow = new ArrayList<>();
            }

            response.setColumns(columns);
            response.setRows(tableRow);

            return response;
        } catch (Exception e) {
            DEException.throwException(ResultCode.DATA_IS_WRONG.code(), e.getMessage() + "\n\n" + ExceptionUtils.getStackTrace(e));
        }
        return null;
    }

    /**
     * 从配置构建 ChartViewFieldDTO
     * 
     * @param config 字段查询配置
     * @param tableId 数据集ID
     * @param isDimension 是否为维度字段（true=维度"d"，false=指标"q"）
     * @param index 字段在数组中的索引位置（自动设置，不需要从接口传递）
     * @return ChartViewFieldDTO 对象
     */
    private ChartViewFieldDTO buildFieldFromConfig(FieldQueryConfig config, Long tableId, boolean isDimension, int index) {
        // 参数校验
        if (StringUtils.isBlank(config.getFieldName())) {
            DEException.throwException(ResultCode.DATA_IS_WRONG.code(), "字段 fieldName 不能为空");
        }
        if (config.getDeType() == null) {
            DEException.throwException(ResultCode.DATA_IS_WRONG.code(), "字段 deType 不能为空");
        }
        
        // 根据字段在 dimensions 还是 measures 中自动设置 groupType
        String groupType = isDimension ? "d" : "q";

        // 直接使用传入的 tableId 作为 datasetGroupId
        Long datasetGroupId = tableId;
        
        // 检查是否是计算字段（fieldName 包含 [ 和 ] 表示是计算字段表达式）
        String fieldName = config.getFieldName();
        boolean isCalcField = StringUtils.isNotBlank(fieldName) && fieldName.contains("[") && fieldName.contains("]");
        
        // 创建 ChartViewFieldDTO 对象
        ChartViewFieldDTO field = new ChartViewFieldDTO();
        field.setDatasetGroupId(datasetGroupId);
        
        DatasetTableFieldDTO datasetField = null;
        if (!isCalcField) {
            // 非计算字段：从数据集中查询字段信息
            datasetField = findFieldByName(config.getFieldName(), datasetGroupId);
            if (datasetField == null) {
                DEException.throwException(ResultCode.DATA_IS_WRONG.code(), 
                        String.format("在数据集 %s 中未找到字段名为 '%s' 的字段", datasetGroupId, config.getFieldName()));
            }
            
            // 设置基础字段信息（从数据集字段中获取）
            field.setDataeaseName(datasetField.getDataeaseName());
            field.setDeType(config.getDeType() != null ? config.getDeType() : datasetField.getDeType());
            // 根据字段在 dimensions 还是 measures 中自动设置 groupType，忽略配置中的 groupType
            field.setGroupType(groupType);
            
            // 使用配置中的值，如果没有则使用数据集字段中的值
            field.setName(StringUtils.isNotBlank(config.getName()) ? config.getName() : datasetField.getName());
            field.setOriginName(datasetField.getOriginName());
            
            // 设置其他从数据集字段中获取的信息
            if (datasetField.getDatasourceId() != null) {
                field.setDatasourceId(datasetField.getDatasourceId());
            }
            if (datasetField.getDatasetTableId() != null) {
                field.setDatasetTableId(datasetField.getDatasetTableId());
            }
            if (datasetField.getType() != null) {
                field.setType(datasetField.getType());
            }
            
            // 设置 extField（从数据集字段中获取，如果没有则默认为 EXT_NORMAL）
            if (datasetField.getExtField() != null) {
                field.setExtField(datasetField.getExtField());
            } else {
                field.setExtField(ExtFieldConstant.EXT_NORMAL);
            }
        } else {
            // 计算字段：使用配置中的信息，生成唯一ID和dataeaseName
            field.setDeType(config.getDeType());
            // 根据字段在 dimensions 还是 measures 中自动设置 groupType，忽略配置中的 groupType
            field.setGroupType(groupType);
            field.setName(StringUtils.isNotBlank(config.getName()) ? config.getName() : config.getFieldName());
            // 对于计算字段，originName 就是 fieldName（计算表达式）
            field.setOriginName(fieldName);
            
            // 为计算字段生成唯一的ID（使用当前时间戳和随机数）
            long fieldId = System.currentTimeMillis() + (long)(Math.random() * 1000);
            field.setId(fieldId);
            
            // 为计算字段生成 dataeaseName（基于ID）
            field.setDataeaseName("f_" + Long.toHexString(fieldId));
            
            // 设置计算字段标识
            field.setExtField(ExtFieldConstant.EXT_CALC);
            
            // 为计算字段获取数据源信息（从数据集的第一个字段中获取）
            List<DatasetTableFieldDTO> allFields = datasetTableFieldManage.selectByDatasetGroupId(datasetGroupId);
            if (CollectionUtils.isNotEmpty(allFields)) {
                DatasetTableFieldDTO firstField = allFields.get(0);
                if (firstField.getDatasourceId() != null) {
                    field.setDatasourceId(firstField.getDatasourceId());
                }
                if (firstField.getDatasetTableId() != null) {
                    field.setDatasetTableId(firstField.getDatasetTableId());
                }
                if (firstField.getType() != null) {
                    field.setType(firstField.getType());
                }
            }
        }

        // 应用配置中的日期相关设置
        if (StringUtils.isNotBlank(config.getDateStyle())) {
            field.setDateStyle(config.getDateStyle());
        } else {
            field.setDateStyle("y_M_d");
        }
        if (StringUtils.isNotBlank(config.getDatePattern())) {
            field.setDatePattern(config.getDatePattern());
        } else {
            field.setDatePattern("date_sub");
        }
        if (StringUtils.isNotBlank(config.getDateShowFormat())) {
            field.setDateShowFormat(config.getDateShowFormat());
        } else {
            field.setDateShowFormat("y_M_d");
        }
        field.setChartType("bar");

        // 设置默认聚合函数
        if (StringUtils.isBlank(config.getSummary())) {
            if (field.getDeType() == 0 || field.getDeType() == 1 || field.getDeType() == 7) {
                field.setSummary("count");
            } else {
                field.setSummary("sum");
            }
        } else {
            field.setSummary(config.getSummary());
        }

        // 设置默认比较计算配置（如果未传递则使用默认值）
        if (config.getCompareCalc() == null) {
            ChartFieldCompareDTO chartFieldCompareDTO = new ChartFieldCompareDTO();
            chartFieldCompareDTO.setType("none");
            field.setCompareCalc(chartFieldCompareDTO);
        } else {
            field.setCompareCalc(config.getCompareCalc());
        }

        // 设置默认格式化配置（如果未传递则使用默认值）
        if (config.getFormatterCfg() == null) {
            FormatterCfgDTO formatterCfg = new FormatterCfgDTO();
            formatterCfg.setUnitLanguage(Lang.isChinese() ? "ch" : "en");
            field.setFormatterCfg(formatterCfg);
        } else {
            field.setFormatterCfg(config.getFormatterCfg());
        }

        // 设置默认排序
        if (StringUtils.isBlank(config.getSort())) {
            field.setSort("none");
        } else {
            field.setSort(config.getSort());
        }

        // 设置默认过滤
        if (config.getFilter() == null) {
            field.setFilter(new ArrayList<>());
        } else {
            field.setFilter(config.getFilter());
        }

        // 根据在数组中的位置自动设置 index（不需要从接口传递）
        field.setIndex(index);

        // 应用其他配置
        applyFieldConfig(field, config);

        return field;
    }

    /**
     * 根据字段名从数据集中查找字段
     * 支持通过 originName、name 或 dbFieldName 查找
     * 
     * @param fieldName 字段名（可以是 originName、name 或 dbFieldName）
     * @param datasetGroupId 数据集ID
     * @return DatasetTableFieldDTO 对象，如果未找到则返回 null
     */
    private DatasetTableFieldDTO findFieldByName(String fieldName, Long datasetGroupId) {
        if (StringUtils.isBlank(fieldName) || datasetGroupId == null) {
            return null;
        }

        // 获取数据集的所有字段
        List<DatasetTableFieldDTO> fields = datasetTableFieldManage.selectByDatasetGroupId(datasetGroupId);
        
        if (CollectionUtils.isEmpty(fields)) {
            return null;
        }

        // 根据字段名匹配（优先匹配 originName，其次 name，最后 dbFieldName）
        for (DatasetTableFieldDTO field : fields) {
            if (StringUtils.equalsIgnoreCase(field.getOriginName(), fieldName)
                    || StringUtils.equalsIgnoreCase(field.getName(), fieldName)
                    || StringUtils.equalsIgnoreCase(field.getDbFieldName(), fieldName)) {
                return field;
            }
        }

        return null;
    }

    /**
     * 应用字段查询配置到字段对象
     * 
     * @param field 字段对象
     * @param config 查询配置
     */
    private void applyFieldConfig(ChartViewFieldDTO field, FieldQueryConfig config) {
        if (config == null || field == null) {
            return;
        }

        // 应用聚合函数
        if (StringUtils.isNotBlank(config.getSummary())) {
            field.setSummary(config.getSummary());
        }

        // 应用排序
        if (StringUtils.isNotBlank(config.getSort())) {
            field.setSort(config.getSort());
        }

        // 应用过滤条件
        if (CollectionUtils.isNotEmpty(config.getFilter())) {
            field.setFilter(config.getFilter());
        }

        // 应用自定义排序
        if (CollectionUtils.isNotEmpty(config.getCustomSort())) {
            field.setCustomSort(config.getCustomSort());
        }

        // 应用日期相关配置
        if (StringUtils.isNotBlank(config.getDateStyle())) {
            field.setDateStyle(config.getDateStyle());
        }
        if (StringUtils.isNotBlank(config.getDatePattern())) {
            field.setDatePattern(config.getDatePattern());
        }
        if (StringUtils.isNotBlank(config.getDateShowFormat())) {
            field.setDateShowFormat(config.getDateShowFormat());
        }

        // formatterCfg 和 compareCalc 已在 buildFieldFromConfig 中设置默认值，这里不需要处理
        // index 已在 buildFieldFromConfig 中根据数组位置自动设置，不需要从接口传递
    }

    /**
     * 将 QueryFilterDTO 转换为 FilterTreeObj
     * 
     * @param queryFilter QueryFilterDTO 对象
     * @param tableId 数据集ID
     * @return FilterTreeObj 对象
     */
    private FilterTreeObj convertQueryFilterToFilterTree(QueryFilterDTO queryFilter, Long tableId) {
        if (queryFilter == null) {
            return null;
        }

        FilterTreeObj filterTree = new FilterTreeObj();
        filterTree.setLogic(queryFilter.getLogic() != null ? queryFilter.getLogic() : "and");
        filterTree.setItems(new ArrayList<>());

        if (CollectionUtils.isEmpty(queryFilter.getItems())) {
            return filterTree;
        }

        for (QueryFilterDTO.FilterConditionItem item : queryFilter.getItems()) {
            if (item == null) {
                continue;
            }

            FilterTreeItem filterItem = new FilterTreeItem();
            
            if ("tree".equals(item.getType()) && item.getSubTree() != null) {
                // 嵌套条件树
                filterItem.setType("tree");
                filterItem.setSubTree(convertQueryFilterToFilterTree(item.getSubTree(), tableId));
            } else {
                // 单个条件
                filterItem.setType("item");
                filterItem.setFilterType("logic");
                filterItem.setTerm(item.getTerm());
                filterItem.setValueType(item.getValueType());

                // 根据字段名查找字段ID
                if (StringUtils.isNotBlank(item.getFieldName()) && tableId != null) {
                    DatasetTableFieldDTO field = findFieldByName(item.getFieldName(), tableId);
                    if (field != null) {
                        filterItem.setFieldId(field.getId());
                        filterItem.setField(field);
                        LogUtil.info("Filter field found: fieldName={}, fieldId={}", item.getFieldName(), field.getId());
                    } else {
                        LogUtil.warn("Filter field not found: fieldName={}, tableId={}", item.getFieldName(), tableId);
                    }
                } else {
                    LogUtil.warn("Filter field name or tableId is empty: fieldName={}, tableId={}", item.getFieldName(), tableId);
                }

                // 处理值
                if ("fixed".equals(item.getValueType())) {
                    if (item.getEnumValue() != null && !item.getEnumValue().isEmpty()) {
                        // 枚举值列表
                        List<String> enumValueList = new ArrayList<>();
                        for (Object val : item.getEnumValue()) {
                            if (val != null) {
                                enumValueList.add(val.toString());
                            }
                        }
                        if (!enumValueList.isEmpty()) {
                            filterItem.setEnumValue(enumValueList);
                        }
                    } else if (item.getValue() != null) {
                        // 单个值
                        filterItem.setValue(item.getValue().toString());
                    }
                } else if ("relative".equals(item.getValueType()) && item.getRelativeValue() != null) {
                    // 相对值处理（转换为 DynamicTimeSetting）
                    filterItem.setFilterTypeTime("dynamicDate");
                    DynamicTimeSetting dynamicTimeSetting = convertRelativeValueToDynamicTimeSetting(item.getRelativeValue());
                    filterItem.setDynamicTimeSetting(dynamicTimeSetting);
                }
            }

            filterTree.getItems().add(filterItem);
        }

        return filterTree;
    }

    /**
     * 将 RelativeValueSetting 转换为 DynamicTimeSetting
     * 
     * @param relativeValue RelativeValueSetting 对象
     * @return DynamicTimeSetting 对象
     */
    private DynamicTimeSetting convertRelativeValueToDynamicTimeSetting(QueryFilterDTO.RelativeValueSetting relativeValue) {
        if (relativeValue == null) {
            return null;
        }

        DynamicTimeSetting dynamicTimeSetting = new DynamicTimeSetting();
        
        String timeType = relativeValue.getTimeType();
        Integer offset = relativeValue.getOffset() != null ? relativeValue.getOffset() : 0;
        String unit = relativeValue.getUnit() != null ? relativeValue.getUnit() : "day";

        // 处理预设的时间类型（today, yesterday, thisWeek, lastWeek, thisMonth, lastMonth）
        if (StringUtils.isNotBlank(timeType)) {
            // 映射 timeType 到 relativeToCurrent
            switch (timeType) {
                case "today":
                    dynamicTimeSetting.setRelativeToCurrent("today");
                    dynamicTimeSetting.setTimeGranularity("date");
                    dynamicTimeSetting.setRelativeToCurrentType("date");
                    break;
                case "yesterday":
                    dynamicTimeSetting.setRelativeToCurrent("yesterday");
                    dynamicTimeSetting.setTimeGranularity("date");
                    dynamicTimeSetting.setRelativeToCurrentType("date");
                    break;
                case "thisWeek":
                    // thisWeek 需要特殊处理，转换为 custom 模式
                    dynamicTimeSetting.setRelativeToCurrent("custom");
                    dynamicTimeSetting.setTimeGranularity("date");
                    dynamicTimeSetting.setRelativeToCurrentType("date");
                    // 计算本周开始时间（周一）
                    dynamicTimeSetting.setTimeNum(0);
                    dynamicTimeSetting.setAround("f");
                    break;
                case "lastWeek":
                    // lastWeek 转换为 custom 模式，上周
                    dynamicTimeSetting.setRelativeToCurrent("custom");
                    dynamicTimeSetting.setTimeGranularity("date");
                    dynamicTimeSetting.setRelativeToCurrentType("date");
                    dynamicTimeSetting.setTimeNum(7);
                    dynamicTimeSetting.setAround("f");
                    break;
                case "thisMonth":
                    dynamicTimeSetting.setRelativeToCurrent("thisMonth");
                    dynamicTimeSetting.setTimeGranularity("month");
                    dynamicTimeSetting.setRelativeToCurrentType("month");
                    break;
                case "lastMonth":
                    dynamicTimeSetting.setRelativeToCurrent("lastMonth");
                    dynamicTimeSetting.setTimeGranularity("month");
                    dynamicTimeSetting.setRelativeToCurrentType("month");
                    break;
                default:
                    // 未知类型，使用 custom 模式
                    dynamicTimeSetting.setRelativeToCurrent("custom");
                    break;
            }
        } else {
            // 如果没有 timeType，使用 custom 模式
            dynamicTimeSetting.setRelativeToCurrent("custom");
        }

        // 如果有 offset，覆盖 timeNum 和 around
        if (offset != 0) {
            dynamicTimeSetting.setTimeNum(Math.abs(offset));
            dynamicTimeSetting.setAround(offset < 0 ? "f" : "b"); // 负数表示前，正数表示后
        } else if (dynamicTimeSetting.getTimeNum() == null) {
            dynamicTimeSetting.setTimeNum(0);
        }

        // 设置时间粒度和相对类型（如果还没有设置）
        if (StringUtils.isBlank(dynamicTimeSetting.getTimeGranularity())) {
            // 根据 unit 设置
            switch (unit) {
                case "year":
                    dynamicTimeSetting.setTimeGranularity("year");
                    dynamicTimeSetting.setRelativeToCurrentType("year");
                    break;
                case "month":
                    dynamicTimeSetting.setTimeGranularity("month");
                    dynamicTimeSetting.setRelativeToCurrentType("month");
                    break;
                case "week":
                    dynamicTimeSetting.setTimeGranularity("date");
                    dynamicTimeSetting.setRelativeToCurrentType("date");
                    break;
                case "day":
                default:
                    dynamicTimeSetting.setTimeGranularity("date");
                    dynamicTimeSetting.setRelativeToCurrentType("date");
                    break;
            }
        }

        // 如果没有设置 around，默认为 "f"（前）
        if (StringUtils.isBlank(dynamicTimeSetting.getAround())) {
            dynamicTimeSetting.setAround("f");
        }

        return dynamicTimeSetting;
    }
}

