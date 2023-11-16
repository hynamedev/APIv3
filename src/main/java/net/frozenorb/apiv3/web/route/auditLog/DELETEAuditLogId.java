package net.frozenorb.apiv3.web.route.auditLog;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.domain.AuditLogEntry;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.SyncUtils;
import net.frozenorb.apiv3.util.UuidUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class DELETEAuditLogId implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		AuditLogEntry auditLogEntry = SyncUtils.runBlocking(v -> AuditLogEntry.findById(ctx.request().getParam("auditLogEntryId"), v));

		if (auditLogEntry == null) {
			ErrorUtils.respondNotFound(ctx, "Audit log entry", ctx.request().getParam("auditLogEntryId"));
			return;
		}

		if (!auditLogEntry.isReversible()) {
			ErrorUtils.respondInvalidInput(ctx, "Audit log entry referenced is not reversible.");
			return;
		}

		SyncUtils.<Void>runBlocking(v -> auditLogEntry.getType().reverse(auditLogEntry, v));

		JsonObject requestBody = ctx.getBodyAsJson();

		if (requestBody.containsKey("revertedBy")) {
			AuditLog.log(UuidUtils.parseUuid(requestBody.getString("revertedBy")), requestBody.getString("revertedByIp"), ctx, AuditLogActionType.AUDIT_LOG_REVERT, ImmutableMap.of("auditLogEntryId", auditLogEntry.getId()), (ignored, error) -> {
				if (error != null) {
					ErrorUtils.respondInternalError(ctx, error);
				} else {
					APIv3.respondJson(ctx, 200, auditLogEntry);
				}
			});
		} else {
			APIv3.respondJson(ctx, 200, auditLogEntry);
		}
	}

}