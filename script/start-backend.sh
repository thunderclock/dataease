#!/bin/bash

# DataEase 后端服务启动脚本
# 功能：启动已打包好的后端服务
# 注意：使用前请先运行 script/package_all.sh 进行编译和打包
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

# 检查并关闭占用端口的进程
kill_port_process() {
    local PID=$1
    local PORT=$2
    
    if [ -z "$PID" ] || [ "$PID" = "-" ]; then
        return 0
    fi
    
    # 检查进程是否存在
    if ! ps -p "$PID" > /dev/null 2>&1; then
        return 0
    fi
    
    log_info "正在关闭进程 $PID..."
    if kill "$PID" 2>/dev/null; then
        # 等待进程退出
        local count=0
        while ps -p "$PID" > /dev/null 2>&1 && [ $count -lt 10 ]; do
            sleep 1
            count=$((count + 1))
        done
        
        if ps -p "$PID" > /dev/null 2>&1; then
            log_warning "进程未正常退出，尝试强制关闭..."
            kill -9 "$PID" 2>/dev/null
            sleep 1
        fi
        
        if ! ps -p "$PID" > /dev/null 2>&1; then
            log_success "进程 $PID 已关闭"
            return 0
        else
            log_error "无法关闭进程 $PID，请手动关闭"
            return 1
        fi
    else
        log_error "无法关闭进程 $PID，请检查权限或手动关闭"
        return 1
    fi
}

# 检查端口是否被占用
check_port() {
    local PORT=${1:-8100}
    log_info "检查端口 $PORT 是否被占用..."
    
    # 先检查是否有 PID 文件
    SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
    PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
    PID_FILE="$PROJECT_ROOT/backend.pid"
    
    if [ -f "$PID_FILE" ]; then
        local OLD_PID=$(cat "$PID_FILE" 2>/dev/null | tr -d '[:space:]')
        if [ -n "$OLD_PID" ] && ps -p "$OLD_PID" > /dev/null 2>&1; then
            local PROCESS_INFO=$(ps -p "$OLD_PID" -o comm=,args= 2>/dev/null | head -n 1)
            log_warning "发现之前启动的后端进程仍在运行"
            log_warning "进程ID: $OLD_PID"
            log_warning "进程信息: $PROCESS_INFO"
            
            # 如果是交互式终端，询问用户是否关闭
            if [ -t 0 ]; then
                echo ""
                read -p "是否关闭该进程? (y/n): " -n 1 -r
                echo ""
                if [[ $REPLY =~ ^[Yy]$ ]]; then
                    if kill_port_process "$OLD_PID" "$PORT"; then
                        rm -f "$PID_FILE"
                    else
                        exit 1
                    fi
                else
                    log_error "用户取消操作，退出"
                    exit 1
                fi
            else
                # 非交互式终端，直接尝试关闭
                log_info "非交互式终端，尝试关闭进程 $OLD_PID..."
                if kill_port_process "$OLD_PID" "$PORT"; then
                    rm -f "$PID_FILE"
                else
                    log_error "无法关闭进程，请手动关闭: kill $OLD_PID"
                    exit 1
                fi
            fi
        else
            # PID 文件存在但进程不存在，删除过期的 PID 文件
            rm -f "$PID_FILE"
            log_info "清理过期的 PID 文件"
        fi
    fi
    
    # 检查端口是否被占用
    local PID=""
    if command -v lsof &> /dev/null; then
        # macOS/Linux 使用 lsof
        PID=$(lsof -ti:$PORT 2>/dev/null | head -n 1)
    elif command -v netstat &> /dev/null; then
        # 使用 netstat (Linux)
        PID=$(netstat -tlnp 2>/dev/null | grep ":$PORT " | awk '{print $7}' | cut -d'/' -f1 | head -n 1)
    elif command -v ss &> /dev/null; then
        # 使用 ss (Linux)
        PID=$(ss -tlnp 2>/dev/null | grep ":$PORT " | awk '{print $6}' | cut -d',' -f2 | cut -d'=' -f2 | head -n 1)
    fi
    
    if [ -n "$PID" ] && [ "$PID" != "-" ]; then
        # 检查进程是否存在
        if ps -p "$PID" > /dev/null 2>&1; then
            local PROCESS_INFO=$(ps -p "$PID" -o comm=,args= 2>/dev/null | head -n 1)
            log_warning "端口 $PORT 已被进程占用"
            log_warning "进程ID: $PID"
            log_warning "进程信息: $PROCESS_INFO"
            
            # 如果是交互式终端，询问用户是否关闭
            if [ -t 0 ]; then
                echo ""
                read -p "是否关闭占用端口的进程? (y/n): " -n 1 -r
                echo ""
                if [[ $REPLY =~ ^[Yy]$ ]]; then
                    if kill_port_process "$PID" "$PORT"; then
                        log_success "端口 $PORT 已释放"
                    else
                        exit 1
                    fi
                else
                    log_error "用户取消操作，退出"
                    exit 1
                fi
            else
                # 非交互式终端，直接提示
                log_error "端口 $PORT 被占用，请先关闭占用端口的进程"
                log_error "可以使用以下命令关闭: kill $PID"
                exit 1
            fi
        else
            log_success "端口 $PORT 未被占用"
        fi
    else
        log_success "端口 $PORT 未被占用"
    fi
}

