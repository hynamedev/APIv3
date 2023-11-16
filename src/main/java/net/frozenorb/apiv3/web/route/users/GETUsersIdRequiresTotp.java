package net.frozenorb.apiv3.web.route.users;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.service.totp.RequiresTotpResult;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.IpUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class GETUsersIdRequiresTotp implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		User.findById(ctx.request().getParam("userId"), (user, error) -> {
			if (error != null) {
				ErrorUtils.respondInternalError(ctx, error);
				return;
			}

			if (user == null) {
				ErrorUtils.respondNotFound(ctx, "User", ctx.request().getParam("userId"));
				return;
			}

			String userIp = ctx.request().getParam("userIp");

			if (!IpUtils.isValidIp(userIp)) {
				ErrorUtils.respondInvalidInput(ctx, "Ip address \"" + userIp + "\" is not valid.");
				return;
			}

			user.requiresTotpAuthorization(userIp, (requiresTotpResult, error2) -> {
				if (error2 != null) {
					ErrorUtils.respondInternalError(ctx, error2);
				} else {
					APIv3.respondJson(ctx, 200, ImmutableMap.of(
							"required", (requiresTotpResult == RequiresTotpResult.REQUIRED_NO_EXEMPTIONS),
							"message", requiresTotpResult.name()
					));
				}
			});
		});
	}

}