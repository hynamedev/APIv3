package net.frozenorb.apiv3.web.route.auditLog;

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
public final class POSTAuditLog implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		JsonObject requestBody = ctx.getBodyAsJson();

		User.findById(requestBody.getString("user"), (user, error) -> {
			if (error != null) {
				ErrorUtils.respondInternalError(ctx, error);
				return;
			}

			if (user == null) {
				ErrorUtils.respondNotFound(ctx, "User", requestBody.getString("user"));
				return;
			}

			String userIp = requestBody.getString("userIp");

			if (!IpUtils.isValidIp(userIp)) {
				ErrorUtils.respondInvalidInput(ctx, "Ip address \"" + userIp + "\" is not valid.");
				return;
			}

			AuditLogActionType type;

			try {
				type = AuditLogActionType.valueOf(requestBody.getString("type", "").toUpperCase());
			} catch (IllegalArgumentException ignored) {
				ErrorUtils.respondNotFound(ctx, "Audit log action type", requestBody.getString("type"));
				return;
			}

			AuditLog.log(user.getId(), userIp, ctx, type, requestBody.getJsonObject("metadata").getMap(), (auditLogEntry, error2) -> {
				if (error2 != null) {
					ErrorUtils.respondInternalError(ctx, error2);
				} else {
					APIv3.respondJson(ctx, 200, auditLogEntry);
				}
			});
		});
	}

}