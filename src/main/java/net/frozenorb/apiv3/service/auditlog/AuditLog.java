package net.frozenorb.apiv3.service.auditlog;

import com.google.common.collect.ImmutableMap;

import com.mongodb.async.SingleResultCallback;

import net.frozenorb.apiv3.domain.AuditLogEntry;

import java.util.Map;
import java.util.UUID;

import io.vertx.ext.web.RoutingContext;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AuditLog {

	public static void log(UUID performedBy, String performedByIp, RoutingContext ctx, AuditLogActionType actionType, SingleResultCallback<AuditLogEntry> callback) {
		log(performedBy, performedByIp, ctx, actionType, ImmutableMap.of(), callback);
	}

	public static void log(UUID performedBy, String performedByIp, RoutingContext ctx, AuditLogActionType actionType, Map<String, Object> actionData, SingleResultCallback<AuditLogEntry> callback) {
		AuditLogEntry entry = new AuditLogEntry(performedBy, performedByIp, ctx.get("actor"), ctx.request().remoteAddress().host(), actionType, actionData);
		entry.insert((ignored, error) -> {
			if (error != null) {
				callback.onResult(null, error);
			} else {
				callback.onResult(entry, null);
			}
		});
	}

}