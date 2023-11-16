package net.frozenorb.apiv3.web.route.grants;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.Grant;
import net.frozenorb.apiv3.util.ErrorUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class GETGrantsId implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		Grant.findById(ctx.request().getParam("grantId"), (grant, error) -> {
			if (error != null) {
				ErrorUtils.respondInternalError(ctx, error);
			} else {
				APIv3.respondJson(ctx, 200, grant);
			}
		});
	}

}