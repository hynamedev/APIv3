package net.frozenorb.apiv3.web.route.users;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.IpUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class POSTUsersIdVerifyTotp implements Handler<RoutingContext> {

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

			if (user.getTotpSecret() == null) {
				ErrorUtils.respondInvalidInput(ctx, "User provided does not have totp code set.");
				return;
			}

			JsonObject requestBody = ctx.getBodyAsJson();
			String userIp = requestBody.getString("userIp");

			if (!IpUtils.isValidIp(userIp)) {
				ErrorUtils.respondInvalidInput(ctx, "Ip address \"" + userIp + "\" is not valid.");
				return;
			}

			user.checkTotpAuthorization(requestBody.getInteger("totpCode", -1), userIp, (totpAuthorizationResult, error2) -> {
				if (error2 != null) {
					ErrorUtils.respondInternalError(ctx, error2);
					return;
				}

				AuditLog.log(user.getId(), userIp, ctx, AuditLogActionType.USER_VERIFY_TOTP, (ignored, error3) -> {
					if (error3 != null) {
						ErrorUtils.respondInternalError(ctx, error3);
					} else {
						APIv3.respondJson(ctx, 200, ImmutableMap.of(
								"authorized", totpAuthorizationResult.isAuthorized(),
								"message", totpAuthorizationResult.name()
						));
					}
				});
			});
		});
	}

}