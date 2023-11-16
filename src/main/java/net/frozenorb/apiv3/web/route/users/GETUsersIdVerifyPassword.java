package net.frozenorb.apiv3.web.route.users;

import com.mongodb.async.SingleResultCallback;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.service.usersession.UserSessionService;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.IpUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class GETUsersIdVerifyPassword implements Handler<RoutingContext> {

	@Autowired private UserSessionService userSessionService;

	public void handle(RoutingContext ctx) {
		User user = SyncUtils.runBlocking(v -> User.findById(ctx.request().getParam("userId"), v));

		if (user == null) {
			user = SyncUtils.runBlocking(v -> User.findByLastUsernameLower(ctx.request().getParam("userId"), v));
		}

		if (user == null) {
			user = SyncUtils.runBlocking(v -> User.findByConfirmedEmail(ctx.request().getParam("userId"), v));
		}

		if (user == null) {
			ErrorUtils.respondNotFound(ctx, "User", ctx.request().getParam("userId"));
			return;
		}

		if (user.getPassword() == null) {
			ErrorUtils.respondInvalidInput(ctx, "User provided does not have password set.");
			return;
		}

		final UUID finalUuid = user.getId();
		boolean authorized = user.checkPassword(ctx.request().getParam("password"));
		String userIp = ctx.request().getParam("userIp");

		if (!IpUtils.isValidIp(userIp)) {
			ErrorUtils.respondInvalidInput(ctx, "IP address \"" + userIp + "\" is not valid.");
			return;
		}

		AuditLog.log(user.getId(), userIp, ctx, authorized ? AuditLogActionType.USER_LOGIN_SUCCESS : AuditLogActionType.USER_LOGIN_FAIL, (ignored, error) -> {
			if (error != null) {
				ErrorUtils.respondInternalError(ctx, error);
				return;
			}

			SingleResultCallback<String> responseCallback = (session, sessionError) -> {
				if (sessionError != null) {
					ErrorUtils.respondInternalError(ctx, sessionError);
					return;
				}

				Map<String, Object> result = new HashMap<>();

				result.put("authorized", authorized);
				result.put("uuid", finalUuid);

				if (authorized) {
					result.put("session", session);
				}

				APIv3.respondJson(ctx, 200, result);
			};

			if (authorized) {
				userSessionService.createSession(finalUuid, userIp, responseCallback);
			} else {
				responseCallback.onResult(null, null);
			}
		});
	}

}
