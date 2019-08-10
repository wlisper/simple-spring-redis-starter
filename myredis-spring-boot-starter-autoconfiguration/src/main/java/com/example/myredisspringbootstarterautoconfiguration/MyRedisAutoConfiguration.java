package com.example.myredisspringbootstarterautoconfiguration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

@Configuration
@ConditionalOnWebApplication
@EnableConfigurationProperties(RedisProperties.class)
public class MyRedisAutoConfiguration {

    @Autowired
    RedisProperties redisProperties;

    @Bean
    public JedisPool jedisPool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(redisProperties.getMaxActive());
        poolConfig.setMaxIdle(redisProperties.getMaxIdle());
        poolConfig.setMaxWaitMillis(redisProperties.getMaxWait());
        poolConfig.setTestOnBorrow(redisProperties.isTestOnBorrow());
        if (StringUtils.isEmpty(redisProperties)) {
            return new JedisPool(poolConfig, redisProperties.getHost(), redisProperties.getPort());
        } else {
            return new JedisPool(poolConfig, redisProperties.getHost(), redisProperties.getPort(),
                    Protocol.DEFAULT_TIMEOUT, redisProperties.getPassword(), Protocol.DEFAULT_DATABASE, null);
        }
    }
}
