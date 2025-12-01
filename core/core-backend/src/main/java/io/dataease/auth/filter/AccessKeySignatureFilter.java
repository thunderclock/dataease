package io.dataease.auth.filter;

import io.dataease.auth.manage.AccessKeyManage;
import io.dataease.auth.entity.AccessKey;
import io.dataease.auth.bo.TokenUserBO;
import io.dataease.auth.utils.SignatureUtils;
import io.dataease.constant.AuthConstant;
import io.dataease.exception.DEException;
import io.dataease.result.ResultCode;
import io.dataease.utils.LogUtil;
import io.dataease.utils.UserUtils;
import io.dataease.utils.CommonBeanFactory;
import io.dataease.user.dao.UserMapper;
import io.dataease.user.entity.User;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

/**
 * AccessKey 签名验证 Filter
 * 用于验证 commonData 接口的 POST 请求签名
 * 
 * 注意：此类不在 FilterConfig 中通过 @Component 自动注册，
 * 而是在 AccessKeyFilterConfig 中通过 @Bean 方法注册，以便注入 AccessKeyManage
 */
public class AccessKeySignatureFilter implements Filter {

    private static final String ACCESS_KEY_HEADER = "X-ACCESS-KEY";
    private static final String TIMESTAMP_HEADER = "X-TIMESTAMP";
    private static final String SIGNATURE_HEADER = "X-SIGNATURE";
    private static final long TIMESTAMP_TOLERANCE = 5 * 60 * 1000; // 5 分钟时间容差

    private final AccessKeyManage accessKeyManage;

    public AccessKeySignatureFilter(AccessKeyManage accessKeyManage) {
        this.accessKeyManage = accessKeyManage;
    }
    
    /**
     * 获取 UserMapper（延迟加载，避免循环依赖）
     */
    private UserMapper getUserMapper() {
        return CommonBeanFactory.getBean(UserMapper.class);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // 只处理 POST 请求且路径为 /commonData 开头的请求
        String requestURI = request.getRequestURI();
        boolean isCommonDataPath = requestURI.startsWith("/commonData") || 
                                  requestURI.startsWith(AuthConstant.DE_API_PREFIX + "/commonData");
        
        if (!"POST".equalsIgnoreCase(request.getMethod()) || !isCommonDataPath) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        try {
            // 获取请求头（HttpServletRequest.getHeader 是大小写不敏感的，但为了兼容性，我们尝试多种方式）
            String accessKey = request.getHeader(ACCESS_KEY_HEADER);
            String timestampStr = request.getHeader(TIMESTAMP_HEADER);
            String signature = request.getHeader(SIGNATURE_HEADER);
            
            // 如果标准 header 名称获取不到，尝试小写版本（某些 HTTP 客户端会转换）
            if (StringUtils.isBlank(accessKey)) {
                accessKey = request.getHeader(ACCESS_KEY_HEADER.toLowerCase());
            }
            if (StringUtils.isBlank(timestampStr)) {
                timestampStr = request.getHeader(TIMESTAMP_HEADER.toLowerCase());
            }
            if (StringUtils.isBlank(signature)) {
                signature = request.getHeader(SIGNATURE_HEADER.toLowerCase());
            }
            
            // 尝试从枚举中获取（某些情况下 headers 可能以不同的大小写形式存在）
            if (StringUtils.isBlank(accessKey) || StringUtils.isBlank(timestampStr) || StringUtils.isBlank(signature)) {
                java.util.Enumeration<String> headerNames = request.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    String headerValue = request.getHeader(headerName);
                    if (StringUtils.isBlank(accessKey) && headerName.equalsIgnoreCase(ACCESS_KEY_HEADER)) {
                        accessKey = headerValue;
                    }
                    if (StringUtils.isBlank(timestampStr) && headerName.equalsIgnoreCase(TIMESTAMP_HEADER)) {
                        timestampStr = headerValue;
                    }
                    if (StringUtils.isBlank(signature) && headerName.equalsIgnoreCase(SIGNATURE_HEADER)) {
                        signature = headerValue;
                    }
                }
            }

            // 验证必需的请求头
            if (StringUtils.isBlank(accessKey) || StringUtils.isBlank(timestampStr) || StringUtils.isBlank(signature)) {
                // 调试：打印所有请求头
                java.util.Enumeration<String> headerNames = request.getHeaderNames();
                StringBuilder allHeaders = new StringBuilder();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    allHeaders.append(headerName).append(": ").append(request.getHeader(headerName)).append("; ");
                }
                DEException.throwException(ResultCode.DATA_IS_WRONG.code(), 
                    "Missing required headers: " + ACCESS_KEY_HEADER + ", " + TIMESTAMP_HEADER + ", " + SIGNATURE_HEADER 
                    + ". All headers: " + allHeaders.toString());
            }

            // 验证时间戳
            long timestamp;
            try {
                timestamp = Long.parseLong(timestampStr);
            } catch (NumberFormatException e) {
                DEException.throwException(ResultCode.DATA_IS_WRONG.code(), "Invalid timestamp format");
            }
            timestamp = Long.parseLong(timestampStr);
            
            long currentTime = System.currentTimeMillis();
            long timeDiff = Math.abs(currentTime - timestamp);
            if (timeDiff > TIMESTAMP_TOLERANCE) {
                DEException.throwException(ResultCode.DATA_IS_WRONG.code(), 
                    "Timestamp expired or invalid. Current: " + currentTime + ", Provided: " + timestamp);
            }

