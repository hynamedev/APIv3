package net.frozenorb.apiv3.web.route.prefixGrants;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.Grant;
import net.frozenorb.apiv3.domain.PrefixGrant;
import net.frozenorb.apiv3.util.ErrorUtils;
import org.springframework.stereotype.Component;

@Component
public final class GETPrefixGrantsId implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		PrefixGrant.findById(ctx.request().getParam("grantId"), (grant, error) -> {
			if (error != null) {
				ErrorUtils.respondInternalError(ctx, error);
			} else {
				APIv3.respondJson(ctx, 200, grant);
			}
		});
	}

}