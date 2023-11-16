package net.frozenorb.apiv3.config;

import net.frozenorb.apiv3.ApiVerticle;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;

@Configuration
public class VertxConfig {

    @Bean
    public Vertx vertx(ApiVerticle apiVerticle) {
        return apiVerticle.getVertx();
    }

    @Bean
    public HttpServerOptions httpServerOptions(
        @Value("${http.keystorePassword}") String keystorePassword,
        @Value("${http.keystoreFile}") String keystoreFile
    ) {
        HttpServerOptions options = new HttpServerOptions();

        if (!keystoreFile.isEmpty()) {
            options.setSsl(true);
            options.setKeyStoreOptions(new JksOptions()
                .setPassword(keystorePassword)
                .setPath(keystoreFile)
            );
        }

        options.setCompressionSupported(true);
        return options;
    }

}