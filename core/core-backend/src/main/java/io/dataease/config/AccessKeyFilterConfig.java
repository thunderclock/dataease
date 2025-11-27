package io.dataease.config;

import io.dataease.auth.filter.AccessKeySignatureFilter;
import io.dataease.auth.manage.AccessKeyManage;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AccessKey 签名验证 Filter 配置
 */
@Configuration
public class AccessKeyFilterConfig {

    @Bean
    public FilterRegistrationBean<AccessKeySignatureFilter> accessKeySignatureFilter(AccessKeyManage accessKeyManage) {
        FilterRegistrationBean<AccessKeySignatureFilter> filter = new FilterRegistrationBean<>();
        filter.setName("accessKeySignatureFilter");
        filter.setFilter(new AccessKeySignatureFilter(accessKeyManage));
        filter.addUrlPatterns("/commonData/*", "/de2api/commonData/*");
        filter.setOrder(2); // 在 TokenFilter (order=0) 和 CommunityTokenFilter (order=5) 之间
        return filter;
    }
}

