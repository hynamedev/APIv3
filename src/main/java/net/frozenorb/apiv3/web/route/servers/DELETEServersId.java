package net.frozenorb.apiv3.web.route.servers;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.unsorted.actor.ActorType;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.domain.AccessToken;
import net.frozenorb.apiv3.domain.Server;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.SyncUtils;
import net.frozenorb.apiv3.util.UuidUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class DELETEServersId implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		Server server = Server.findById(ctx.request().getParam("serverId"));

		if (server == null) {
			ErrorUtils.respondNotFound(ctx, "Server", ctx.request().getParam("serverId"));
			return;
		}

		SyncUtils.<Void>runBlocking(v -> server.insert(v));

		SyncUtils.runBlocking(v -> {
			AccessToken.findByNameAndType(server.getId(), ActorType.SERVER, (accessToken, error) -> {
				if (error != null) {
					v.onResult(null, error);
				} else if (accessToken != null) {
					accessToken.delete((ignored, error2) -> {
						if (error2 != null) {
							v.onResult(null, error2);
						} else {
							v.onResult(null, null);
						}
					});
				} else {
					v.onResult(null, new NullPointerException("Access token not found."));
				}
			});
		});

		JsonObject requestBody = ctx.getBodyAsJson();

		if (requestBody.containsKey("removedBy")) {
			AuditLog.log(UuidUtils.parseUuid(requestBody.getString("removedBy")), requestBody.getString("removedByIp"), ctx, AuditLogActionType.SERVER_DELETE, ImmutableMap.of("serverId", server.getId()), (ignored, error) -> {
				if (error != null) {
					ErrorUtils.respondInternalError(ctx, error);
				} else {
					APIv3.respondJson(ctx, 200, server);
				}
			});
		} else {
			APIv3.respondJson(ctx, 200, server);
		}
	}

}