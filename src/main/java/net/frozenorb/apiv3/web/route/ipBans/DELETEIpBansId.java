package net.frozenorb.apiv3.web.route.ipBans;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.domain.IpBan;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class DELETEIpBansId implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		IpBan ipBan = SyncUtils.runBlocking(v -> IpBan.findById(ctx.request().getParam("ipBanId"), v));

		if (ipBan == null) {
			ErrorUtils.respondNotFound(ctx, "IpBan", ctx.request().getParam("ipBanId"));
			return;
		} else if (!ipBan.isActive()) {
			ErrorUtils.respondInvalidInput(ctx, "Cannot remove an inactive ip ban.");
			return;
		}

		JsonObject requestBody = ctx.getBodyAsJson();
		// We purposely don't do a null check, ip ban removals don't have to have a user/ip.
		User removedBy = SyncUtils.runBlocking(v -> User.findById(requestBody.getString("removedBy"), v));
		String reason = requestBody.getString("reason");

		if (reason == null || reason.trim().isEmpty()) {
			ErrorUtils.respondRequiredInput(ctx, "reason");
			return;
		}

		SyncUtils.<Void>runBlocking(v -> ipBan.delete(removedBy, reason, v));

		if (removedBy != null) {
			AuditLog.log(removedBy.getId(), requestBody.getString("removedByIp"), ctx, AuditLogActionType.IP_BAN_DELETE, ImmutableMap.of("punishmentId", ipBan.getId()), (ignored, error) -> {
				if (error != null) {
					ErrorUtils.respondInternalError(ctx, error);
				} else {
					APIv3.respondJson(ctx, 200, ipBan);
				}
			});
		} else {
			APIv3.respondJson(ctx, 200, ipBan);
		}
	}

}