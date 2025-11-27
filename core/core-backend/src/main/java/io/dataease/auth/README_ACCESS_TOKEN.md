# AccessToken 签名验证机制使用说明

## 概述

AccessToken 签名验证机制用于保护 `commonData` 接口的 POST 请求，确保请求的完整性和真实性。

## 功能特性

1. **AccessToken 生成**：系统可以生成 AccessToken 和对应的 AccessSecret
2. **签名验证**：使用 HMAC-SHA256 算法对 POST 请求进行签名验证
3. **时间戳验证**：防止重放攻击，时间戳容差为 5 分钟
4. **自动过期检查**：支持设置 AccessToken 过期时间

## 数据库表结构

表名：`core_access_token`

字段说明：
- `id`: 主键
- `access_token`: AccessToken（唯一）
- `access_secret`: AccessSecret（用于签名）
- `name`: 名称/描述
- `creator`: 创建人ID
- `create_time`: 创建时间
- `update_time`: 更新时间
- `expire_time`: 过期时间（null 表示永不过期）
- `enable`: 是否启用（1-启用，0-禁用）
- `last_use_time`: 最后使用时间

## API 接口

### 1. 生成 AccessToken

**请求：**
```http
POST /accessToken/generate
Content-Type: application/json

{
  "name": "API客户端1",
  "expireTime": 1735689600000  // 可选，过期时间戳，null 表示永不过期
}
```

**响应：**
```json
{
  "id": 1,
  "accessToken": "de_abc123...",
  "accessSecret": "xyz789...",  // 只在生成时返回一次，请妥善保存
  "name": "API客户端1",
  "creator": 1,
  "createTime": 1701234567890,
  "expireTime": 1735689600000,
  "enable": true
}
```

### 2. 禁用 AccessToken

**请求：**
```http
POST /accessToken/disable/{id}
```

### 3. 删除 AccessToken

**请求：**
```http
DELETE /accessToken/{id}
```

## 客户端使用方式

### 1. 生成签名

使用以下算法生成签名：

```java
// 伪代码
String signString = accessSecret + timestamp + requestBody;
byte[] hash = HMAC_SHA256(signString, accessSecret);
String signature = Base64.encode(hash);
```

**签名规则：**
- 待签名字符串 = `accessSecret + timestamp + requestBody`
- 使用 HMAC-SHA256 算法，密钥为 `accessSecret`
- 对结果进行 Base64 编码

### 2. 发送请求

在请求头中添加以下字段：

```http
POST /commonData/queryData
X-ACCESS-TOKEN: de_abc123...
X-TIMESTAMP: 1701234567890
X-SIGNATURE: xyz789...（Base64编码的签名）
Content-Type: application/json

{
  "tableId": 123,
  "dimensions": [...],
  "measures": [...]
}
```

### 3. 示例代码（Java）

```java
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class AccessTokenClient {
    private String accessToken;
    private String accessSecret;
    
    public String generateSignature(long timestamp, String requestBody) {
        try {
            String signString = accessSecret + timestamp + requestBody;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                accessSecret.getBytes("UTF-8"), 
                "HmacSHA256"
            );
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(signString.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }
    
    public void sendRequest(String url, String requestBody) {
        long timestamp = System.currentTimeMillis();
        String signature = generateSignature(timestamp, requestBody);
        
        // 使用 HTTP 客户端发送请求
        // 添加请求头：
        // X-ACCESS-TOKEN: {accessToken}
        // X-TIMESTAMP: {timestamp}
        // X-SIGNATURE: {signature}
    }
}
```

### 4. 示例代码（Python）

```python
import hmac
import hashlib
import base64
import time
import requests

class AccessTokenClient:
    def __init__(self, access_token, access_secret):
        self.access_token = access_token
        self.access_secret = access_secret
    
    def generate_signature(self, timestamp, request_body):
        sign_string = self.access_secret + str(timestamp) + request_body
        hash_bytes = hmac.new(
            self.access_secret.encode('utf-8'),
            sign_string.encode('utf-8'),
            hashlib.sha256
        ).digest()
        return base64.b64encode(hash_bytes).decode('utf-8')
    
    def send_request(self, url, request_body):
        timestamp = int(time.time() * 1000)
        signature = self.generate_signature(timestamp, request_body)
        
        headers = {
            'X-ACCESS-TOKEN': self.access_token,
            'X-TIMESTAMP': str(timestamp),
            'X-SIGNATURE': signature,
            'Content-Type': 'application/json'
        }
        
        response = requests.post(url, data=request_body, headers=headers)
        return response
```

## 安全注意事项

1. **AccessSecret 保密**：AccessSecret 只在生成时返回一次，请妥善保存，不要泄露
2. **时间戳同步**：确保客户端和服务器时间同步，时间差不能超过 5 分钟
3. **HTTPS 传输**：建议在生产环境使用 HTTPS 传输，保护 AccessToken 和签名
4. **定期更换**：建议定期更换 AccessToken，提高安全性
5. **禁用不用的 Token**：不再使用的 AccessToken 应及时禁用或删除

## 错误码说明

- `DATA_IS_WRONG`: 请求头缺失、时间戳无效、AccessToken 无效或签名验证失败
- `SYSTEM_INNER_ERROR`: 系统内部错误

## 验证流程

1. Filter 拦截 `/commonData/*` 的 POST 请求
2. 检查必需的请求头（X-ACCESS-TOKEN, X-TIMESTAMP, X-SIGNATURE）
3. 验证时间戳是否在容差范围内（±5 分钟）
4. 查询 AccessToken 并检查是否有效（启用、未过期）
5. 读取请求体并验证签名
6. 更新最后使用时间
7. 继续处理请求

## 注意事项

- 请求体只能读取一次，Filter 使用 `CachedBodyHttpServletRequest` 包装请求以支持多次读取
- 签名验证失败会返回 401 状态码
- AccessToken 过期或禁用会导致验证失败

