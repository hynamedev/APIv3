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
public final class DELETEServerGroupsId implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		ServerGroup serverGroup = ServerGroup.findById(ctx.request().getParam("serverGroupId"));

		if (serverGroup == null) {
			ErrorUtils.respondNotFound(ctx, "Server group", ctx.request().getParam("serverGroupId"));
			return;
		}

		SyncUtils.<Void>runBlocking(v -> serverGroup.delete(v));

		JsonObject requestBody = ctx.getBodyAsJson();

		if (requestBody.containsKey("removedBy")) {
			AuditLog.log(UuidUtils.parseUuid(requestBody.getString("emovedBy")), requestBody.getString("emovedByIp"), ctx, AuditLogActionType.SERVER_GROUP_DELETE, ImmutableMap.of("serverGroupId", serverGroup.getId()), (ignored, error) -> {
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