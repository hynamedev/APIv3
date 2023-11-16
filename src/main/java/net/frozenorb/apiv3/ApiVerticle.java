package net.frozenorb.apiv3;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;

@Component
public final class ApiVerticle extends AbstractVerticle {

    @PostConstruct
    public void deploy() {
        Vertx.vertx().deployVerticle(this);
    }

}