# 检查JAR文件是否存在
check_jar() {
    # 获取脚本所在目录的父目录
    SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
    PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
    BACKEND_DIR="$PROJECT_ROOT/core/core-backend"
    JAR_FILE="$BACKEND_DIR/target/CoreApplication.jar"
    
    # 检查可执行JAR文件是否存在（Spring Boot打包后的可执行JAR）
    if [ ! -f "$JAR_FILE" ]; then
        log_error "未找到后端可执行JAR文件: $JAR_FILE" >&2
        log_error "请先运行以下命令进行编译和打包：" >&2
        log_error "  ./script/package_all.sh" >&2
        exit 1
    fi
    
    # 只输出JAR文件路径到stdout，日志输出到stderr
    echo "$JAR_FILE"
}

# 启动后端服务
start_backend() {
    log_info "启动DataEase后端服务..."
    
    # 获取脚本所在目录的父目录
        SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
        PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
        BACKEND_DIR="$PROJECT_ROOT/core/core-backend"
    
    # 检查JAR文件（日志输出到stderr，路径输出到stdout）
    log_info "检查后端JAR文件..."
    JAR_FILE=$(check_jar)
    log_success "找到JAR文件: $JAR_FILE"
    
    # 切换到后端目录
    cd "$BACKEND_DIR"
    log_info "工作目录: $(pwd)"
        
    # 检查配置文件
    if [ ! -f "src/main/resources/application.yml" ]; then
        log_warning "未找到application.yml配置文件，使用默认配置"
    fi
    
    # 设置JVM参数（如果未设置）
    if [ -z "$JAVA_OPTS" ]; then
    export JAVA_OPTS="-Xms512m -Xmx2048m -XX:+UseG1GC -XX:+UseStringDeduplication"
    fi
    
    # 构建Spring Boot启动参数
    SPRING_ARGS="--spring.profiles.active=local"
    
    # 如果指定了MySQL端口，则覆盖数据库URL
    if [ -n "$MYSQL_PORT" ]; then
        # 从配置文件中读取默认的数据库URL，然后替换端口
        # 默认格式: jdbc:mysql://127.0.0.1:3306/dataease?...
        # 替换为: jdbc:mysql://127.0.0.1:${MYSQL_PORT}/dataease?...
        DEFAULT_DB_URL="jdbc:mysql://127.0.0.1:3306/dataease?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
        NEW_DB_URL=$(echo "$DEFAULT_DB_URL" | sed "s|:3306/|:${MYSQL_PORT}/|")
        SPRING_ARGS="$SPRING_ARGS --spring.datasource.url=$NEW_DB_URL"
        log_info "MySQL端口已设置为: $MYSQL_PORT"
    fi
    
    # 设置缓存目录路径（可通过环境变量 DATAEASE_CACHE_DIR 或命令行参数配置，默认为 $BACKEND_DIR/cache）
    if [ -n "$DATAEASE_CACHE_DIR" ]; then
        CACHE_DIR="$DATAEASE_CACHE_DIR"
    else
        CACHE_DIR="$BACKEND_DIR/cache"
    fi
    
    # 转换为绝对路径（如果是相对路径）
    if [[ ! "$CACHE_DIR" = /* ]]; then
        # 如果是相对路径，基于当前工作目录（$BACKEND_DIR）解析
        if [[ "$CACHE_DIR" == "$BACKEND_DIR/cache" ]]; then
            # 已经是基于 BACKEND_DIR 的路径，直接使用
            :
        else
            # 其他相对路径，转换为绝对路径
            CACHE_DIR="$(cd "$(dirname "$CACHE_DIR")" 2>/dev/null && pwd)/$(basename "$CACHE_DIR")" || CACHE_DIR="$BACKEND_DIR/$CACHE_DIR"
        fi
    fi
    
    # 确保缓存目录存在且可写
    if [ ! -d "$CACHE_DIR" ]; then
        mkdir -p "$CACHE_DIR"
        log_info "创建缓存目录: $CACHE_DIR"
    fi
    
    # 检查目录是否可写
    if [ ! -w "$CACHE_DIR" ]; then
        log_error "缓存目录不可写: $CACHE_DIR"
        log_error "请检查目录权限或使用环境变量 DATAEASE_CACHE_DIR 指定其他目录"
        exit 1
    fi
    
    # 将缓存路径添加到JVM参数中（通过系统属性传递，确保在ApplicationContextInitializer之前可用）
    export JAVA_OPTS="$JAVA_OPTS -Ddataease.path.ehcache=$CACHE_DIR"
    
    # 启动应用
    log_info "启动参数: $JAVA_OPTS"
    log_info "JAR文件: $JAR_FILE"
    log_info "缓存目录: $CACHE_DIR"
    if [ -n "$MYSQL_PORT" ]; then
        log_info "MySQL端口: $MYSQL_PORT"
    fi
    
    # 使用java -jar启动（eval确保JAVA_OPTS中的多个参数正确展开）
    eval "java $JAVA_OPTS -jar \"$JAR_FILE\" $SPRING_ARGS" &
    
    BACKEND_PID=$!
    PID_FILE="$PROJECT_ROOT/backend.pid"
    echo $BACKEND_PID > "$PID_FILE"
    
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
    echo "DataEase 后端服务启动脚本"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "注意: 使用前请先运行 ./script/package_all.sh 进行编译和打包"
    echo ""
    echo "选项:"
    echo "  -h, --help         显示帮助信息"
    echo "  -s, --skip         跳过环境检查"
    echo "  -d, --debug        启用调试模式"
    echo "  -c, --cache        指定缓存目录路径"
    echo "  -p, --mysql-port   指定MySQL端口（默认: 3306）"
    echo ""
    echo "环境变量:"
    echo "  DATAEASE_CACHE_DIR  指定缓存目录路径（默认: core/core-backend/cache）"
    echo "  MYSQL_PORT          指定MySQL端口（默认: 3306）"
    echo ""
    echo "示例:"
    echo "  $0                              # 正常启动（使用默认缓存目录和MySQL端口）"
    echo "  $0 -s                           # 跳过环境检查启动"
    echo "  $0 -d                           # 调试模式启动"
    echo "  $0 -c /tmp/dataease-cache       # 使用指定缓存目录"
    echo "  $0 -p 13306                     # 使用MySQL端口13306"
    echo "  $0 -p 13306 -c /tmp/cache       # 同时指定MySQL端口和缓存目录"
    echo "  DATAEASE_CACHE_DIR=/tmp/cache $0 # 通过环境变量指定缓存目录"
    echo "  MYSQL_PORT=13306 $0             # 通过环境变量指定MySQL端口"
}

# 主函数
main() {
    local SKIP_CHECK=false
    local DEBUG=false
    local CACHE_DIR_ARG=""
    
    # 如果环境变量中已设置MySQL端口，则使用环境变量的值
    if [ -n "$MYSQL_PORT" ]; then
        log_info "从环境变量读取MySQL端口: $MYSQL_PORT"
    fi
    
    # 解析命令行参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -s|--skip)
                SKIP_CHECK=true
                shift
                ;;
            -d|--debug)
                DEBUG=true
                shift
                ;;
            -c|--cache)
                if [ -z "$2" ]; then
                    log_error "选项 -c/--cache 需要指定缓存目录路径"
                    show_help
                    exit 1
                fi
                CACHE_DIR_ARG="$2"
                export DATAEASE_CACHE_DIR="$2"
                shift 2
                ;;
            -p|--mysql-port)
                if [ -z "$2" ]; then
                    log_error "选项 -p/--mysql-port 需要指定MySQL端口号"
                    show_help
                    exit 1
                fi
                # 验证端口号是否为数字
                if ! [[ "$2" =~ ^[0-9]+$ ]] || [ "$2" -lt 1 ] || [ "$2" -gt 65535 ]; then
                    log_error "无效的端口号: $2，端口号必须是1-65535之间的数字"
                    exit 1
                fi
                export MYSQL_PORT="$2"
                log_info "MySQL端口设置为: $MYSQL_PORT"
                shift 2
                ;;
            *)
                log_error "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    log_info "DataEase 后端服务启动脚本"
    log_info "=================================="
    
    # 环境检查
    if [ "$SKIP_CHECK" = false ]; then
        check_java
        check_port 8100
    else
        log_warning "跳过环境检查"
        # 即使跳过环境检查，也检查端口（避免端口冲突）
        check_port 8100
    fi
    
    # 设置调试模式
    if [ "$DEBUG" = true ]; then
        if [ -z "$JAVA_OPTS" ]; then
            export JAVA_OPTS="-Xms512m -Xmx2048m -XX:+UseG1GC -XX:+UseStringDeduplication"
        fi
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
trap 'log_info "正在停止服务..."; if [ ! -z "$BACKEND_PID" ]; then kill $BACKEND_PID 2>/dev/null; fi; SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"; PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"; rm -f "$PROJECT_ROOT/backend.pid" 2>/dev/null; log_success "服务已停止"; exit 0' INT TERM

# 执行主函数
main "$@"
