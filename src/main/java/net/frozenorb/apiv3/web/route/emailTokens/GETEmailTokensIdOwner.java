package net.frozenorb.apiv3.web.route.emailTokens;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.util.ErrorUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class GETEmailTokensIdOwner implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		User.findByEmailToken(ctx.request().getParam("emailToken"), (user, error) -> {
			if (error != null) {
				ErrorUtils.respondInternalError(ctx, error);
			} else {
				APIv3.respondJson(ctx, 200, user);
			}
		});
	}

}