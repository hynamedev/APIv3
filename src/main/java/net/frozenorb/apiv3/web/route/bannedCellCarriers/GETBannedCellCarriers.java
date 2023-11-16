package net.frozenorb.apiv3.web.route.bannedCellCarriers;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.BannedCellCarrier;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class GETBannedCellCarriers implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		APIv3.respondJson(ctx, 200, BannedCellCarrier.findAll());
	}

}