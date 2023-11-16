package net.frozenorb.apiv3.web.route.punishments;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.domain.AuditLogEntry;
import net.frozenorb.apiv3.domain.Punishment;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.unsorted.Permissions;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class DELETEUsersIdActivePunishment implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		User target = SyncUtils.runBlocking(v -> User.findById(ctx.request().getParam("userId"), v));

		if (target == null) {
			ErrorUtils.respondNotFound(ctx, "User", ctx.request().getParam("userId"));
			return;
		}

		JsonObject requestBody = ctx.getBodyAsJson();
		Punishment.PunishmentType type = Punishment.PunishmentType.valueOf(requestBody.getString("type", "").toUpperCase());
		// We purposely don't do a null check, punishment removals don't have to have a user/ip.
		User removedBy = SyncUtils.runBlocking(v -> User.findById(requestBody.getString("removedBy"), v));
		String reason = requestBody.getString("reason");

		if (reason == null || reason.trim().isEmpty()) {
			ErrorUtils.respondRequiredInput(ctx, "reason");
			return;
		}

		if (removedBy != null) {
			boolean allowed = SyncUtils.runBlocking(v -> removedBy.hasPermissionAnywhere(Permissions.REMOVE_PUNISHMENT + "." + type.name().toLowerCase(), v));

			if (!allowed) {
				ErrorUtils.respondOther(ctx, 409, "User given does not have permission to remove this punishment.", "userDoesNotHavePermission", ImmutableMap.of());
				return;
			}
		}

		List<Punishment> punishments = SyncUtils.runBlocking(v -> Punishment.findByUserAndType(target, ImmutableSet.of(type), v));
		List<Punishment> removedPunishments = new LinkedList<>();

		for (Punishment punishment : punishments) {
			if (!punishment.isActive()) continue;

			SyncUtils.<Void>runBlocking(v -> punishment.delete(removedBy, reason, v));
			SyncUtils.<AuditLogEntry>runBlocking(v -> AuditLog.log(removedBy == null ? null : removedBy.getId(), requestBody.getString("removedByIp"), ctx, AuditLogActionType.PUNISHMENT_DELETE, ImmutableMap.of("punishmentId", punishment.getId()), v));
			removedPunishments.add(punishment);
		}

		if (!removedPunishments.isEmpty()) {
			APIv3.respondJson(ctx, 200, removedPunishments);
		} else {
			ErrorUtils.respondOther(ctx, 409, "User provided has no active punishments.", "noActivePunishments", ImmutableMap.of());
		}
	}

}