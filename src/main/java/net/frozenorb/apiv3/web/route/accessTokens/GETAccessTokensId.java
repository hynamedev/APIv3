package net.frozenorb.apiv3.web.route.accessTokens;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.AccessToken;
import net.frozenorb.apiv3.util.ErrorUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class GETAccessTokensId implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		AccessToken.findById(ctx.request().getParam("accessToken"), (accessToken, error) -> {
			if (error != null) {
				ErrorUtils.respondInternalError(ctx, error);
			} else {
				APIv3.respondJson(ctx, 200, accessToken);
			}
		});
	}

}