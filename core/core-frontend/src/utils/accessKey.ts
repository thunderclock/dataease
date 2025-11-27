import CryptoJS from 'crypto-js/crypto-js'

/**
 * AccessKey 签名工具类
 * 用于生成和验证 AccessKey 签名
 */
export class AccessKeySigner {
  private accessKey: string
  private accessSecret: string

  constructor(accessKey: string, accessSecret: string) {
    this.accessKey = accessKey
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
   * @returns 包含 AccessKey、时间戳和签名的请求头
   */
  generateHeaders(requestBody: string): Record<string, string> {
    const timestamp = Date.now()
    const signature = this.generateSignature(timestamp, requestBody)

    return {
      'X-ACCESS-KEY': this.accessKey,
      'X-TIMESTAMP': timestamp.toString(),
      'X-SIGNATURE': signature
    }
  }
}

/**
 * 使用 AccessKey 发送 POST 请求
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
  const signer = new AccessKeySigner(accessKey, accessSecret)
  const requestBody = typeof data === 'string' ? data : JSON.stringify(data)
  const headers = signer.generateHeaders(requestBody)

  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...headers
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
  axiosInstance: any,
  url: string,
  data: any,
  accessKey: string,
  accessSecret: string
): Promise<any> {
  const signer = new AccessKeySigner(accessKey, accessSecret)
  // 将数据序列化为 JSON 字符串，用于签名和请求体
  const requestBody = typeof data === 'string' ? data : JSON.stringify(data)
  const headers = signer.generateHeaders(requestBody)

  // 使用 axios 的默认配置，但确保 headers 被正确设置
  // 注意：axios 的 headers 是大小写不敏感的，但为了兼容性，我们使用标准格式
  const config: any = {
    headers: {
      'Content-Type': 'application/json',
      'X-ACCESS-KEY': headers['X-ACCESS-KEY'],
      'X-TIMESTAMP': headers['X-TIMESTAMP'],
      'X-SIGNATURE': headers['X-SIGNATURE']
    },
    // 禁用 axios 的自动序列化，因为我们已经传递了字符串
    transformRequest: [
      (data: any) => {
        // 如果 data 已经是字符串，直接返回
        return typeof data === 'string' ? data : JSON.stringify(data)
      }
    ]
  }

  // 传递序列化后的字符串作为请求体，确保签名验证通过
  // 注意：axios.post 的第二个参数是 data，第三个参数是 config
  return axiosInstance.post(url, requestBody, config)
}
