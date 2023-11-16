package net.frozenorb.apiv3.web.route.notificationTemplates;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.domain.NotificationTemplate;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.SyncUtils;
import net.frozenorb.apiv3.util.UuidUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class POSTNotificationTemplates implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		JsonObject requestBody = ctx.getBodyAsJson();
		String id = requestBody.getString("id");
		String subject = requestBody.getString("subject");
		String body = requestBody.getString("body");

		NotificationTemplate notificationTemplate = new NotificationTemplate(id, subject, body);
		SyncUtils.<Void>runBlocking(v -> notificationTemplate.insert(v));

		if (requestBody.containsKey("addedBy")) {
			AuditLog.log(UuidUtils.parseUuid(requestBody.getString("addedBy")), requestBody.getString("addedByIp"), ctx, AuditLogActionType.NOTIFICATION_TEMPLATE_CREATE, ImmutableMap.of("notificationTemplateId", id), (ignored, error) -> {
				if (error != null) {
					ErrorUtils.respondInternalError(ctx, error);
				} else {
					APIv3.respondJson(ctx, 200, notificationTemplate);
				}
			});
		} else {
			APIv3.respondJson(ctx, 200, notificationTemplate);
		}
	}

}