# DataEase AccessToken 签名验证 - 前端使用指南

## 概述

本文档介绍如何在前端使用 AccessToken 对 `commonData` 接口进行签名验证。

## 文件说明

1. **`accessToken.ts`** - AccessToken 签名工具类（TypeScript）
2. **`accessTokenExample.js`** - JavaScript 示例代码
3. **`accessTokenExample.ts`** - TypeScript 示例代码
4. **`accessTokenExample.html`** - 可直接在浏览器中运行的 HTML 示例

## 快速开始

### 1. 在 Vue 组件中使用

```typescript
import { queryData, queryChartData } from '@/api/chart'

// 配置 AccessToken
const accessToken = 'de_abc123...'
const accessSecret = 'xyz789...'

// 调用接口（会自动使用 AccessToken 签名）
const response = await queryData(
  {
    tableId: 123,
    dimensions: [...],
    measures: [...],
    pageInfo: { pageNum: 1, pageSize: 10 }
  },
  accessToken,
  accessSecret
)
```

### 2. 在 ChartDataServiceDialog 中使用

在数据服务对话框中：
1. 切换到 "AccessToken 配置" 标签页
2. 输入 AccessToken 和 AccessSecret
3. 切换到 "查询数据" 或 "查询图表数据" 标签页
4. 点击 "测试查询"，系统会自动使用 AccessToken 签名

## 签名算法

### 签名规则

```javascript
// 1. 构建待签名字符串
const signString = accessSecret + timestamp + requestBody

// 2. 使用 HMAC-SHA256 生成签名
const hash = CryptoJS.HmacSHA256(signString, accessSecret)

// 3. Base64 编码
const signature = CryptoJS.enc.Base64.stringify(hash)
```

### 请求头

```javascript
{
  'X-ACCESS-TOKEN': 'de_abc123...',
  'X-TIMESTAMP': '1701234567890',
  'X-SIGNATURE': 'xyz789...',
  'Content-Type': 'application/json'
}
```

## 完整示例

### 使用 fetch API

```javascript
import CryptoJS from 'crypto-js'

const accessToken = 'de_abc123...'
const accessSecret = 'xyz789...'
const url = 'http://localhost:8100/commonData/queryData'
const requestData = {
  tableId: 123,
  dimensions: [],
  measures: [],
  pageInfo: { pageNum: 1, pageSize: 10 }
}

// 生成签名
const timestamp = Date.now()
const requestBody = JSON.stringify(requestData)
const signString = accessSecret + timestamp + requestBody
const hash = CryptoJS.HmacSHA256(signString, accessSecret)
const signature = CryptoJS.enc.Base64.stringify(hash)

// 发送请求
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

const result = await response.json()
console.log(result)
```

### 使用 axios

```javascript
import axios from 'axios'
import CryptoJS from 'crypto-js'

const accessToken = 'de_abc123...'
const accessSecret = 'xyz789...'
const url = 'http://localhost:8100/commonData/queryData'
const requestData = {
  tableId: 123,
  dimensions: [],
  measures: [],
  pageInfo: { pageNum: 1, pageSize: 10 }
}

// 生成签名
const timestamp = Date.now()
const requestBody = JSON.stringify(requestData)
const signString = accessSecret + timestamp + requestBody
const hash = CryptoJS.HmacSHA256(signString, accessSecret)
const signature = CryptoJS.enc.Base64.stringify(hash)

// 发送请求
const response = await axios.post(url, requestData, {
  headers: {
    'X-ACCESS-TOKEN': accessToken,
    'X-TIMESTAMP': timestamp.toString(),
    'X-SIGNATURE': signature
  }
})

console.log(response.data)
```

### 使用 AccessTokenSigner 类

```typescript
import { AccessTokenSigner, postWithAccessToken } from '@/utils/accessToken'

const accessToken = 'de_abc123...'
const accessSecret = 'xyz789...'

// 方式一：使用工具函数
const response = await postWithAccessToken(
  'http://localhost:8100/commonData/queryData',
  { tableId: 123, dimensions: [], measures: [], pageInfo: { pageNum: 1, pageSize: 10 } },
  accessToken,
  accessSecret
)

// 方式二：使用签名工具类
const signer = new AccessTokenSigner(accessToken, accessSecret)
const requestBody = JSON.stringify({ tableId: 123, ... })
const headers = signer.generateHeaders(requestBody)

const response = await fetch('http://localhost:8100/commonData/queryData', {
  method: 'POST',
  headers: { ...headers, 'Content-Type': 'application/json' },
  body: requestBody
})
```

## 在 ChartDataServiceDialog 中配置

1. 打开数据服务对话框
2. 切换到 "AccessToken 配置" 标签页
3. 输入：
   - **AccessToken**: 从后端获取的 AccessToken（如：`de_abc123...`）
   - **AccessSecret**: 从后端获取的 AccessSecret（如：`xyz789...`）
4. 切换到 "查询数据" 或 "查询图表数据" 标签页
5. 配置查询参数
6. 点击 "测试查询"，系统会自动使用 AccessToken 签名验证

## 注意事项

1. **时间戳同步**：确保客户端和服务器时间同步，时间差不能超过 5 分钟
2. **请求体一致性**：签名时使用的请求体必须与发送的请求体完全一致（包括空格、换行等）
3. **AccessSecret 保密**：AccessSecret 只在生成时返回一次，请妥善保存，不要泄露
4. **HTTPS 传输**：建议在生产环境使用 HTTPS 传输，保护 AccessToken 和签名

## 错误处理

常见错误及解决方案：

1. **Missing required headers**: 缺少必需的请求头，检查是否添加了 `X-ACCESS-TOKEN`、`X-TIMESTAMP`、`X-SIGNATURE`
2. **Invalid timestamp format**: 时间戳格式错误，确保使用毫秒级时间戳
3. **Timestamp expired or invalid**: 时间戳过期或无效，检查客户端和服务器时间是否同步
4. **Invalid access token**: AccessToken 无效，检查 AccessToken 是否正确、是否已启用、是否已过期
5. **Invalid signature**: 签名验证失败，检查：
   - AccessSecret 是否正确
   - 请求体是否与签名时使用的完全一致
   - 签名算法是否正确

## 测试工具

可以使用 `accessTokenExample.html` 文件在浏览器中直接测试：

1. 打开 `accessTokenExample.html` 文件
2. 填写 Base URL、AccessToken、AccessSecret
3. 填写请求体 JSON
4. 点击 "测试 queryData" 或 "测试 queryChartData"
5. 查看结果

## 相关文件

- 后端实现：`core/core-backend/src/main/java/io/dataease/auth/`
- 前端工具：`core/core-frontend/src/utils/accessToken.ts`
- API 调用：`core/core-frontend/src/api/chart.ts`
- 组件：`core/core-frontend/src/components/visualization/ChartDataServiceDialog.vue`

