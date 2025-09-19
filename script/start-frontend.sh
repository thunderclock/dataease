#!/bin/bash

# DataEase 前端开发环境启动脚本
# 作者: DataEase Team
# 版本: 2.10.12

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

# 检查Node.js环境
check_node() {
    log_info "检查Node.js环境..."
    
    if ! command -v node &> /dev/null; then
        log_error "Node.js未安装，请先安装Node.js 16或更高版本"
        log_info "推荐使用nvm安装:"
        echo "  curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash"
        echo "  nvm install 18"
        echo "  nvm use 18"
        exit 1
    fi
    
    NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
    if [ "$NODE_VERSION" -lt 16 ]; then
        log_error "Node.js版本过低，需要Node.js 16或更高版本，当前版本: $(node -v)"
        exit 1
    fi
    
    log_success "Node.js环境检查通过，版本: $(node -v)"
}

# 检查npm环境
check_npm() {
    log_info "检查npm环境..."
    
    if ! command -v npm &> /dev/null; then
        log_error "npm未安装，请先安装npm"
        exit 1
    fi
    
    log_success "npm环境检查通过，版本: $(npm -v)"
}

# 检查pnpm环境
check_pnpm() {
    log_info "检查pnpm环境..."
    
    if ! command -v pnpm &> /dev/null; then
        log_warning "pnpm未安装，将使用npm作为包管理器"
        log_info "推荐安装pnpm以获得更好的性能:"
        echo "  npm install -g pnpm"
        return 1
    fi
    
    log_success "pnpm环境检查通过，版本: $(pnpm -v)"
    return 0
}

# 安装依赖
install_dependencies() {
    log_info "检查并安装依赖..."
    
    # 获取脚本所在目录的父目录
    SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
    PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
    FRONTEND_DIR="$PROJECT_ROOT/core/core-frontend"
    
    if [ ! -d "$FRONTEND_DIR" ]; then
        log_error "前端目录不存在: $FRONTEND_DIR"
        exit 1
    fi

    cd "$FRONTEND_DIR"
        
    # 检查package-lock.json或pnpm-lock.yaml
    if [ -f "pnpm-lock.yaml" ]; then
        PACKAGE_MANAGER="pnpm"
    elif [ -f "package-lock.json" ]; then
        PACKAGE_MANAGER="npm"
    else
        log_info "未找到锁文件，将使用npm安装依赖"
        PACKAGE_MANAGER="npm"
    fi
    
    # 检查node_modules是否存在
    if [ ! -d "node_modules" ]; then
        log_info "安装依赖包..."
        if [ "$PACKAGE_MANAGER" = "pnpm" ] && check_pnpm; then
            pnpm install
        else
            npm install
        fi
        
        if [ $? -eq 0 ]; then
            log_success "依赖安装成功"
        else
            log_error "依赖安装失败"
            exit 1
        fi
    else
        log_success "依赖已存在，跳过安装"
    fi
}

# 清理构建文件
clean_build() {
    log_info "清理构建文件..."
    
    # 获取脚本所在目录的父目录
    SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
    PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
    FRONTEND_DIR="$PROJECT_ROOT/core/core-frontend"
    
    cd "$FRONTEND_DIR"
    
    # 清理dist目录
    if [ -d "dist" ]; then
        rm -rf dist
        log_info "已清理dist目录"
    fi
    
    # 清理缓存
    if [ -d ".vite" ]; then
        rm -rf .vite
        log_info "已清理Vite缓存"
    fi
    
    log_success "构建文件清理完成"
}

