import request from '@/config/axios'
import { originNameHandleWithArr, originNameHandleBackWithArr } from '@/utils/CalculateFields'
import { cloneDeep } from 'lodash-es'
export interface Field {
  id: number | string
  datasourceId: number | string
  datasetTableId: number | string
  datasetGroupId: number | string
  originName: string
  name: string
  dataeaseName: string
  groupType: string
  type: string
  deType: number
  deExtractType: number
  extField: number
  checked: boolean
  fieldShortName: string
  desensitized: boolean
}

export interface ComponentInfo {
  id: string
  name: string
  deType: number
  type: string
  datasetId: string
}

export const getFieldByDQ = async (id, chartId, data): Promise<IResponse> => {
  return request.post({ url: `/chart/listByDQ/${id}/${chartId}`, data: data }).then(res => {
    originNameHandleBackWithArr(res?.data, ['dimensionList', 'quotaList'])
    return res?.data
  })
}

export const copyChartField = async (id, chartId): Promise<IResponse> => {
  return request.post({ url: `/chart/copyField/${id}/${chartId}`, data: {} }).then(res => {
    return res?.data
  })
}

export const deleteChartField = async (id): Promise<IResponse> => {
  return request.post({ url: `/chart/deleteField/${id}`, data: {} }).then(res => {
    return res?.data
  })
}

export const deleteChartFieldByChartId = async (chartId): Promise<IResponse> => {
  return request.post({ url: `/chart/deleteFieldByChart/${chartId}`, data: {} }).then(res => {
    return res?.data
  })
}

// 通过图表对象获取数据
export const getData = async (data): Promise<IResponse> => {
  delete data.data
  const copyData = cloneDeep(data)
  const fields = [
    'xAxis',
    'xAxisExt',
    'yAxis',
    'yAxisExt',
    'extBubble',
    'extLabel',
    'extStack',
    'extTooltip',
    'extColor'
  ]
  const dataFields = ['fields', 'sourceFields']
  originNameHandleWithArr(copyData, fields)
  return request.post({ url: '/chartData/getData', data: copyData }).then(res => {
    if (res.code === 0) {
      originNameHandleBackWithArr(res?.data, fields)
      // 动态计算字段在数据中，也需要转码
      originNameHandleWithArr(res?.data?.data, dataFields)
      originNameHandleBackWithArr(res?.data?.data, dataFields)
      originNameHandleBackWithArr(res?.data?.data?.left, ['fields'])
      originNameHandleBackWithArr(res?.data?.data?.right, ['fields'])
      return res?.data
    } else {
      originNameHandleBackWithArr(res, fields)
      originNameHandleBackWithArr(res?.data, dataFields)
      originNameHandleBackWithArr(res?.data?.left, ['fields'])
      originNameHandleBackWithArr(res?.data?.right, ['fields'])
      return res
    }
  })
}

export const innerExportDetails = async (data): Promise<IResponse> => {
  return request.post({
    url: '/chartData/innerExportDetails',
    method: 'post',
    data: data,
    loading: true,
    responseType: 'blob'
  })
}

export const innerExportDataSetDetails = async (data): Promise<IResponse> => {
  return request.post({
    url: '/chartData/innerExportDataSetDetails',
    method: 'post',
    data: data,
    loading: true,
    responseType: 'blob'
  })
}

// 通过图表id获取数据
export const getChart = async (id): Promise<IResponse> => {
  return request.post({ url: `/chart/getChart/${id}`, data: {} }).then(res => {
    return res?.data
  })
}

// 单个图表保存测试
export const saveChart = async (data): Promise<IResponse> => {
  delete data.data
  return request.post({ url: '/chart/save', data }).then(res => {
    return res?.data
  })
}

// 获取单个字段枚举值
export const getFieldData = async ({ fieldId, fieldType, data }): Promise<IResponse> => {
  delete data.data
  return request
    .post({ url: `/chartData/getFieldData/${fieldId}/${fieldType}`, data })
    .then(res => {
      return res
    })
}

// 获取下钻字段枚举值
export const getDrillFieldData = async ({ fieldId, data }): Promise<IResponse> => {
  delete data.data
  return request.post({ url: `/chartData/getDrillFieldData/${fieldId}`, data }).then(res => {
    return res
  })
}

// 查询数据（简化版）
export const queryChartData = async (
  data,
  accessKey?: string,
  accessSecret?: string
): Promise<IResponse> => {
  // 如果提供了 AccessKey，使用签名方式请求
  if (accessKey && accessSecret) {
    const { axiosPostWithAccessKey } = await import('@/utils/accessKey')
    const axiosModule = await import('@/config/axios/service')
    const axiosInstance = axiosModule.service
    return axiosPostWithAccessKey(
      axiosInstance,
      '/commonData/queryChartData',
      data,
      accessKey,
      accessSecret
    )
  }
  // 否则使用普通方式请求
  return request.post({ url: '/commonData/queryChartData', data }).then(res => {
    return res
  })
}

// 根据字段名查询数据
export const queryData = async (
  data,
  accessKey?: string,
  accessSecret?: string
): Promise<IResponse> => {
  // 如果提供了 AccessKey，使用签名方式请求
  if (accessKey && accessSecret) {
    const { axiosPostWithAccessKey } = await import('@/utils/accessKey')
    const axiosModule = await import('@/config/axios/service')
    const axiosInstance = axiosModule.service
    return axiosPostWithAccessKey(
      axiosInstance,
      '/commonData/queryData',
      data,
      accessKey,
      accessSecret
    )
  }
  // 否则使用普通方式请求
  return request.post({ url: '/commonData/queryData', data }).then(res => {
    return res
  })
}

export const getChartDetail = async (id: string): Promise<IResponse> => {
  return request.post({ url: `chart/getDetail/${id}`, data: {} }).then(res => {
    return res
  })
}

export const checkSameDataSet = async (viewIdSource, viewIdTarget) =>
  request.get({ url: '/chart/checkSameDataSet/' + viewIdSource + '/' + viewIdTarget })
