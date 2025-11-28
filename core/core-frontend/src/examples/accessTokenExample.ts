/**
 * DataEase AccessToken 签名验证示例代码 (TypeScript)
 *
 * 此文件展示了如何在 TypeScript/JavaScript 中使用 AccessToken 和 AccessSecret
 * 对 commonData 接口进行签名验证
 *
 * 安装依赖：
 * npm install crypto-js
 * npm install axios (可选)
 */

import CryptoJS from 'crypto-js'
import axios, { AxiosInstance } from 'axios'

// ==================== AccessToken 签名工具类 ====================

/**
 * AccessToken 签名工具类
 */
export class AccessTokenSigner {
  private accessToken: string
  private accessSecret: string

  constructor(accessToken: string, accessSecret: string) {
    this.accessToken = accessToken
    this.accessSecret = accessSecret
  }

  /**
   * 生成签名
   * @param timestamp 时间戳（毫秒）
   * @param requestBody 请求体（JSON 字符串）
   * @returns Base64 编码的签名
   */
  generateSignature(timestamp: number, requestBody: string): string {
    // 待签名字符串：accessSecret + timestamp + requestBody
    const signString = this.accessSecret + timestamp + (requestBody || '')

    // 使用 HMAC-SHA256 算法生成签名
    const hash = CryptoJS.HmacSHA256(signString, this.accessSecret)

    // Base64 编码
    return CryptoJS.enc.Base64.stringify(hash)
  }

  /**
   * 生成请求头
   * @param requestBody 请求体（JSON 字符串）
   * @returns 包含 AccessToken、时间戳和签名的请求头
   */
  generateHeaders(requestBody: string): Record<string, string> {
    const timestamp = Date.now()
    const signature = this.generateSignature(timestamp, requestBody)

    return {
      'X-ACCESS-TOKEN': this.accessToken,
      'X-TIMESTAMP': timestamp.toString(),
      'X-SIGNATURE': signature,
      'Content-Type': 'application/json'
    }
  }
}

// ==================== 使用 fetch API ====================

/**
 * 使用 AccessToken 发送 POST 请求（使用 fetch）
 * @param url 请求 URL
 * @param data 请求数据
 * @param accessToken AccessToken
 * @param accessSecret AccessSecret
 * @returns Promise
 */
export async function postWithAccessToken(
  url: string,
  data: any,
  accessToken: string,
  accessSecret: string
): Promise<any> {
  const signer = new AccessTokenSigner(accessToken, accessSecret)
  const requestBody = typeof data === 'string' ? data : JSON.stringify(data)
  const headers = signer.generateHeaders(requestBody)

  const response = await fetch(url, {
    method: 'POST',
    headers: headers,
    body: requestBody
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({ msg: response.statusText }))
    throw new Error(error.msg || `HTTP error! status: ${response.status}`)
  }

  return response.json()
}

// ==================== 使用 axios ====================

/**
 * 使用 AccessToken 发送 POST 请求（使用 axios）
 * @param axiosInstance axios 实例
 * @param url 请求 URL
 * @param data 请求数据
 * @param accessToken AccessToken
 * @param accessSecret AccessSecret
 * @returns Promise
 */
export async function axiosPostWithAccessToken(
  axiosInstance: AxiosInstance,
  url: string,
  data: any,
  accessToken: string,
  accessSecret: string
): Promise<any> {
  const signer = new AccessTokenSigner(accessToken, accessSecret)
  const requestBody = typeof data === 'string' ? data : JSON.stringify(data)
  const headers = signer.generateHeaders(requestBody)

  const response = await axiosInstance.post(url, data, {
    headers: {
      ...headers
    }
  })

  return response.data
}

// ==================== 使用示例 ====================

/**
 * 示例：调用 queryData 接口
 */
