# DataEase 开发环境启动脚本

本目录包含了DataEase项目的开发环境启动脚本，帮助开发者快速启动和停止开发环境。

## 脚本说明

### 1. start-backend.sh - 后端启动脚本

启动DataEase后端服务（Spring Boot应用）。

**功能特性：**
- 自动检查Java和Maven环境
- 检查数据库连接状态
- 支持清理编译
- 支持调试模式
- 自动设置JVM参数

**使用方法：**
```bash
# 正常启动
./script/start-backend.sh

# 清理编译后启动
./script/start-backend.sh -c

# 跳过环境检查启动
./script/start-backend.sh -s

# 调试模式启动
./script/start-backend.sh -d

# 显示帮助
./script/start-backend.sh -h
```

**服务地址：**
- 后端API: http://localhost:8081
- API文档: http://localhost:8081/doc.html
- 健康检查: http://localhost:8081/actuator/health

### 2. start-frontend.sh - 前端启动脚本

启动DataEase前端服务（Vite开发服务器）。

**功能特性：**
- 自动检查Node.js和npm环境
- 支持pnpm包管理器
- 自动安装依赖
- 支持清理构建文件
- 支持自定义端口

**使用方法：**
```bash
# 正常启动
./script/start-frontend.sh

# 清理构建文件后启动
./script/start-frontend.sh -c

# 跳过环境检查启动
./script/start-frontend.sh -s

# 重新安装依赖后启动
./script/start-frontend.sh -i

# 指定端口启动
./script/start-frontend.sh -p 3000

# 显示帮助
./script/start-frontend.sh -h
```

**服务地址：**
- 前端应用: http://localhost:8080
- 管理后台: http://localhost:8080
- 移动端: http://localhost:8080/mobile.html

### 3. start-all.sh - 完整环境启动脚本

同时启动前端和后端服务。

**功能特性：**
- 一键启动完整开发环境
- 支持后台运行
- 支持单独启动前端或后端
- 自动等待服务启动

**使用方法：**
```bash
# 启动完整环境
./script/start-all.sh

# 仅启动后端
./script/start-all.sh -b

# 仅启动前端
./script/start-all.sh -f

# 清理后启动
./script/start-all.sh -c

# 后台运行
./script/start-all.sh -d

# 显示帮助
./script/start-all.sh -h
```

### 4. stop-all.sh - 停止服务脚本

停止所有DataEase相关服务。

**功能特性：**
- 优雅停止服务
- 清理残留进程
- 支持强制停止
- 清理PID文件

**使用方法：**
```bash
# 正常停止服务
./script/stop-all.sh

# 强制停止所有相关进程
./script/stop-all.sh -f

# 显示帮助
./script/stop-all.sh -h
```

## 环境要求

### 后端环境
- **Java**: 11或更高版本
- **Maven**: 3.6或更高版本
- **MySQL**: 8.0或更高版本
- **内存**: 至少2GB可用内存

### 前端环境
- **Node.js**: 16或更高版本
- **npm**: 8或更高版本
- **pnpm**: 推荐使用（可选）

## 快速开始

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd dataease
   ```

2. **设置脚本权限**
   ```bash
   chmod +x script/*.sh
   ```

3. **启动完整开发环境**
   ```bash
   ./script/start-all.sh
   ```

4. **访问应用**
   - 前端: http://localhost:8080
   - 后端API: http://localhost:8081
   - API文档: http://localhost:8081/doc.html

5. **停止服务**
   ```bash
   ./script/stop-all.sh
   ```

## 常见问题

### 1. 端口被占用
如果8080或8081端口被占用，可以：
- 停止占用端口的进程
- 修改配置文件中的端口设置
- 使用前端脚本的`-p`参数指定其他端口

### 2. 数据库连接失败
确保MySQL服务正在运行：
```bash
# macOS
brew services start mysql

# Linux
sudo systemctl start mysql

# Docker
docker run -d --name mysql -e MYSQL_ROOT_PASSWORD=root -p 3306:3306 mysql:8.0
```

### 3. 依赖安装失败
尝试清理并重新安装：
```bash
# 前端依赖
./script/start-frontend.sh -i

# 后端依赖
./script/start-backend.sh -c
```

### 4. 服务启动失败
检查日志文件：
```bash
# 查看后端日志
tail -f logs/backend.log

# 查看前端日志
tail -f logs/frontend.log
```

## 开发建议

1. **使用后台模式**：开发时可以使用`-d`参数后台运行服务
2. **定期清理**：使用`-c`参数定期清理构建文件
3. **监控日志**：关注控制台输出和日志文件
4. **环境隔离**：建议使用Docker或虚拟环境隔离开发环境

## 脚本维护

如需修改脚本，请注意：
1. 保持脚本的可移植性
2. 添加适当的错误处理
3. 更新文档说明
4. 测试各种使用场景

## 贡献

欢迎提交Issue和Pull Request来改进这些脚本！
