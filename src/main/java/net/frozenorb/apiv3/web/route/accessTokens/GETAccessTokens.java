package net.frozenorb.apiv3.web.route.accessTokens;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.AccessToken;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.service.totp.TotpAuthorizationResult;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class GETAccessTokens implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		User user = SyncUtils.runBlocking(v -> User.findById(ctx.request().getParam("user"), v));

		if (user == null) {
			ErrorUtils.respondNotFound(ctx, "User", ctx.request().getParam("user"));
			return;
		}

		int code = Integer.parseInt(ctx.request().getParam("totpCode"));
		TotpAuthorizationResult totpAuthorizationResult = SyncUtils.runBlocking(v -> user.checkTotpAuthorization(code, null, v));

		if (!totpAuthorizationResult.isAuthorized()) {
			ErrorUtils.respondInvalidInput(ctx, "Totp authorization failed: " + totpAuthorizationResult.name());
			return;
		}

		AccessToken.findAll((accessTokens, error) -> {
			if (error != null) {
				ErrorUtils.respondInternalError(ctx, error);
			} else {
				APIv3.respondJson(ctx, 200, accessTokens);
			}
		});
	}

}