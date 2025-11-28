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

### 5. package_all.sh - 项目打包脚本

打包DataEase项目（SDK、前端、后端）。

**功能特性：**
- 全量打包：编译和安装 SDK、前端、后端
- 仅后端打包：编译和安装 SDK、后端（不包括前端）
- 自动检查Maven环境
- 详细的日志输出

**使用方法：**
```bash
# 全量打包（SDK + 前端 + 后端）
./script/package_all.sh

# 仅打包后端（SDK + core-backend）
./script/package_all.sh backend

# 显示帮助
./script/package_all.sh help
```

### 6. build_docker.sh - Docker 镜像构建脚本

构建DataEase Docker镜像。

**功能特性：**
- 自动检查必要文件
- 支持跳过项目打包
- 支持指定镜像名称和标签
- 支持推送到镜像仓库
- 详细的构建日志

**使用方法：**
```bash
# 使用默认配置构建镜像
./script/build_docker.sh

# 指定标签
./script/build_docker.sh --tag v2.10.12

# 构建 x64 架构镜像
./script/build_docker.sh --platform x64

# 构建 arm 架构镜像
./script/build_docker.sh --platform arm

# 跳过项目打包，直接构建镜像
./script/build_docker.sh --skip-build

# 构建并推送到仓库
./script/build_docker.sh --registry registry.example.com/dataease --push

# 完整示例（x64 架构）
./script/build_docker.sh --name dataease --tag v2.10.12 --platform x64 --registry registry.example.com/dataease --push

# 显示帮助
./script/build_docker.sh --help
```

### 7. build_and_push_docker.sh - Docker 镜像构建并推送脚本

构建DataEase Docker镜像并自动推送到华为云SWR仓库。

**功能特性：**
- 自动使用当前 git branch 名称作为标签
- 自动推送到华为云 SWR 仓库
- 支持跳过项目打包
- 自动处理特殊字符
- 详细的构建和推送日志

**使用方法：**
```bash
# 使用默认配置构建并推送（使用当前 git branch 作为标签）
./script/build_and_push_docker.sh

# 构建 x64 架构镜像并推送
./script/build_and_push_docker.sh --platform x64

# 构建 arm 架构镜像并推送
./script/build_and_push_docker.sh --platform arm

# 跳过项目打包，直接构建并推送
./script/build_and_push_docker.sh --skip-build

# 指定镜像名称
./script/build_and_push_docker.sh --name dataease

# 完整示例（x64 架构）
./script/build_and_push_docker.sh --name dataease --platform x64

# 显示帮助
./script/build_and_push_docker.sh --help
```

**说明：**
- 镜像将推送到：`swr.cn-southwest-2.myhuaweicloud.com/fangcang`
- 标签将使用当前 git branch 名称（特殊字符会被替换为下划线）
- 支持指定平台：`x64` (linux/amd64) 或 `arm` (linux/arm64)
- 指定平台时会使用 Docker buildx 构建跨平台镜像
- 首次使用需要登录华为云 SWR：`docker login swr.cn-southwest-2.myhuaweicloud.com`

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
