/**
 * DataEase AccessToken 签名验证示例代码
 *
 * 此文件展示了如何使用 AccessToken 和 AccessSecret 对 commonData 接口进行签名验证
 */

// ==================== 方式一：使用原生 fetch API ====================

/**
 * AccessToken 签名工具类
 */
class AccessTokenSigner {
  constructor(accessToken, accessSecret) {
    this.accessToken = accessToken
    this.accessSecret = accessSecret
  }

  /**
   * 生成签名
   * @param {number} timestamp - 时间戳（毫秒）
   * @param {string} requestBody - 请求体（JSON 字符串）
   * @returns {Promise<string>} Base64 编码的签名
   */
  async generateSignature(timestamp, requestBody) {
    // 待签名字符串：accessSecret + timestamp + requestBody
    const signString = this.accessSecret + timestamp + (requestBody || '')

    // 使用 Web Crypto API 生成 HMAC-SHA256 签名
    const encoder = new TextEncoder()
    const keyData = encoder.encode(this.accessSecret)
    const messageData = encoder.encode(signString)

    const cryptoKey = await crypto.subtle.importKey(
      'raw',
      keyData,
      { name: 'HMAC', hash: 'SHA-256' },
      false,
      ['sign']
    )

    const signature = await crypto.subtle.sign('HMAC', cryptoKey, messageData)

    // 转换为 Base64
    return btoa(String.fromCharCode(...new Uint8Array(signature)))
  }

  /**
   * 生成请求头
   * @param {string} requestBody - 请求体（JSON 字符串）
   * @returns {Promise<Object>} 包含 AccessToken、时间戳和签名的请求头
   */
  async generateHeaders(requestBody) {
    const timestamp = Date.now()
    const signature = await this.generateSignature(timestamp, requestBody)

    return {
      'X-ACCESS-TOKEN': this.accessToken,
      'X-TIMESTAMP': timestamp.toString(),
      'X-SIGNATURE': signature,
      'Content-Type': 'application/json'
    }
  }
}

/**
 * 使用 AccessToken 发送 POST 请求（使用 fetch）
 * @param {string} url - 请求 URL
 * @param {Object} data - 请求数据
 * @param {string} accessToken - AccessToken
 * @param {string} accessSecret - AccessSecret
 * @returns {Promise<Object>} 响应数据
 */
async function postWithAccessToken(url, data, accessToken, accessSecret) {
  const signer = new AccessTokenSigner(accessToken, accessSecret)
  const requestBody = JSON.stringify(data)
  const headers = await signer.generateHeaders(requestBody)

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

// ==================== 方式二：使用 CryptoJS 库（适用于浏览器环境） ====================

/**
 * 使用 CryptoJS 生成签名（需要引入 crypto-js 库）
 * npm install crypto-js
 * import CryptoJS from 'crypto-js'
 */
function generateSignatureWithCryptoJS(accessSecret, timestamp, requestBody) {
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  const CryptoJS = require('crypto-js')

  // 待签名字符串：accessSecret + timestamp + requestBody
  const signString = accessSecret + timestamp + (requestBody || '')

  // 使用 HMAC-SHA256 算法生成签名
  const hash = CryptoJS.HmacSHA256(signString, accessSecret)

  // Base64 编码
  return CryptoJS.enc.Base64.stringify(hash)
}

/**
 * 使用 CryptoJS 发送请求
 */
async function postWithAccessTokenCryptoJS(url, data, accessToken, accessSecret) {
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  const CryptoJS = require('crypto-js')
  const timestamp = Date.now()
  const requestBody = JSON.stringify(data)
  const signature = generateSignatureWithCryptoJS(accessSecret, timestamp, requestBody)

  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'X-ACCESS-TOKEN': accessToken,
      'X-TIMESTAMP': timestamp.toString(),
      'X-SIGNATURE': signature,
      'Content-Type': 'application/json'
    },
    body: requestBody
  })

  return response.json()
}

// ==================== 方式三：使用 axios ====================

/**
 * 使用 axios 发送带 AccessToken 签名的请求
 * npm install axios
 * import axios from 'axios'
 */
async function axiosPostWithAccessToken(url, data, accessToken, accessSecret) {
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  const axios = require('axios')
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  const CryptoJS = require('crypto-js')

  const timestamp = Date.now()
  const requestBody = JSON.stringify(data)

  // 生成签名
  const signString = accessSecret + timestamp + requestBody
  const hash = CryptoJS.HmacSHA256(signString, accessSecret)
  const signature = CryptoJS.enc.Base64.stringify(hash)

  const response = await axios.post(url, data, {
    headers: {
      'X-ACCESS-TOKEN': accessToken,
      'X-TIMESTAMP': timestamp.toString(),
      'X-SIGNATURE': signature
    }
  })

  return response.data
}

// ==================== 使用示例 ====================

/**
 * 示例：调用 queryData 接口
 */
async function exampleQueryData() {
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

    // 方式二：使用 CryptoJS + fetch
    const response2 = await postWithAccessTokenCryptoJS(
      `${baseUrl}/commonData/queryData`,
      requestData,
      accessToken,
      accessSecret
    )
    console.log('Response (CryptoJS):', response2)

    // 方式三：使用 axios
    const response3 = await axiosPostWithAccessToken(
      `${baseUrl}/commonData/queryData`,
      requestData,
      accessToken,
      accessSecret
    )
    console.log('Response (axios):', response3)
  } catch (error) {
    console.error('Request failed:', error)
  }
}

/**
 * 示例：调用 queryChartData 接口
 */
async function exampleQueryChartData() {
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
  } catch (error) {
    console.error('Request failed:', error)
  }
}

// ==================== Node.js 环境示例 ====================

/**
 * Node.js 环境下的签名生成（使用 crypto 模块）
 */
// eslint-disable-next-line @typescript-eslint/no-var-requires
const crypto = require('crypto')

function generateSignatureNodeJS(accessSecret, timestamp, requestBody) {
  const signString = accessSecret + timestamp + (requestBody || '')
  const hmac = crypto.createHmac('sha256', accessSecret)
  hmac.update(signString)
  return hmac.digest('base64')
}

/**
 * Node.js 环境下使用 axios 发送请求
 */
async function nodeJSExample() {
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
  } catch (error) {
    console.error('Request failed:', error.response?.data || error.message)
  }
}

// ==================== 导出（如果使用 ES6 模块） ====================

// export { AccessTokenSigner, postWithAccessToken, postWithAccessTokenCryptoJS, axiosPostWithAccessToken }

// ==================== 使用说明 ====================

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
