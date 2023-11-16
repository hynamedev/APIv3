package net.frozenorb.apiv3.web.route.servers;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.Server;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class GETServersPlayerCountId implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
	    Server server = Server.findById(ctx.request().getParam("serverId"));
		APIv3.respondJson(ctx, 200, server == null ? Integer.valueOf(0) : Integer.valueOf(server.getPlayers().size()));
	}

}