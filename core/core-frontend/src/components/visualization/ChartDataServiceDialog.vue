<template>
  <el-dialog
    v-model="dialogVisible"
    :title="t('visualization.data_service')"
    width="80%"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="data-service-dialog">
      <el-tabs v-model="activeTab" class="service-tabs">
        <!-- AccessKey 配置标签页 -->
        <el-tab-pane :label="t('visualization.access_key_config')" name="accessKey">
          <div class="access-key-config">
            <el-form :model="accessKeyForm" label-width="150px" label-position="left">
              <el-form-item :label="t('visualization.access_key')">
                <el-input
                  v-model="accessKeyForm.accessKey"
                  :placeholder="t('visualization.access_key_placeholder')"
                  type="textarea"
                  :rows="2"
                />
              </el-form-item>
              <el-form-item :label="t('visualization.access_secret')">
                <el-input
                  v-model="accessKeyForm.accessSecret"
                  :placeholder="t('visualization.access_secret_placeholder')"
                  type="textarea"
                  :rows="2"
                  show-password
                />
              </el-form-item>
              <el-form-item>
                <el-alert
                  :title="t('visualization.access_key_tip')"
                  type="info"
                  :closable="false"
                  show-icon
                />
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- queryChartData 标签页（使用ID等信息） -->
        <el-tab-pane :label="t('visualization.query_chart_data')" name="queryChartData">
          <el-tabs v-model="queryChartDataSubTab" type="border-card" class="sub-tabs">
            <!-- 配置子标签页 -->
            <el-tab-pane :label="t('visualization.config')" name="config">
              <div class="config-form">
                <el-form :model="configForm" label-width="120px" label-position="left">
                  <el-form-item :label="t('dataset.dataset')">
                    <el-input
                      v-model="configForm.tableId"
                      :placeholder="t('dataset.dataset_id')"
                      type="number"
                    />
                  </el-form-item>
                  <el-form-item :label="t('chart.dimension')">
                    <el-select
                      v-model="configForm.dimensions"
                      multiple
                      filterable
                      :placeholder="t('chart.select_dimension')"
                      style="width: 100%"
                    >
                      <el-option
                        v-for="field in availableFields"
                        :key="field.id"
                        :label="field.name || field.chartShowName"
                        :value="field"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item :label="t('chart.quota')">
                    <el-select
                      v-model="configForm.measures"
                      multiple
                      filterable
                      :placeholder="t('chart.select_quota')"
                      style="width: 100%"
                    >
                      <el-option
                        v-for="field in availableFields"
                        :key="field.id"
                        :label="field.name || field.chartShowName"
                        :value="field"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item :label="t('chart.filter')">
                    <el-button type="primary" size="small" @click="addFilter">
                      {{ t('chart.add_filter') }}
                    </el-button>
                    <div
                      v-if="configForm.filters && configForm.filters.length > 0"
                      class="filter-list"
                    >
                      <div
                        v-for="(filter, index) in configForm.filters"
                        :key="index"
                        class="filter-item"
                      >
                        <span>{{ filter.fieldId }}: {{ filter.operator }}</span>
                        <el-button type="danger" size="small" text @click="removeFilter(index)">{{
                          t('chart.delete')
                        }}</el-button>
                      </div>
                    </div>
                  </el-form-item>
                  <el-form-item :label="t('chart.pagination')">
                    <el-row :gutter="20">
                      <el-col :span="12">
                        <el-input-number
                          v-model="configForm.pageInfo.goPage"
                          :min="1"
                          :label="t('chart.current_page')"
                          style="width: 100%"
                        />
                      </el-col>
                      <el-col :span="12">
                        <el-input-number
                          v-model="configForm.pageInfo.pageSize"
                          :min="1"
                          :max="10000"
                          :label="t('chart.page_size')"
                          style="width: 100%"
                        />
                      </el-col>
                    </el-row>
                  </el-form-item>
                </el-form>
              </div>
            </el-tab-pane>

            <!-- 调试子标签页 -->
            <el-tab-pane :label="t('visualization.debug')" name="debug">
              <div class="debug-panel">
                <div class="debug-actions">
                  <el-button
                    type="primary"
                    :loading="queryChartDataDebugLoading"
                    @click="testQueryChartData"
                  >
                    {{ t('visualization.test_query') }}
                  </el-button>
                  <el-button @click="formatQueryChartDataRequest">
                    {{ t('visualization.format_request') }}
                  </el-button>
                  <el-button @click="copyQueryChartDataRequest">
                    {{ t('visualization.copy_request') }}
                  </el-button>
                </div>
                <el-divider />
                <div class="request-response">
                  <div class="request-section">
                    <h4>{{ t('visualization.request') }}</h4>
                    <el-input
                      v-model="queryChartDataRequestJson"
                      type="textarea"
                      :rows="10"
                      readonly
                      class="json-view"
                    />
                  </div>
                  <div class="response-section">
                    <h4>{{ t('visualization.response') }}</h4>
                    <el-input
                      v-model="queryChartDataResponseJson"
                      type="textarea"
                      :rows="10"
                      readonly
                      class="json-view"
                    />
                  </div>
                </div>
                <div v-if="queryChartDataDebugResult" class="result-table">
                  <h4>{{ t('visualization.result_data') }}</h4>
                  <el-table
                    :data="queryChartDataDebugResult.rows"
                    border
                    style="width: 100%"
                    max-height="400"
                  >
                    <el-table-column
                      v-for="column in queryChartDataDebugResult.columns"
                      :key="column"
                      :prop="column"
                      :label="column"
                    />
                  </el-table>
                  <div class="pagination-info">
                    <span
                      >{{ t('visualization.total_items') }}:
                      {{ queryChartDataDebugResult.totalItems }}</span
                    >
                    <span
                      >{{ t('visualization.total_pages') }}:
                      {{ queryChartDataDebugResult.totalPage }}</span
                    >
                    <span
                      >{{ t('visualization.current_page') }}:
                      {{ queryChartDataDebugResult.currentPage }}</span
                    >
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <!-- 保存子标签页 -->
            <el-tab-pane :label="t('visualization.save')" name="save">
              <div class="save-panel">
                <el-form :model="queryChartDataSaveForm" label-width="120px" label-position="left">
                  <el-form-item :label="t('visualization.service_name')">
                    <el-input
                      v-model="queryChartDataSaveForm.name"
                      :placeholder="t('visualization.service_name_placeholder')"
                    >
                    </el-input>
                  </el-form-item>
                  <el-form-item :label="t('visualization.service_description')">
                    <el-input
                      v-model="queryChartDataSaveForm.description"
                      type="textarea"
                      :rows="3"
                      :placeholder="t('visualization.service_description_placeholder')"
                    />
                  </el-form-item>
                  <el-form-item>
                    <el-button
                      type="primary"
                      :loading="queryChartDataSaveLoading"
                      @click="saveQueryChartDataService"
                    >
                      {{ t('visualization.save_service') }}
                    </el-button>
                    <el-button @click="loadQueryChartDataSavedService">
                      {{ t('visualization.load_saved') }}
                    </el-button>
                  </el-form-item>
                </el-form>
                <div v-if="queryChartDataSavedServices.length > 0" class="saved-services">
                  <h4>{{ t('visualization.saved_services') }}</h4>
                  <el-table :data="queryChartDataSavedServices" border style="width: 100%">
                    <el-table-column prop="name" :label="t('visualization.service_name')" />
                    <el-table-column
                      prop="description"
                      :label="t('visualization.service_description')"
                    >
                    </el-table-column>
                    <el-table-column prop="createTime" :label="t('visualization.create_time')" />
                    <el-table-column :label="t('visualization.operation')" width="200">
                      <template #default="scope">
                        <el-button size="small" @click="loadQueryChartDataService(scope.row)">{{
                          t('visualization.load')
                        }}</el-button>
                        <el-button
                          size="small"
                          type="danger"
                          @click="deleteQueryChartDataService(scope.row)"
                        >
                          {{ t('visualization.delete') }}
                        </el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-tab-pane>

        <!-- queryData 标签页（从图表配置解析） -->
        <el-tab-pane :label="t('visualization.query_data')" name="queryData">
          <el-tabs v-model="queryDataSubTab" type="border-card" class="sub-tabs">
            <!-- 配置子标签页 -->
            <el-tab-pane :label="t('visualization.config')" name="config">
              <div class="config-form">
                <el-alert
                  :title="t('visualization.query_data_tips')"
                  type="info"
                  :closable="false"
                  show-icon
                  style="margin-bottom: 20px"
                />
                <el-form :model="queryDataForm" label-width="120px" label-position="left">
                  <el-form-item :label="t('dataset.dataset')">
                    <el-input
                      v-model="queryDataForm.tableId"
                      :placeholder="t('dataset.dataset_id')"
                      type="number"
                      disabled
                    />
                    <span class="form-tip">{{ t('visualization.auto_from_chart') }}</span>
                  </el-form-item>
                  <el-form-item :label="t('chart.dimension')">
                    <el-tag
                      v-for="(dim, index) in queryDataForm.dimensions"
                      :key="index"
                      style="margin-right: 8px; margin-bottom: 8px"
                    >
                      {{ dim.fieldName || dim.name || dim.originName }}
                    </el-tag>
                    <span v-if="queryDataForm.dimensions.length === 0" class="empty-tip">
                      {{ t('visualization.no_dimensions') }}
                    </span>
                  </el-form-item>
                  <el-form-item :label="t('chart.quota')">
                    <el-tag
                      v-for="(measure, index) in queryDataForm.measures"
                      :key="index"
                      type="success"
                      style="margin-right: 8px; margin-bottom: 8px"
                    >
                      {{ measure.fieldName || measure.name || measure.originName }}
                    </el-tag>
                    <span v-if="queryDataForm.measures.length === 0" class="empty-tip">
                      {{ t('visualization.no_measures') }}
                    </span>
                  </el-form-item>
                  <el-form-item :label="t('chart.filter')">
                    <QueryFilterEditor
                      :filter="queryDataForm.filters"
                      :available-fields="availableFields"
                      @update:filter="queryDataForm.filters = $event"
                    />
                  </el-form-item>
                  <el-form-item :label="t('chart.pagination')">
                    <el-row :gutter="20">
                      <el-col :span="12">
                        <el-input-number
                          v-model="queryDataForm.pageInfo.pageNum"
                          :min="1"
                          :label="t('chart.current_page')"
                          style="width: 100%"
                        />
                      </el-col>
                      <el-col :span="12">
                        <el-input-number
                          v-model="queryDataForm.pageInfo.pageSize"
                          :min="1"
                          :max="10000"
                          :label="t('chart.page_size')"
                          style="width: 100%"
                        />
                      </el-col>
                    </el-row>
                  </el-form-item>
                </el-form>
              </div>
            </el-tab-pane>

            <!-- 调试子标签页 -->
            <el-tab-pane :label="t('visualization.debug')" name="debug">
              <div class="debug-panel">
                <div class="debug-actions">
                  <el-button type="primary" :loading="queryDataDebugLoading" @click="testQueryData">
                    {{ t('visualization.test_query') }}
                  </el-button>
                  <el-button @click="formatQueryDataRequest">
                    {{ t('visualization.format_request') }}
                  </el-button>
                  <el-button @click="copyQueryDataRequest">
                    {{ t('visualization.copy_request') }}
                  </el-button>
                </div>
                <el-divider />
                <div class="request-response">
                  <div class="request-section">
                    <h4>{{ t('visualization.request') }}</h4>
                    <el-input
                      v-model="queryDataRequestJson"
                      type="textarea"
                      :rows="10"
                      readonly
                      class="json-view"
                    />
                  </div>
                  <div class="response-section">
                    <h4>{{ t('visualization.response') }}</h4>
                    <el-input
                      v-model="queryDataResponseJson"
                      type="textarea"
                      :rows="10"
                      readonly
                      class="json-view"
                    />
                  </div>
                </div>
                <div v-if="queryDataDebugResult" class="result-table">
                  <h4>{{ t('visualization.result_data') }}</h4>
                  <el-table
                    :data="queryDataDebugResult.rows"
                    border
                    style="width: 100%"
                    max-height="400"
                  >
                    <el-table-column
                      v-for="column in queryDataDebugResult.columns"
                      :key="column"
                      :prop="column"
                      :label="column"
                    />
                  </el-table>
                  <div class="pagination-info">
                    <span
                      >{{ t('visualization.total_items') }}:
                      {{ queryDataDebugResult.totalItems }}</span
                    >
                    <span
                      >{{ t('visualization.total_pages') }}:
                      {{ queryDataDebugResult.totalPage }}</span
                    >
                    <span
                      >{{ t('visualization.current_page') }}:
                      {{ queryDataDebugResult.currentPage }}</span
                    >
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <!-- 保存子标签页 -->
            <el-tab-pane :label="t('visualization.save')" name="save">
              <div class="save-panel">
                <el-form :model="queryDataSaveForm" label-width="120px" label-position="left">
                  <el-form-item :label="t('visualization.service_name')">
                    <el-input
                      v-model="queryDataSaveForm.name"
                      :placeholder="t('visualization.service_name_placeholder')"
                    >
                    </el-input>
                  </el-form-item>
                  <el-form-item :label="t('visualization.service_description')">
                    <el-input
                      v-model="queryDataSaveForm.description"
                      type="textarea"
                      :rows="3"
                      :placeholder="t('visualization.service_description_placeholder')"
                    />
                  </el-form-item>
                  <el-form-item>
                    <el-button
                      type="primary"
                      :loading="queryDataSaveLoading"
                      @click="saveQueryDataService"
                    >
                      {{ t('visualization.save_service') }}
                    </el-button>
                    <el-button @click="loadQueryDataSavedService">
                      {{ t('visualization.load_saved') }}
                    </el-button>
                  </el-form-item>
                </el-form>
                <div v-if="queryDataSavedServices.length > 0" class="saved-services">
                  <h4>{{ t('visualization.saved_services') }}</h4>
                  <el-table :data="queryDataSavedServices" border style="width: 100%">
                    <el-table-column prop="name" :label="t('visualization.service_name')" />
                    <el-table-column
                      prop="description"
                      :label="t('visualization.service_description')"
                    >
                    </el-table-column>
                    <el-table-column prop="createTime" :label="t('visualization.create_time')" />
                    <el-table-column :label="t('visualization.operation')" width="200">
                      <template #default="scope">
                        <el-button size="small" @click="loadQueryDataService(scope.row)">{{
                          t('visualization.load')
                        }}</el-button>
                        <el-button
                          size="small"
                          type="danger"
                          @click="deleteQueryDataService(scope.row)"
                        >
                          {{ t('visualization.delete') }}
                        </el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-tab-pane>
      </el-tabs>
    </div>
    <template #footer>
      <el-button @click="handleClose">{{ t('visualization.close') }}</el-button>
      <el-button type="primary" @click="handleSave">{{ t('visualization.save') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus-secondary'
import { useI18n } from '@/hooks/web/useI18n'
import { queryChartData, queryData } from '@/api/chart'
import { cloneDeep } from 'lodash-es'
import QueryFilterEditor from './QueryFilterEditor.vue'

const { t } = useI18n()

const props = defineProps<{
  view: any
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'close'): void
}>()

const dialogVisible = computed({
  get: () => props.visible,
  set: val => emit('update:visible', val)
})

const activeTab = ref('queryChartData')
const queryChartDataSubTab = ref('config')
const queryDataSubTab = ref('config')

// queryChartData 配置表单（使用ID等信息）
const configForm = reactive({
  tableId: null as number | null,
  dimensions: [] as any[],
  measures: [] as any[],
  filters: [] as any[],
  pageInfo: {
    goPage: 1,
    pageSize: 10
  },
  sceneId: null as number | null
})

// queryData 配置表单（从图表配置解析）
const queryDataForm = reactive({
  tableId: null as number | null,
  dimensions: [] as any[],
  measures: [] as any[],
  filters: null as any, // QueryFilterDTO 结构
  pageInfo: {
    pageNum: 1,
    pageSize: 10
  }
})

// queryChartData 调试相关状态
const queryChartDataDebugLoading = ref(false)
const queryChartDataRequestJson = ref('')
const queryChartDataResponseJson = ref('')
const queryChartDataDebugResult = ref<any>(null)

// queryData 调试相关状态
const queryDataDebugLoading = ref(false)
const queryDataRequestJson = ref('')
const queryDataResponseJson = ref('')
const queryDataDebugResult = ref<any>(null)

// queryChartData 保存相关状态
const queryChartDataSaveForm = reactive({
  name: '',
  description: ''
})
const queryChartDataSaveLoading = ref(false)
const queryChartDataSavedServices = ref<any[]>([])

// queryData 保存相关状态
const queryDataSaveForm = reactive({
  name: '',
  description: ''
})
const queryDataSaveLoading = ref(false)
const queryDataSavedServices = ref<any[]>([])

// 可用字段列表
const availableFields = ref<any[]>([])

// AccessKey 配置表单
const accessKeyForm = reactive({
  accessKey: '',
  accessSecret: ''
})

// 初始化配置
const initConfig = () => {
  if (props.view) {
    // queryChartData 配置（使用ID等信息）
    configForm.tableId = props.view.tableId
    configForm.sceneId = props.view.sceneId
    configForm.dimensions = props.view.xAxis ? cloneDeep(props.view.xAxis) : []
    configForm.measures = props.view.yAxis ? cloneDeep(props.view.yAxis) : []

    // queryData 配置（从图表配置解析）
    queryDataForm.tableId = props.view.tableId
    // 解析维度和指标为 queryData 格式
    queryDataForm.dimensions = parseFieldsForQueryData(props.view.xAxis || [])
    queryDataForm.measures = parseFieldsForQueryData(props.view.yAxis || [])

    // 收集所有可用字段（从各个轴中收集）
    const allFields: any[] = []
    if (props.view.xAxis) allFields.push(...props.view.xAxis)
    if (props.view.xAxisExt) allFields.push(...props.view.xAxisExt)
    if (props.view.yAxis) allFields.push(...props.view.yAxis)
    if (props.view.yAxisExt) allFields.push(...props.view.yAxisExt)
    if (props.view.extStack) allFields.push(...props.view.extStack)
    if (props.view.extBubble) allFields.push(...props.view.extBubble)
    if (props.view.extLabel) allFields.push(...props.view.extLabel)
    if (props.view.extTooltip) allFields.push(...props.view.extTooltip)
    if (props.view.extColor) allFields.push(...props.view.extColor)
    // 去重（根据 id），并确保包含字段名信息
    const fieldMap = new Map()
    allFields.forEach(field => {
      if (field.id && !fieldMap.has(field.id)) {
        // 确保字段包含 name、originName 和 dataeaseName
        const fieldWithName = {
          ...field,
          name: field.name || field.chartShowName,
          originName: field.originName || field.name,
          dataeaseName: field.dataeaseName
        }
        fieldMap.set(field.id, fieldWithName)
      }
    })
    availableFields.value = Array.from(fieldMap.values())
  }
}

// 将图表字段解析为 queryData 格式
const parseFieldsForQueryData = (fields: any[]): any[] => {
  if (!fields || fields.length === 0) {
    return []
  }
  return fields.map(field => {
    // 优先使用 name，其次 originName，最后 dataeaseName 作为 fieldName
    const fieldName = field.name || field.originName || field.dataeaseName
    return {
      fieldName: fieldName,
      deType: field.deType,
      // groupType 不需要传递，后端会根据字段在 dimensions 还是 measures 中自动设置
      name: field.name,
      // originName 不需要传递，后端会从数据集字段中自动获取
      datasetGroupId: field.datasetGroupId,
      summary: field.summary,
      sort: field.sort,
      filter: field.filter,
      customSort: field.customSort,
      dateStyle: field.dateStyle,
      datePattern: field.datePattern,
      dateShowFormat: field.dateShowFormat,
      formatterCfg: field.formatterCfg,
      // chartShowName 不需要传递，数据服务接口不需要此字段
      compareCalc: field.compareCalc
      // index 不需要传递，后端会根据字段在 dimensions 和 measures 数组中的位置自动设置
    } as any
  })
}

// 添加过滤条件
const addFilter = () => {
  configForm.filters.push({
    fieldId: '',
    operator: 'eq',
    value: []
  })
}

// 移除过滤条件
const removeFilter = (index: number) => {
  configForm.filters.splice(index, 1)
}

// queryData 过滤条件由 QueryFilterEditor 组件管理，这里不需要单独的函数

// queryChartData 测试查询
const testQueryChartData = async () => {
  queryChartDataDebugLoading.value = true
  try {
    if (!configForm.tableId) {
      ElMessage.warning(t('dataset.dataset_id_required'))
      queryChartDataDebugLoading.value = false
      return
    }
    const requestData = {
      tableId: configForm.tableId,
      dimensions: configForm.dimensions,
      measures: configForm.measures,
      filters: configForm.filters,
      pageInfo: configForm.pageInfo,
      sceneId: configForm.sceneId
    }
    queryChartDataRequestJson.value = JSON.stringify(requestData, null, 2)

    // 如果配置了 AccessKey，使用签名方式请求
    const response = await queryChartData(
      requestData,
      accessKeyForm.accessKey || undefined,
      accessKeyForm.accessSecret || undefined
    )
    queryChartDataResponseJson.value = JSON.stringify(response, null, 2)

    if (response.code === 0 && response.data) {
      queryChartDataDebugResult.value = response.data
      ElMessage.success(t('visualization.query_success'))
    } else {
      ElMessage.error(response.msg || t('visualization.query_failed'))
    }
  } catch (error: any) {
    queryChartDataResponseJson.value = JSON.stringify({ error: error.message }, null, 2)
    ElMessage.error(error.message || t('visualization.query_failed'))
  } finally {
    queryChartDataDebugLoading.value = false
  }
}

// queryChartData 格式化请求
const formatQueryChartDataRequest = () => {
  try {
    const parsed = JSON.parse(queryChartDataRequestJson.value)
    queryChartDataRequestJson.value = JSON.stringify(parsed, null, 2)
    ElMessage.success(t('visualization.format_success'))
  } catch (error) {
    ElMessage.error(t('visualization.format_failed'))
  }
}

// queryChartData 复制请求
const copyQueryChartDataRequest = () => {
  navigator.clipboard.writeText(queryChartDataRequestJson.value)
  ElMessage.success(t('visualization.copy_success'))
}

// queryChartData 保存服务
const saveQueryChartDataService = () => {
  if (!queryChartDataSaveForm.name) {
    ElMessage.warning(t('visualization.service_name_required'))
    return
  }

  queryChartDataSaveLoading.value = true
  try {
    const serviceData = {
      name: queryChartDataSaveForm.name,
      description: queryChartDataSaveForm.description,
      config: cloneDeep(configForm),
      createTime: new Date().toISOString()
    }

    // 保存到本地存储
    const saved = localStorage.getItem('chartDataServices_queryChartData')
    const services = saved ? JSON.parse(saved) : []
    services.push(serviceData)
    localStorage.setItem('chartDataServices_queryChartData', JSON.stringify(services))

    queryChartDataSavedServices.value = services
    ElMessage.success(t('visualization.save_success'))
    queryChartDataSaveForm.name = ''
    queryChartDataSaveForm.description = ''
  } catch (error) {
    ElMessage.error(t('visualization.save_failed'))
  } finally {
    queryChartDataSaveLoading.value = false
  }
}

// queryChartData 加载保存的服务
const loadQueryChartDataSavedService = () => {
  const saved = localStorage.getItem('chartDataServices_queryChartData')
  if (saved) {
    queryChartDataSavedServices.value = JSON.parse(saved)
  }
}

// queryChartData 加载服务配置
const loadQueryChartDataService = (service: any) => {
  Object.assign(configForm, service.config)
  ElMessage.success(t('visualization.load_success'))
}

// queryChartData 删除服务
const deleteQueryChartDataService = (service: any) => {
  const saved = localStorage.getItem('chartDataServices_queryChartData')
  if (saved) {
    const services = JSON.parse(saved)
    const index = services.findIndex((s: any) => s.name === service.name)
    if (index > -1) {
      services.splice(index, 1)
      localStorage.setItem('chartDataServices_queryChartData', JSON.stringify(services))
      queryChartDataSavedServices.value = services
      ElMessage.success(t('visualization.delete_success'))
    }
  }
}

// queryData 测试查询
const testQueryData = async () => {
  queryDataDebugLoading.value = true
  try {
    if (!queryDataForm.tableId) {
      ElMessage.warning(t('dataset.dataset_id_required'))
      queryDataDebugLoading.value = false
      return
    }
    if (queryDataForm.dimensions.length === 0 && queryDataForm.measures.length === 0) {
      ElMessage.warning(t('visualization.dimensions_or_measures_required'))
      queryDataDebugLoading.value = false
      return
    }
    const requestData = {
      tableId: queryDataForm.tableId,
      dimensions: queryDataForm.dimensions,
      measures: queryDataForm.measures,
      filters: queryDataForm.filters,
      pageInfo: queryDataForm.pageInfo
    }
    queryDataRequestJson.value = JSON.stringify(requestData, null, 2)

    // 如果配置了 AccessKey，使用签名方式请求
    const response = await queryData(
      requestData,
      accessKeyForm.accessKey || undefined,
      accessKeyForm.accessSecret || undefined
    )
    queryDataResponseJson.value = JSON.stringify(response, null, 2)

    if (response.code === 0 && response.data) {
      queryDataDebugResult.value = response.data
      ElMessage.success(t('visualization.query_success'))
    } else {
      ElMessage.error(response.msg || t('visualization.query_failed'))
    }
  } catch (error: any) {
    queryDataResponseJson.value = JSON.stringify({ error: error.message }, null, 2)
    ElMessage.error(error.message || t('visualization.query_failed'))
  } finally {
    queryDataDebugLoading.value = false
  }
}

// queryData 格式化请求
const formatQueryDataRequest = () => {
  try {
    const parsed = JSON.parse(queryDataRequestJson.value)
    queryDataRequestJson.value = JSON.stringify(parsed, null, 2)
    ElMessage.success(t('visualization.format_success'))
  } catch (error) {
    ElMessage.error(t('visualization.format_failed'))
  }
}

// queryData 复制请求
const copyQueryDataRequest = () => {
  navigator.clipboard.writeText(queryDataRequestJson.value)
  ElMessage.success(t('visualization.copy_success'))
}

// queryData 保存服务
const saveQueryDataService = () => {
  if (!queryDataSaveForm.name) {
    ElMessage.warning(t('visualization.service_name_required'))
    return
  }

  queryDataSaveLoading.value = true
  try {
    const serviceData = {
      name: queryDataSaveForm.name,
      description: queryDataSaveForm.description,
      config: cloneDeep(queryDataForm),
      createTime: new Date().toISOString()
    }

    // 保存到本地存储
    const saved = localStorage.getItem('chartDataServices_queryData')
    const services = saved ? JSON.parse(saved) : []
    services.push(serviceData)
    localStorage.setItem('chartDataServices_queryData', JSON.stringify(services))

    queryDataSavedServices.value = services
    ElMessage.success(t('visualization.save_success'))
    queryDataSaveForm.name = ''
    queryDataSaveForm.description = ''
  } catch (error) {
    ElMessage.error(t('visualization.save_failed'))
  } finally {
    queryDataSaveLoading.value = false
  }
}

// queryData 加载保存的服务
const loadQueryDataSavedService = () => {
  const saved = localStorage.getItem('chartDataServices_queryData')
  if (saved) {
    queryDataSavedServices.value = JSON.parse(saved)
  }
}

// queryData 加载服务配置
const loadQueryDataService = (service: any) => {
  Object.assign(queryDataForm, service.config)
  ElMessage.success(t('visualization.load_success'))
}

// queryData 删除服务
const deleteQueryDataService = (service: any) => {
  const saved = localStorage.getItem('chartDataServices_queryData')
  if (saved) {
    const services = JSON.parse(saved)
    const index = services.findIndex((s: any) => s.name === service.name)
    if (index > -1) {
      services.splice(index, 1)
      localStorage.setItem('chartDataServices_queryData', JSON.stringify(services))
      queryDataSavedServices.value = services
      ElMessage.success(t('visualization.delete_success'))
    }
  }
}

// 保存配置
const handleSave = () => {
  // 可以在这里添加保存逻辑
  ElMessage.success(t('visualization.save_success'))
}

// 关闭对话框
const handleClose = () => {
  dialogVisible.value = false
  emit('close')
}

// 监听视图变化
watch(
  () => props.view,
  () => {
    if (props.visible) {
      initConfig()
      loadQueryChartDataSavedService()
      loadQueryDataSavedService()
    }
  },
  { immediate: true, deep: true }
)

// 监听对话框显示
watch(
  () => props.visible,
  val => {
    if (val) {
      initConfig()
      loadQueryChartDataSavedService()
      loadQueryDataSavedService()
    }
  }
)

onMounted(() => {
  if (props.visible) {
    initConfig()
    loadQueryChartDataSavedService()
    loadQueryDataSavedService()
  }
})
</script>

<style lang="less" scoped>
.data-service-dialog {
  .service-tabs {
    min-height: 500px;

    :deep(.el-tabs__content) {
      padding: 0;
    }
  }

  .sub-tabs {
    border: none;
    box-shadow: none;

    :deep(.el-tabs__header) {
      margin-bottom: 20px;
    }

    :deep(.el-tabs__content) {
      padding: 0;
    }
  }

  .config-form {
    padding: 20px;

    .form-tip {
      margin-left: 10px;
      color: #909399;
      font-size: 12px;
    }

    .empty-tip {
      color: #909399;
      font-size: 12px;
      font-style: italic;
    }
  }

  .filter-list {
    margin-top: 10px;
    .filter-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px;
      margin-bottom: 8px;
      background: #f5f7fa;
      border-radius: 4px;
    }
  }

  .debug-panel {
    padding: 20px;

    .debug-actions {
      margin-bottom: 20px;
    }

    .request-response {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;
      margin-bottom: 20px;

      .request-section,
      .response-section {
        h4 {
          margin-bottom: 10px;
        }

        .json-view {
          font-family: 'Courier New', monospace;
        }
      }
    }

    .result-table {
      margin-top: 20px;

      .pagination-info {
        margin-top: 10px;
        display: flex;
        gap: 20px;
      }
    }
  }

  .save-panel {
    padding: 20px;

    .saved-services {
      margin-top: 30px;
    }
  }
}
</style>