export async function exampleQueryData() {
  const baseUrl = 'http://localhost:8100' // 替换为实际的 DataEase 服务地址
  const accessToken = 'de_abc123...' // 替换为实际的 AccessToken
  const accessSecret = 'xyz789...' // 替换为实际的 AccessSecret

  const requestData = {
    tableId: 123,
    dimensions: [
      {
        fieldName: 'field1',
        deType: 0,
        name: '字段1'
      }
    ],
    measures: [
      {
        fieldName: 'field2',
        deType: 2,
        name: '字段2',
        summary: 'sum'
      }
    ],
    filters: null,
    pageInfo: {
      pageNum: 1,
      pageSize: 10
    }
  }

  try {
    // 方式一：使用 fetch
    const response1 = await postWithAccessToken(
      `${baseUrl}/commonData/queryData`,
      requestData,
      accessToken,
      accessSecret
    )
    console.log('Response (fetch):', response1)

    // 方式二：使用 axios
    const axiosInstance = axios.create({
      baseURL: baseUrl,
      timeout: 30000
    })
    const response2 = await axiosPostWithAccessToken(
      axiosInstance,
      '/commonData/queryData',
      requestData,
      accessToken,
      accessSecret
    )
    console.log('Response (axios):', response2)
  } catch (error: any) {
    console.error('Request failed:', error.message)
  }
}

/**
 * 示例：调用 queryChartData 接口
 */
export async function exampleQueryChartData() {
  const baseUrl = 'http://localhost:8100'
  const accessToken = 'de_abc123...'
  const accessSecret = 'xyz789...'

  const requestData = {
    tableId: 123,
    dimensions: [
      // ChartViewFieldDTO 格式的字段
    ],
    measures: [
      // ChartViewFieldDTO 格式的字段
    ],
    filters: [],
    pageInfo: {
      goPage: 1,
      pageSize: 10
    },
    sceneId: 1
  }

  try {
    const response = await postWithAccessToken(
      `${baseUrl}/commonData/queryChartData`,
      requestData,
      accessToken,
      accessSecret
    )
    console.log('Response:', response)
  } catch (error: any) {
    console.error('Request failed:', error.message)
  }
}

// ==================== Node.js 环境示例 ====================

/**
 * Node.js 环境下的签名生成（使用 crypto 模块）
 */
import * as crypto from 'crypto'

export function generateSignatureNodeJS(
  accessSecret: string,
  timestamp: number,
  requestBody: string
): string {
  const signString = accessSecret + timestamp + (requestBody || '')
  const hmac = crypto.createHmac('sha256', accessSecret)
  hmac.update(signString)
  return hmac.digest('base64')
}

/**
 * Node.js 环境下使用 axios 发送请求
 */
export async function nodeJSExample() {
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  const axios = require('axios')
  const accessToken = 'de_abc123...'
  const accessSecret = 'xyz789...'
  const url = 'http://localhost:8100/commonData/queryData'

  const requestData = {
    tableId: 123,
    dimensions: [],
    measures: [],
    pageInfo: { pageNum: 1, pageSize: 10 }
  }

  const timestamp = Date.now()
  const requestBody = JSON.stringify(requestData)
  const signature = generateSignatureNodeJS(accessSecret, timestamp, requestBody)

  try {
    const response = await axios.post(url, requestData, {
      headers: {
        'X-ACCESS-TOKEN': accessToken,
        'X-TIMESTAMP': timestamp.toString(),
        'X-SIGNATURE': signature
      }
    })
    console.log('Response:', response.data)
  } catch (error: any) {
    console.error('Request failed:', error.response?.data || error.message)
  }
}

// ==================== 签名算法说明 ====================

/**
 * 签名算法说明：
 *
 * 1. 待签名字符串 = accessSecret + timestamp + requestBody
 *    - accessSecret: AccessSecret（字符串）
 *    - timestamp: 时间戳（毫秒，字符串形式）
 *    - requestBody: 请求体（JSON 字符串，如果为空则使用空字符串）
 *
 * 2. 使用 HMAC-SHA256 算法对待签名字符串进行签名
 *    - 密钥：accessSecret
 *    - 消息：待签名字符串
 *
 * 3. 对签名结果进行 Base64 编码
 *
 * 4. 在请求头中添加：
 *    - X-ACCESS-TOKEN: AccessToken
 *    - X-TIMESTAMP: 时间戳（毫秒）
 *    - X-SIGNATURE: Base64 编码的签名
 *
 * 注意事项：
 * - 时间戳必须在服务器当前时间的 ±5 分钟内，否则会被拒绝
 * - 请求体必须是 JSON 字符串，且与签名时使用的字符串完全一致
 * - AccessSecret 必须保密，不要泄露
 */
