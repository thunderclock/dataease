package io.dataease.auth.filter;

import io.dataease.auth.manage.AccessTokenManage;
import io.dataease.auth.entity.AccessToken;
import io.dataease.auth.utils.SignatureUtils;
import io.dataease.constant.AuthConstant;
import io.dataease.exception.DEException;
import io.dataease.result.ResultCode;
import io.dataease.utils.LogUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
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
 * AccessToken 签名验证 Filter
 * 用于验证 commonData 接口的 POST 请求签名
 * 
 * 注意：此类不在 FilterConfig 中通过 @Component 自动注册，
 * 而是在 AccessTokenFilterConfig 中通过 @Bean 方法注册，以便注入 AccessTokenManage
 */
public class AccessTokenSignatureFilter implements Filter {

    private static final String ACCESS_TOKEN_HEADER = "X-ACCESS-TOKEN";
    private static final String TIMESTAMP_HEADER = "X-TIMESTAMP";
    private static final String SIGNATURE_HEADER = "X-SIGNATURE";
    private static final long TIMESTAMP_TOLERANCE = 5 * 60 * 1000; // 5 分钟时间容差

    private final AccessTokenManage accessTokenManage;

    public AccessTokenSignatureFilter(AccessTokenManage accessTokenManage) {
        this.accessTokenManage = accessTokenManage;
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
            String accessToken = request.getHeader(ACCESS_TOKEN_HEADER);
            String timestampStr = request.getHeader(TIMESTAMP_HEADER);
            String signature = request.getHeader(SIGNATURE_HEADER);
            
            // 如果标准 header 名称获取不到，尝试小写版本（某些 HTTP 客户端会转换）
            if (StringUtils.isBlank(accessToken)) {
                accessToken = request.getHeader(ACCESS_TOKEN_HEADER.toLowerCase());
            }
            if (StringUtils.isBlank(timestampStr)) {
                timestampStr = request.getHeader(TIMESTAMP_HEADER.toLowerCase());
            }
            if (StringUtils.isBlank(signature)) {
                signature = request.getHeader(SIGNATURE_HEADER.toLowerCase());
            }
            
            // 尝试从枚举中获取（某些情况下 headers 可能以不同的大小写形式存在）
            if (StringUtils.isBlank(accessToken) || StringUtils.isBlank(timestampStr) || StringUtils.isBlank(signature)) {
                java.util.Enumeration<String> headerNames = request.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    String headerValue = request.getHeader(headerName);
                    if (StringUtils.isBlank(accessToken) && headerName.equalsIgnoreCase(ACCESS_TOKEN_HEADER)) {
                        accessToken = headerValue;
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
            if (StringUtils.isBlank(accessToken) || StringUtils.isBlank(timestampStr) || StringUtils.isBlank(signature)) {
                // 调试：打印所有请求头
                java.util.Enumeration<String> headerNames = request.getHeaderNames();
                StringBuilder allHeaders = new StringBuilder();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    allHeaders.append(headerName).append(": ").append(request.getHeader(headerName)).append("; ");
                }
                DEException.throwException(ResultCode.DATA_IS_WRONG.code(), 
                    "Missing required headers: " + ACCESS_TOKEN_HEADER + ", " + TIMESTAMP_HEADER + ", " + SIGNATURE_HEADER 
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

            // 查询 AccessToken
            AccessToken tokenEntity = accessTokenManage.getByToken(accessToken);
            if (tokenEntity == null) {
                DEException.throwException(ResultCode.DATA_IS_WRONG.code(), "Invalid access token");
            }

            // 读取请求体（需要包装请求以支持多次读取）
            CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
            String requestBody = getRequestBody(cachedRequest);

            // 验证签名
            boolean isValid = SignatureUtils.verifySignature(
                signature, 
                tokenEntity.getAccessSecret(), 
                timestamp, 
                requestBody
            );

            if (!isValid) {
                LogUtil.warn("Signature verification failed for AccessToken: " + accessToken);
                DEException.throwException(ResultCode.DATA_IS_WRONG.code(), "Invalid signature");
            }

            // 更新最后使用时间
            accessTokenManage.updateLastUseTime(accessToken);

            LogUtil.info("AccessToken signature verified successfully: " + accessToken);

            // 继续处理请求（使用缓存的请求）
            filterChain.doFilter(cachedRequest, servletResponse);

        } catch (DEException e) {
            sendErrorResponse(response, e.getCode(), e.getMessage());
        } catch (Exception e) {
            LogUtil.error("AccessToken signature verification error", e);
            sendErrorResponse(response, ResultCode.SYSTEM_INNER_ERROR.code(), "Signature verification error: " + e.getMessage());
        }
    }

    /**
     * 读取请求体
     */
    private String getRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
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

    /**
     * 缓存请求体的 HttpServletRequest 包装类
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
     * 缓存的 ServletInputStream
     */
    private static class CachedBodyServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream buffer;

        public CachedBodyServletInputStream(byte[] contents) {
            this.buffer = new ByteArrayInputStream(contents);
        }

        @Override
        public int read() throws IOException {
            return buffer.read();
        }

        @Override
        public boolean isFinished() {
            return buffer.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            throw new UnsupportedOperationException();
        }
    }
}

