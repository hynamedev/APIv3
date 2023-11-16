package net.frozenorb.apiv3.web.route.users;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.service.totp.TotpService;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.IpUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class POSTUsersIdSetupTotp implements Handler<RoutingContext> {

	@Autowired private TotpService totpService;

	public void handle(RoutingContext ctx) {
		User user = SyncUtils.runBlocking(v -> User.findById(ctx.request().getParam("userId"), v));

		if (user == null) {
			ErrorUtils.respondNotFound(ctx, "User", ctx.request().getParam("userId"));
			return;
		}

		if (user.getTotpSecret() != null) {
			ErrorUtils.respondInvalidInput(ctx, "User provided already has totp setup.");
			return;
		}

		JsonObject requestBody = ctx.getBodyAsJson();
		String secret = requestBody.getString("secret");
		int totpCode = requestBody.getInteger("totpCode", -1);

		if (totpService.authorizeUser(secret, totpCode)) {
			user.setTotpSecret(secret);
			SyncUtils.<Void>runBlocking(v -> user.save(v));
			String userIp = requestBody.getString("userIp");

			if (!IpUtils.isValidIp(userIp)) {
				ErrorUtils.respondInvalidInput(ctx, "Ip address \"" + userIp + "\" is not valid.");
				return;
			}

			AuditLog.log(user.getId(), userIp, ctx, AuditLogActionType.USER_SETUP_TOTP, (ignored, error) -> {
				if (error != null) {
					ErrorUtils.respondInternalError(ctx, error);
				} else {
					APIv3.respondJson(ctx, 200, ImmutableMap.of(
							"success", true
					));
				}
			});
		} else {
			ErrorUtils.respondOther(ctx, 400, "Confirmation code provided did not match.", "badConfirmationCode", ImmutableMap.of());
		}
	}

}