#!/bin/bash

# DataEase 打包脚本
# 功能：
#   - 全量打包：编译和安装 SDK、前端、后端
#   - 仅后端打包：编译和安装 SDK、后端（不包括前端）
# 作者: DataEase Team
# 版本: 2.10.12
# 用法：
#   ./package_all.sh          # 全量打包
#   ./package_all.sh backend  # 仅打包后端（包括SDK和core-backend）

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 获取脚本所在目录的父目录
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# 切换到项目根目录
cd "$PROJECT_ROOT"
log_info "项目根目录: $PROJECT_ROOT"

# 检查Maven环境
check_maven() {
    log_info "检查Maven环境..."
    
    if ! command -v mvn &> /dev/null; then
        log_error "Maven未安装，请先安装Maven"
        exit 1
    fi
    
    log_success "Maven环境检查通过，版本: $(mvn -v | head -n 1)"
}

# 安装SDK模块
install_sdk() {
    log_info "=================================="
    log_info "安装SDK模块"
    log_info "=================================="
    
    cd "$PROJECT_ROOT/sdk"
    log_info "进入SDK目录: $(pwd)"
    
    log_info "开始安装SDK模块（包括api-base）..."
    mvn clean install -DskipTests
    
    if [ $? -eq 0 ]; then
        log_success "SDK模块安装成功"
        cd "$PROJECT_ROOT"
    else
        log_error "SDK模块安装失败"
        exit 1
    fi
}

# 编译和安装前端
build_frontend() {
    log_info "=================================="
    log_info "编译和安装前端"
    log_info "=================================="
    
    cd "$PROJECT_ROOT/core/core-frontend"
    log_info "进入前端目录: $(pwd)"
    
    log_info "开始编译和安装前端..."
    log_info "这将执行: npm install 和 npm run build:distributed"
    
    mvn clean install -DskipTests
    
    if [ $? -eq 0 ]; then
        log_success "前端编译和安装成功"
        cd "$PROJECT_ROOT"
    else
        log_error "前端编译和安装失败"
        exit 1
    fi
}

# 编译和安装后端
build_backend() {
    log_info "=================================="
    log_info "编译和安装后端"
    log_info "=================================="
    
    cd "$PROJECT_ROOT/core/core-backend"
    log_info "进入后端目录: $(pwd)"
    
    log_info "开始编译和安装后端..."
    
    mvn clean install -DskipTests
    
    if [ $? -eq 0 ]; then
        log_success "后端编译和安装成功"
        cd "$PROJECT_ROOT"
    else
        log_error "后端编译和安装失败"
        exit 1
    fi
}

# 仅打包后端（包括SDK和core-backend）
package_backend_only() {
    log_info "=================================="
    log_info "DataEase 后端打包脚本"
    log_info "=================================="
    log_info "开始时间: $(date '+%Y-%m-%d %H:%M:%S')"
    log_info ""
    
    # 检查Maven环境
    check_maven
    
    # 安装SDK模块
    install_sdk
    
    # 编译和安装后端
    build_backend
    
    log_info ""
    log_info "=================================="
    log_success "后端打包完成！"
    log_info "=================================="
    log_info "结束时间: $(date '+%Y-%m-%d %H:%M:%S')"
    log_info ""
    log_info "生成的文件位置："
    log_info "  后端JAR文件: $PROJECT_ROOT/core/core-backend/target/core-backend-*.jar"
    log_info ""
}

# 全量打包主函数
main() {
    log_info "=================================="
    log_info "DataEase 全量打包脚本"
    log_info "=================================="
    log_info "开始时间: $(date '+%Y-%m-%d %H:%M:%S')"
    log_info ""
    
    # 检查Maven环境
    check_maven
    
    # 安装SDK模块
    install_sdk
    
    # 编译和安装前端
    build_frontend
    
    # 编译和安装后端
    build_backend
    
    log_info ""
    log_info "=================================="
    log_success "全量打包完成！"
    log_info "=================================="
    log_info "结束时间: $(date '+%Y-%m-%d %H:%M:%S')"
    log_info ""
    log_info "生成的文件位置："
    log_info "  前端构建产物: $PROJECT_ROOT/core/core-frontend/dist"
    log_info "  后端JAR文件: $PROJECT_ROOT/core/core-backend/target/core-backend-*.jar"
    log_info ""
}

# 显示帮助信息
show_help() {
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  (无参数)    全量打包（SDK + 前端 + 后端）"
    echo "  backend     仅打包后端（SDK + core-backend）"
    echo "  help        显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0              # 全量打包"
    echo "  $0 backend      # 仅打包后端"
}

# 执行主函数
if [ "$1" = "backend" ]; then
    package_backend_only "$@"
elif [ "$1" = "help" ] || [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    show_help
else
    main "$@"
fi

