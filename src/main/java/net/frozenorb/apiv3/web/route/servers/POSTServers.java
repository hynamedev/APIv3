package net.frozenorb.apiv3.web.route.servers;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.domain.AccessToken;
import net.frozenorb.apiv3.domain.Server;
import net.frozenorb.apiv3.domain.ServerGroup;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.IpUtils;
import net.frozenorb.apiv3.util.SyncUtils;
import net.frozenorb.apiv3.util.UuidUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class POSTServers implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		JsonObject requestBody = ctx.getBodyAsJson();
		String id = requestBody.getString("id");
		String displayName = requestBody.getString("displayName");
		ServerGroup group = ServerGroup.findById(requestBody.getString("group"));
		String ip = requestBody.getString("ip");

		if (group == null) {
			ErrorUtils.respondNotFound(ctx, "Server group", requestBody.getString("group"));
			return;
		}

		String ipHost = ip.split(":")[0];

		if (!IpUtils.isValidIp(ipHost)) {
			ErrorUtils.respondInvalidInput(ctx, "Ip address \"" + ip + "\" is not valid.");
			return;
		}

		Server server = new Server(id, displayName, group, ip);
		SyncUtils.<Void>runBlocking(v -> server.insert(v));

		AccessToken accessToken = new AccessToken(server);
		SyncUtils.<Void>runBlocking(v -> accessToken.insert(v));

		if (requestBody.containsKey("addedBy")) {
			AuditLog.log(UuidUtils.parseUuid(requestBody.getString("addedBy")), requestBody.getString("addedByIp"), ctx, AuditLogActionType.SERVER_CREATE, ImmutableMap.of("serverId", id), (ignored, error) -> {
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