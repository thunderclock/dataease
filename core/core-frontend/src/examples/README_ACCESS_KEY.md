# AccessKey 签名验证前端使用说明

## 概述

本文档介绍如何在前端使用 AccessKey 签名验证机制调用 DataEase 的 `commonData` 接口。

## 核心文件

- `src/utils/accessKey.ts`: AccessKey 签名工具类
- `src/api/accessKey.ts`: AccessKey 管理 API
- `src/api/chart.ts`: 图表数据查询 API（支持 AccessKey 签名）

## 使用方法

### 1. 在组件中使用

```typescript
import { queryChartData, queryData } from '@/api/chart'

// 使用 AccessKey 签名方式查询
const response = await queryChartData(
  requestData,
  'de_abc123...',  // accessKey
  'xyz789...'      // accessSecret
)

// 或者不使用签名（普通方式）
const response = await queryChartData(requestData)
```

### 2. 直接使用工具类

```typescript
import { axiosPostWithAccessKey } from '@/utils/accessKey'
import { service } from '@/config/axios/service'

const response = await axiosPostWithAccessKey(
  service,
  '/commonData/queryData',
  requestData,
  'de_abc123...',  // accessKey
  'xyz789...'      // accessSecret
)
```

## 签名算法

签名使用 HMAC-SHA256 算法：

```typescript
// 待签名字符串
const signString = accessSecret + timestamp + requestBody

// 使用 HMAC-SHA256 生成签名
const hash = CryptoJS.HmacSHA256(signString, accessSecret)

// Base64 编码
const signature = CryptoJS.enc.Base64.stringify(hash)
```

## 请求头

签名请求会自动添加以下请求头：

- `X-ACCESS-KEY`: AccessKey
- `X-TIMESTAMP`: 时间戳（毫秒）
- `X-SIGNATURE`: Base64 编码的签名

## 测试工具

在 `ChartDataServiceDialog` 组件中提供了测试界面，可以：
1. 配置 AccessKey 和 AccessSecret
2. 测试查询图表数据和查询数据接口
3. 查看请求和响应 JSON

## 功能特性

1. **自动签名**：使用 AccessKey 时会自动生成签名
2. **请求头保护**：axios 拦截器会保护 AccessKey 相关的请求头不被覆盖
3. **错误处理**：自动处理签名验证失败等错误

