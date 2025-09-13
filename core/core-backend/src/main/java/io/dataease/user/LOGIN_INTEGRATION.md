# DataEase 登录功能集成说明

## 概述

本文档说明了如何将新创建的用户管理服务集成到 DataEase 的登录系统中，实现统一的用户认证和管理。

## 修改内容

### 1. 登录服务器修改

**文件**: `core/core-backend/src/main/java/io/dataease/substitute/permissions/login/SubstituleLoginServer.java`

#### 主要变更：
- ✅ 集成了 `UserManage` 用户管理服务
- ✅ 支持 admin 用户和普通用户登录
- ✅ 添加了用户状态检查（启用/禁用）
- ✅ 实现了客户端IP获取和记录
- ✅ 更新用户最后登录信息

#### 登录流程：
1. **解密登录参数**：使用 RSA 解密用户名和密码
2. **用户类型判断**：
   - 如果是 `admin` 用户：使用配置文件中的密码验证
   - 如果是普通用户：调用 `UserManage.authenticateUser()` 验证
3. **用户状态检查**：验证用户是否启用
4. **生成Token**：创建 JWT Token 并返回
5. **记录登录信息**：更新最后登录时间和IP

### 2. 用户管理服务增强

**文件**: `core/core-backend/src/main/java/io/dataease/user/manage/UserManage.java`

#### 新增方法：
- ✅ `authenticateUser(String account, String password)` - 用户认证
- ✅ 改进了密码加密和验证方法
- ✅ 使用 MD5 加密（生产环境建议使用 BCrypt）

### 3. 数据库初始化

**文件**: `core/core-backend/src/main/resources/db/migration/V2.10.12__user_management_ddl.sql`

#### 测试用户：
- **admin** / `DataEase@123456` - 系统管理员
- **test1** / `123456` - 测试用户1
- **test2** / `123456` - 测试用户2  
- **disabled** / `123456` - 禁用用户（用于测试禁用状态）

## 使用方法

### 1. 数据库初始化

执行数据库初始化脚本：
```sql
-- 位置：core/core-backend/src/main/resources/db/migration/V2.10.12__user_management_ddl.sql
```

### 2. 启动应用

启动 DataEase 应用后，登录功能将自动使用新的用户管理服务。

### 3. 测试登录

#### 使用 admin 账号登录：
- 用户名：`admin`
- 密码：`DataEase@123456`

#### 使用普通用户登录：
- 用户名：`test1` 或 `test2`
- 密码：`123456`

#### 测试禁用用户：
- 用户名：`disabled`
- 密码：`123456`
- 预期结果：登录失败，提示"用户已被禁用"

### 4. 前端登录

前端登录页面无需修改，继续使用现有的登录接口：
```javascript
// 前端登录API调用
const param = { 
  name: rsaEncryp(username), 
  pwd: rsaEncryp(password) 
}
loginApi(param)
```

## 功能特性

### ✅ 支持的登录方式
- **Admin 用户**：使用配置文件密码
- **普通用户**：使用数据库中的用户账号密码
- **RSA 加密**：前端传输的密码使用 RSA 加密
- **MD5 验证**：后端使用 MD5 验证密码

### ✅ 安全特性
- **密码加密**：数据库中的密码使用 MD5 加密存储
- **用户状态检查**：禁用用户无法登录
- **登录记录**：记录最后登录时间和IP地址
- **客户端IP获取**：支持多种代理环境下的真实IP获取

### ✅ 错误处理
- **用户不存在**：提示用户名或密码错误
- **密码错误**：提示用户名或密码错误
- **用户禁用**：提示用户已被禁用
- **参数验证**：检查用户名和密码是否为空

## 配置说明

### 1. Admin 用户配置

Admin 用户的密码在 `SubstituleLoginConfig` 中配置：
```java
// 默认密码：DataEase@123456
// 可通过配置文件修改
```

### 2. 数据库配置

确保数据库连接配置正确，用户表已创建并包含测试数据。

### 3. MyBatis 配置

确保 MyBatis 扫描路径包含用户相关的 Mapper：
```yaml
mybatis:
  mapper-locations: classpath:mybatis/*.xml
  type-aliases-package: io.dataease.user.entity
```

## 扩展功能

### 1. 添加新用户

可以通过用户管理接口创建新用户：
```bash
POST /user/create
{
  "username": "新用户",
  "account": "newuser",
  "password": "123456",
  "email": "newuser@example.com",
  "orgId": 1,
  "roleIds": [2]
}
```

### 2. 修改用户状态

可以通过用户管理接口启用/禁用用户：
```bash
POST /user/enable
{
  "id": 2,
  "enable": false
}
```

### 3. 重置用户密码

可以通过用户管理接口重置用户密码：
```bash
POST /user/resetPwd/2
```

## 故障排除

### 1. 登录失败
- 检查用户名和密码是否正确
- 检查用户是否被禁用
- 查看应用日志中的错误信息

### 2. 数据库连接问题
- 检查数据库连接配置
- 确认用户表已创建
- 验证测试数据是否存在

### 3. 密码验证问题
- 确认密码加密方式一致
- 检查 MD5 工具类是否可用
- 验证数据库中的密码格式

### 4. Token 生成问题
- 检查 JWT 相关依赖
- 确认 Token 生成算法正确
- 验证 Token 过期时间设置

## 安全建议

### 1. 生产环境
- 使用 BCrypt 替代 MD5 进行密码加密
- 启用 HTTPS 传输
- 设置强密码策略
- 定期更新用户密码

### 2. 监控和日志
- 记录所有登录尝试
- 监控异常登录行为
- 设置登录失败次数限制
- 实现账户锁定机制

## 版本信息

- 基于 DataEase v2.10.12
- 修改时间：2024年
- 兼容性：与现有 DataEase 功能完全兼容
- 测试状态：已通过基本功能测试
