#!/bin/bash

# DataEase Docker 镜像构建并推送到华为云 SWR 脚本
# 功能：
#   - 构建 DataEase Docker 镜像
#   - 使用当前 git branch 名称作为标签
#   - 推送到华为云 SWR 仓库
# 作者: DataEase Team
# 版本: 2.10.12
# 用法：
#   ./build_and_push_docker.sh                    # 使用默认配置构建并推送
#   ./build_and_push_docker.sh --skip-build       # 跳过项目打包，直接构建镜像
#   ./build_and_push_docker.sh --name dataease    # 指定镜像名称

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 默认配置
DEFAULT_IMAGE_NAME="dataease"
DEFAULT_REGISTRY="swr.cn-southwest-2.myhuaweicloud.com/fangcang"
SKIP_BUILD=false
IMAGE_NAME=""
PLATFORM=""

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

# 显示帮助信息
show_help() {
    echo "DataEase Docker 镜像构建并推送到华为云 SWR 脚本"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  --name NAME        指定镜像名称（默认: dataease）"
    echo "  --platform PLAT    指定构建平台: x64 (linux/amd64) 或 arm (linux/arm64)（默认: 自动检测）"
    echo "  --skip-build       跳过项目打包，直接构建 Docker 镜像"
    echo "  --help, -h          显示此帮助信息"
    echo ""
    echo "说明:"
    echo "  - 镜像将推送到: $DEFAULT_REGISTRY"
    echo "  - 标签将使用当前 git branch 名称"
    echo "  - 如果 branch 名称包含特殊字符，将被替换为下划线"
    echo ""
    echo "示例:"
    echo "  $0                                    # 使用默认配置构建并推送"
    echo "  $0 --platform x64                    # 构建 x64 架构镜像并推送"
    echo "  $0 --platform arm                    # 构建 arm 架构镜像并推送"
    echo "  $0 --skip-build                      # 跳过打包，直接构建并推送"
    echo "  $0 --name dataease                   # 指定镜像名称"
    echo "  $0 --name dataease --platform x64    # 完整示例"
}

# 解析命令行参数
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --name)
                IMAGE_NAME="$2"
                shift 2
                ;;
            --platform)
                PLATFORM="$2"
                shift 2
                ;;
            --skip-build)
                SKIP_BUILD=true
                shift
                ;;
            --help|-h)
                show_help
                exit 0
                ;;
            *)
                log_error "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done

    # 设置默认值
    IMAGE_NAME=${IMAGE_NAME:-$DEFAULT_IMAGE_NAME}
    
    # 处理平台参数
    if [ -n "$PLATFORM" ]; then
        case "$PLATFORM" in
            x64|amd64|linux/amd64)
                PLATFORM="linux/amd64"
                ;;
            arm|arm64|linux/arm64)
                PLATFORM="linux/arm64"
                ;;
            *)
                log_error "不支持的平台: $PLATFORM"
                log_error "支持的平台: x64, arm"
                exit 1
                ;;
        esac
    fi
}

# 获取当前 git branch 名称
get_git_branch() {
    local branch_name
    
    # 检查是否在 git 仓库中
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        log_warning "当前目录不是 git 仓库，使用 'latest' 作为标签"
        echo "latest"
        return
    fi
    
    # 获取当前分支名称
    branch_name=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "latest")
    
    # 如果无法获取分支名称，使用 latest
    if [ -z "$branch_name" ] || [ "$branch_name" = "HEAD" ]; then
        log_warning "无法获取 git branch 名称，使用 'latest' 作为标签"
        echo "latest"
        return
    fi
    
    # 替换特殊字符为下划线（Docker 标签不允许的特殊字符）
    branch_name=$(echo "$branch_name" | sed 's/[^a-zA-Z0-9._-]/_/g')
    
    # 如果替换后为空，使用 latest
    if [ -z "$branch_name" ]; then
        log_warning "分支名称无效，使用 'latest' 作为标签"
        echo "latest"
        return
    fi
    
    echo "$branch_name"
}

