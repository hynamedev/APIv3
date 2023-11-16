package net.frozenorb.apiv3.web.route.bannedAsns;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.BannedAsn;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class GETBannedAsns implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		APIv3.respondJson(ctx, 200, BannedAsn.findAll());
	}

}