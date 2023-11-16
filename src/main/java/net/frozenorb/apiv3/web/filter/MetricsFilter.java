package net.frozenorb.apiv3.web.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.redis.RedisClient;

@Component
public final class MetricsFilter implements Handler<RoutingContext> {

	@Autowired private RedisClient redisClient;

	@Override
	public void handle(RoutingContext ctx) {
		redisClient.incr("apiv3:requests:total", (totalUpdateResult) -> {
			if (totalUpdateResult.failed()) {
				totalUpdateResult.cause().printStackTrace();
			}
		});

		// we purposely just immediately go on, we don't really care if
		// this fails and are fine to just go on without it.
		ctx.next();
	}

}