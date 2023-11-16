package net.frozenorb.apiv3.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

import io.vertx.core.Vertx;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

@Configuration
public class RedisConfig {

    @Bean
    public RedisClient redisClient(Vertx vertx, RedisOptions redisOptions) {
        return RedisClient.create(vertx, redisOptions);
    }

    @Bean
    public RedisOptions redisOptions(@Value("${redisUri}") URI redisUri) {
        return new RedisOptions()
            .setAddress(redisUri.getHost())
            .setPort(redisUri.getPort());
    }

}