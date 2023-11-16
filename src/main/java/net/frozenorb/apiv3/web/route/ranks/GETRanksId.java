package net.frozenorb.apiv3.web.route.ranks;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.Rank;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class GETRanksId implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		APIv3.respondJson(ctx, 200, Rank.findById(ctx.request().getParam("rankId")));
	}

}