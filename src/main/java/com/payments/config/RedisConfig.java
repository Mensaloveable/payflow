package com.payments.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis configuration:
 *   - JSON serialization (human-readable keys + values in Redis)
 *   - Per-cache TTL configuration
 *   - Rate-limiting support via raw RedisTemplate
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * Jackson ObjectMapper configured for Redis:
     * - Java 8 time types (ZonedDateTime etc.)
     * - Type information embedded so deserialization works across restarts
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
            mapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }

    /**
     * Generic RedisTemplate<String, Object> — used for rate limiting
     * and any direct Redis operations outside of @Cacheable.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory,
                                                        ObjectMapper redisObjectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        GenericJackson2JsonRedisSerializer jsonSerializer =
            new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * CacheManager with per-cache TTL policies:
     *
     *  Cache name            TTL          Purpose
     *  ──────────────────    ───────      ──────────────────────────────────
     *  payment-stats         60s          Dashboard totals (recomputed often)
     *  recent-transactions   30s          Latest 20 rows (high write rate)
     *  transaction-detail    10m          Single transaction lookup by TXN-ID
     *  method-stats          5m           Per-method breakdown chart data
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory,
                                     ObjectMapper redisObjectMapper) {
        GenericJackson2JsonRedisSerializer jsonSerializer =
            new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
            .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("payment-stats",        defaultConfig.entryTtl(Duration.ofSeconds(60)));
        cacheConfigs.put("recent-transactions",  defaultConfig.entryTtl(Duration.ofSeconds(30)));
        cacheConfigs.put("transaction-detail",   defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("method-stats",         defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }
}