# 启动前端服务
start_frontend() {
    log_info "启动DataEase前端服务..."

    log_info "当前目录: $(pwd)"

    if(!$(pwd) indexof "core/core-frontend"); then
         # 获取脚本所在目录的父目录
         SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

        PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
        FRONTEND_DIR="$PROJECT_ROOT/core/core-frontend"
    else
        FRONTEND_DIR="$(pwd)"
    fi
    
    cd "$FRONTEND_DIR"

   
    
    # 检查配置文件
    if [ ! -f "vite.config.ts" ]; then
        log_error "未找到vite.config.ts配置文件"
        exit 1
    fi
    
    # 设置环境变量
    export NODE_ENV=development
    export VITE_APP_TITLE="DataEase"
    
    # 检查后端服务是否运行
    log_info "检查后端服务连接..."
    if curl -s http://localhost:8081/actuator/health > /dev/null; then
        log_success "后端服务连接正常"
    else
        log_warning "后端服务未运行，请先启动后端服务"
        log_info "可以使用以下命令启动后端:"
        echo "  ./script/start-backend.sh"
    fi
    
    # 启动开发服务器
    log_info "启动Vite开发服务器..."
    log_info "工作目录: $(pwd)"
    
    npm install
    log_info "安装依赖完成"
    npm run dev &
    
    FRONTEND_PID=$!
    echo $FRONTEND_PID > ../frontend.pid
    
    log_success "前端服务启动中，PID: $FRONTEND_PID"
    log_info "服务地址: http://localhost:8080"
    log_info "管理后台: http://localhost:8080"
    log_info "移动端: http://localhost:8080/mobile.html"
    
    # 等待服务启动
    log_info "等待服务启动..."
    sleep 5
    
    # 检查服务是否启动成功
    if curl -s http://localhost:8080 > /dev/null; then
        log_success "前端服务启动成功！"
    else
        log_warning "服务可能还在启动中，请稍等..."
    fi
}

# 显示帮助信息
show_help() {
    echo "DataEase 前端开发环境启动脚本"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -h, --help     显示帮助信息"
    echo "  -c, --clean    清理构建文件后启动"
    echo "  -s, --skip     跳过环境检查"
    echo "  -i, --install  强制重新安装依赖"
    echo "  -p, --port     指定端口号 (默认: 8080)"
    echo ""
    echo "示例:"
    echo "  $0              # 正常启动"
    echo "  $0 -c           # 清理后启动"
    echo "  $0 -s           # 跳过环境检查启动"
    echo "  $0 -i           # 重新安装依赖后启动"
    echo "  $0 -p 3000      # 指定端口启动"
}

# 主函数
main() {
    local CLEAN=false
    local SKIP_CHECK=false
    local FORCE_INSTALL=false
    local PORT=8080
    
    # 解析命令行参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -c|--clean)
                CLEAN=true
                shift
                ;;
            -s|--skip)
                SKIP_CHECK=true
                shift
                ;;
            -i|--install)
                FORCE_INSTALL=true
                shift
                ;;
            -p|--port)
                PORT="$2"
                shift 2
                ;;
            *)
                log_error "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    log_info "DataEase 前端开发环境启动脚本"
    log_info "=================================="
    
    # 环境检查
    if [ "$SKIP_CHECK" = false ]; then
        check_node
        check_npm
    else
        log_warning "跳过环境检查"
    fi
    
    # 清理构建文件
    if [ "$CLEAN" = true ]; then
        clean_build
    fi
    
    # 强制安装依赖
    if [ "$FORCE_INSTALL" = true ]; then
        log_info "强制重新安装依赖..."
        SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
        PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
        FRONTEND_DIR="$PROJECT_ROOT/core/core-frontend"
        cd "$FRONTEND_DIR"
        rm -rf node_modules package-lock.json pnpm-lock.yaml
        install_dependencies
    else
        install_dependencies
    fi
    
    # 设置端口
    export VITE_PORT=$PORT
    
    # 启动服务
    start_frontend
    
    log_info "=================================="
    log_success "前端服务启动完成！"
    log_info "按 Ctrl+C 停止服务"
    
    # 等待用户中断
    wait $FRONTEND_PID
}

# 捕获中断信号
trap 'log_info "正在停止服务..."; kill $FRONTEND_PID 2>/dev/null; rm -f ../frontend.pid; log_success "服务已停止"; exit 0' INT TERM

# 执行主函数
main "$@"
