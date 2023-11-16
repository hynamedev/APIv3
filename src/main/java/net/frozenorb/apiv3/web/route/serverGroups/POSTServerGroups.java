package net.frozenorb.apiv3.web.route.serverGroups;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.domain.ServerGroup;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.SyncUtils;
import net.frozenorb.apiv3.util.UuidUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class POSTServerGroups implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		JsonObject requestBody = ctx.getBodyAsJson();
		String id = requestBody.getString("id");
		String image = requestBody.getString("image");

		ServerGroup serverGroup = new ServerGroup(id, image);
		SyncUtils.<Void>runBlocking(v -> serverGroup.insert(v));

		if (requestBody.containsKey("addedBy")) {
			AuditLog.log(UuidUtils.parseUuid(requestBody.getString("addedBy")), requestBody.getString("addedByIp"), ctx, AuditLogActionType.SERVER_GROUP_CREATE, ImmutableMap.of("serverGroupId", id), (ignored, error) -> {
				if (error != null) {
					ErrorUtils.respondInternalError(ctx, error);
				} else {
					APIv3.respondJson(ctx, 200, serverGroup);
				}
			});
		} else {
			APIv3.respondJson(ctx, 200, serverGroup);
		}
	}

}