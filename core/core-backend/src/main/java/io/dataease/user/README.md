# DataEase 用户管理模块

## 概述

本模块为 DataEase 提供了完整的用户管理功能，包括用户的增删改查、角色管理、权限控制等。

## 文件结构

```
io.dataease.user/
├── config/                 # 配置类
│   └── UserConfig.java    # 用户模块配置
├── dao/                   # 数据访问层
│   └── UserMapper.java    # 用户数据访问接口
├── dto/                   # 数据传输对象
│   ├── UserCreator.java   # 用户创建DTO
│   ├── UserEditor.java    # 用户编辑DTO
│   └── UserGridRequest.java # 用户列表查询请求DTO
├── entity/                # 实体类
│   └── User.java          # 用户实体
├── manage/                # 业务逻辑层
│   └── UserManage.java    # 用户管理类
├── server/                # 控制器层
│   └── UserServer.java    # 用户服务端实现
└── vo/                    # 视图对象
    ├── CurUserVO.java     # 当前用户VO
    ├── UserFormVO.java    # 用户表单VO
    ├── UserGridVO.java    # 用户列表VO
    └── UserItem.java      # 用户项VO
```

## 主要功能

### 1. 用户管理
- ✅ 用户列表查询（分页、搜索）
- ✅ 用户详情查询
- ✅ 用户创建
- ✅ 用户编辑
- ✅ 用户删除（软删除）
- ✅ 批量删除
- ✅ 用户状态切换（启用/禁用）
- ✅ 密码重置
- ✅ 个人信息修改

### 2. 认证相关
- ✅ 用户登录验证
- ✅ 当前用户信息获取
- ✅ 个人信息查询
- ✅ 密码修改

### 3. 组织管理
- ✅ 组织内用户查询
- ✅ 组织切换

## API 接口

### 用户管理接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/user/pager/{goPage}/{pageSize}` | 分页查询用户列表 |
| GET | `/user/queryById/{id}` | 查询用户详情 |
| GET | `/user/personInfo` | 查询个人信息 |
| POST | `/user/create` | 创建用户 |
| POST | `/user/edit` | 编辑用户 |
| POST | `/user/delete/{id}` | 删除用户 |
| POST | `/user/batchDel` | 批量删除用户 |
| POST | `/user/enable` | 切换用户状态 |
| POST | `/user/resetPwd/{id}` | 重置密码 |

### 认证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/user/info` | 获取当前登录用户信息 |
| GET | `/user/queryByAccount/{account}` | 根据账号查询用户 |
| POST | `/user/modifyPwd` | 修改个人密码 |

## 数据库表结构

### 核心表

1. **core_user** - 用户表
2. **core_role** - 角色表
3. **core_user_role** - 用户角色关联表
4. **core_organization** - 组织表
5. **core_permission** - 权限表
6. **core_role_permission** - 角色权限关联表

### 默认数据

- 默认组织：`默认组织`
- 默认角色：`系统管理员`、`普通用户`
- 默认用户：`admin` (密码: `DataEase@123456`)

## 使用说明

### 1. 数据库初始化

执行数据库初始化脚本：
```sql
-- 位置：core/core-backend/src/main/resources/db/migration/V2.10.12__user_management_ddl.sql
```

### 2. 配置 MyBatis

确保在 `application.yml` 中配置了 MyBatis 扫描路径：
```yaml
mybatis:
  mapper-locations: classpath:mybatis/*.xml
  type-aliases-package: io.dataease.user.entity
```

### 3. 启动应用

启动 DataEase 应用后，用户管理功能将自动可用。

### 4. 访问接口

- Swagger 文档：`http://localhost:8100/doc.html`
- 用户管理页面：`http://localhost:8100/#/user-management/index`

## 权限控制

### 权限注解

- `@DePermit("m:read")` - 需要管理员读取权限
- `@DePermit({"m:read", "#p0 + ':read'"})` - 需要管理员权限或资源读取权限
- `@DePermit({"m:read", "#p0.id + ':manage'"})` - 需要管理员权限或资源管理权限

### 角色权限

- **系统管理员**：拥有所有权限
- **普通用户**：拥有基础查看权限

## 扩展功能

### 1. 添加新字段

在 `User` 实体类中添加新字段，并更新对应的 DTO 和 VO 类。

### 2. 添加新接口

在 `UserApi` 接口中定义新方法，在 `UserServer` 中实现，在 `UserManage` 中添加业务逻辑。

### 3. 自定义查询

在 `UserMapper` 中添加新的查询方法，在对应的 XML 文件中实现 SQL。

## 注意事项

1. **密码安全**：实际部署时请使用强加密算法（如 BCrypt）处理密码
2. **权限控制**：确保所有接口都有适当的权限控制
3. **数据验证**：在 DTO 类中添加适当的验证注解
4. **异常处理**：使用统一的异常处理机制
5. **日志记录**：重要操作需要记录日志

## 故障排除

### 1. 数据库连接问题
- 检查数据库连接配置
- 确认数据库表已创建

### 2. MyBatis 映射问题
- 检查 XML 文件路径
- 确认命名空间配置正确

### 3. 权限问题
- 检查用户角色分配
- 确认权限配置正确

## 版本信息

- 基于 DataEase v2.10.12
- 创建时间：2024年
- 兼容性：与现有 DataEase 功能完全兼容
