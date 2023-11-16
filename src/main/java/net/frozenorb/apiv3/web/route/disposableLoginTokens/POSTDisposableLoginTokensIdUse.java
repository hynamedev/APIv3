package net.frozenorb.apiv3.web.route.disposableLoginTokens;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.service.disposablelogintoken.DisposableLoginTokenService;
import net.frozenorb.apiv3.service.usersession.UserSessionService;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.IpUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class POSTDisposableLoginTokensIdUse implements Handler<RoutingContext> {

	@Autowired private UserSessionService userSessionService;
	@Autowired private DisposableLoginTokenService disposableLoginTokenService;

	public void handle(RoutingContext ctx) {
		JsonObject requestBody = ctx.getBodyAsJson();
		String disposableLoginToken = ctx.request().getParam("disposableLoginToken");
		String userIp = requestBody.getString("userip");

		if (disposableLoginToken == null || disposableLoginToken.isEmpty()) {
			ErrorUtils.respondRequiredInput(ctx, "disposableLoginToken");
			return;
		}

		if (!IpUtils.isValidIp(userIp)) {
			ErrorUtils.respondInvalidInput(ctx, "Ip address \"" + userIp + "\" is not valid.");
			return;
		}

		disposableLoginTokenService.attemptLogin(disposableLoginToken, userIp, (user, error) -> {
			if (error != null) {
				ErrorUtils.respondInternalError(ctx, error);
				return;
			}

			if (user == null) {
				ErrorUtils.respondOther(ctx, 409, "Disposable login token provided is not valid", "disposableLoginTokenNotValid", ImmutableMap.of());
				return;
			}

			String session = SyncUtils.runBlocking(v -> userSessionService.createSession(user.getId(), userIp, v));

			AuditLog.log(user.getId(), userIp, ctx, AuditLogActionType.DISPOSABLE_LOGIN_TOKEN_USE, (ignored, error2) -> {
				if (error2 != null) {
					ErrorUtils.respondInternalError(ctx, error2);
					return;
				}

				APIv3.respondJson(ctx, 200, ImmutableMap.of(
						"authorized", true,
						"uuid", user.getId(),
						"session", session)
				);
			});
		});
	}

}