/**
 * AccessKey 签名验证示例（TypeScript）
 *
 * 使用说明：
 * 1. 在项目中使用：import { axiosPostWithAccessKey } from '@/utils/accessKey'
 * 2. 或者直接使用此文件中的函数
 */

import CryptoJS from 'crypto-js/crypto-js'
import axios, { AxiosInstance } from 'axios'

/**
 * 生成签名
 * @param accessSecret AccessSecret
 * @param timestamp 时间戳（毫秒）
 * @param requestBody 请求体（JSON 字符串）
 * @returns Base64 编码的签名
 */
export function generateSignature(
  accessSecret: string,
  timestamp: number,
  requestBody: string
): string {
  // 待签名字符串：accessSecret + timestamp + requestBody
  const signString = accessSecret + timestamp + (requestBody || '')

  // 使用 HMAC-SHA256 算法生成签名
  const hash = CryptoJS.HmacSHA256(signString, accessSecret)

  // Base64 编码
  return CryptoJS.enc.Base64.stringify(hash)
}

/**
 * 使用 AccessKey 发送 POST 请求（使用 fetch）
 * @param url 请求 URL
 * @param data 请求数据
 * @param accessKey AccessKey
 * @param accessSecret AccessSecret
 * @returns Promise
 */
export async function postWithAccessKey(
  url: string,
  data: any,
  accessKey: string,
  accessSecret: string
): Promise<any> {
  const requestBody = typeof data === 'string' ? data : JSON.stringify(data)
  const timestamp = Date.now()
  const signature = generateSignature(accessSecret, timestamp, requestBody)

  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-ACCESS-KEY': accessKey,
      'X-TIMESTAMP': timestamp.toString(),
      'X-SIGNATURE': signature
    },
    body: requestBody
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({ msg: response.statusText }))
    throw new Error(error.msg || `HTTP error! status: ${response.status}`)
  }

  return response.json()
}

/**
 * 使用 AccessKey 发送 POST 请求（使用 axios）
 * @param axiosInstance axios 实例
 * @param url 请求 URL
 * @param data 请求数据
 * @param accessKey AccessKey
 * @param accessSecret AccessSecret
 * @returns Promise
 */
export async function axiosPostWithAccessKey(
  axiosInstance: AxiosInstance,
  url: string,
  data: any,
  accessKey: string,
  accessSecret: string
): Promise<any> {
  const requestBody = typeof data === 'string' ? data : JSON.stringify(data)
  const timestamp = Date.now()
  const signature = generateSignature(accessSecret, timestamp, requestBody)

  return axiosInstance.post(url, requestBody, {
    headers: {
      'Content-Type': 'application/json',
      'X-ACCESS-KEY': accessKey,
      'X-TIMESTAMP': timestamp.toString(),
      'X-SIGNATURE': signature
    },
    transformRequest: [(data: any) => data] // 禁用自动序列化
  })
}

// 使用示例
export async function example() {
  const accessKey = 'de_abc123...'
  const accessSecret = 'xyz789...'
  const baseUrl = 'http://localhost:8100'

  // 示例 1: 查询数据（字段名方式）
  const queryDataRequest = {
    tableId: 123,
    dimensions: [
      {
        fieldName: 'field1',
        formatterCfg: null,
        compareCalc: null
      }
    ],
    measures: [
      {
        fieldName: 'field2',
        formatterCfg: null,
        compareCalc: null
      }
    ],
    filters: null,
    pageInfo: {
      pageNum: 1,
      pageSize: 10
    }
  }

  try {
    const response1 = await postWithAccessKey(
      `${baseUrl}/commonData/queryData`,
      queryDataRequest,
      accessKey,
      accessSecret
    )
    console.log('查询数据响应:', response1)
  } catch (error) {
    console.error('查询数据失败:', error)
  }

  // 示例 2: 查询图表数据（ID方式）
  const queryChartDataRequest = {
    tableId: 123,
    dimensions: [
      {
        id: 'field1_id'
        // ... 其他字段
      }
    ],
    measures: [
      {
        id: 'field2_id'
        // ... 其他字段
      }
    ],
    filters: [],
    pageInfo: {
      pageNum: 1,
      pageSize: 10
    },
    sceneId: 456
  }

  try {
    const response2 = await postWithAccessKey(
      `${baseUrl}/commonData/queryChartData`,
      queryChartDataRequest,
      accessKey,
      accessSecret
    )
    console.log('查询图表数据响应:', response2)
  } catch (error) {
    console.error('查询图表数据失败:', error)
  }
}
