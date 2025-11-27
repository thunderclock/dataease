<template>
  <div class="query-filter-editor">
    <div v-if="filter" class="filter-tree">
      <!-- 逻辑关系选择 -->
      <div class="logic-selector">
        <el-select
          :model-value="filter.logic"
          @update:model-value="updateLogic"
          size="small"
          style="width: 80px"
        >
          <el-option label="AND" value="and" />
          <el-option label="OR" value="or" />
        </el-select>
        <el-button v-if="showDelete" type="danger" size="small" text @click="$emit('delete')">
          {{ t('chart.delete') }}
        </el-button>
      </div>

      <!-- 条件项列表 -->
      <div v-if="filter.items && filter.items.length > 0" class="filter-items">
        <div v-for="(item, index) in filter.items" :key="index" class="filter-item">
          <!-- 单个条件 -->
          <div v-if="item.type === 'item'" class="condition-item">
            <el-select
              v-model="item.fieldName"
              :placeholder="t('chart.select_field')"
              filterable
              style="width: 200px; margin-right: 8px"
            >
              <el-option
                v-for="field in availableFields"
                :key="field.id"
                :label="field.name || field.originName || field.dataeaseName"
                :value="field.name || field.originName || field.dataeaseName"
              />
            </el-select>

            <el-select
              v-model="item.term"
              :placeholder="t('chart.select_operator')"
              style="width: 120px; margin-right: 8px"
            >
              <el-option label="等于" value="eq" />
              <el-option label="不等于" value="not_eq" />
              <el-option label="小于" value="lt" />
              <el-option label="小于等于" value="le" />
              <el-option label="大于" value="gt" />
              <el-option label="大于等于" value="ge" />
              <el-option label="包含" value="in" />
              <el-option label="不包含" value="not_in" />
              <el-option label="模糊匹配" value="like" />
              <el-option label="不模糊匹配" value="not_like" />
              <el-option label="为空" value="null" />
              <el-option label="不为空" value="not_null" />
              <el-option label="为空字符串" value="empty" />
              <el-option label="不为空字符串" value="not_empty" />
              <el-option label="区间" value="between" />
            </el-select>

            <!-- 值类型选择 -->
            <el-select
              v-model="item.valueType"
              :placeholder="t('chart.value_type')"
              style="width: 100px; margin-right: 8px"
            >
              <el-option label="固定值" value="fixed" />
              <el-option label="相对值" value="relative" />
            </el-select>

            <!-- 固定值输入 -->
            <template v-if="item.valueType === 'fixed'">
              <el-input
                v-if="!['null', 'not_null', 'empty', 'not_empty'].includes(item.term)"
                v-model="item.value"
                :placeholder="t('chart.condition_value')"
                style="width: 200px; margin-right: 8px"
              />
              <el-select
                v-if="['in', 'not_in'].includes(item.term)"
                v-model="item.enumValue"
                multiple
                filterable
                allow-create
                default-first-option
                :placeholder="t('chart.select_values')"
                style="width: 200px; margin-right: 8px"
              />
            </template>

            <!-- 相对值设置 -->
            <template v-if="item.valueType === 'relative'">
              <el-select
                v-model="item.relativeValue.timeType"
                :placeholder="t('chart.time_type')"
                style="width: 120px; margin-right: 8px"
              >
                <el-option label="今天" value="today" />
                <el-option label="昨天" value="yesterday" />
                <el-option label="本周" value="thisWeek" />
                <el-option label="上周" value="lastWeek" />
                <el-option label="本月" value="thisMonth" />
                <el-option label="上月" value="lastMonth" />
              </el-select>
              <el-input-number
                v-model="item.relativeValue.offset"
                :placeholder="t('chart.offset')"
                style="width: 100px; margin-right: 8px"
              />
              <el-select
                v-model="item.relativeValue.unit"
                :placeholder="t('chart.unit')"
                style="width: 80px; margin-right: 8px"
              >
                <el-option label="天" value="day" />
                <el-option label="周" value="week" />
                <el-option label="月" value="month" />
                <el-option label="年" value="year" />
              </el-select>
            </template>

            <el-button type="danger" size="small" text @click="removeItem(index)">
              {{ t('chart.delete') }}
            </el-button>
          </div>

          <!-- 嵌套条件树 -->
          <QueryFilterEditor
            v-else-if="item.type === 'tree'"
            :filter="item.subTree"
            :available-fields="availableFields"
            :show-delete="true"
            @delete="removeItem(index)"
            @update:filter="updateSubTree(index, $event)"
          />
        </div>
      </div>

      <!-- 添加条件按钮 -->
      <div class="filter-actions">
        <el-button size="small" @click="addCondition">
          {{ t('chart.add_condition') }}
        </el-button>
        <el-button size="small" @click="addNestedTree">
          {{ t('chart.add_nested_condition') }}
        </el-button>
      </div>
    </div>
    <div v-else class="empty-filter">
      <el-button size="small" @click="initFilter">
        {{ t('chart.add_filter') }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'

const { t } = useI18n()

interface FilterConditionItem {
  type: 'item' | 'tree'
  fieldName?: string
  term?: string
  valueType?: 'fixed' | 'relative'
  value?: any
  enumValue?: any[]
  relativeValue?: {
    timeType?: string
    offset?: number
    unit?: string
  }
  subTree?: QueryFilter
}

interface QueryFilter {
  logic: 'and' | 'or'
  items: FilterConditionItem[]
}

const props = defineProps<{
  filter: QueryFilter | null
  availableFields: any[]
  showDelete?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:filter', value: QueryFilter): void
  (e: 'delete'): void
}>()

const filter = computed({
  get: () => props.filter,
  set: val => {
    if (val) {
      emit('update:filter', val as QueryFilter)
    }
  }
})

const initFilter = () => {
  const newFilter: QueryFilter = {
    logic: 'and',
    items: []
  }
  emit('update:filter', newFilter)
}

const addCondition = () => {
  if (!filter.value) {
    initFilter()
    return
  }
  if (!filter.value.items) {
    filter.value.items = []
  }
  filter.value.items.push({
    type: 'item',
    fieldName: '',
    term: 'eq',
    valueType: 'fixed',
    value: '',
    relativeValue: {
      timeType: 'today',
      offset: 0,
      unit: 'day'
    }
  })
  emit('update:filter', { ...filter.value })
}

const addNestedTree = () => {
  if (!filter.value) {
    initFilter()
    return
  }
  if (!filter.value.items) {
    filter.value.items = []
  }
  filter.value.items.push({
    type: 'tree',
    subTree: {
      logic: 'and',
      items: []
    }
  })
  emit('update:filter', { ...filter.value })
}

const updateLogic = (logic: 'and' | 'or') => {
  if (filter.value) {
    filter.value.logic = logic
    emit('update:filter', { ...filter.value })
  }
}

const updateSubTree = (index: number, subTree: QueryFilter) => {
  if (filter.value && filter.value.items) {
    filter.value.items[index].subTree = subTree
    emit('update:filter', { ...filter.value })
  }
}

const removeItem = (index: number) => {
  if (filter.value && filter.value.items) {
    filter.value.items.splice(index, 1)
    emit('update:filter', { ...filter.value })
  }
}
</script>

<style lang="less" scoped>
.query-filter-editor {
  .filter-tree {
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    padding: 12px;
  }

  .logic-selector {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
  }

  .filter-items {
    margin-left: 20px;
    margin-bottom: 12px;
  }

  .filter-item {
    margin-bottom: 8px;
    padding: 8px;
    background-color: #f5f7fa;
    border-radius: 4px;
  }

  .condition-item {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
  }

  .filter-actions {
    margin-top: 12px;
    display: flex;
    gap: 8px;
  }

  .empty-filter {
    text-align: center;
    padding: 20px;
  }
}
</style>
