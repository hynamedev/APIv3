package net.frozenorb.apiv3.web.route;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.unsorted.actor.Actor;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class GETWhoAmI implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		Actor actor = ctx.get("actor");

		APIv3.respondJson(ctx, 200, ImmutableMap.of(
				"name", actor.getName(),
				"type", actor.getType(),
				"authorized", actor.isAuthorized()
		));
	}

}