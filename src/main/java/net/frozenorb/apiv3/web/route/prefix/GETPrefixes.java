package net.frozenorb.apiv3.web.route.prefix;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.Prefix;
import net.frozenorb.apiv3.domain.Rank;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public final class GETPrefixes implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		APIv3.respondJson(ctx, 200, Prefix.findAll());
	}

}