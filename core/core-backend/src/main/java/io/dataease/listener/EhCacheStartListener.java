package io.dataease.listener;


import io.dataease.utils.ConfigUtils;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class EhCacheStartListener implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        String property = applicationContext.getEnvironment().getProperty("dataease.login_timeout", String.class, "480");
        System.setProperty("dataease.login_timeout", property);

        // 优先使用系统属性（可通过 -Ddataease.path.ehcache 传递）
        String ehcache = System.getProperty("dataease.path.ehcache");
        if (ehcache == null || ehcache.isEmpty()) {
            // 如果系统属性不存在，尝试从配置文件读取
            ehcache = ConfigUtils.getConfig("dataease.path.ehcache", "/opt/dataease2.0/cache");
        }
        System.setProperty("dataease.path.ehcache", ehcache);
    }
}
