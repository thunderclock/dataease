package io.dataease.user.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 用户模块配置类
 */
@Configuration
@MapperScan("io.dataease.user.dao")
public class UserConfig {
    
}
