package net.frozenorb.apiv3.web.route.punishments;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.domain.Punishment;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.unsorted.Permissions;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class DELETEPunishmentsId implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		Punishment punishment = SyncUtils.runBlocking(v -> Punishment.findById(ctx.request().getParam("punishmentId"), v));

		if (punishment == null) {
			ErrorUtils.respondNotFound(ctx, "Punishment", ctx.request().getParam("punishmentId"));
			return;
		} else if (!punishment.isActive()) {
			ErrorUtils.respondInvalidInput(ctx, "Cannot remove an inactive punishment.");
			return;
		}

		JsonObject requestBody = ctx.getBodyAsJson();
		// We purposely don't do a null check, punishment removals don't have to have a user/ip.
		User removedBy = SyncUtils.runBlocking(v -> User.findById(requestBody.getString("removedBy"), v));
		String reason = requestBody.getString("reason");

		if (reason == null || reason.trim().isEmpty()) {
			ErrorUtils.respondRequiredInput(ctx, "reason");
			return;
		}

		if (removedBy != null) {
			boolean allowed = SyncUtils.runBlocking(v -> removedBy.hasPermissionAnywhere(Permissions.REMOVE_PUNISHMENT + "." + punishment.getType().name().toLowerCase(), v));

			if (!allowed) {
				ErrorUtils.respondOther(ctx, 409, "User given does not have permission to remove this punishment.", "userDoesNotHavePermission", ImmutableMap.of());
				return;
			}
		}

		SyncUtils.<Void>runBlocking(v -> punishment.delete(removedBy, reason, v));

		if (removedBy != null) {
			AuditLog.log(removedBy.getId(), requestBody.getString("removedByIp"), ctx, AuditLogActionType.PUNISHMENT_DELETE, ImmutableMap.of("punishmentId", punishment.getId()), (ignored, error) -> {
				if (error != null) {
					ErrorUtils.respondInternalError(ctx, error);
				} else {
					APIv3.respondJson(ctx, 200, punishment);
				}
			});
		} else {
			APIv3.respondJson(ctx, 200, punishment);
		}
	}

}