            // 查询 AccessKey
            AccessKey keyEntity = accessKeyManage.getByKey(accessKey);
            if (keyEntity == null) {
                DEException.throwException(ResultCode.DATA_IS_WRONG.code(), "Invalid access key");
            }

            // 读取请求体（需要包装请求以支持多次读取）
            CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
            String requestBody = getRequestBody(cachedRequest);

            // 调试日志：打印签名验证相关信息
            String expectedSignature = SignatureUtils.generateSignature(
                keyEntity.getAccessSecret(), 
                timestamp, 
                requestBody
            );
            
            LogUtil.info("=== AccessKey Signature Verification Debug ===");
            LogUtil.info("AccessKey: " + accessKey);
            LogUtil.info("Timestamp: " + timestamp + " (current: " + System.currentTimeMillis() + ")");
            LogUtil.info("Request Body Length: " + (requestBody != null ? requestBody.length() : 0));
            LogUtil.info("Request Body Preview: " + (requestBody != null && requestBody.length() > 100 
                ? requestBody.substring(0, 100) + "..." : requestBody));
            LogUtil.info("Received Signature: " + signature);
            LogUtil.info("Expected Signature: " + expectedSignature);
            LogUtil.info("Signatures Match: " + signature.equals(expectedSignature));
            LogUtil.info("=============================================");

            // 验证签名
            boolean isValid = SignatureUtils.verifySignature(
                signature, 
                keyEntity.getAccessSecret(), 
                timestamp, 
                requestBody
            );

            if (!isValid) {
                LogUtil.warn("Signature verification failed for AccessKey: " + accessKey);
                LogUtil.warn("Expected: " + expectedSignature);
                LogUtil.warn("Received: " + signature);
                DEException.throwException(ResultCode.DATA_IS_WRONG.code(), "Invalid signature");
            }

            // 更新最后使用时间
            accessKeyManage.updateLastUseTime(accessKey);
            
            LogUtil.info("AccessKey signature verified successfully: " + accessKey);

            // 设置绑定的用户信息，用于权限控制（支持行级权限）
            if (keyEntity.getUserId() != null) {
                try {
                    UserMapper userMapper = getUserMapper();
                    if (userMapper != null) {
                        // 直接查询用户实体
                        User user = userMapper.selectById(keyEntity.getUserId());
                        if (user != null && user.getId() != null && (user.getDeleted() == null || !user.getDeleted())) {
                            // 创建 TokenUserBO 并设置用户信息
                            TokenUserBO tokenUserBO = new TokenUserBO();
                            tokenUserBO.setUserId(user.getId());
                            // 获取用户的组织ID（如果有）
                            Long orgId = user.getOrgId();
                            if (orgId != null) {
                                tokenUserBO.setDefaultOid(orgId);
                            } else {
                                // 如果没有组织ID，使用默认值
                                tokenUserBO.setDefaultOid(1L);
                            }
                            // 设置用户信息到 ThreadLocal，用于后续权限检查
                            UserUtils.setUserInfo(tokenUserBO);
                            LogUtil.info("Set user info for AccessKey: userId=" + user.getId() + ", orgId=" + tokenUserBO.getDefaultOid());
                        } else {
                            LogUtil.warn("User not found or deleted for AccessKey userId: " + keyEntity.getUserId());
                        }
                    } else {
                        LogUtil.warn("UserMapper not available, cannot set user info for AccessKey");
                    }
                } catch (Exception e) {
                    LogUtil.error("Failed to set user info for AccessKey: " + accessKey, e);
                    // 即使设置用户信息失败，也继续处理请求（但可能无法通过权限检查）
                }
            } else {
                LogUtil.warn("AccessKey has no bound user: " + accessKey);
            }

            // 继续处理请求
            filterChain.doFilter(cachedRequest, servletResponse);
        } catch (DEException e) {
            sendErrorResponse(response, e.getCode(), e.getMessage());
        } catch (Exception e) {
            LogUtil.error("AccessKey signature verification error", e);
            sendErrorResponse(response, ResultCode.SYSTEM_INNER_ERROR.code(), "Signature verification error: " + e.getMessage());
        }
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, Integer code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        
        String jsonResponse = String.format("{\"code\":%d,\"msg\":\"%s\",\"data\":null}", code, message);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    private String getRequestBody(HttpServletRequest request) throws IOException {
        // 使用字节流读取，确保保留所有字符（包括换行符）
        byte[] bodyBytes = StreamUtils.copyToByteArray(request.getInputStream());
        return new String(bodyBytes, StandardCharsets.UTF_8);
    }

    /**
     * 包装 HttpServletRequest 以支持多次读取请求体
     */
    private static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private byte[] cachedBody;

        public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            InputStream requestInputStream = request.getInputStream();
            this.cachedBody = StreamUtils.copyToByteArray(requestInputStream);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new CachedBodyServletInputStream(this.cachedBody);
        }

        @Override
        public BufferedReader getReader() throws IOException {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.cachedBody);
            return new BufferedReader(new InputStreamReader(byteArrayInputStream, StandardCharsets.UTF_8));
        }
    }

    /**
     * 可缓存的 ServletInputStream
     */
    private static class CachedBodyServletInputStream extends ServletInputStream {
        private InputStream cachedBodyInputStream;

        public CachedBodyServletInputStream(byte[] cachedBody) {
            this.cachedBodyInputStream = new ByteArrayInputStream(cachedBody);
        }

        @Override
        public boolean isFinished() {
            try {
                return cachedBodyInputStream.available() == 0;
            } catch (IOException e) {
                return false;
            }
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read() throws IOException {
            return this.cachedBodyInputStream.read();
        }
    }
}