# 检查 Docker 环境
check_docker() {
    log_info "检查 Docker 环境..."
    
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        log_error "Docker 服务未运行，请启动 Docker 服务"
        exit 1
    fi
    
    log_success "Docker 环境检查通过，版本: $(docker --version)"
}

# 检查必要的文件
check_files() {
    log_info "检查必要的文件..."
    
    local missing_files=()
    
    # 检查 Dockerfile
    if [ ! -f "$PROJECT_ROOT/Dockerfile" ]; then
        missing_files+=("Dockerfile")
    fi
    
    # 检查后端 JAR 文件
    local jar_file=$(find "$PROJECT_ROOT/core/core-backend/target" -name "CoreApplication.jar" 2>/dev/null | head -n 1)
    if [ -z "$jar_file" ]; then
        missing_files+=("core/core-backend/target/CoreApplication.jar")
    fi
    
    # 检查前端构建产物
    if [ ! -d "$PROJECT_ROOT/core/core-frontend/dist" ]; then
        missing_files+=("core/core-frontend/dist")
    fi
    
    # 检查 drivers 目录
    if [ ! -d "$PROJECT_ROOT/drivers" ]; then
        missing_files+=("drivers")
    fi
    
    # 检查 mapFiles 目录
    if [ ! -d "$PROJECT_ROOT/mapFiles" ]; then
        missing_files+=("mapFiles")
    fi
    
    # 检查 staticResource 目录
    if [ ! -d "$PROJECT_ROOT/staticResource" ]; then
        missing_files+=("staticResource")
    fi
    
    if [ ${#missing_files[@]} -gt 0 ]; then
        log_warning "以下文件或目录缺失:"
        for file in "${missing_files[@]}"; do
            log_warning "  - $file"
        done
        
        if [ "$SKIP_BUILD" = false ]; then
            log_info "将执行项目打包以生成缺失的文件..."
            return 1
        else
            log_error "缺少必要文件，且已指定 --skip-build，无法继续"
            exit 1
        fi
    else
        log_success "所有必要文件检查通过"
        return 0
    fi
}

# 打包项目
build_project() {
    log_info "=================================="
    log_info "开始打包项目"
    log_info "=================================="
    
    local package_script="$PROJECT_ROOT/script/package_all.sh"
    
    if [ -f "$package_script" ]; then
        log_info "使用 package_all.sh 脚本打包项目..."
        bash "$package_script"
    else
        log_warning "未找到 package_all.sh，尝试直接使用 Maven 打包..."
        check_maven
        install_sdk
        build_frontend
        build_backend
    fi
    
    log_success "项目打包完成"
}

# 检查 Maven 环境（如果使用 Maven 打包）
check_maven() {
    if ! command -v mvn &> /dev/null; then
        log_error "Maven 未安装，请先安装 Maven"
        exit 1
    fi
    log_success "Maven 环境检查通过"
}

# 安装 SDK 模块
install_sdk() {
    log_info "安装 SDK 模块..."
    cd "$PROJECT_ROOT/sdk"
    mvn clean install -DskipTests
    cd "$PROJECT_ROOT"
}

# 编译前端
build_frontend() {
    log_info "编译前端..."
    cd "$PROJECT_ROOT/core/core-frontend"
    mvn clean install -DskipTests
    cd "$PROJECT_ROOT"
}

# 编译后端
build_backend() {
    log_info "编译后端..."
    cd "$PROJECT_ROOT/core/core-backend"
    mvn clean install -DskipTests
    cd "$PROJECT_ROOT"
}

# 检查并设置 buildx
setup_buildx() {
    log_info "检查 Docker buildx..."
    
    if ! docker buildx version &> /dev/null; then
        log_warning "Docker buildx 不可用，将使用标准 docker build"
        return 1
    fi
    
    # 检查是否存在 builder
    if ! docker buildx ls | grep -q "dataease-builder"; then
        log_info "创建 buildx builder: dataease-builder"
        if ! docker buildx create --name dataease-builder --use 2>/dev/null; then
            log_warning "无法创建 buildx builder，尝试使用 desktop-linux"
            if docker buildx ls | grep -q "desktop-linux"; then
                docker buildx use desktop-linux 2>/dev/null || true
                log_info "使用 desktop-linux builder"
                return 0
            else
                log_warning "尝试使用默认 builder"
                docker buildx use default 2>/dev/null || true
                return 0
            fi
        fi
    else
        log_info "使用现有的 buildx builder: dataease-builder"
        docker buildx use dataease-builder 2>/dev/null || true
    fi
    
    # 初始化 builder（如果需要）
    log_info "初始化 buildx builder..."
    if ! docker buildx inspect --bootstrap &> /dev/null; then
        log_warning "builder 初始化失败，尝试删除并重新创建..."
        
        # 尝试删除旧的 builder
        docker buildx rm dataease-builder 2>/dev/null || true
        
        # 重新创建 builder
        if docker buildx create --name dataease-builder --use 2>/dev/null; then
            log_info "成功重新创建 buildx builder"
            docker buildx inspect --bootstrap &> /dev/null || {
                log_warning "builder 仍然无法启动，尝试使用 desktop-linux"
                if docker buildx ls | grep -q "desktop-linux"; then
                    docker buildx use desktop-linux 2>/dev/null || true
                    log_info "使用 desktop-linux builder"
                    return 0
                fi
            }
        else
            log_warning "无法重新创建 builder，尝试使用 desktop-linux"
            if docker buildx ls | grep -q "desktop-linux"; then
                docker buildx use desktop-linux 2>/dev/null || true
                log_info "使用 desktop-linux builder"
                return 0
            else
                log_warning "所有 buildx builder 都不可用，将使用标准 docker build"
                return 1
            fi
        fi
    fi
    
    # 验证 builder 是否真的可用
    if docker buildx inspect --bootstrap &> /dev/null; then
        log_success "buildx builder 已就绪"
        return 0
    else
        log_warning "buildx builder 无法启动，将使用标准 docker build"
        return 1
    fi
}

# 构建 Docker 镜像
build_docker_image() {
    local tag=$1
    local full_image_name="$DEFAULT_REGISTRY/$IMAGE_NAME:$tag"
    
    log_info "=================================="
    log_info "开始构建 Docker 镜像"
    log_info "=================================="
    
    log_info "镜像名称: $full_image_name"
    log_info "构建上下文: $PROJECT_ROOT"
    
    if [ -n "$PLATFORM" ]; then
        log_info "目标平台: $PLATFORM"
        
        # 使用 buildx 构建指定平台的镜像
        if setup_buildx; then
            log_info "使用 Docker buildx 构建多平台镜像..."
            cd "$PROJECT_ROOT"
            
            # 构建并直接推送（因为总是要推送）
            if ! docker buildx build \
                --platform "$PLATFORM" \
                -t "$full_image_name" \
                --push \
                . 2>&1; then
                log_error "buildx 构建失败，尝试使用标准 docker build"
                log_warning "注意：标准 docker build 可能无法构建跨平台镜像"
                cd "$PROJECT_ROOT"
                docker build --platform "$PLATFORM" -t "$full_image_name" .
                # 使用标准构建后需要手动推送
                log_info "使用标准构建，需要手动推送镜像"
            fi
        else
            log_warning "buildx 不可用，使用标准 docker build"
            log_warning "注意：标准 docker build 可能无法构建跨平台镜像，建议修复 buildx 后重试"
            cd "$PROJECT_ROOT"
            docker build --platform "$PLATFORM" -t "$full_image_name" .
            # 使用标准构建后需要手动推送
            log_info "使用标准构建，需要手动推送镜像"
        fi
    else
        # 未指定平台，使用标准构建
        log_info "未指定平台，使用当前系统架构构建"
        cd "$PROJECT_ROOT"
        docker build -t "$full_image_name" .
    fi
    
    if [ $? -eq 0 ]; then
        log_success "Docker 镜像构建成功: $full_image_name"
        
        # 显示镜像信息（如果镜像在本地）
        if [ -z "$PLATFORM" ]; then
            log_info "镜像信息:"
            docker images "$full_image_name" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}" 2>/dev/null || true
        fi
        
        echo "$full_image_name"
    else
        log_error "Docker 镜像构建失败"
        exit 1
    fi
}

# 推送 Docker 镜像
push_docker_image() {
    local image_name=$1
    
    log_info "=================================="
    log_info "推送 Docker 镜像到华为云 SWR"
    log_info "=================================="
    
    log_info "推送镜像: $image_name"
    
    # 检查是否已登录华为云 SWR
    log_info "检查华为云 SWR 登录状态..."
    if ! docker info | grep -q "swr.cn-southwest-2.myhuaweicloud.com" 2>/dev/null; then
        log_warning "可能需要登录华为云 SWR，请确保已执行:"
        log_warning "  docker login swr.cn-southwest-2.myhuaweicloud.com"
        log_info ""
        read -p "是否现在登录华为云 SWR? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            docker login swr.cn-southwest-2.myhuaweicloud.com
        fi
    fi
    
    log_info "开始推送镜像..."
    docker push "$image_name"
    
    if [ $? -eq 0 ]; then
        log_success "镜像推送成功: $image_name"
    else
        log_error "镜像推送失败，请检查："
        log_error "  1. 是否已登录华为云 SWR: docker login swr.cn-southwest-2.myhuaweicloud.com"
        log_error "  2. 是否有推送权限"
        log_error "  3. 网络连接是否正常"
        exit 1
    fi
}

# 主函数
main() {
    log_info "=================================="
    log_info "DataEase Docker 镜像构建并推送脚本"
    log_info "=================================="
    log_info "开始时间: $(date '+%Y-%m-%d %H:%M:%S')"
    log_info ""
    
    # 获取脚本所在目录的父目录
    SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
    PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
    
    # 切换到项目根目录
    cd "$PROJECT_ROOT"
    log_info "项目根目录: $PROJECT_ROOT"
    log_info ""
    
    # 解析参数
    parse_args "$@"
    
    # 获取 git branch 名称作为标签
    IMAGE_TAG=$(get_git_branch)
    log_info "使用标签: $IMAGE_TAG (来自 git branch)"
    log_info ""
    
    # 检查 Docker 环境
    check_docker
    log_info ""
    
    # 检查必要文件
    if ! check_files; then
        log_info ""
        # 如果文件缺失且未跳过构建，则执行打包
        if [ "$SKIP_BUILD" = false ]; then
            build_project
            log_info ""
            # 再次检查文件
            if ! check_files; then
                log_error "打包后仍有文件缺失，无法继续"
                exit 1
            fi
        fi
    fi
    
    log_info ""
    
    # 构建 Docker 镜像（如果指定了平台，buildx 会直接推送）
    FULL_IMAGE_NAME=$(build_docker_image "$IMAGE_TAG")
    
    log_info ""
    
    # 如果未使用 buildx 推送，则手动推送
    if [ -z "$PLATFORM" ]; then
        push_docker_image "$FULL_IMAGE_NAME"
    else
        # 检查是否已经通过 buildx 推送（buildx 失败时会回退到标准构建）
        if docker images "$FULL_IMAGE_NAME" --format "{{.Repository}}:{{.Tag}}" 2>/dev/null | grep -q "$FULL_IMAGE_NAME"; then
            log_info "镜像已构建，开始推送..."
            push_docker_image "$FULL_IMAGE_NAME"
        else
            log_info "镜像已通过 buildx 直接推送到仓库"
        fi
    fi
    
    log_info ""
    log_info "=================================="
    log_success "Docker 镜像构建并推送完成！"
    log_info "=================================="
    log_info "结束时间: $(date '+%Y-%m-%d %H:%M:%S')"
    log_info ""
    log_info "镜像信息:"
    log_info "  完整镜像名称: $FULL_IMAGE_NAME"
    log_info "  仓库地址: $DEFAULT_REGISTRY"
    log_info "  镜像名称: $IMAGE_NAME"
    log_info "  标签: $IMAGE_TAG"
    log_info ""
    log_info "使用以下命令拉取镜像:"
    log_info "  docker pull $FULL_IMAGE_NAME"
    log_info ""
    log_info "使用以下命令运行容器:"
    log_info "  docker run -d -p 8100:8100 --name dataease $FULL_IMAGE_NAME"
    log_info ""
}

# 执行主函数
main "$@"

