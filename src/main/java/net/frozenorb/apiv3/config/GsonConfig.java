package net.frozenorb.apiv3.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.frozenorb.apiv3.serialization.gson.FollowAnnotationExclusionStrategy;
import net.frozenorb.apiv3.serialization.gson.InstantTypeAdapter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

@Configuration
public class GsonConfig {

    @Bean
    public Gson gson() {
        return new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
            .setExclusionStrategies(new FollowAnnotationExclusionStrategy())
            .create();
    }

}