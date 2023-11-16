package net.frozenorb.apiv3.web.route.prefixGrants;

import com.google.common.collect.ImmutableMap;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.Grant;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.unsorted.Permissions;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.SyncUtils;
import org.springframework.stereotype.Component;

@Component
public final class DELETEPrefixGrantsId implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		Grant grant = SyncUtils.runBlocking(v -> Grant.findById(ctx.request().getParam("grantId"), v));

		if (grant == null) {
			ErrorUtils.respondNotFound(ctx, "Grant", ctx.request().getParam("grantId"));
			return;
		} else if (!grant.isActive()) {
			ErrorUtils.respondInvalidInput(ctx, "Cannot remove an inactive grant.");
			return;
		}

		JsonObject requestBody = ctx.getBodyAsJson();
		// We purposely don't do a null check, grant removals don't have to have a user/ip.
		User removedBy = SyncUtils.runBlocking(v -> User.findById(requestBody.getString("removedBy"), v));
		String reason = requestBody.getString("reason");

		if (reason == null || reason.trim().isEmpty()) {
			ErrorUtils.respondRequiredInput(ctx, "reason");
			return;
		}

		if (removedBy != null) {
			boolean allowed = SyncUtils.runBlocking(v -> removedBy.hasPermissionAnywhere(Permissions.REMOVE_PREFIXGRANT + "." + grant.getRank(), v));

			if (!allowed) {
				ErrorUtils.respondOther(ctx, 409, "User given does not have permission to remove this grant.", "userDoesNotHavePermission", ImmutableMap.of());
				return;
			}
		}

		SyncUtils.<Void>runBlocking(v -> grant.delete(removedBy, reason, v));

		if (removedBy != null) {
			AuditLog.log(removedBy.getId(), requestBody.getString("removedByIp"), ctx, AuditLogActionType.PREFIXGRANT_DELETE, ImmutableMap.of("grantId", grant.getId()), (ignored, error) -> {
				if (error != null) {
					ErrorUtils.respondInternalError(ctx, error);
				} else {
					APIv3.respondJson(ctx, 200, grant);
				}
			});
		} else {
			APIv3.respondJson(ctx, 200, grant);
		}
	}

}