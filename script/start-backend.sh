#!/bin/bash

# DataEase 后端开发环境启动脚本
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

# 检查Java环境
check_java() {
    log_info "检查Java环境..."
    
    if ! command -v java &> /dev/null; then
        log_error "Java未安装，请先安装Java 11或更高版本"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 11 ]; then
        log_error "Java版本过低，需要Java 11或更高版本，当前版本: $JAVA_VERSION"
        exit 1
    fi
    
    log_success "Java环境检查通过，版本: $(java -version 2>&1 | head -n 1)"
}

# 检查Maven环境
check_maven() {
    log_info "检查Maven环境..."
    
    if ! command -v mvn &> /dev/null; then
        log_error "Maven未安装，请先安装Maven"
        exit 1
    fi
    
    log_success "Maven环境检查通过，版本: $(mvn -version | head -n 1)"
}

# 清理和编译
clean_and_compile() {
    log_info "清理并编译项目..."
    
    # 获取脚本所在目录的父目录
    SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
    PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
    
    cd "$PROJECT_ROOT"
    
    # 清理target目录
    log_info "清理target目录..."
    find . -name "target" -type d -exec rm -rf {} + 2>/dev/null || true
    
    # 编译项目
    log_info "开始编译SDK"
    log_info "$(pwd)"
    if($(pwd) indexof "core/core-backend"); then
        cd "../../"
    fi
    mvn clean install -DskipTests

    cd "./core/core-backend"
    log_info "$(pwd)"

    log_info "开始编译项目..."
    mvn clean compile -DskipTests -T 4
    
    if [ $? -eq 0 ]; then
        log_success "项目编译成功"
    else
        log_error "项目编译失败"
        exit 1
    fi
}

# 启动后端服务
start_backend() {
    log_info "启动DataEase后端服务..."
    
    # 获取脚本所在目录的父目录
    log_info "$(pwd)"
    if [!$(pwd) indexof "core/core-backend"]; then
        log_info "$(pwd)"
        SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
        PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
        BACKEND_DIR="$PROJECT_ROOT/core/core-backend"
        if [ ! -d "$BACKEND_DIR" ]; then
          log_error "后端目录不存在: $BACKEND_DIR"
          exit 1
        fi
    fi
        
    # 检查配置文件
    if [ ! -f "src/main/resources/application.yml" ]; then
        log_warning "未找到application.yml配置文件，使用默认配置"
    fi
    
    # 设置JVM参数
    export JAVA_OPTS="-Xms512m -Xmx2048m -XX:+UseG1GC -XX:+UseStringDeduplication"
    
    # 启动应用
    log_info "启动参数: $JAVA_OPTS"
    log_info "工作目录: $(pwd)"
    
    mvn spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.jvmArguments="$JAVA_OPTS" &
    
    BACKEND_PID=$!
    echo $BACKEND_PID > ../backend.pid
    
    log_success "后端服务启动中，PID: $BACKEND_PID"
    log_info "服务地址: http://localhost:8081"
    log_info "API文档: http://localhost:8081/doc.html"
    
    # 等待服务启动
    log_info "等待服务启动..."
    sleep 10
    
    # 检查服务是否启动成功
    if curl -s http://localhost:8081/actuator/health > /dev/null; then
        log_success "后端服务启动成功！"
    else
        log_warning "服务可能还在启动中，请稍等..."
    fi
}

# 显示帮助信息
show_help() {
    echo "DataEase 后端开发环境启动脚本"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -h, --help     显示帮助信息"
    echo "  -c, --clean    清理并重新编译"
    echo "  -s, --skip     跳过环境检查"
    echo "  -d, --debug    启用调试模式"
    echo ""
    echo "示例:"
    echo "  $0              # 正常启动"
    echo "  $0 -c           # 清理编译后启动"
    echo "  $0 -s           # 跳过环境检查启动"
    echo "  $0 -d           # 调试模式启动"
}

# 主函数
main() {
    local CLEAN=false
    local SKIP_CHECK=false
    local DEBUG=false
    
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
            -d|--debug)
                DEBUG=true
                shift
                ;;
            *)
                log_error "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    log_info "DataEase 后端开发环境启动脚本"
    log_info "=================================="
    
    # 环境检查
    if [ "$SKIP_CHECK" = false ]; then
        check_java
        check_maven
    else
        log_warning "跳过环境检查"
    fi
    
    # 清理编译
    if [ "$CLEAN" = true ]; then
        clean_and_compile
    fi
    
    # 设置调试模式
    if [ "$DEBUG" = true ]; then
        export JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
        log_info "调试模式已启用，端口: 5005"
    fi
    
    # 启动服务
    start_backend
    
    log_info "=================================="
    log_success "后端服务启动完成！"
    log_info "按 Ctrl+C 停止服务"
    
    # 等待用户中断
    wait $BACKEND_PID
}

# 捕获中断信号
trap 'log_info "正在停止服务..."; kill $BACKEND_PID 2>/dev/null; rm -f ../backend.pid; log_success "服务已停止"; exit 0' INT TERM

# 执行主函数
main "$@"
