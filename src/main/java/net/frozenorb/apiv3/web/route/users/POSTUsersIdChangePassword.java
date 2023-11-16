package net.frozenorb.apiv3.web.route.users;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.service.totp.RequiresTotpResult;
import net.frozenorb.apiv3.service.totp.TotpAuthorizationResult;
import net.frozenorb.apiv3.service.usersession.UserSessionService;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.IpUtils;
import net.frozenorb.apiv3.util.PasswordUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class POSTUsersIdChangePassword implements Handler<RoutingContext> {

	@Autowired private UserSessionService userSessionService;

	public void handle(RoutingContext ctx) {
		User user = SyncUtils.runBlocking(v -> User.findById(ctx.request().getParam("userId"), v));
		JsonObject requestBody = ctx.getBodyAsJson();

		if (user == null) {
			ErrorUtils.respondNotFound(ctx, "User", ctx.request().getParam("userId"));
			return;
		}

		if (user.getPassword() == null) {
			ErrorUtils.respondInvalidInput(ctx, "User provided does not have password set.");
			return;
		}

		if (!requestBody.containsKey("currentPassword")) {
			ErrorUtils.respondRequiredInput(ctx, "currentPassword");
			return;
		}

		if (!user.checkPassword(requestBody.getString("currentPassword"))) {
			ErrorUtils.respondInvalidInput(ctx, "Could not authorize password change.");
			return;
		}

		RequiresTotpResult requiresTotp = SyncUtils.runBlocking(v -> user.requiresTotpAuthorization(null, v));

		if (requiresTotp == RequiresTotpResult.REQUIRED_NO_EXEMPTIONS) {
			int code = requestBody.getInteger("totpCode", -1);
			TotpAuthorizationResult totpAuthorizationResult = SyncUtils.runBlocking(v -> user.checkTotpAuthorization(code, null, v));

			if (!totpAuthorizationResult.isAuthorized()) {
				ErrorUtils.respondInvalidInput(ctx, "Totp authorization failed: " + totpAuthorizationResult.name());
				return;
			}
		}

		String newPassword = requestBody.getString("newPassword");

		if (PasswordUtils.isTooShort(newPassword)) {
			ErrorUtils.respondOther(ctx, 409, "Your password is too short.", "passwordTooShort", ImmutableMap.of());
			return;
		}

		if (PasswordUtils.isTooSimple(newPassword)) {
			ErrorUtils.respondOther(ctx, 409, "Your password is too simple.", "passwordTooSimple", ImmutableMap.of());
			return;
		}

		user.updatePassword(newPassword);
		SyncUtils.<Void>runBlocking(v -> user.save(v));
		SyncUtils.<Void>runBlocking(v -> userSessionService.invalidateAllSessions(user.getId(), v));
		String userIp = requestBody.getString("userIp");

		if (!IpUtils.isValidIp(userIp)) {
			ErrorUtils.respondInvalidInput(ctx, "Ip address \"" + userIp + "\" is not valid.");
			return;
		}

		AuditLog.log(user.getId(), userIp, ctx, AuditLogActionType.USER_CHANGE_PASSWORD, (ignored, error) -> {
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
