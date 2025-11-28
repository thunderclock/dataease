# DataEase 数据服务 API 调用文档

## 概述

DataEase 数据服务提供了两个核心接口，用于查询数据集中的数据。所有接口都需要使用 AccessKey 进行签名验证。

## 目录

- [鉴权机制](#鉴权机制)
- [接口地址](#接口地址)
- [接口说明](#接口说明)
  - [1. 查询图表数据（queryChartData）](#1-查询图表数据querychartdata)
  - [2. 查询数据（queryData）](#2-查询数据querydata)
- [代码示例](#代码示例)
  - [TypeScript/JavaScript 示例](#typescriptjavascript-示例)
  - [Java 示例](#java-示例)
  - [Python 示例](#python-示例)

---

## 鉴权机制

### AccessKey 签名验证

所有 POST 请求都需要在请求头中包含以下信息：

| 请求头 | 说明 | 示例 |
|--------|------|------|
| `X-ACCESS-KEY` | AccessKey | `de_3GDh6y5d4FKSgaUFWwrgnQMKiOH6NZ` |
| `X-TIMESTAMP` | 时间戳（毫秒） | `1764228543472` |
| `X-SIGNATURE` | 签名（Base64编码） | `Rz5OZsW+7ej+WXPTv9nBq76rRDJ/LVfzeEBrFYob4e0=` |

### 签名算法

1. **构建待签名字符串**：
   ```
   signString = accessSecret + timestamp + requestBody
   ```
   其中：
   - `accessSecret`: AccessSecret（字符串）
   - `timestamp`: 时间戳（毫秒，字符串形式）
   - `requestBody`: 请求体（JSON 字符串）

2. **生成 HMAC-SHA256 签名**：
   ```
   hash = HMAC-SHA256(signString, accessSecret)
   ```

3. **Base64 编码**：
   ```
   signature = Base64.encode(hash)
   ```

### 安全要求

- **时间戳容差**：时间戳必须在服务器当前时间的 ±5 分钟内，否则请求会被拒绝
- **请求体一致性**：签名时使用的请求体必须与发送的请求体完全一致（包括空格、换行等）
- **AccessSecret 保密**：AccessSecret 只在生成时返回一次，请妥善保存，不要泄露
- **HTTPS 传输**：建议在生产环境使用 HTTPS 传输，保护 AccessKey 和签名

### 获取 AccessKey

1. 登录 DataEase 系统
2. 进入"AccessKey 管理"页面
3. 点击"生成 AccessKey"
4. 填写名称和过期时间（可选）
5. 保存返回的 AccessKey 和 AccessSecret（AccessSecret 只会显示一次）

---

## 接口地址

**基础 URL**：`http://your-dataease-server:port`

**接口路径**：
- 查询图表数据：`POST /commonData/queryChartData`
- 查询数据：`POST /commonData/queryData`

**完整 URL 示例**：
- `http://localhost:8100/commonData/queryChartData`
- `http://localhost:8100/commonData/queryData`

---

## 接口说明

### 1. 查询图表数据（queryChartData）

使用图表字段 ID 查询数据，适用于从已有图表配置中获取数据。

#### 请求参数

**Content-Type**: `application/json`

**请求体结构**：

```json
{
  "tableId": 123,
  "dimensions": [
    {
      "id": "field_id_1",
      "name": "字段名",
      "deType": 0,
      "summary": null,
      "sort": "none",
      "filter": null,
      "customSort": null,
      "dateStyle": null,
      "datePattern": null,
      "dateShowFormat": null,
      "formatterCfg": null,
      "compareCalc": null
    }
  ],
  "measures": [
    {
      "id": "field_id_2",
      "name": "字段名",
      "deType": 2,
      "summary": "sum",
      "sort": "desc",
      "filter": null,
      "customSort": null,
      "dateStyle": null,
      "datePattern": null,
      "dateShowFormat": null,
      "formatterCfg": null,
      "compareCalc": null
    }
  ],
  "filters": [
    {
      "fieldId": "123456|DE|param_name",
      "operator": "eq",
      "value": ["value1"],
      "parameters": [
        {
          "id": "123456|DE|param_name",
          "variableName": "param_name",
          "datasetTableId": 123,
          "datasetGroupId": 456,
          "operator": "eq",
          "value": ["value1"]
        }
      ]
    }
  ],
  "pageInfo": {
    "goPage": 1,
    "pageSize": 10
  },
  "sceneId": 456,
  "id": 789,
  "dataFrom": "dataset"
}
```

**参数说明**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `tableId` | Long | 是 | 数据集表ID |
| `dimensions` | List<ChartViewFieldDTO> | 是 | 维度字段列表 |
| `measures` | List<ChartViewFieldDTO> | 是 | 度量字段列表 |
| `filters` | List<ChartExtFilterDTO> | 否 | 过滤条件列表（支持数据集参数） |
| `pageInfo` | PageInfo | 否 | 分页信息 |
| `sceneId` | Long | 否 | 场景ID（用于权限校验） |
| `id` | Long | 否 | 图表ID（用于模板数据获取） |
| `dataFrom` | String | 否 | 数据来源：`template`（模板数据）或 `dataset`（数据集数据） |

**ChartExtFilterDTO 参数**（用于 `filters` 字段）：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `fieldId` | String | 是 | 字段ID或参数ID（参数ID格式：`{datasetTableId}\|DE\|{variableName}`） |
| `operator` | String | 是 | 操作符：`eq`、`in`、`between` 等 |
| `value` | List<String> | 是 | 过滤值列表 |
| `parameters` | List<SqlVariableDetails> | 否 | 数据集参数列表（用于数据集参数过滤） |

**SqlVariableDetails 参数**（用于 `parameters` 字段）：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | String | 是 | 参数ID（格式：`{datasetTableId}\|DE\|{variableName}`） |
| `variableName` | String | 是 | 参数变量名（SQL中定义的变量名） |
| `datasetTableId` | Long | 是 | 数据集表ID |
| `datasetGroupId` | Long | 是 | 数据集组ID |
| `operator` | String | 是 | 操作符：`eq`、`in`、`between` 等 |
| `value` | List<String> | 是 | 参数值列表 |

**PageInfo 参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `goPage` | Long | 否 | 当前页码（从1开始） |
| `pageSize` | Long | 否 | 每页大小 |

#### 响应结构

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "columns": ["字段1", "字段2"],
    "rows": [
      {
        "字段1": "值1",
        "字段2": 100
      }
    ],
    "totalItems": 100,
    "totalPage": 10,
    "currentPage": 1
  }
}
```

**响应字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `columns` | List<String> | 字段名列表（列名） |
| `rows` | List<Map<String, Object>> | 数据行列表，每行是一个 Map |
| `totalItems` | Long | 总记录数 |
| `totalPage` | Long | 总页数 |
| `currentPage` | Long | 当前页码 |

---

### 2. 查询数据（queryData）

使用字段名查询数据，适用于直接通过字段名进行查询的场景。

#### 请求参数

**Content-Type**: `application/json`

**请求体结构**：

```json
{
  "tableId": 123,
  "dimensions": [
    {
      "fieldName": "field1",
      "deType": 0,
      "name": "字段1",
      "summary": null,
      "sort": "none",
      "filter": null,
      "customSort": null,
      "dateStyle": null,
      "datePattern": null,
      "dateShowFormat": null,
      "formatterCfg": null,
      "compareCalc": null
    }
  ],
  "measures": [
    {
      "fieldName": "field2",
      "deType": 2,
      "name": "字段2",
      "summary": "sum",
      "sort": "desc",
      "filter": null,
      "customSort": null,
      "dateStyle": null,
      "datePattern": null,
      "dateShowFormat": null,
      "formatterCfg": null,
      "compareCalc": null
    }
  ],
  "filters": {
    "logic": "and",
    "items": [
      {
        "type": "item",
        "fieldName": "field3",
        "term": "eq",
        "valueType": "fixed",
        "value": "value1"
      }
    ]
  },
  "params": [
    {
      "variableName": "start_date",
      "datasetTableId": 123456,
      "datasetGroupId": 789,
      "operator": "eq",
      "value": ["2024-01-01"]
    }
  ],
  "pageInfo": {
    "pageNum": 1,
    "pageSize": 10
  }
}
```

**参数说明**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `tableId` | Long | 是 | 数据集表ID |
| `dimensions` | List<FieldQueryConfig> | 是 | 维度字段配置列表 |
| `measures` | List<FieldQueryConfig> | 是 | 度量字段配置列表 |
| `filters` | QueryFilterDTO | 否 | 过滤条件（支持嵌套条件） |
| `params` | List<DatasetParam> | 否 | 数据集参数列表（用于替换 SQL 中的参数变量） |
| `pageInfo` | PageInfo | 否 | 分页信息 |

**FieldQueryConfig 参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `fieldName` | String | 是 | 字段名（可以是 originName、name 或 dbFieldName） |
| `deType` | Integer | 是 | 字段类型：0-文本，1-时间，2-整型数值，3-浮点数值，4-布尔，5-地理位置，6-二进制，7-URL |
| `name` | String | 否 | 字段显示名称 |
| `summary` | String | 否 | 聚合函数（如 sum, count, avg, max, min 等） |
| `sort` | String | 否 | 排序方式：asc（升序）、desc（降序）、none（不排序） |
| `formatterCfg` | FormatterCfgDTO | 否 | 格式化配置（可选，不传递则使用默认值） |
| `compareCalc` | ChartFieldCompareDTO | 否 | 比较计算配置（可选，不传递则使用默认值） |

**QueryFilterDTO 参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `logic` | String | 是 | 逻辑关系：`and`（并且）或 `or`（或者） |
| `items` | List<FilterConditionItem> | 是 | 过滤条件项列表 |

**FilterConditionItem 参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `type` | String | 是 | 条件类型：`item`（单个条件）或 `tree`（嵌套条件树） |
| `fieldName` | String | 是（type=item时） | 字段名 |
| `term` | String | 是（type=item时） | 关系符：`eq`（等于）、`not_eq`（不等于）、`lt`（小于）、`le`（小于等于）、`gt`（大于）、`ge`（大于等于）、`in`（包含）、`not_in`（不包含）、`like`（模糊匹配）、`not_like`（不模糊匹配）、`null`（为空）、`not_null`（不为空）、`empty`（为空字符串）、`not_empty`（不为空字符串）、`between`（区间） |
| `valueType` | String | 是（type=item时） | 对比值类型：`fixed`（固定值）或 `relative`（相对值） |
| `value` | Object | 否 | 固定值（当 valueType 为 fixed 时使用） |
| `enumValue` | List<Object> | 否 | 枚举值列表（用于 in、not_in 等操作符） |
| `relativeValue` | RelativeValueSetting | 否 | 相对值设置（当 valueType 为 relative 时使用） |
| `subTree` | QueryFilterDTO | 否 | 嵌套条件树（当 type 为 tree 时使用） |

**RelativeValueSetting 参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `timeType` | String | 是 | 相对时间类型：如 `today`（今天）、`yesterday`（昨天）、`thisWeek`（本周）等 |
| `offset` | Integer | 否 | 相对数量（如：-7 表示7天前，+7 表示7天后） |
| `unit` | String | 否 | 时间单位：`day`（天）、`week`（周）、`month`（月）、`year`（年） |

**DatasetParam 参数**（用于 `params` 字段）：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `variableName` | String | 是 | 参数变量名（SQL 中定义的变量名，如：`start_date`） |
| `datasetTableId` | Long | 是 | 数据集表ID |
| `datasetGroupId` | Long | 否 | 数据集组ID（可选） |
| `operator` | String | 否 | 操作符：`eq`（等于）、`in`（包含）、`between`（区间）等。默认为 `eq`，如果未提供则使用默认值 |
| `value` | List<String> | 是 | 参数值列表。多个值用逗号分隔，`in` 操作符可传入多个值，`between` 操作符传入两个值 |

**DatasetParam 使用说明**：
- 参数 ID 会自动从 `datasetTableId` 和 `variableName` 构造（格式：`{datasetTableId}|DE|{variableName}`）
- 如果未提供 `operator`，默认使用 `eq`
- `value` 必须是字符串数组，即使只有一个值也需要使用数组格式
- 参数值会替换 SQL 查询中对应的变量，例如：SQL 中的 `${start_date}` 会被替换为传入的值

**DatasetParam 示例**：

```json
{
  "params": [
    {
      "variableName": "start_date",
      "datasetTableId": 123456,
      "datasetGroupId": 789,
      "operator": "eq",
      "value": ["2024-01-01"]
    },
    {
      "variableName": "end_date",
      "datasetTableId": 123456,
      "operator": "between",
      "value": ["2024-01-01", "2024-12-31"]
    },
    {
      "variableName": "status",
      "datasetTableId": 123456,
      "operator": "in",
      "value": ["active", "pending", "completed"]
    }
  ]
}
```

### 3. 在 filter 中使用数据集参数（queryChartData）

`queryChartData` 接口的 `filters` 字段支持设置数据集参数，用于在 SQL 查询中替换参数变量。

**使用场景**：
- 数据集 SQL 中包含参数变量，如：`SELECT * FROM table WHERE date >= ${start_date}`
- 需要在查询时动态传入参数值

**示例**：

```json
{
  "tableId": 123,
  "dimensions": [...],
  "measures": [...],
  "filters": [
    {
      "fieldId": "123456|DE|start_date",
      "operator": "eq",
      "value": ["2024-01-01"],
      "parameters": [
        {
          "id": "123456|DE|start_date",
          "variableName": "start_date",
          "datasetTableId": 123456,
          "datasetGroupId": 789,
          "operator": "eq",
          "value": ["2024-01-01"],
          "deType": 1
        }
      ]
    }
  ]
}
```

**注意事项**：
1. 参数 ID 格式：`{datasetTableId}|DE|{variableName}`
2. `parameters` 数组中的参数必须包含完整的参数信息（id、variableName、datasetTableId 等）
3. `operator` 和 `value` 会应用到参数上，用于 SQL 变量替换
4. 如果数据集 SQL 中定义了参数变量（如 `${variable_name}`），参数值会替换对应的变量

**PageInfo 参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `pageNum` | Long | 否 | 当前页码（从1开始） |
| `pageSize` | Long | 否 | 每页大小 |

#### 响应结构

与 `queryChartData` 接口相同。

---

## 代码示例

### TypeScript/JavaScript 示例

#### 1. 签名生成工具类

```typescript
import CryptoJS from 'crypto-js/crypto-js'

/**
 * AccessKey 签名工具类
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
   */
  generateSignature(timestamp: number, requestBody: string): string {
    const signString = this.accessSecret + timestamp + (requestBody || '')
    const hash = CryptoJS.HmacSHA256(signString, this.accessSecret)
    return CryptoJS.enc.Base64.stringify(hash)
  }

  /**
   * 生成请求头
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
```

#### 2. 使用 fetch 发送请求

```typescript
/**
 * 使用 AccessKey 发送 POST 请求（使用 fetch）
 */
async function postWithAccessKey(
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

// 调用示例：查询数据
async function queryDataExample() {
  const baseUrl = 'http://localhost:8100'
  const accessKey = 'de_3GDh6y5d4FKSgaUFWwrgnQMKiOH6NZ'
  const accessSecret = 'your_access_secret_here'

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
    filters: {
      logic: 'and',
      items: [
        {
          type: 'item',
          fieldName: 'field3',
          term: 'eq',
          valueType: 'fixed',
        value: 'value1'
      }
    ]
  },
  params: [
    {
      variableName: 'start_date',
      datasetTableId: 123456,
      datasetGroupId: 789,
      operator: 'eq',
      value: ['2024-01-01']
    }
  ],
  pageInfo: {
    pageNum: 1,
    pageSize: 10
  }
  }

  try {
    const response = await postWithAccessKey(
      `${baseUrl}/commonData/queryData`,
      requestData,
      accessKey,
      accessSecret
    )
    console.log('查询结果:', response)
  } catch (error) {
    console.error('请求失败:', error)
  }
}
```

#### 3. 使用 axios 发送请求

```typescript
import axios, { AxiosInstance } from 'axios'
import CryptoJS from 'crypto-js/crypto-js'

/**
 * 使用 AccessKey 发送 POST 请求（使用 axios）
 */
async function axiosPostWithAccessKey(
  axiosInstance: AxiosInstance,
  url: string,
  data: any,
  accessKey: string,
  accessSecret: string
): Promise<any> {
  const requestBody = typeof data === 'string' ? data : JSON.stringify(data)
  const timestamp = Date.now()
  
  // 生成签名
  const signString = accessSecret + timestamp + requestBody
  const hash = CryptoJS.HmacSHA256(signString, accessSecret)
  const signature = CryptoJS.enc.Base64.stringify(hash)

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
const axiosInstance = axios.create({
  baseURL: 'http://localhost:8100',
  timeout: 30000
})

const response = await axiosPostWithAccessKey(
  axiosInstance,
  '/commonData/queryData',
  requestData,
  accessKey,
  accessSecret
)
```

---

### Java 示例

#### 1. 签名工具类

```java
package com.example.dataease;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * AccessKey 签名工具类
 */
public class AccessKeySigner {
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    
    private String accessKey;
    private String accessSecret;
    
    public AccessKeySigner(String accessKey, String accessSecret) {
        this.accessKey = accessKey;
        this.accessSecret = accessSecret;
    }
    
    /**
     * 生成签名
     */
    public String generateSignature(long timestamp, String requestBody) {
        try {
            String signString = accessSecret + timestamp + (requestBody != null ? requestBody : "");
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                accessSecret.getBytes(StandardCharsets.UTF_8), 
                HMAC_SHA256_ALGORITHM
            );
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(signString.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }
    
    /**
     * 生成请求头
     */
    public java.util.Map<String, String> generateHeaders(String requestBody) {
        long timestamp = System.currentTimeMillis();
        String signature = generateSignature(timestamp, requestBody);
        
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        headers.put("X-ACCESS-KEY", accessKey);
        headers.put("X-TIMESTAMP", String.valueOf(timestamp));
        headers.put("X-SIGNATURE", signature);
        headers.put("Content-Type", "application/json");
        
        return headers;
    }
}
```

#### 2. 使用 OkHttp 发送请求

```java
package com.example.dataease;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import java.io.IOException;
import java.util.Map;

/**
 * DataEase 数据服务客户端
 */
public class DataEaseClient {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client;
    private final String baseUrl;
    private final AccessKeySigner signer;
    private final ObjectMapper objectMapper;
    
    public DataEaseClient(String baseUrl, String accessKey, String accessSecret) {
        this.baseUrl = baseUrl;
        this.signer = new AccessKeySigner(accessKey, accessSecret);
        this.client = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 查询数据
     */
    public Map<String, Object> queryData(QueryDataRequest request) throws IOException {
        String requestBody = objectMapper.writeValueAsString(request);
        Map<String, String> headers = signer.generateHeaders(requestBody);
        
        RequestBody body = RequestBody.create(requestBody, JSON);
        Request.Builder requestBuilder = new Request.Builder()
            .url(baseUrl + "/commonData/queryData")
            .post(body);
        
        // 添加请求头
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            requestBuilder.addHeader(entry.getKey(), entry.getValue());
        }
        
        Request httpRequest = requestBuilder.build();
        
        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code: " + response);
            }
            
            String responseBody = response.body().string();
            return objectMapper.readValue(responseBody, Map.class);
        }
    }
    
    /**
     * 查询图表数据
     */
    public Map<String, Object> queryChartData(QueryChartDataRequest request) throws IOException {
        String requestBody = objectMapper.writeValueAsString(request);
        Map<String, String> headers = signer.generateHeaders(requestBody);
        
        RequestBody body = RequestBody.create(requestBody, JSON);
        Request.Builder requestBuilder = new Request.Builder()
            .url(baseUrl + "/commonData/queryChartData")
            .post(body);
        
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            requestBuilder.addHeader(entry.getKey(), entry.getValue());
        }
        
        Request httpRequest = requestBuilder.build();
        
        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code: " + response);
            }
            
            String responseBody = response.body().string();
            return objectMapper.readValue(responseBody, Map.class);
        }
    }
}

// 使用示例
public class Example {
    public static void main(String[] args) throws IOException {
        String baseUrl = "http://localhost:8100";
        String accessKey = "de_3GDh6y5d4FKSgaUFWwrgnQMKiOH6NZ";
        String accessSecret = "your_access_secret_here";
        
        DataEaseClient client = new DataEaseClient(baseUrl, accessKey, accessSecret);
        
        // 构建请求
        QueryDataRequest request = new QueryDataRequest();
        request.setTableId(123L);
        
        // 设置维度
        FieldQueryConfig dimension = new FieldQueryConfig();
        dimension.setFieldName("field1");
        dimension.setDeType(0);
        dimension.setName("字段1");
        request.setDimensions(java.util.Arrays.asList(dimension));
        
        // 设置度量
        FieldQueryConfig measure = new FieldQueryConfig();
        measure.setFieldName("field2");
        measure.setDeType(2);
        measure.setName("字段2");
        measure.setSummary("sum");
        request.setMeasures(java.util.Arrays.asList(measure));
        
        // 设置数据集参数（可选）
        QueryDataRequest.DatasetParam param = new QueryDataRequest.DatasetParam();
        param.setVariableName("start_date");
        param.setDatasetTableId(123456L);
        param.setDatasetGroupId(789L);
        param.setOperator("eq");
        param.setValue(java.util.Arrays.asList("2024-01-01"));
        request.setParams(java.util.Arrays.asList(param));
        
        // 设置分页
        QueryDataRequest.PageInfo pageInfo = new QueryDataRequest.PageInfo();
        pageInfo.setPageNum(1L);
        pageInfo.setPageSize(10L);
        request.setPageInfo(pageInfo);
        
        // 发送请求
        Map<String, Object> response = client.queryData(request);
        System.out.println("查询结果: " + response);
    }
}
```

#### 3. 使用 Spring RestTemplate 发送请求

```java
package com.example.dataease;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

/**
 * 使用 Spring RestTemplate 的示例
 */
public class DataEaseRestTemplateClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final AccessKeySigner signer;
    private final ObjectMapper objectMapper;
    
    public DataEaseRestTemplateClient(String baseUrl, String accessKey, String accessSecret) {
        this.baseUrl = baseUrl;
        this.signer = new AccessKeySigner(accessKey, accessSecret);
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    public Map<String, Object> queryData(QueryDataRequest request) throws Exception {
        String requestBody = objectMapper.writeValueAsString(request);
        Map<String, String> headers = signer.generateHeaders(requestBody);
        
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpHeaders.add(entry.getKey(), entry.getValue());
        }
        
        HttpEntity<String> entity = new HttpEntity<>(requestBody, httpHeaders);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            baseUrl + "/commonData/queryData",
            entity,
            Map.class
        );
        
        return response.getBody();
    }
}
```

---

### Python 示例

#### 1. 签名工具类

```python
import hmac
import hashlib
import base64
import time
import json
from typing import Dict, Any

class AccessKeySigner:
    """AccessKey 签名工具类"""
    
    def __init__(self, access_key: str, access_secret: str):
        self.access_key = access_key
        self.access_secret = access_secret
    
    def generate_signature(self, timestamp: int, request_body: str) -> str:
        """生成签名"""
        sign_string = self.access_secret + str(timestamp) + (request_body or "")
        hash_bytes = hmac.new(
            self.access_secret.encode('utf-8'),
            sign_string.encode('utf-8'),
            hashlib.sha256
        ).digest()
        return base64.b64encode(hash_bytes).decode('utf-8')
    
    def generate_headers(self, request_body: str) -> Dict[str, str]:
        """生成请求头"""
        timestamp = int(time.time() * 1000)
        signature = self.generate_signature(timestamp, request_body)
        
        return {
            'X-ACCESS-KEY': self.access_key,
            'X-TIMESTAMP': str(timestamp),
            'X-SIGNATURE': signature,
            'Content-Type': 'application/json'
        }
```

#### 2. 使用 requests 库发送请求

```python
import requests
import json
from typing import Dict, Any, Optional

class DataEaseClient:
    """DataEase 数据服务客户端"""
    
    def __init__(self, base_url: str, access_key: str, access_secret: str):
        self.base_url = base_url.rstrip('/')
        self.signer = AccessKeySigner(access_key, access_secret)
        self.session = requests.Session()
    
    def query_data(
        self,
        table_id: int,
        dimensions: list,
        measures: list,
        filters: Optional[Dict] = None,
        params: Optional[list] = None,
        page_num: int = 1,
        page_size: int = 10
    ) -> Dict[str, Any]:
        """查询数据"""
        request_data = {
            "tableId": table_id,
            "dimensions": dimensions,
            "measures": measures,
            "filters": filters,
            "params": params,
            "pageInfo": {
                "pageNum": page_num,
                "pageSize": page_size
            }
        }
        
        request_body = json.dumps(request_data, ensure_ascii=False)
        headers = self.signer.generate_headers(request_body)
        
        url = f"{self.base_url}/commonData/queryData"
        response = self.session.post(
            url,
            data=request_body.encode('utf-8'),
            headers=headers
        )
        
        response.raise_for_status()
        return response.json()
    
    def query_chart_data(
        self,
        table_id: int,
        dimensions: list,
        measures: list,
        filters: Optional[list] = None,
        scene_id: Optional[int] = None,
        go_page: int = 1,
        page_size: int = 10
    ) -> Dict[str, Any]:
        """查询图表数据"""
        request_data = {
            "tableId": table_id,
            "dimensions": dimensions,
            "measures": measures,
            "filters": filters or [],
            "pageInfo": {
                "goPage": go_page,
                "pageSize": page_size
            }
        }
        
        if scene_id:
            request_data["sceneId"] = scene_id
        
        request_body = json.dumps(request_data, ensure_ascii=False)
        headers = self.signer.generate_headers(request_body)
        
        url = f"{self.base_url}/commonData/queryChartData"
        response = self.session.post(
            url,
            data=request_body.encode('utf-8'),
            headers=headers
        )
        
        response.raise_for_status()
        return response.json()

# 使用示例
if __name__ == "__main__":
    base_url = "http://localhost:8100"
    access_key = "de_3GDh6y5d4FKSgaUFWwrgnQMKiOH6NZ"
    access_secret = "your_access_secret_here"
    
    client = DataEaseClient(base_url, access_key, access_secret)
    
    # 查询数据示例
    try:
        response = client.query_data(
            table_id=123,
            dimensions=[
                {
                    "fieldName": "field1",
                    "deType": 0,
                    "name": "字段1"
                }
            ],
            measures=[
                {
                    "fieldName": "field2",
                    "deType": 2,
                    "name": "字段2",
                    "summary": "sum"
                }
            ],
            filters={
                "logic": "and",
                "items": [
                    {
                        "type": "item",
                        "fieldName": "field3",
                        "term": "eq",
                        "valueType": "fixed",
                        "value": "value1"
                    }
                ]
            },
            params=[
                {
                    "variableName": "start_date",
                    "datasetTableId": 123456,
                    "datasetGroupId": 789,
                    "operator": "eq",
                    "value": ["2024-01-01"]
                }
            ],
            page_num=1,
            page_size=10
        )
        
        print("查询结果:")
        print(json.dumps(response, indent=2, ensure_ascii=False))
        
    except requests.exceptions.RequestException as e:
        print(f"请求失败: {e}")
        if hasattr(e.response, 'text'):
            print(f"错误详情: {e.response.text}")
```

#### 3. 使用 aiohttp 异步请求

```python
import aiohttp
import asyncio
import json
from typing import Dict, Any, Optional

class AsyncDataEaseClient:
    """DataEase 数据服务异步客户端"""
    
    def __init__(self, base_url: str, access_key: str, access_secret: str):
        self.base_url = base_url.rstrip('/')
        self.signer = AccessKeySigner(access_key, access_secret)
    
    async def query_data(
        self,
        table_id: int,
        dimensions: list,
        measures: list,
        filters: Optional[Dict] = None,
        params: Optional[list] = None,
        page_num: int = 1,
        page_size: int = 10
    ) -> Dict[str, Any]:
        """异步查询数据"""
        request_data = {
            "tableId": table_id,
            "dimensions": dimensions,
            "measures": measures,
            "filters": filters,
            "params": params,
            "pageInfo": {
                "pageNum": page_num,
                "pageSize": page_size
            }
        }
        
        request_body = json.dumps(request_data, ensure_ascii=False)
        headers = self.signer.generate_headers(request_body)
        
        url = f"{self.base_url}/commonData/queryData"
        
        async with aiohttp.ClientSession() as session:
            async with session.post(
                url,
                data=request_body.encode('utf-8'),
                headers=headers
            ) as response:
                response.raise_for_status()
                return await response.json()

# 异步使用示例
async def main():
    client = AsyncDataEaseClient(
        "http://localhost:8100",
        "de_3GDh6y5d4FKSgaUFWwrgnQMKiOH6NZ",
        "your_access_secret_here"
    )
    
    response = await client.query_data(
        table_id=123,
        dimensions=[{"fieldName": "field1", "deType": 0, "name": "字段1"}],
        measures=[{"fieldName": "field2", "deType": 2, "name": "字段2", "summary": "sum"}]
    )
    
    print(json.dumps(response, indent=2, ensure_ascii=False))

# 运行异步示例
# asyncio.run(main())
```

---

## 错误码说明

| 错误码 | 说明 | 解决方案 |
|--------|------|----------|
| `50002` | 请求头缺失、时间戳无效、AccessKey 无效或签名验证失败 | 检查请求头是否正确设置，时间戳是否在容差范围内，AccessKey 是否有效，签名是否正确 |
| `50001` | 系统内部错误 | 查看服务器日志获取详细错误信息 |

## 常见问题

### 1. 签名验证失败

**可能原因**：
- AccessSecret 不正确
- 请求体与签名时使用的字符串不一致
- 时间戳超出容差范围（±5分钟）

**解决方案**：
- 确认 AccessSecret 正确
- 确保请求体 JSON 字符串与签名时完全一致（包括空格、换行等）
- 检查客户端和服务器时间是否同步

### 2. 时间戳过期

**错误信息**：`Timestamp expired or invalid`

**解决方案**：
- 确保客户端和服务器时间同步
- 时间戳容差为 ±5 分钟，如果时间差过大，请调整系统时间

### 3. AccessKey 无效

**错误信息**：`Invalid access key`

**解决方案**：
- 检查 AccessKey 是否正确
- 确认 AccessKey 已启用（enable=true）
- 检查 AccessKey 是否已过期

### 4. 字段不存在

**错误信息**：`Unknown column 'xxx' in 'field list'`

**解决方案**：
- 确认字段名（fieldName）正确
- 检查字段是否存在于指定的数据集中
- 对于计算字段，确保表达式格式正确

---

## 完整示例

### 查询数据完整示例（带过滤条件）

```json
{
  "tableId": 123,
  "dimensions": [
    {
      "fieldName": "region",
      "deType": 0,
      "name": "地区"
    },
    {
      "fieldName": "date",
      "deType": 1,
      "name": "日期",
      "dateStyle": "yyyy-MM-dd",
      "datePattern": "-",
      "dateShowFormat": "yyyy-MM-dd"
    }
  ],
  "measures": [
    {
      "fieldName": "sales_amount",
      "deType": 3,
      "name": "销售金额",
      "summary": "sum"
    },
    {
      "fieldName": "order_count",
      "deType": 2,
      "name": "订单数量",
      "summary": "count"
    }
  ],
  "filters": {
    "logic": "and",
    "items": [
      {
        "type": "item",
        "fieldName": "region",
        "term": "in",
        "valueType": "fixed",
        "enumValue": ["华东", "华南", "华北"]
      },
      {
        "type": "item",
        "fieldName": "date",
        "term": "between",
        "valueType": "fixed",
        "value": ["2024-01-01", "2024-12-31"]
      },
      {
        "type": "tree",
        "subTree": {
          "logic": "or",
          "items": [
            {
              "type": "item",
              "fieldName": "sales_amount",
              "term": "ge",
              "valueType": "fixed",
              "value": 10000
            },
            {
              "type": "item",
              "fieldName": "order_count",
              "term": "ge",
              "valueType": "fixed",
              "value": 100
            }
          ]
        }
      }
    ]
  },
  "pageInfo": {
    "pageNum": 1,
    "pageSize": 20
  }
}
```

---

## 技术支持

如有问题，请联系 DataEase 技术支持团队。

**文档版本**：v1.0  
**最后更新**：2025-01-27

