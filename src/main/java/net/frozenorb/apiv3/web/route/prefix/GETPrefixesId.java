package net.frozenorb.apiv3.web.route.prefix;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.Prefix;
import org.springframework.stereotype.Component;

@Component
public final class GETPrefixesId implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		APIv3.respondJson(ctx, 200, Prefix.findById(ctx.request().getParam("prefixId")));
	}

}