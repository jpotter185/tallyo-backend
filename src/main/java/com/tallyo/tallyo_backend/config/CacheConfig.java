package com.tallyo.tallyo_backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("currentContext");
    }

    @Scheduled(fixedRate = 300000) // 300,000 ms = 5 minutes
    @CacheEvict(value = "currentContext", allEntries = true)
    public void evictCurrentContextCache() {}
}
