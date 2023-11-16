package net.frozenorb.apiv3.web.route.emailTokens;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.IpUtils;
import net.frozenorb.apiv3.util.PasswordUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class POSTEmailTokensIdConfirm implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		User user = SyncUtils.runBlocking(v -> User.findByEmailToken(ctx.request().getParam("emailToken"), v));

		if (user == null) {
			ErrorUtils.respondNotFound(ctx, "Email token", ctx.request().getParam("emailToken"));
			return;
		}

		if (user.getEmail() != null) {
			ErrorUtils.respondOther(ctx, 409, "User provided already has email set.", "emailAlreadySet", ImmutableMap.of());
			return;
		}

		if ((System.currentTimeMillis() - user.getPendingEmailTokenSetAt().toEpochMilli()) > TimeUnit.DAYS.toMillis(2)) {
			ErrorUtils.respondInvalidInput(ctx, "Email token is expired");
			return;
		}

		User sameEmail = SyncUtils.runBlocking(v -> User.findByConfirmedEmail(user.getPendingEmail(), v));

		if (sameEmail != null) {
			ErrorUtils.respondInvalidInput(ctx, user.getPendingEmail() + " is already in use.");
			return;
		}

		JsonObject requestBody = ctx.getBodyAsJson();
		String password = requestBody.getString("password");

		if (PasswordUtils.isTooShort(password)) {
			ErrorUtils.respondOther(ctx, 409, "Your password is too short.", "passwordTooShort", ImmutableMap.of());
			return;
		}

		if (PasswordUtils.isTooSimple(password)) {
			ErrorUtils.respondOther(ctx, 409, "Your password is too simple.", "passwordTooSimple", ImmutableMap.of());
			return;
		}

		user.completeEmailRegistration(user.getPendingEmail());
		user.updatePassword(password);
		SyncUtils.<Void>runBlocking(v -> user.save(v));
		String userIp = requestBody.getString("userIp");

		if (!IpUtils.isValidIp(userIp)) {
			ErrorUtils.respondInvalidInput(ctx, "IP address \"" + userIp + "\" is not valid.");
			return;
		}

		AuditLog.log(user.getId(), userIp, ctx, AuditLogActionType.USER_CONFIRM_EMAIL, (ignored, error) -> {
			if (error != null) {
				ErrorUtils.respondInternalError(ctx, error);
			} else {
				APIv3.respondJson(ctx, 200, ImmutableMap.of(
						"success", true
				));
			}
		});
	}

}