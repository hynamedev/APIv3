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
public final class DELETENotificationTemplatesId implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		NotificationTemplate notificationTemplate = SyncUtils.runBlocking(v -> NotificationTemplate.findById(ctx.request().getParam("notificationTemplateId"), v));

		if (notificationTemplate == null) {
			ErrorUtils.respondNotFound(ctx, "Notification template", ctx.request().getParam("notificationTemplateId"));
			return;
		}

		SyncUtils.<Void>runBlocking(v -> notificationTemplate.delete(v));

		JsonObject requestBody = ctx.getBodyAsJson();

		if (requestBody.containsKey("removedBy")) {
			AuditLog.log(UuidUtils.parseUuid(requestBody.getString("removedBy")), requestBody.getString("removedByIp"), ctx, AuditLogActionType.NOTIFICATION_TEMPLATE_DELETE, ImmutableMap.of("notificationTemplateId", notificationTemplate.getId()), (ignored, error) -> {
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