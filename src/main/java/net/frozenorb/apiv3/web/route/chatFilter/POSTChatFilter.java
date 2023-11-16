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
public final class POSTChatFilter implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		JsonObject requestBody = ctx.getBodyAsJson();
		String id = requestBody.getString("id");
		String regex = requestBody.getString("regex");

		ChatFilterEntry chatFilterEntry = new ChatFilterEntry(id, regex);
		SyncUtils.<Void>runBlocking(v -> chatFilterEntry.insert(v));

		if (requestBody.containsKey("addedBy")) {
			AuditLog.log(UuidUtils.parseUuid(requestBody.getString("addedBy")), requestBody.getString("addedByIp"), ctx, AuditLogActionType.CHAT_FILTER_ENTRY_CREATE, ImmutableMap.of("chatFilterEntryId", id), (ignored, error) -> {
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