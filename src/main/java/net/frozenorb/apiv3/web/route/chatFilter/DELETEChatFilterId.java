package net.frozenorb.apiv3.web.route.chatFilter;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.domain.ChatFilterEntry;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.SyncUtils;
import net.frozenorb.apiv3.util.UuidUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class DELETEChatFilterId implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		ChatFilterEntry chatFilterEntry = SyncUtils.runBlocking(v -> ChatFilterEntry.findById(ctx.request().getParam("chatFilterEntryId"), v));

		if (chatFilterEntry == null) {
			ErrorUtils.respondNotFound(ctx, "Chat filter entry", ctx.request().getParam("chatFilterEntryId"));
			return;
		}

		SyncUtils.<Void>runBlocking(v -> chatFilterEntry.delete(v));

		JsonObject requestBody = ctx.getBodyAsJson();

		if (requestBody.containsKey("removedBy")) {
			AuditLog.log(UuidUtils.parseUuid(requestBody.getString("removedBy")), requestBody.getString("removedByIp"), ctx, AuditLogActionType.CHAT_FILTER_ENTRY_DELETE, ImmutableMap.of("chatFilterEntryId", chatFilterEntry.getId()), (ignored, error) -> {
				if (error != null) {
					ErrorUtils.respondInternalError(ctx, error);
				} else {
					APIv3.respondJson(ctx, 200, chatFilterEntry);
				}
			});
		} else {
			APIv3.respondJson(ctx, 200, chatFilterEntry);
		}
	}

}