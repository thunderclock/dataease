/**
 * AccessKey 签名验证示例
 * 
 * 使用说明：
 * 1. 在浏览器控制台中运行此代码
 * 2. 或者将代码保存为 HTML 文件并在浏览器中打开
 * 
 * 注意：需要先引入 CryptoJS 和 axios 库
 */

// 引入 CryptoJS（如果使用 CDN，可以跳过）
// <script src="https://cdn.jsdelivr.net/npm/crypto-js@4.1.1/crypto-js.js"></script>
// <script src="https://cdn.jsdelivr.net/npm/axios@1.6.0/dist/axios.min.js"></script>

/**
 * 生成签名
 * @param {string} accessSecret AccessSecret
 * @param {number} timestamp 时间戳（毫秒）
 * @param {string} requestBody 请求体（JSON 字符串）
 * @returns {string} Base64 编码的签名
 */
function generateSignature(accessSecret, timestamp, requestBody) {
  // 待签名字符串：accessSecret + timestamp + requestBody
  const signString = accessSecret + timestamp + (requestBody || '')
  
  // 使用 HMAC-SHA256 算法生成签名
  const hash = CryptoJS.HmacSHA256(signString, accessSecret)
  
  // Base64 编码
  return CryptoJS.enc.Base64.stringify(hash)
}

/**
 * 使用 AccessKey 发送 POST 请求（使用 fetch）
 * @param {string} url 请求 URL
 * @param {object} data 请求数据
 * @param {string} accessKey AccessKey
 * @param {string} accessSecret AccessSecret
 * @returns {Promise} 响应 Promise
 */
async function postWithAccessKey(url, data, accessKey, accessSecret) {
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
 * @param {string} url 请求 URL
 * @param {object} data 请求数据
 * @param {string} accessKey AccessKey
 * @param {string} accessSecret AccessSecret
 * @returns {Promise} 响应 Promise
 */
async function axiosPostWithAccessKey(url, data, accessKey, accessSecret) {
  const requestBody = typeof data === 'string' ? data : JSON.stringify(data)
  const timestamp = Date.now()
  const signature = generateSignature(accessSecret, timestamp, requestBody)
  
  return axios.post(url, requestBody, {
    headers: {
      'Content-Type': 'application/json',
      'X-ACCESS-KEY': accessKey,
      'X-TIMESTAMP': timestamp.toString(),
      'X-SIGNATURE': signature
    },
    transformRequest: [(data) => data] // 禁用自动序列化
  })
}

// 使用示例
async function example() {
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
        id: 'field1_id',
        // ... 其他字段
      }
    ],
    measures: [
      {
        id: 'field2_id',
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
    const response2 = await axiosPostWithAccessKey(
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

// 运行示例（取消注释以运行）
// example()

