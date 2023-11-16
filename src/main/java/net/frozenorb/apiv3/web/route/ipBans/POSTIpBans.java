package net.frozenorb.apiv3.web.route.ipBans;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.domain.IpBan;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.IpUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.springframework.stereotype.Component;

import java.time.Instant;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class POSTIpBans implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		JsonObject requestBody = ctx.getBodyAsJson();
		String userIp = requestBody.getString("userIp");

		if (!IpUtils.isValidIp(userIp)) {
			ErrorUtils.respondInvalidInput(ctx, "Ip address \"" + userIp + "\" is not valid.");
			return;
		}

		String reason = requestBody.getString("reason");

		if (reason == null || reason.trim().isEmpty()) {
			ErrorUtils.respondRequiredInput(ctx, "reason");
			return;
		}

		Instant expiresAt = null;

		if (requestBody.containsKey("expiresIn") && requestBody.getLong("expiresIn") != -1) {
			long expiresInMillis = requestBody.getLong("expiresIn") * 1000;
			expiresAt = Instant.ofEpochMilli(System.currentTimeMillis() + expiresInMillis);
		}

		if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
			ErrorUtils.respondInvalidInput(ctx, "Expiration time cannot be in the past.");
			return;
		}

		// We purposely don't do a null check, ip bans don't have to have a source.
		User addedBy = SyncUtils.runBlocking(v -> User.findById(requestBody.getString("addedBy"), v));

		IpBan ipBan = new IpBan(userIp, reason, expiresAt, addedBy, ctx.get("actor"));
		SyncUtils.<Void>runBlocking(v -> ipBan.insert(v));

		if (addedBy != null) {
			AuditLog.log(addedBy.getId(), requestBody.getString("addedByIp"), ctx, AuditLogActionType.IP_BAN_CREATE, ImmutableMap.of("ipBanId", ipBan.getId()), (ignored, error) -> {
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