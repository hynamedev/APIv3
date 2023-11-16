package net.frozenorb.apiv3.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;

@Configuration
public class HttpClientConfig {

    @Bean
    public HttpClient httpsClient(Vertx vertx) {
        return vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true));
    }

    @Bean
    public HttpClient httpClient(Vertx vertx) {
        return vertx.createHttpClient();
    }

}