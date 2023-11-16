package net.frozenorb.apiv3.web.filter;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.unsorted.actor.Actor;
import net.frozenorb.apiv3.util.ErrorUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class AuthorizationFilter implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext ctx) {
		Actor actor = ctx.get("actor");

		if (actor.isAuthorized()) {
			ctx.next();
		} else {
			ErrorUtils.respondOther(ctx, 403, "Failed to authorize as an approved actor.", "failedToAuthorizeNotApprovedActor", ImmutableMap.of());
		}
	}

}